package com.fluttiris.admincontrol.document.application;

import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stockage sur disque local des fichiers déposés (pas de S3/objet — cohérent
 * avec l'absence d'infra cloud/Docker sur cet environnement). chemin_stockage
 * en base n'est jamais qu'un nom de fichier généré ici (UUID + extension),
 * jamais le nom original ni un chemin fourni par le client — élimine tout
 * risque de path traversal ou de collision.
 */
@Service
public class DocumentStorageService {

    private final Path storageDir;
    private final long tailleMaxOctets;
    private final Set<String> extensionsAutorisees;

    public DocumentStorageService(
        @Value("${app.documents.storage-dir}") String storageDir,
        @Value("${app.documents.taille-max-mo}") long tailleMaxMo,
        @Value("${app.documents.extensions-autorisees}") String extensionsAutorisees) {
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
        this.tailleMaxOctets = tailleMaxMo * 1024 * 1024;
        this.extensionsAutorisees = Arrays.stream(extensionsAutorisees.split(","))
            .map(e -> e.trim().toLowerCase()).collect(Collectors.toUnmodifiableSet());
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le répertoire de stockage des documents (" + this.storageDir + ")", e);
        }
    }

    public record StoredFile(String cheminStockage, String nomFichierOriginal, String typeMime, long tailleOctets) {
    }

    public StoredFile stocker(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessRuleViolationException("Aucun fichier fourni");
        }
        if (fichier.getSize() > tailleMaxOctets) {
            throw new BusinessRuleViolationException(
                "Le fichier dépasse la taille maximale autorisée (" + (tailleMaxOctets / 1024 / 1024) + " Mo)");
        }
        String nomOriginal = fichier.getOriginalFilename() != null && !fichier.getOriginalFilename().isBlank()
            ? fichier.getOriginalFilename() : "fichier";
        String extension = extension(nomOriginal);
        if (extension.isEmpty() || !extensionsAutorisees.contains(extension)) {
            throw new BusinessRuleViolationException(
                "Type de fichier non autorisé (extensions acceptées : " + String.join(", ", extensionsAutorisees) + ")");
        }
        String nomStockage = UUID.randomUUID() + "." + extension;
        Path cible = storageDir.resolve(nomStockage);
        try {
            fichier.transferTo(cible);
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de l'enregistrement du fichier", e);
        }
        String typeMime = fichier.getContentType() != null ? fichier.getContentType() : "application/octet-stream";
        return new StoredFile(nomStockage, nomOriginal, typeMime, fichier.getSize());
    }

    public Resource charger(String cheminStockage) {
        Path chemin = storageDir.resolve(cheminStockage).normalize();
        if (!chemin.startsWith(storageDir) || !Files.exists(chemin)) {
            throw new EntityNotFoundException("Fichier introuvable sur le serveur");
        }
        return new FileSystemResource(chemin);
    }

    private String extension(String nomFichier) {
        int point = nomFichier.lastIndexOf('.');
        return point >= 0 && point < nomFichier.length() - 1 ? nomFichier.substring(point + 1).toLowerCase() : "";
    }
}

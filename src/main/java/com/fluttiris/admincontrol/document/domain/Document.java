package com.fluttiris.admincontrol.document.domain;

import com.fluttiris.admincontrol.common.audit.Auditable;
import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "document")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends Auditable {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "type_document_id", nullable = false)
    private UUID typeDocumentId;

    @Column(name = "salarie_id")
    private UUID salarieId;

    @Column(name = "entreprise_id")
    private UUID entrepriseId;

    @Column(name = "chantier_id")
    private UUID chantierId;

    @Column(name = "fichier_url")
    private String fichierUrl;

    /** Nom du fichier tel que déposé par l'utilisateur (affiché à l'écran, jamais utilisé comme nom sur disque). */
    @Column(name = "nom_fichier_original")
    private String nomFichierOriginal;

    @Column(name = "type_mime")
    private String typeMime;

    @Column(name = "taille_octets")
    private Long tailleOctets;

    /** Clé de stockage interne (nom de fichier généré, voir DocumentStorageService) — jamais exposée telle quelle au frontend. */
    @Column(name = "chemin_stockage")
    private String cheminStockage;

    @Column(name = "date_debut_validite")
    private LocalDate dateDebutValidite;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Column(name = "date_relance")
    private LocalDate dateRelance;

    private String mentions;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validation", nullable = false)
    private StatutValidation statutValidation = StatutValidation.EN_ATTENTE;

    /** Motif choisi lors d'un refus (voir DocumentEtat) — null tant que le document n'a jamais été refusé. */
    @Column(name = "document_etat_id")
    private UUID documentEtatId;

    /**
     * @param nomFichierOriginal, typeMime, tailleOctets, cheminStockage infos du fichier réellement déposé
     *                            (tous nullable — un document peut exister sans fichier, ex. une ligne créée
     *                            en attente avant tout dépôt).
     */
    public static Document creer(UUID typeDocumentId, UUID salarieId, UUID entrepriseId, UUID chantierId,
                                  String fichierUrl, String nomFichierOriginal, String typeMime, Long tailleOctets,
                                  String cheminStockage, LocalDate dateDebutValidite, LocalDate dateExpiration,
                                  LocalDate dateRelance, String mentions) {
        if ((salarieId == null) == (entrepriseId == null)) {
            throw new BusinessRuleViolationException("Un document doit cibler exactement un salarié OU une entreprise");
        }
        Document document = new Document();
        document.typeDocumentId = typeDocumentId;
        document.salarieId = salarieId;
        document.entrepriseId = entrepriseId;
        document.chantierId = chantierId;
        document.fichierUrl = fichierUrl;
        document.nomFichierOriginal = nomFichierOriginal;
        document.typeMime = typeMime;
        document.tailleOctets = tailleOctets;
        document.cheminStockage = cheminStockage;
        document.dateDebutValidite = dateDebutValidite;
        document.dateExpiration = dateExpiration;
        document.dateRelance = dateRelance;
        document.mentions = mentions;
        return document;
    }

    /**
     * Les dates de validité sont saisies ici, par l'administrateur qui valide — jamais par
     * l'entreprise au dépôt (voir {@link #creer}) — pour attester que le contrôle manuel du
     * document (et non simplement son dépôt) a bien été réalisé par l'administration.
     */
    public void valider(LocalDate dateDebutValidite, LocalDate dateExpiration) {
        this.statutValidation = StatutValidation.VALIDE;
        this.documentEtatId = null;
        this.dateDebutValidite = dateDebutValidite;
        this.dateExpiration = dateExpiration;
    }

    public void refuser(UUID documentEtatId) {
        this.statutValidation = StatutValidation.REFUSE;
        this.documentEtatId = documentEtatId;
    }
}

package com.fluttiris.admincontrol.messagerie.application.champs;

import com.fluttiris.admincontrol.document.domain.DocumentRepository;
import com.fluttiris.admincontrol.document.domain.TypeDocument;
import com.fluttiris.admincontrol.document.domain.TypeDocumentRepository;
import com.fluttiris.admincontrol.messagerie.domain.ChampSurveillable;
import com.fluttiris.admincontrol.messagerie.domain.EvenementDetecte;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentExpirationChamp implements ChampSurveillable {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DocumentRepository documentRepository;
    private final TypeDocumentRepository typeDocumentRepository;

    @Override
    public String getId() {
        return "DOCUMENT_DATE_EXPIRATION";
    }

    @Override
    public String getEntiteLibelle() {
        return "Document";
    }

    @Override
    public String getChampLibelle() {
        return "Date d'expiration";
    }

    @Override
    public List<EvenementDetecte> rechercher(LocalDate dateCible) {
        return documentRepository.findByDateExpiration(dateCible).stream()
            .map(document -> {
                String libelle = typeDocumentRepository.findById(document.getTypeDocumentId())
                    .map(TypeDocument::getLibelle).orElse("");
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("DATE", dateCible.format(FORMAT_DATE));
                placeholders.put("DOCUMENT_LIBELLE", libelle);
                return new EvenementDetecte(document.getId(), document.getChantierId(), placeholders);
            })
            .toList();
    }
}

package com.fluttiris.admincontrol.document.application;

import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.document.domain.CibleDocument;
import com.fluttiris.admincontrol.document.domain.FormatDocument;
import com.fluttiris.admincontrol.document.domain.TypeDocument;
import com.fluttiris.admincontrol.document.domain.TypeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TypeDocumentService {

    private final TypeDocumentRepository typeDocumentRepository;

    public TypeDocument creer(String libelle, CibleDocument cible, boolean obligatoire, FormatDocument format,
                               UUID corpsDeMetierId, UUID paysId, String zoneRequise, boolean dateDebutValiditeRequise,
                               boolean dateFinValiditeRequise, int nbJoursRelanceAvant, int nbJoursRecurrence,
                               boolean retireAccordAcces) {
        TypeDocument type = TypeDocument.creer(libelle, cible, obligatoire, format, corpsDeMetierId, paysId, zoneRequise,
            dateDebutValiditeRequise, dateFinValiditeRequise, nbJoursRelanceAvant, nbJoursRecurrence, retireAccordAcces);
        return typeDocumentRepository.save(type);
    }

    @Transactional(readOnly = true)
    public TypeDocument obtenir(UUID id) {
        return typeDocumentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Type de document", id));
    }

    public TypeDocument modifier(UUID id, String libelle, CibleDocument cible, boolean obligatoire, FormatDocument format,
                                  UUID corpsDeMetierId, UUID paysId, String zoneRequise, boolean dateDebutValiditeRequise,
                                  boolean dateFinValiditeRequise, int nbJoursRelanceAvant, int nbJoursRecurrence,
                                  boolean retireAccordAcces) {
        TypeDocument type = obtenir(id);
        type.modifier(libelle, cible, obligatoire, format, corpsDeMetierId, paysId, zoneRequise, dateDebutValiditeRequise,
            dateFinValiditeRequise, nbJoursRelanceAvant, nbJoursRecurrence, retireAccordAcces);
        return type;
    }

    @Transactional(readOnly = true)
    public List<TypeDocument> lister() {
        return typeDocumentRepository.findAll();
    }

    public void supprimer(UUID id) {
        obtenir(id).supprimer();
    }
}

package com.fluttiris.admincontrol.document.application;

import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.document.domain.DocumentEtat;
import com.fluttiris.admincontrol.document.domain.DocumentEtatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentEtatService {

    private final DocumentEtatRepository documentEtatRepository;

    public DocumentEtat creer(String titre, boolean parDefaut, boolean dateExpiree, boolean valideLeDocument) {
        DocumentEtat etat = DocumentEtat.creer(titre, parDefaut, dateExpiree, valideLeDocument);
        return documentEtatRepository.save(etat);
    }

    public DocumentEtat modifier(UUID id, String titre, boolean parDefaut, boolean dateExpiree, boolean valideLeDocument) {
        DocumentEtat etat = obtenir(id);
        etat.modifier(titre, parDefaut, dateExpiree, valideLeDocument);
        return etat;
    }

    @Transactional(readOnly = true)
    public DocumentEtat obtenir(UUID id) {
        return documentEtatRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("État de document", id));
    }

    @Transactional(readOnly = true)
    public List<DocumentEtat> lister() {
        return documentEtatRepository.findAll();
    }

    public void supprimer(UUID id) {
        obtenir(id).supprimer();
    }
}

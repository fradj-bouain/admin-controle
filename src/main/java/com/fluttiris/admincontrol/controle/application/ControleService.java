package com.fluttiris.admincontrol.controle.application;

import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.controle.domain.Controle;
import com.fluttiris.admincontrol.controle.domain.ControleRepository;
import com.fluttiris.admincontrol.controle.domain.RapportControle;
import com.fluttiris.admincontrol.controle.domain.RapportControleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ControleService {

    private final ControleRepository controleRepository;
    private final RapportControleRepository rapportControleRepository;

    public Controle creer(UUID chantierId, UUID controleurUtilisateurId, LocalDate dateControle, String remarques,
                           UUID controleTiersId, LocalDate dateFin, boolean termine) {
        Controle controle = Controle.creer(chantierId, controleurUtilisateurId, dateControle, remarques,
            controleTiersId, dateFin, termine);
        return controleRepository.save(controle);
    }

    @Transactional(readOnly = true)
    public Controle obtenir(UUID id) {
        return controleRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contrôle", id));
    }

    @Transactional(readOnly = true)
    public List<Controle> listerParChantier(UUID chantierId) {
        return controleRepository.findByChantierId(chantierId);
    }

    public void supprimer(UUID id) {
        Controle controle = obtenir(id);
        controle.supprimer();
    }

    public RapportControle genererRapport(UUID controleId, int nbSalariesControles, int nbAccords, int nbRefus,
                                           int nbNouvellesEntreprises, int nbNouveauxSalaries, int nbEntreprises,
                                           int nbSalariesDetaches, UUID responsableUtilisateurId) {
        obtenir(controleId); // valide l'existence du contrôle
        RapportControle rapport = RapportControle.creer(controleId, nbSalariesControles, nbAccords, nbRefus,
            nbNouvellesEntreprises, nbNouveauxSalaries, nbEntreprises, nbSalariesDetaches, responsableUtilisateurId);
        return rapportControleRepository.save(rapport);
    }

    @Transactional(readOnly = true)
    public List<RapportControle> listerRapports() {
        return rapportControleRepository.findAllByOrderByCreatedAtDesc();
    }

    public RapportControle envoyerRapport(UUID rapportId) {
        RapportControle rapport = rapportControleRepository.findById(rapportId)
            .orElseThrow(() -> new EntityNotFoundException("Rapport", rapportId));
        rapport.marquerEnvoye();
        return rapport;
    }
}

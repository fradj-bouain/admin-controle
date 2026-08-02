package com.fluttiris.admincontrol.common.security;

import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import com.fluttiris.admincontrol.chantier.domain.Chantier;
import com.fluttiris.admincontrol.chantier.domain.ChantierRepository;
import com.fluttiris.admincontrol.controle.domain.ControleRepository;
import com.fluttiris.admincontrol.controle.domain.RapportControleRepository;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantierRepository;
import com.fluttiris.admincontrol.salarie.domain.AffectationSalarieChantierRepository;
import com.fluttiris.admincontrol.salarie.domain.SalarieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Autorisation basée sur l'appartenance directe d'une entité au périmètre du
 * compte courant (Client sur un chantier, Contrôleur sur un contrôle, Entreprise
 * sur un salarié) — pendant de {@link com.fluttiris.admincontrol.entreprise.application.ChantierAuthorizationService},
 * qui couvre le cas contextuel (rôle Principale/STT1/STT2 par chantier).
 */
@Service("scopeAuthz")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScopeAuthorizationService {

    private final ChantierRepository chantierRepository;
    private final ControleRepository controleRepository;
    private final RapportControleRepository rapportControleRepository;
    private final SalarieRepository salarieRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AffectationSalarieChantierRepository affectationSalarieChantierRepository;
    private final AffectationEntrepriseChantierRepository affectationEntrepriseChantierRepository;

    public boolean chantierAppartientAuClient(UUID chantierId, UUID clientId) {
        return chantierRepository.findById(chantierId)
            .map(c -> clientId.equals(c.getClientId()))
            .orElse(false);
    }

    public boolean controleAppartientAOrganisme(UUID controleId, UUID controleTiersId) {
        return controleRepository.findById(controleId)
            .map(c -> controleTiersId.equals(c.getControleTiersId()))
            .orElse(false);
    }

    public boolean salarieAppartientAEntreprise(UUID salarieId, UUID entrepriseId) {
        return salarieRepository.findById(salarieId)
            .map(s -> entrepriseId.equals(s.getEntrepriseEmployeurId()))
            .orElse(false);
    }

    public boolean utilisateurAppartientAClient(UUID utilisateurId, UUID clientId) {
        return utilisateurRepository.findById(utilisateurId)
            .map(u -> clientId.equals(u.getClientId()))
            .orElse(false);
    }

    public boolean utilisateurAppartientAEntreprise(UUID utilisateurId, UUID entrepriseId) {
        return utilisateurRepository.findById(utilisateurId)
            .map(u -> entrepriseId.equals(u.getEntrepriseId()))
            .orElse(false);
    }

    /** Le salarié est affecté à au moins un chantier de ce client (via ses affectations chantier). */
    public boolean salarieAccessibleParClient(UUID salarieId, UUID clientId) {
        List<UUID> chantierIds = chantierIdsDuClient(clientId);
        if (chantierIds.isEmpty()) {
            return false;
        }
        return affectationSalarieChantierRepository.findBySalarieId(salarieId).stream()
            .anyMatch(a -> chantierIds.contains(a.getChantierId()));
    }

    /** L'entreprise a une affectation sur au moins un chantier de ce client. */
    public boolean entrepriseAccessibleParClient(UUID entrepriseId, UUID clientId) {
        List<UUID> chantierIds = chantierIdsDuClient(clientId);
        if (chantierIds.isEmpty()) {
            return false;
        }
        return affectationEntrepriseChantierRepository.findByEntrepriseId(entrepriseId).stream()
            .anyMatch(a -> chantierIds.contains(a.getChantierId()));
    }

    public boolean controleAppartientAuClient(UUID controleId, UUID clientId) {
        return controleRepository.findById(controleId)
            .map(c -> chantierRepository.findById(c.getChantierId())
                .map(chantier -> clientId.equals(chantier.getClientId()))
                .orElse(false))
            .orElse(false);
    }

    /** L'entreprise a une affectation sur le chantier de ce contrôle. */
    public boolean controleAccessibleParEntreprise(UUID controleId, UUID entrepriseId) {
        return controleRepository.findById(controleId)
            .map(c -> !affectationEntrepriseChantierRepository
                .findByChantierIdAndEntrepriseId(c.getChantierId(), entrepriseId).isEmpty())
            .orElse(false);
    }

    public boolean rapportAppartientAuClient(UUID rapportId, UUID clientId) {
        return rapportControleRepository.findById(rapportId)
            .map(r -> controleAppartientAuClient(r.getControleId(), clientId))
            .orElse(false);
    }

    public boolean rapportAccessibleParEntreprise(UUID rapportId, UUID entrepriseId) {
        return rapportControleRepository.findById(rapportId)
            .map(r -> controleAccessibleParEntreprise(r.getControleId(), entrepriseId))
            .orElse(false);
    }

    public boolean rapportAppartientAOrganisme(UUID rapportId, UUID controleTiersId) {
        return rapportControleRepository.findById(rapportId)
            .map(r -> controleAppartientAOrganisme(r.getControleId(), controleTiersId))
            .orElse(false);
    }

    private List<UUID> chantierIdsDuClient(UUID clientId) {
        return chantierRepository.findByClientId(clientId).stream().map(Chantier::getId).toList();
    }
}

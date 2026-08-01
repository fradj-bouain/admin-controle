package com.fluttiris.admincontrol.common.security;

import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import com.fluttiris.admincontrol.chantier.domain.ChantierRepository;
import com.fluttiris.admincontrol.controle.domain.ControleRepository;
import com.fluttiris.admincontrol.salarie.domain.SalarieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SalarieRepository salarieRepository;
    private final UtilisateurRepository utilisateurRepository;

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
}

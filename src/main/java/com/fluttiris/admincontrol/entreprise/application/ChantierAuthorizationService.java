package com.fluttiris.admincontrol.entreprise.application;

import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantierRepository;
import com.fluttiris.admincontrol.entreprise.domain.RoleEntreprise;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/**
 * Autorisation CONTEXTUELLE : le rôle d'une entreprise (Principale/STT1/STT2)
 * n'existe que rapporté à un chantier précis, il ne peut donc jamais être
 * porté par un claim statique du JWT (cf. {@link com.fluttiris.admincontrol.config.SecurityConfig}).
 * Ce service est le point d'entrée unique pour les vérifications @PreAuthorize
 * qui dépendent du couple (entreprise, chantier).
 *
 * Une entreprise peut avoir plusieurs affectations actives sur un même chantier
 * (ex : Principale ET STT1) : les vérifications ci-dessous se basent donc sur
 * l'ensemble de ses affectations, pas sur une seule. Pour roleSur, c'est le rôle
 * le plus élevé (Principale > STT1 > STT2, dans l'ordre de l'enum) qui prévaut.
 *
 * Exemple d'usage :
 * {@code @PreAuthorize("@chantierAuthz.canManageSousTraitants(#chantierId, principal.entrepriseId)")}
 */
@Service("chantierAuthz")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChantierAuthorizationService {

    private final AffectationEntrepriseChantierRepository affectationRepository;

    public Optional<RoleEntreprise> roleSur(UUID chantierId, UUID entrepriseId) {
        return affectationRepository.findByChantierIdAndEntrepriseId(chantierId, entrepriseId).stream()
            .filter(a -> a.estActive())
            .map(a -> a.getRole())
            .min(Comparator.comparing(RoleEntreprise::ordinal));
    }

    /** Une Principale ou un STT1 peuvent gérer les sous-traitants qu'ils ont invités sur CE chantier. */
    public boolean canManageSousTraitants(UUID chantierId, UUID entrepriseId) {
        return affectationRepository.findByChantierIdAndEntrepriseId(chantierId, entrepriseId).stream()
            .filter(a -> a.estActive())
            .anyMatch(a -> a.peutGererSousTraitants());
    }

    /** Toute entreprise activement affectée à ce chantier peut gérer ses propres salariés dessus. */
    public boolean canManageOwnSalaries(UUID chantierId, UUID entrepriseId) {
        return affectationRepository.findByChantierIdAndEntrepriseId(chantierId, entrepriseId).stream()
            .anyMatch(a -> a.estActive());
    }

    public boolean isAffectedToChantier(UUID chantierId, UUID entrepriseId) {
        return !affectationRepository.findByChantierIdAndEntrepriseId(chantierId, entrepriseId).isEmpty();
    }
}

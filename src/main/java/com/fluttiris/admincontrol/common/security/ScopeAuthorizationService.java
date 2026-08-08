package com.fluttiris.admincontrol.common.security;

import com.fluttiris.admincontrol.auth.domain.UtilisateurRepository;
import com.fluttiris.admincontrol.chantier.domain.Chantier;
import com.fluttiris.admincontrol.chantier.domain.ChantierRepository;
import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateur;
import com.fluttiris.admincontrol.chantier.domain.ChantierUtilisateurRepository;
import com.fluttiris.admincontrol.controle.domain.ControleRepository;
import com.fluttiris.admincontrol.controle.domain.RapportControleRepository;
import com.fluttiris.admincontrol.document.domain.Document;
import com.fluttiris.admincontrol.document.domain.DocumentRepository;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantier;
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
    private final ChantierUtilisateurRepository chantierUtilisateurRepository;
    private final ControleRepository controleRepository;
    private final RapportControleRepository rapportControleRepository;
    private final SalarieRepository salarieRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AffectationSalarieChantierRepository affectationSalarieChantierRepository;
    private final AffectationEntrepriseChantierRepository affectationEntrepriseChantierRepository;
    private final DocumentRepository documentRepository;

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

    /**
     * Le chantier appartient à ce client ET a été explicitement assigné à ce compte Client
     * via chantier_utilisateur. Sans assignation explicite, un compte Client n'a accès à
     * aucun chantier — l'accès est strictement opt-in, jamais accordé par défaut.
     */
    public boolean chantierAccessibleParClient(UUID chantierId, UUID clientId, UUID utilisateurId) {
        boolean appartientAuClient = chantierRepository.findById(chantierId)
            .map(c -> clientId.equals(c.getClientId()))
            .orElse(false);
        if (!appartientAuClient) {
            return false;
        }
        return chantierIdsAssignes(utilisateurId).contains(chantierId);
    }

    /** Le salarié est affecté à au moins un chantier accessible par ce compte Client. */
    public boolean salarieAccessibleParClient(UUID salarieId, UUID clientId, UUID utilisateurId) {
        List<UUID> chantierIds = chantierIdsAccessiblesPourClient(clientId, utilisateurId);
        if (chantierIds.isEmpty()) {
            return false;
        }
        return affectationSalarieChantierRepository.findBySalarieId(salarieId).stream()
            .anyMatch(a -> chantierIds.contains(a.getChantierId()));
    }

    /** L'entreprise a une affectation sur au moins un chantier accessible par ce compte Client. */
    public boolean entrepriseAccessibleParClient(UUID entrepriseId, UUID clientId, UUID utilisateurId) {
        List<UUID> chantierIds = chantierIdsAccessiblesPourClient(clientId, utilisateurId);
        if (chantierIds.isEmpty()) {
            return false;
        }
        return affectationEntrepriseChantierRepository.findByEntrepriseId(entrepriseId).stream()
            .anyMatch(a -> chantierIds.contains(a.getChantierId()));
    }

    /** Le client a un chantier sur lequel cette entreprise a une affectation. */
    public boolean clientAccessibleParEntreprise(UUID clientId, UUID entrepriseId) {
        List<UUID> chantierIdsEntreprise = affectationEntrepriseChantierRepository.findByEntrepriseId(entrepriseId).stream()
            .map(AffectationEntrepriseChantier::getChantierId).toList();
        if (chantierIdsEntreprise.isEmpty()) {
            return false;
        }
        return chantierRepository.findByClientId(clientId).stream()
            .map(Chantier::getId).anyMatch(chantierIdsEntreprise::contains);
    }

    public boolean controleAppartientAuClient(UUID controleId, UUID clientId, UUID utilisateurId) {
        return controleRepository.findById(controleId)
            .map(c -> chantierAccessibleParClient(c.getChantierId(), clientId, utilisateurId))
            .orElse(false);
    }

    /** L'entreprise a une affectation sur le chantier de ce contrôle. */
    public boolean controleAccessibleParEntreprise(UUID controleId, UUID entrepriseId) {
        return controleRepository.findById(controleId)
            .map(c -> !affectationEntrepriseChantierRepository
                .findByChantierIdAndEntrepriseId(c.getChantierId(), entrepriseId).isEmpty())
            .orElse(false);
    }

    public boolean rapportAppartientAuClient(UUID rapportId, UUID clientId, UUID utilisateurId) {
        return rapportControleRepository.findById(rapportId)
            .map(r -> controleAppartientAuClient(r.getControleId(), clientId, utilisateurId))
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

    /**
     * Centralise pour /documents/{id}/fichier exactement le même scoping que
     * DocumentController.lister() applique déjà à la liste — un document est
     * accessible si son salarié/entreprise l'est, selon le même arbre de règles.
     * SUPER_ADMIN et les comptes internes (ni entreprise, ni client) passent toujours.
     */
    public boolean documentAccessible(UUID documentId, CurrentUser currentUser) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return false;
        }
        if (currentUser.entrepriseId().isPresent()) {
            UUID entrepriseId = currentUser.entrepriseId().get();
            if (document.getSalarieId() != null) {
                return salarieAppartientAEntreprise(document.getSalarieId(), entrepriseId);
            }
            return entrepriseId.equals(document.getEntrepriseId());
        }
        if (currentUser.clientId().isPresent()) {
            UUID clientId = currentUser.clientId().get();
            UUID utilisateurId = currentUser.keycloakId();
            if (document.getSalarieId() != null) {
                return salarieAccessibleParClient(document.getSalarieId(), clientId, utilisateurId);
            }
            return entrepriseAccessibleParClient(document.getEntrepriseId(), clientId, utilisateurId);
        }
        return true;
    }

    /** Chantiers accessibles par ce compte Client : uniquement ceux qui lui ont été
        explicitement assignés (voir chantier_utilisateur). Aucune assignation = aucun accès. */
    private List<UUID> chantierIdsAccessiblesPourClient(UUID clientId, UUID utilisateurId) {
        List<UUID> chantierIdsDuClient = chantierIdsDuClient(clientId);
        return chantierIdsAssignes(utilisateurId).stream().filter(chantierIdsDuClient::contains).toList();
    }

    private List<UUID> chantierIdsAssignes(UUID utilisateurId) {
        return chantierUtilisateurRepository.findByUtilisateurId(utilisateurId).stream()
            .map(ChantierUtilisateur::getChantierId).toList();
    }

    private List<UUID> chantierIdsDuClient(UUID clientId) {
        return chantierRepository.findByClientId(clientId).stream().map(Chantier::getId).toList();
    }
}

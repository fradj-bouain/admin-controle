package com.fluttiris.admincontrol.entreprise.api.dto;

import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantier;
import com.fluttiris.admincontrol.entreprise.domain.RoleEntreprise;

import java.time.LocalDate;
import java.util.UUID;

public record AffectationEntrepriseChantierResponse(
    UUID id,
    UUID chantierId,
    // Nom du chantier — résolu en lot pour la liste transverse (voir EntrepriseController,
    // GET /entreprises/affectations) ; null sur les réponses qui ne le fournissent pas
    // (le contexte du chantier est déjà connu, ex: GET /chantiers/{id}/entreprises).
    String nomChantier,
    UUID entrepriseId,
    // Raison sociale de l'entreprise affectée — permet d'afficher les sous-traitants d'un
    // chantier sans que le front doive rappeler /entreprises/{id} (une Entreprise n'a de
    // toute façon pas le droit de consulter la fiche d'une autre entreprise, voir
    // EntrepriseController.obtenir). Null quand non résolue (voir from(a) sans map).
    String raisonSocialeEntreprise,
    RoleEntreprise role,
    UUID affectationParenteId,
    LocalDate dateDebut,
    LocalDate dateFin,
    String statut,
    // Email/téléphone/adresse de contact propres à CETTE relation (entreprise, chantier) —
    // voir AffectationEntrepriseChantier.emailContact/telephoneContact/adresseContact.
    // Chacun null = pas de valeur spécifique à ce chantier.
    String emailContact,
    String telephoneContact,
    String adresseContact
) {
    public static AffectationEntrepriseChantierResponse from(AffectationEntrepriseChantier a) {
        return from(a, null);
    }

    public static AffectationEntrepriseChantierResponse from(AffectationEntrepriseChantier a, String raisonSocialeEntreprise) {
        return from(a, raisonSocialeEntreprise, null);
    }

    public static AffectationEntrepriseChantierResponse from(AffectationEntrepriseChantier a, String raisonSocialeEntreprise,
                                                               String nomChantier) {
        return new AffectationEntrepriseChantierResponse(a.getId(), a.getChantierId(), nomChantier, a.getEntrepriseId(),
            raisonSocialeEntreprise, a.getRole(), a.getAffectationParenteId(), a.getDateDebut(), a.getDateFin(), a.getStatut(),
            a.getEmailContact(), a.getTelephoneContact(), a.getAdresseContact());
    }
}

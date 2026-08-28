package com.fluttiris.admincontrol.chantier.domain;

import com.fluttiris.admincontrol.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "chantier")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA uniquement ; passer par le service applicatif
public class Chantier extends Auditable {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String nom;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    private String prestation;

    private String adresse;

    @Column(name = "adresse_2")
    private String adresse2;

    @Column(name = "adresse_3")
    private String adresse3;

    @Column(name = "code_postal")
    private String codePostal;

    private String ville;

    @Column(name = "pays_id")
    private UUID paysId;

    @Column(name = "chef_chantier_utilisateur_id")
    private UUID chefChantierUtilisateurId;

    @Column(name = "salarie_responsable_id")
    private UUID salarieResponsableId;

    @Column(name = "note_interne")
    private String noteInterne;

    @Column(name = "note_client")
    private String noteClient;

    @Column(name = "ips_allowed")
    private String ipsAllowed;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin_prevue")
    private LocalDate dateFinPrevue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutChantier statut = StatutChantier.ACTIF;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_controles")
    private RecurrenceControles recurrenceControles;

    @Column(name = "date_prochain_controle")
    private LocalDate dateProchainControle;

    public static Chantier creer(String nom, UUID clientId, String prestation, String adresse, String adresse2,
                                  String adresse3, String codePostal, String ville, UUID paysId,
                                  String noteInterne, String noteClient, String ipsAllowed,
                                  LocalDate dateDebut, LocalDate dateFinPrevue) {
        Chantier chantier = new Chantier();
        chantier.nom = nom;
        chantier.clientId = clientId;
        chantier.prestation = prestation;
        chantier.adresse = adresse;
        chantier.adresse2 = adresse2;
        chantier.adresse3 = adresse3;
        chantier.codePostal = codePostal;
        chantier.ville = ville;
        chantier.paysId = paysId;
        chantier.noteInterne = noteInterne;
        chantier.noteClient = noteClient;
        chantier.ipsAllowed = ipsAllowed;
        chantier.dateDebut = dateDebut;
        chantier.dateFinPrevue = dateFinPrevue;
        return chantier;
    }

    public void modifier(String nom, UUID clientId, String prestation, String adresse, String adresse2,
                          String adresse3, String codePostal, String ville, UUID paysId, String noteInterne,
                          String noteClient, String ipsAllowed, LocalDate dateDebut, LocalDate dateFinPrevue) {
        this.nom = nom;
        this.clientId = clientId;
        this.prestation = prestation;
        this.adresse = adresse;
        this.adresse2 = adresse2;
        this.adresse3 = adresse3;
        this.codePostal = codePostal;
        this.ville = ville;
        this.paysId = paysId;
        this.noteInterne = noteInterne;
        this.noteClient = noteClient;
        this.ipsAllowed = ipsAllowed;
        this.dateDebut = dateDebut;
        this.dateFinPrevue = dateFinPrevue;
    }

    public void definirControles(RecurrenceControles recurrenceControles, LocalDate dateProchainControle) {
        this.recurrenceControles = recurrenceControles;
        this.dateProchainControle = dateProchainControle;
    }

    public void desactiver() {
        this.statut = StatutChantier.INACTIF;
    }

    public void activer() {
        this.statut = StatutChantier.ACTIF;
    }

    public void affecterChefChantier(UUID utilisateurId) {
        this.chefChantierUtilisateurId = utilisateurId;
    }

    /** Retire la désignation sans en affecter une autre — utilisé par le bouton étoile
        (cliquer sur l'étoile de la personne déjà désignée l'enlève) directement dans la
        liste Utilisateurs du Back Office, plus de carte "Responsable du chantier" à part. */
    public void retirerChefChantier() {
        this.chefChantierUtilisateurId = null;
    }

    public void affecterSalarieResponsable(UUID salarieId) {
        this.salarieResponsableId = salarieId;
    }

    /** Miroir de retirerChefChantier() pour le salarié responsable — voir la liste Salariés. */
    public void retirerSalarieResponsable() {
        this.salarieResponsableId = null;
    }
}

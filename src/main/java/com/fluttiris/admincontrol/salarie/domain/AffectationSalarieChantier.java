package com.fluttiris.admincontrol.salarie.domain;

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
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Table pivot historisée Salarié <-> Chantier. Ne porte JAMAIS de FK directe
 * salarie->chantier/entreprise (cf. limite identifiée sur l'existant) : un
 * salarié peut avoir plusieurs affectations successives ou même simultanées
 * (profils transverses). L'entreprise pour laquelle il intervient sur CE
 * chantier référence une affectation_entreprise_chantier réellement active,
 * pas l'entreprise brute.
 */
@Entity
@Table(name = "affectation_salarie_chantier")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AffectationSalarieChantier extends Auditable {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "salarie_id", nullable = false)
    private UUID salarieId;

    @Column(name = "chantier_id", nullable = false)
    private UUID chantierId;

    @Column(name = "affectation_entreprise_chantier_id", nullable = false)
    private UUID affectationEntrepriseChantierId;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut = LocalDate.now();

    @Column(name = "date_fin")
    private LocalDate dateFin;

    /** Statut d'ENGAGEMENT sur ce chantier (ce salarié y travaille-t-il actuellement ?) —
        distinct de statutAcces ci-dessous (l'administratif est-il en règle pour y accéder ?).
        Les deux axes sont indépendants : un salarié peut être ACTIF ici alors que son accès
        est encore EN_ATTENTE (badge pas encore édité), voir AffectationSalarieChantierService. */
    @Column(nullable = false)
    private String statut = "ACTIF";

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_acces", nullable = false)
    private StatutAcces statutAcces = StatutAcces.EN_ATTENTE;

    @Column(name = "motif_refus")
    private String motifRefus;

    /** Suivi terrain (équipement de protection individuelle), relevé lors des passages sur site. */
    @Column(name = "epi_gants", nullable = false)
    private boolean epiGants = false;

    @Column(name = "epi_casque", nullable = false)
    private boolean epiCasque = false;

    @Column(name = "epi_chaussures", nullable = false)
    private boolean epiChaussures = false;

    @Column(name = "badge_edite", nullable = false)
    private boolean badgeEdite = false;

    @Column(nullable = false)
    private boolean present = false;

    public static AffectationSalarieChantier creer(UUID salarieId, UUID chantierId, UUID affectationEntrepriseChantierId) {
        AffectationSalarieChantier affectation = new AffectationSalarieChantier();
        affectation.salarieId = salarieId;
        affectation.chantierId = chantierId;
        affectation.affectationEntrepriseChantierId = affectationEntrepriseChantierId;
        return affectation;
    }

    public void accorderAcces() {
        this.statutAcces = StatutAcces.ACCORDE;
    }

    public void refuserAcces(String motif) {
        this.statutAcces = StatutAcces.REFUSE;
        this.motifRefus = motif;
    }

    /** Conservée pour compatibilité (appelée historiquement en fin d'affectation) — se comporte
        désormais exactement comme désactiver(), voir plus bas. */
    public void cloturer() {
        desactiver();
    }

    /** Miroir de AffectationEntrepriseChantier.desactiver() : ne modifie que CETTE ligne,
        jamais le salarié ni son entreprise. */
    public void desactiver() {
        this.statut = "INACTIF";
        this.dateFin = LocalDate.now();
    }

    /** Rouvre une affectation précédemment désactivée — n'existait pas avant (l'ancien
        cloturer() était à sens unique), demandé explicitement pour ne pas perdre le lien
        avec l'historique (dates, EPI, présence déjà enregistrés) d'un salarié qui revient
        sur un chantier qu'il avait quitté. */
    public void reactiver() {
        this.statut = "ACTIF";
        this.dateFin = null;
    }

    public boolean estActive() {
        return "ACTIF".equals(statut);
    }

    public void majSuivi(boolean epiGants, boolean epiCasque, boolean epiChaussures, boolean badgeEdite, boolean present) {
        this.epiGants = epiGants;
        this.epiCasque = epiCasque;
        this.epiChaussures = epiChaussures;
        this.badgeEdite = badgeEdite;
        this.present = present;
    }
}

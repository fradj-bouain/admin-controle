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
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "salarie")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Salarie extends Auditable {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "nationalite_pays_id")
    private UUID nationalitePaysId;

    /** Employeur légal (contrat de travail) — ne change pas au gré des affectations chantier. */
    @Column(name = "entreprise_employeur_id", nullable = false)
    private UUID entrepriseEmployeurId;

    @Column(name = "type_salarie_id")
    private UUID typeSalarieId;

    @Column(name = "type_contrat_id")
    private UUID typeContratId;

    @Column(name = "fonction_id")
    private UUID fonctionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutSalarie statut = StatutSalarie.ACTIF;

    public static Salarie creer(String nom, String prenom, LocalDate dateNaissance, UUID nationalitePaysId,
                                 UUID entrepriseEmployeurId, UUID typeSalarieId, UUID typeContratId, UUID fonctionId) {
        Salarie salarie = new Salarie();
        salarie.nom = nom;
        salarie.prenom = prenom;
        salarie.dateNaissance = dateNaissance;
        salarie.nationalitePaysId = nationalitePaysId;
        salarie.entrepriseEmployeurId = entrepriseEmployeurId;
        salarie.typeSalarieId = typeSalarieId;
        salarie.typeContratId = typeContratId;
        salarie.fonctionId = fonctionId;
        return salarie;
    }

    public void modifier(String nom, String prenom, LocalDate dateNaissance, UUID nationalitePaysId,
                          UUID entrepriseEmployeurId, UUID typeSalarieId, UUID typeContratId, UUID fonctionId) {
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.nationalitePaysId = nationalitePaysId;
        this.entrepriseEmployeurId = entrepriseEmployeurId;
        this.typeSalarieId = typeSalarieId;
        this.typeContratId = typeContratId;
        this.fonctionId = fonctionId;
    }

    public void desactiver() {
        this.statut = StatutSalarie.INACTIF;
    }

    public void activer() {
        this.statut = StatutSalarie.ACTIF;
    }
}

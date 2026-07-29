package com.fluttiris.admincontrol.entreprise.domain;

import com.fluttiris.admincontrol.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

/**
 * IMPORTANT : cette entité ne porte AUCUN champ de rôle (Principale/STT1/STT2).
 * Le rôle est strictement contextuel à un chantier donné : voir
 * {@link AffectationEntrepriseChantier}. Une même entreprise peut être
 * Principale sur un chantier et STT2 sur un autre.
 */
@Entity
@Table(name = "entreprise")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entreprise extends Auditable {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "raison_sociale", nullable = false)
    private String raisonSociale;

    private String siret;

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

    @Column(name = "corps_de_metier_id")
    private UUID corpsDeMetierId;

    private String telephone;

    @Column(name = "telephone_2")
    private String telephone2;

    @Column(name = "telephone_3")
    private String telephone3;

    private String fax;

    private String email;

    @Column(name = "email_2")
    private String email2;

    @Column(name = "email_3")
    private String email3;

    @Column(name = "forme_juridique")
    private String formeJuridique;

    private String siren;

    @Column(name = "rcs_rci")
    private String rcsRci;

    @Column(name = "tva_intra")
    private String tvaIntra;

    @Column(name = "num_cotisant")
    private String numCotisant;

    @Column(name = "responsable_signataire_agrement")
    private String responsableSignataireAgrement;

    private String commentaire;

    @Column(name = "date_desactivation")
    private Instant dateDesactivation;

    @Column(nullable = false)
    private boolean actif = true;

    public static Entreprise creer(String raisonSociale, String siret, String adresse, String adresse2,
                                    String adresse3, String codePostal, String ville, UUID paysId,
                                    UUID corpsDeMetierId, String telephone, String telephone2, String telephone3,
                                    String fax, String email, String email2, String email3, String formeJuridique,
                                    String siren, String rcsRci, String tvaIntra, String numCotisant,
                                    String responsableSignataireAgrement, String commentaire) {
        Entreprise entreprise = new Entreprise();
        entreprise.raisonSociale = raisonSociale;
        entreprise.siret = siret;
        entreprise.adresse = adresse;
        entreprise.adresse2 = adresse2;
        entreprise.adresse3 = adresse3;
        entreprise.codePostal = codePostal;
        entreprise.ville = ville;
        entreprise.paysId = paysId;
        entreprise.corpsDeMetierId = corpsDeMetierId;
        entreprise.telephone = telephone;
        entreprise.telephone2 = telephone2;
        entreprise.telephone3 = telephone3;
        entreprise.fax = fax;
        entreprise.email = email;
        entreprise.email2 = email2;
        entreprise.email3 = email3;
        entreprise.formeJuridique = formeJuridique;
        entreprise.siren = siren;
        entreprise.rcsRci = rcsRci;
        entreprise.tvaIntra = tvaIntra;
        entreprise.numCotisant = numCotisant;
        entreprise.responsableSignataireAgrement = responsableSignataireAgrement;
        entreprise.commentaire = commentaire;
        return entreprise;
    }

    public void modifier(String raisonSociale, String siret, String adresse, String adresse2, String adresse3,
                          String codePostal, String ville, UUID paysId, UUID corpsDeMetierId, String telephone,
                          String telephone2, String telephone3, String fax, String email, String email2,
                          String email3, String formeJuridique, String siren, String rcsRci, String tvaIntra,
                          String numCotisant, String responsableSignataireAgrement, String commentaire) {
        this.raisonSociale = raisonSociale;
        this.siret = siret;
        this.adresse = adresse;
        this.adresse2 = adresse2;
        this.adresse3 = adresse3;
        this.codePostal = codePostal;
        this.ville = ville;
        this.paysId = paysId;
        this.corpsDeMetierId = corpsDeMetierId;
        this.telephone = telephone;
        this.telephone2 = telephone2;
        this.telephone3 = telephone3;
        this.fax = fax;
        this.email = email;
        this.email2 = email2;
        this.email3 = email3;
        this.formeJuridique = formeJuridique;
        this.siren = siren;
        this.rcsRci = rcsRci;
        this.tvaIntra = tvaIntra;
        this.numCotisant = numCotisant;
        this.responsableSignataireAgrement = responsableSignataireAgrement;
        this.commentaire = commentaire;
    }

    public void desactiver() {
        this.actif = false;
        this.dateDesactivation = Instant.now();
    }

    public void activer() {
        this.actif = true;
        this.dateDesactivation = null;
    }
}

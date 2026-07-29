package com.fluttiris.admincontrol.client.domain;

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

import java.util.UUID;

@Entity
@Table(name = "client")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client extends Auditable {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "raison_sociale", nullable = false)
    private String raisonSociale;

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

    private String siret;

    @Column(name = "rcs_rci")
    private String rcsRci;

    @Column(name = "tva_intra")
    private String tvaIntra;

    @Column(name = "num_cotisant")
    private String numCotisant;

    @Column(name = "responsable_signataire_agrement")
    private String responsableSignataireAgrement;

    @Column(nullable = false)
    private boolean actif = true;

    public static Client creer(String raisonSociale, String adresse, String adresse2, String adresse3,
                                String codePostal, String ville, UUID paysId, String telephone, String telephone2,
                                String telephone3, String fax, String email, String email2, String email3,
                                String formeJuridique, String siren, String siret, String rcsRci, String tvaIntra,
                                String numCotisant, String responsableSignataireAgrement) {
        Client client = new Client();
        client.raisonSociale = raisonSociale;
        client.adresse = adresse;
        client.adresse2 = adresse2;
        client.adresse3 = adresse3;
        client.codePostal = codePostal;
        client.ville = ville;
        client.paysId = paysId;
        client.telephone = telephone;
        client.telephone2 = telephone2;
        client.telephone3 = telephone3;
        client.fax = fax;
        client.email = email;
        client.email2 = email2;
        client.email3 = email3;
        client.formeJuridique = formeJuridique;
        client.siren = siren;
        client.siret = siret;
        client.rcsRci = rcsRci;
        client.tvaIntra = tvaIntra;
        client.numCotisant = numCotisant;
        client.responsableSignataireAgrement = responsableSignataireAgrement;
        return client;
    }

    public void modifier(String raisonSociale, String adresse, String adresse2, String adresse3, String codePostal,
                          String ville, UUID paysId, String telephone, String telephone2, String telephone3,
                          String fax, String email, String email2, String email3, String formeJuridique,
                          String siren, String siret, String rcsRci, String tvaIntra, String numCotisant,
                          String responsableSignataireAgrement) {
        this.raisonSociale = raisonSociale;
        this.adresse = adresse;
        this.adresse2 = adresse2;
        this.adresse3 = adresse3;
        this.codePostal = codePostal;
        this.ville = ville;
        this.paysId = paysId;
        this.telephone = telephone;
        this.telephone2 = telephone2;
        this.telephone3 = telephone3;
        this.fax = fax;
        this.email = email;
        this.email2 = email2;
        this.email3 = email3;
        this.formeJuridique = formeJuridique;
        this.siren = siren;
        this.siret = siret;
        this.rcsRci = rcsRci;
        this.tvaIntra = tvaIntra;
        this.numCotisant = numCotisant;
        this.responsableSignataireAgrement = responsableSignataireAgrement;
    }

    public void desactiver() {
        this.actif = false;
    }

    public void activer() {
        this.actif = true;
    }
}

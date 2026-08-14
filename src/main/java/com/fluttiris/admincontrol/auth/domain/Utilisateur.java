package com.fluttiris.admincontrol.auth.domain;

import com.fluttiris.admincontrol.common.audit.Auditable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Compte applicatif. Remplace l'ancien fournisseur d'identité Keycloak par une
 * authentification JWT auto-hébergée (login/mot de passe local, JWT signé par
 * le backend lui-même) — voir JwtConfig et AuthenticationService.
 */
@Entity
@Table(name = "utilisateur")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Utilisateur extends Auditable {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** 'M' ou 'MME', optionnel. */
    private String civilite;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "utilisateur_role", joinColumns = @JoinColumn(name = "utilisateur_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    /** Renseigné pour un compte de type Entreprise ; le rôle Principale/STT1/STT2 reste contextuel par chantier. */
    @Column(name = "entreprise_id")
    private UUID entrepriseId;

    /** Renseigné pour un compte Client (maître d'ouvrage), scope de lecture seule sur ses chantiers. */
    @Column(name = "client_id")
    private UUID clientId;

    /** Renseigné pour un compte Contrôleur, scope sur les contrôles de cet organisme (ControleTiers). */
    @Column(name = "controle_tiers_id")
    private UUID controleTiersId;

    @Column(nullable = false)
    private boolean actif = true;

    /** Uniquement pertinent pour un compte Client : true = voit tous les chantiers du
        Client (et tout ce qui en dépend : entreprises, salariés, documents, rapports),
        false = "responsable de chantier", cantonné aux chantiers assignés via
        chantier_utilisateur. Sans effet pour les autres types de compte. */
    @Column(name = "acces_tous_chantiers", nullable = false)
    private boolean accesTousChantiers = false;

    public static Utilisateur creer(String username, String passwordHash, String civilite, String nom,
                                     String prenom, String email, Set<String> roles, UUID entrepriseId, UUID clientId,
                                     UUID controleTiersId) {
        return creer(username, passwordHash, civilite, nom, prenom, email, roles, entrepriseId, clientId,
            controleTiersId, false);
    }

    public static Utilisateur creer(String username, String passwordHash, String civilite, String nom,
                                     String prenom, String email, Set<String> roles, UUID entrepriseId, UUID clientId,
                                     UUID controleTiersId, boolean accesTousChantiers) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.username = username;
        utilisateur.passwordHash = passwordHash;
        utilisateur.civilite = civilite;
        utilisateur.nom = nom;
        utilisateur.prenom = prenom;
        utilisateur.email = email;
        utilisateur.roles = new HashSet<>(roles);
        utilisateur.entrepriseId = entrepriseId;
        utilisateur.clientId = clientId;
        utilisateur.controleTiersId = controleTiersId;
        utilisateur.accesTousChantiers = accesTousChantiers;
        return utilisateur;
    }

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }

    /** Auto-gestion (Mon équipe) comme SUPER_ADMIN : jamais le rôle ni le rattachement
        entreprise/client/controleTiers, qui restent ceux fixés à la création — seule
        l'identité du compte (nom/prénom/email/nom d'utilisateur) est modifiable ici. */
    public void modifier(String nom, String prenom, String email, String username) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.username = username;
    }

    public void changerMotDePasse(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** Réservé au SUPER_ADMIN (jamais via l'auto-gestion "Mon équipe") : bascule un
        compte Client entre "accès total" et "responsable de chantier". */
    public void definirAccesTousChantiers(boolean accesTousChantiers) {
        this.accesTousChantiers = accesTousChantiers;
    }
}

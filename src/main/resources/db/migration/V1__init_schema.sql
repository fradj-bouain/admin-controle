-- ============================================================================
-- Schéma ADMIN-CONTROL'BTP — version consolidée (remplace l'historique
-- V1..V8 issu des itérations successives, squashé car aucune donnée réelle
-- n'existait encore). Reflète l'état final validé : entités métier,
-- authentification JWT, alignement sur l'analyse de la base legacy,
-- intégrité référentielle complète, suppression logique, et données de
-- référence réelles extraites du site legacy (pays, corps de métier,
-- fonctions salarié, organismes de contrôle).
--
-- Principe directeur (cf. analyse de l'existant) : le rôle d'une entreprise
-- (Principale / STT1 / STT2) et son rattachement hiérarchique ne sont JAMAIS
-- des attributs de l'entreprise elle-même : une même entreprise peut être
-- Principale sur un chantier et STT1 sur un autre. Ces informations vivent
-- uniquement sur affectation_entreprise_chantier, propre à chaque chantier.
--
-- Suppression logique : les tables métier "coeur" portent une colonne
-- deleted_at plutôt qu'un vrai DELETE — la ligne reste en base (traçabilité,
-- cohérence des FK vers les tables filles) mais disparaît de toutes les
-- lectures applicatives (filtrage côté ORM). Les tables à valeur d'archive
-- (rapport_controle, message, qr_code, visite_medicale) et les référentiels
-- n'ont volontairement pas cette colonne.
--
-- Ordre des tables : toutes les FK "métier" sont déclarées en ligne. Les FK
-- vers utilisateur sont ajoutées dans un bloc ALTER TABLE final, car
-- utilisateur référence lui-même client/entreprise (dépendance croisée).
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----------------------------------------------------------------------------
-- Référentiels sans dépendance
-- ----------------------------------------------------------------------------

CREATE TABLE pays (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_iso VARCHAR(3) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    zone VARCHAR(30)
);
COMMENT ON COLUMN pays.zone IS
    'France / UE / Hors UE. Convention héritée de la base legacy : la distinction "Europe de l''Est" existait en colonne mais n''était utilisée par aucune logique métier.';

CREATE TABLE corps_de_metier (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    libelle VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE controle_tiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom VARCHAR(150) NOT NULL
);

-- Existaient côté legacy sous forme d'ID figés en dur (adminbtp_types_salarie /
-- adminbtp_types_contrat_salarie). Libellés reconstitués depuis le texte des
-- vues du dump legacy (aucune donnée réelle disponible) — à confirmer avec le client.
CREATE TABLE type_salarie (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);
INSERT INTO type_salarie (code, libelle) VALUES
    ('SALARIE', 'Salarié'),
    ('ARTISAN', 'Artisan'),
    ('DIRIGEANT_SALARIE', 'Dirigeant salarié'),
    ('DIRIGEANT_NON_SALARIE', 'Dirigeant non salarié'),
    ('INTERIMAIRE', 'Intérimaire'),
    ('REPRESENTANT_NON_SALARIE', 'Représentant non salarié'),
    ('APPRENTI', 'Apprenti'),
    ('STAGIAIRE', 'Stagiaire');

CREATE TABLE type_contrat_salarie (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);
INSERT INTO type_contrat_salarie (code, libelle) VALUES
    ('NON_INDIQUE', 'Non indiqué'),
    ('CDI', 'CDI'),
    ('CDD', 'CDD'),
    ('INTERIMAIRE', 'Intérimaire'),
    ('TRAVAILLEUR_DETACHE', 'Travailleur détaché'),
    ('APPRENTISSAGE', 'Apprentissage'),
    ('CONVENTION_STAGE', 'Convention de stage');

CREATE TABLE salarie_fonction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    libelle VARCHAR(150) NOT NULL UNIQUE
);

-- ----------------------------------------------------------------------------
-- Entités métier principales
-- ----------------------------------------------------------------------------

CREATE TABLE client (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    raison_sociale VARCHAR(200) NOT NULL,
    adresse VARCHAR(255),
    adresse_2 VARCHAR(255),
    adresse_3 VARCHAR(255),
    code_postal VARCHAR(20),
    ville VARCHAR(100),
    pays_id UUID REFERENCES pays(id),
    telephone VARCHAR(30),
    telephone_2 VARCHAR(30),
    telephone_3 VARCHAR(30),
    fax VARCHAR(30),
    email VARCHAR(150),
    email_2 VARCHAR(150),
    email_3 VARCHAR(150),
    forme_juridique VARCHAR(50),
    siren VARCHAR(20),
    siret VARCHAR(20),
    rcs_rci VARCHAR(50),
    tva_intra VARCHAR(30),
    num_cotisant VARCHAR(50),
    responsable_signataire_agrement VARCHAR(200),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE chantier (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom VARCHAR(200) NOT NULL,
    client_id UUID NOT NULL REFERENCES client(id),
    prestation VARCHAR(255),
    adresse VARCHAR(255),
    adresse_2 VARCHAR(255),
    adresse_3 VARCHAR(255),
    code_postal VARCHAR(20),
    ville VARCHAR(100),
    pays_id UUID REFERENCES pays(id),
    -- utilisateur désigné responsable/chef de chantier ; FK ajoutée en fin de fichier
    chef_chantier_utilisateur_id UUID,
    -- salarié désigné responsable sur site, distinct du chef de chantier (utilisateur) ; FK plus bas, salarie créée après chantier donc pas de contrainte inline possible ici
    salarie_responsable_id UUID,
    note_interne TEXT,
    note_client TEXT,
    ips_allowed VARCHAR(255),
    date_debut DATE,
    date_fin_prevue DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIF' CHECK (statut IN ('ACTIF', 'INACTIF')),
    recurrence_controles VARCHAR(20)
        CHECK (recurrence_controles IN ('AUCUNE', 'HEBDOMADAIRE', 'MENSUEL', 'TRIMESTRIEL', 'ANNUEL')),
    date_prochain_controle DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_chantier_client ON chantier(client_id);

/**
 * IMPORTANT : cette entité ne porte AUCUN champ de rôle (Principale/STT1/STT2).
 * Le rôle est strictement contextuel à un chantier donné : voir
 * affectation_entreprise_chantier. Une même entreprise peut être
 * Principale sur un chantier et STT2 sur un autre.
 */
CREATE TABLE entreprise (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    raison_sociale VARCHAR(200) NOT NULL,
    siret VARCHAR(20),
    adresse VARCHAR(255),
    adresse_2 VARCHAR(255),
    adresse_3 VARCHAR(255),
    code_postal VARCHAR(20),
    ville VARCHAR(100),
    pays_id UUID REFERENCES pays(id),
    corps_de_metier_id UUID REFERENCES corps_de_metier(id),
    telephone VARCHAR(30),
    telephone_2 VARCHAR(30),
    telephone_3 VARCHAR(30),
    fax VARCHAR(30),
    email VARCHAR(150),
    email_2 VARCHAR(150),
    email_3 VARCHAR(150),
    forme_juridique VARCHAR(50),
    siren VARCHAR(20),
    rcs_rci VARCHAR(50),
    tva_intra VARCHAR(30),
    num_cotisant VARCHAR(50),
    responsable_signataire_agrement VARCHAR(200),
    commentaire TEXT,
    date_desactivation TIMESTAMPTZ,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ
);

-- ----------------------------------------------------------------------------
-- Affectation entreprise <-> chantier : porte le rôle contextuel + la hiérarchie
-- ----------------------------------------------------------------------------

CREATE TABLE affectation_entreprise_chantier (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chantier_id UUID NOT NULL REFERENCES chantier(id),
    entreprise_id UUID NOT NULL REFERENCES entreprise(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('PRINCIPALE', 'STT1', 'STT2')),
    -- référence une AFFECTATION (pas directement une entreprise) : le lien
    -- hiérarchique n'a de sens que dans le contexte de CE chantier.
    affectation_parente_id UUID REFERENCES affectation_entreprise_chantier(id),
    date_debut DATE NOT NULL DEFAULT CURRENT_DATE,
    date_fin DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIF' CHECK (statut IN ('ACTIF', 'INACTIF')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    -- cohérence hiérarchique : PRINCIPALE n'a pas de parent, STT1/STT2 en ont un
    CONSTRAINT chk_hierarchie_role CHECK (
        (role = 'PRINCIPALE' AND affectation_parente_id IS NULL)
        OR (role IN ('STT1', 'STT2') AND affectation_parente_id IS NOT NULL)
    )
);
CREATE INDEX idx_affectation_entreprise_chantier ON affectation_entreprise_chantier(chantier_id);
CREATE INDEX idx_affectation_entreprise_entreprise ON affectation_entreprise_chantier(entreprise_id);
-- une seule affectation active par (chantier, entreprise) ; une ligne supprimée
-- n'occupe plus la place, une nouvelle affectation identique reste possible.
CREATE UNIQUE INDEX uq_entreprise_par_chantier
    ON affectation_entreprise_chantier(chantier_id, entreprise_id)
    WHERE deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- Salariés
-- ----------------------------------------------------------------------------

CREATE TABLE salarie (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    date_naissance DATE,
    nationalite_pays_id UUID REFERENCES pays(id),
    -- employeur légal (contrat de travail) : ne change pas au grè des chantiers
    entreprise_employeur_id UUID NOT NULL REFERENCES entreprise(id),
    type_salarie_id UUID REFERENCES type_salarie(id),
    type_contrat_id UUID REFERENCES type_contrat_salarie(id),
    fonction_id UUID REFERENCES salarie_fonction(id),
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIF' CHECK (statut IN ('ACTIF', 'INACTIF')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_salarie_employeur ON salarie(entreprise_employeur_id);
CREATE INDEX idx_salarie_type_salarie ON salarie(type_salarie_id);
CREATE INDEX idx_salarie_type_contrat ON salarie(type_contrat_id);
CREATE INDEX idx_salarie_fonction ON salarie(fonction_id);

-- Affectation salarié <-> chantier : historisée (périodes), jamais de FK directe
-- id_chantier/id_entreprise sur salarie (cf. limite identifiée sur l'existant).
CREATE TABLE affectation_salarie_chantier (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salarie_id UUID NOT NULL REFERENCES salarie(id),
    chantier_id UUID NOT NULL REFERENCES chantier(id),
    -- l'entreprise pour laquelle le salarié intervient sur CE chantier : doit être
    -- une entreprise réellement affectée à ce chantier (intégrité applicative,
    -- voir AffectationSalarieChantierService).
    affectation_entreprise_chantier_id UUID NOT NULL REFERENCES affectation_entreprise_chantier(id),
    date_debut DATE NOT NULL DEFAULT CURRENT_DATE,
    date_fin DATE,
    statut_acces VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE'
        CHECK (statut_acces IN ('EN_ATTENTE', 'ACCORDE', 'REFUSE')),
    motif_refus TEXT,
    -- suivi terrain (équipement de protection individuelle), relevé lors des passages sur site
    epi_gants BOOLEAN NOT NULL DEFAULT FALSE,
    epi_casque BOOLEAN NOT NULL DEFAULT FALSE,
    epi_chaussures BOOLEAN NOT NULL DEFAULT FALSE,
    badge_edite BOOLEAN NOT NULL DEFAULT FALSE,
    present BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_affectation_salarie_salarie ON affectation_salarie_chantier(salarie_id);
CREATE INDEX idx_affectation_salarie_chantier ON affectation_salarie_chantier(chantier_id);
CREATE INDEX idx_affectation_salarie_aec ON affectation_salarie_chantier(affectation_entreprise_chantier_id);
-- une seule affectation ACTIVE (sans date de fin) par (salarié, chantier) ;
-- l'historique (lignes closes) reste illimité, une ligne supprimée n'occupe plus la place.
CREATE UNIQUE INDEX uq_affectation_salarie_active
    ON affectation_salarie_chantier(salarie_id, chantier_id)
    WHERE date_fin IS NULL AND deleted_at IS NULL;

CREATE TABLE visite_medicale (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salarie_id UUID NOT NULL REFERENCES salarie(id),
    date_visite DATE NOT NULL,
    date_prochaine_visite DATE,
    apte BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_visite_medicale_salarie ON visite_medicale(salarie_id);

-- ----------------------------------------------------------------------------
-- Documents
-- ----------------------------------------------------------------------------

CREATE TABLE type_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- TEXT et non VARCHAR(150) : les intitulés de documents legacy dépassent
    -- largement 150 caractères (ex: conditions d'agrément détaillées).
    libelle TEXT NOT NULL,
    cible VARCHAR(20) NOT NULL CHECK (cible IN ('SALARIE', 'ENTREPRISE')),
    obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    format VARCHAR(10) NOT NULL DEFAULT 'PDF' CHECK (format IN ('PDF', 'WORD')),
    corps_de_metier_id UUID REFERENCES corps_de_metier(id),
    pays_id UUID REFERENCES pays(id),
    date_debut_validite_requise BOOLEAN NOT NULL DEFAULT TRUE,
    date_fin_validite_requise BOOLEAN NOT NULL DEFAULT TRUE,
    nb_jours_relance_avant INT NOT NULL DEFAULT 0,
    nb_jours_recurrence INT NOT NULL DEFAULT 0,
    retire_accord_acces BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE modele_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_document_id UUID NOT NULL REFERENCES type_document(id),
    nom VARCHAR(150) NOT NULL,
    fichier_template_url VARCHAR(500) NOT NULL,
    champs_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Catalogue de motifs de refus (comme le site legacy), utilisé au lieu de
-- texte libre quand on refuse un document.
CREATE TABLE document_etat (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titre VARCHAR(255) NOT NULL,
    par_defaut BOOLEAN NOT NULL DEFAULT FALSE,
    date_expiree BOOLEAN NOT NULL DEFAULT FALSE,
    valide_le_document BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_document_id UUID NOT NULL REFERENCES type_document(id),
    salarie_id UUID REFERENCES salarie(id),
    entreprise_id UUID REFERENCES entreprise(id),
    chantier_id UUID REFERENCES chantier(id),
    document_etat_id UUID REFERENCES document_etat(id),
    fichier_url VARCHAR(500),
    date_debut_validite DATE,
    date_expiration DATE,
    date_relance DATE,
    mentions TEXT,
    statut_validation VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE'
        CHECK (statut_validation IN ('EN_ATTENTE', 'VALIDE', 'REFUSE', 'EXPIRE')),
    metadonnees JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_document_cible CHECK (
        (salarie_id IS NOT NULL AND entreprise_id IS NULL)
        OR (salarie_id IS NULL AND entreprise_id IS NOT NULL)
    )
);
CREATE INDEX idx_document_salarie ON document(salarie_id);
CREATE INDEX idx_document_entreprise ON document(entreprise_id);
CREATE INDEX idx_document_expiration ON document(date_expiration);

-- ----------------------------------------------------------------------------
-- Automatisation messagerie : règles déclenchées par un champ surveillé
-- (échéance de date, ex: expiration d'un document), par un événement métier
-- (création salarié/entreprise, affectation entreprise-chantier), par une
-- récurrence périodique pure, ou manuellement — ciblant un groupe de
-- destinataires ou un destinataire précis.
-- ----------------------------------------------------------------------------

CREATE TABLE regle_automatisation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom VARCHAR(150) NOT NULL,
    type_declencheur VARCHAR(40) NOT NULL DEFAULT 'CHAMP_SURVEILLABLE'
        CHECK (type_declencheur IN ('CHAMP_SURVEILLABLE', 'CREATION_SALARIE', 'CREATION_ENTREPRISE', 'AFFECTATION_ENTREPRISE_CHANTIER', 'PERIODIQUE', 'MANUEL')),
    -- id d'un champ enregistré côté backend (voir ChampSurveillableRegistry) ;
    -- requis seulement si type_declencheur = CHAMP_SURVEILLABLE.
    champ_surveillable_id VARCHAR(100),
    nb_jours_avant INT NOT NULL DEFAULT 0,
    numero_interne VARCHAR(50),
    titre_interne VARCHAR(200),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    cible_groupe VARCHAR(20) NOT NULL DEFAULT 'SPECIFIQUE'
        CHECK (cible_groupe IN ('TOUS_UTILISATEURS', 'TOUS_CLIENTS', 'TOUTES_ENTREPRISES', 'TOUS_SALARIES', 'SPECIFIQUE')),
    destinataire_type VARCHAR(20) CHECK (destinataire_type IS NULL OR destinataire_type IN ('CLIENT', 'ENTREPRISE', 'UTILISATEUR')),
    destinataire_id UUID,
    sujet VARCHAR(200) NOT NULL DEFAULT '',
    contenu TEXT NOT NULL DEFAULT '',
    -- pour type_declencheur = PERIODIQUE : dernière fois où la règle a généré un message
    derniere_execution TIMESTAMPTZ,
    nb_envois INT NOT NULL DEFAULT 0,
    dernier_envoi TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_regle_specifique CHECK (
        (cible_groupe = 'SPECIFIQUE' AND destinataire_type IS NOT NULL AND destinataire_id IS NOT NULL)
        OR (cible_groupe <> 'SPECIFIQUE' AND destinataire_type IS NULL AND destinataire_id IS NULL)
    )
);

CREATE TABLE message_planifie (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    regle_id UUID REFERENCES regle_automatisation(id),
    -- id du Document/Chantier/Salarié/Entreprise ayant déclenché la règle ; sert
    -- de clé de dédoublonnage pour ne pas régénérer le même rappel à chaque scan
    source_entity_id UUID,
    -- FK ajoutée en fin de fichier
    expediteur_utilisateur_id UUID,
    cible_groupe VARCHAR(20) NOT NULL CHECK (cible_groupe IN ('TOUS_UTILISATEURS', 'TOUS_CLIENTS', 'TOUTES_ENTREPRISES', 'TOUS_SALARIES', 'SPECIFIQUE')),
    destinataire_type VARCHAR(20) CHECK (destinataire_type IS NULL OR destinataire_type IN ('CLIENT', 'ENTREPRISE', 'UTILISATEUR')),
    destinataire_id UUID,
    chantier_id UUID REFERENCES chantier(id),
    sujet VARCHAR(200) NOT NULL,
    contenu TEXT NOT NULL,
    date_envoi_prevue TIMESTAMPTZ NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE' CHECK (statut IN ('EN_ATTENTE', 'ENVOYE', 'ANNULE')),
    date_envoi_reelle TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_message_planifie_specifique CHECK (
        (cible_groupe = 'SPECIFIQUE' AND destinataire_type IS NOT NULL AND destinataire_id IS NOT NULL)
        OR (cible_groupe <> 'SPECIFIQUE' AND destinataire_type IS NULL AND destinataire_id IS NULL)
    )
);
CREATE INDEX idx_message_planifie_statut ON message_planifie(statut, date_envoi_prevue);
CREATE UNIQUE INDEX idx_message_planifie_dedup ON message_planifie(regle_id, source_entity_id)
    WHERE regle_id IS NOT NULL AND source_entity_id IS NOT NULL;

-- ----------------------------------------------------------------------------
-- Contrôles et rapports
-- ----------------------------------------------------------------------------

CREATE TABLE controle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chantier_id UUID NOT NULL REFERENCES chantier(id),
    -- utilisateur ayant réalisé le contrôle ; FK ajoutée en fin de fichier
    controleur_utilisateur_id UUID NOT NULL,
    date_controle DATE NOT NULL,
    remarques TEXT,
    controle_tiers_id UUID REFERENCES controle_tiers(id),
    date_fin DATE,
    termine BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_controle_chantier ON controle(chantier_id);
CREATE INDEX idx_controle_controle_tiers ON controle(controle_tiers_id);

CREATE TABLE rapport_controle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    controle_id UUID NOT NULL REFERENCES controle(id),
    nb_salaries_controles INT NOT NULL DEFAULT 0,
    nb_accords INT NOT NULL DEFAULT 0,
    nb_refus INT NOT NULL DEFAULT 0,
    nb_nouvelles_entreprises INT NOT NULL DEFAULT 0,
    nb_nouveaux_salaries INT NOT NULL DEFAULT 0,
    nb_entreprises INT NOT NULL DEFAULT 0,
    nb_salaries_detaches INT NOT NULL DEFAULT 0,
    -- utilisateur responsable du chantier au moment du contrôle ; FK ajoutée en fin de fichier
    responsable_utilisateur_id UUID,
    date_envoi TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_rapport_controle ON rapport_controle(controle_id);

-- ----------------------------------------------------------------------------
-- Messagerie interne
-- ----------------------------------------------------------------------------

CREATE TABLE message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- FK ajoutée en fin de fichier
    expediteur_utilisateur_id UUID NOT NULL,
    chantier_id UUID REFERENCES chantier(id),
    destinataire_type VARCHAR(20) NOT NULL CHECK (destinataire_type IN ('CLIENT', 'ENTREPRISE', 'UTILISATEUR')),
    destinataire_id UUID NOT NULL,
    sujet VARCHAR(200) NOT NULL,
    contenu TEXT NOT NULL,
    lu BOOLEAN NOT NULL DEFAULT FALSE,
    -- FK ajoutée en fin de fichier
    lu_par_utilisateur_id UUID,
    date_lu TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_message_destinataire ON message(destinataire_type, destinataire_id);
CREATE INDEX idx_message_chantier ON message(chantier_id);

-- ----------------------------------------------------------------------------
-- QR Code / carte d'identité numérique
-- ----------------------------------------------------------------------------

CREATE TABLE qr_code (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salarie_id UUID NOT NULL UNIQUE REFERENCES salarie(id),
    code_valeur VARCHAR(255) NOT NULL UNIQUE,
    carte_identite_numerique_url VARCHAR(500),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- Historique (audit métier, distinct des colonnes created_by/updated_by)
-- ----------------------------------------------------------------------------

CREATE TABLE historique_modification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entite VARCHAR(100) NOT NULL,
    entite_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL, -- CREATION, MODIFICATION, SUPPRESSION, ENVOI
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    -- FK ajoutée en fin de fichier
    utilisateur_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_historique_entite ON historique_modification(entite, entite_id);

-- ----------------------------------------------------------------------------
-- Authentification JWT auto-hébergée (remplace le fournisseur d'identité externe)
-- ----------------------------------------------------------------------------

CREATE TABLE utilisateur (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    civilite VARCHAR(10) CHECK (civilite IN ('M', 'MME')),
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    entreprise_id UUID REFERENCES entreprise(id),
    client_id UUID REFERENCES client(id),
    -- compte de type Contrôleur : scopé à un organisme de contrôle, ne voit/gère
    -- que les contrôles de cet organisme (voir ScopeAuthorizationService).
    controle_tiers_id UUID REFERENCES controle_tiers(id),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- FK vers elle-même ajoutée en fin de fichier
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE utilisateur_role (
    utilisateur_id UUID NOT NULL REFERENCES utilisateur(id),
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (utilisateur_id, role)
);

-- ----------------------------------------------------------------------------
-- Portail client : quels utilisateurs côté client voient quel chantier
-- (brique nécessaire pour un futur portail client en lecture seule)
-- ----------------------------------------------------------------------------

CREATE TABLE chantier_utilisateur (
    chantier_id UUID NOT NULL REFERENCES chantier(id),
    -- FK ajoutée en fin de fichier
    utilisateur_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (chantier_id, utilisateur_id)
);
CREATE INDEX idx_chantier_utilisateur_utilisateur ON chantier_utilisateur(utilisateur_id);

-- ============================================================================
-- FK vers utilisateur, ajoutées ici car utilisateur référence lui-même
-- client/entreprise et ne peut donc pas exister avant eux.
-- ============================================================================

ALTER TABLE utilisateur
    ADD CONSTRAINT fk_utilisateur_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_utilisateur_updated_by FOREIGN KEY (updated_by) REFERENCES utilisateur(id);

ALTER TABLE client
    ADD CONSTRAINT fk_client_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_client_updated_by FOREIGN KEY (updated_by) REFERENCES utilisateur(id);

ALTER TABLE chantier
    ADD CONSTRAINT fk_chantier_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_chantier_updated_by FOREIGN KEY (updated_by) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_chantier_chef_chantier FOREIGN KEY (chef_chantier_utilisateur_id) REFERENCES utilisateur(id),
    -- salarie n'existe qu'après chantier dans l'ordre de création ; FK différée ici pour la même raison que les FK vers utilisateur
    ADD CONSTRAINT fk_chantier_salarie_responsable FOREIGN KEY (salarie_responsable_id) REFERENCES salarie(id);

ALTER TABLE entreprise
    ADD CONSTRAINT fk_entreprise_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_entreprise_updated_by FOREIGN KEY (updated_by) REFERENCES utilisateur(id);

ALTER TABLE affectation_entreprise_chantier
    ADD CONSTRAINT fk_aec_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_aec_updated_by FOREIGN KEY (updated_by) REFERENCES utilisateur(id);

ALTER TABLE salarie
    ADD CONSTRAINT fk_salarie_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_salarie_updated_by FOREIGN KEY (updated_by) REFERENCES utilisateur(id);

ALTER TABLE affectation_salarie_chantier
    ADD CONSTRAINT fk_asc_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_asc_updated_by FOREIGN KEY (updated_by) REFERENCES utilisateur(id);

ALTER TABLE document
    ADD CONSTRAINT fk_document_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_document_updated_by FOREIGN KEY (updated_by) REFERENCES utilisateur(id);

ALTER TABLE regle_automatisation
    ADD CONSTRAINT fk_regle_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_regle_updated_by FOREIGN KEY (updated_by) REFERENCES utilisateur(id);

ALTER TABLE controle
    ADD CONSTRAINT fk_controle_controleur FOREIGN KEY (controleur_utilisateur_id) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_controle_created_by FOREIGN KEY (created_by) REFERENCES utilisateur(id);

ALTER TABLE rapport_controle
    ADD CONSTRAINT fk_rapport_responsable FOREIGN KEY (responsable_utilisateur_id) REFERENCES utilisateur(id);

ALTER TABLE message
    ADD CONSTRAINT fk_message_expediteur FOREIGN KEY (expediteur_utilisateur_id) REFERENCES utilisateur(id),
    ADD CONSTRAINT fk_message_lu_par FOREIGN KEY (lu_par_utilisateur_id) REFERENCES utilisateur(id);

ALTER TABLE message_planifie
    ADD CONSTRAINT fk_message_planifie_expediteur FOREIGN KEY (expediteur_utilisateur_id) REFERENCES utilisateur(id);

ALTER TABLE historique_modification
    ADD CONSTRAINT fk_historique_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id);

ALTER TABLE chantier_utilisateur
    ADD CONSTRAINT fk_chantier_utilisateur_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id);

-- ----------------------------------------------------------------------------
-- Données de référence extraites du site legacy (pays, corps de métier,
-- fonctions salarié, organismes de contrôle) — données réelles utilisées en
-- production. Extraction en lecture seule le 2026-07-30.
-- ----------------------------------------------------------------------------

-- Pays (235 lignes)
INSERT INTO pays (code_iso, nom, zone) VALUES
    ('AF', 'Afghanistan', 'HORS_UE'),
    ('ZA', 'Afrique du Sud', 'HORS_UE'),
    ('AL', 'Albanie', 'HORS_UE'),
    ('DZ', 'Algérie', 'HORS_UE'),
    ('DE', 'Allemagne', 'UE'),
    ('AD', 'Andorre', 'HORS_UE'),
    ('AO', 'Angola', 'HORS_UE'),
    ('AI', 'Anguilla', 'HORS_UE'),
    ('AG', 'Antigua-et-Barbuda', 'HORS_UE'),
    ('AN', 'Antilles Néerlandaises', 'HORS_UE'),
    ('SA', 'Arabie Saoudite', 'HORS_UE'),
    ('AR', 'Argentine', 'HORS_UE'),
    ('AM', 'Arménie', 'HORS_UE'),
    ('AW', 'Aruba', 'HORS_UE'),
    ('AU', 'Australie', 'HORS_UE'),
    ('AT', 'Autriche', 'UE'),
    ('AZ', 'Azerbaïdjan', 'HORS_UE'),
    ('BS', 'Bahamas', 'HORS_UE'),
    ('BH', 'Bahreïn', 'HORS_UE'),
    ('BD', 'Bangladesh', 'HORS_UE'),
    ('BB', 'Barbade', 'HORS_UE'),
    ('BE', 'Belgique', 'UE'),
    ('BZ', 'Belize', 'HORS_UE'),
    ('BJ', 'Bénin', 'HORS_UE'),
    ('BM', 'Bermudes', 'HORS_UE'),
    ('BT', 'Bhoutan', 'HORS_UE'),
    ('BY', 'Biélorussie', 'HORS_UE'),
    ('MM', 'Birmanie (Myanmar)', 'HORS_UE'),
    ('BO', 'Bolivie', 'HORS_UE'),
    ('BA', 'Bosnie-Herzégovine', 'HORS_UE'),
    ('BW', 'Botswana', 'HORS_UE'),
    ('BR', 'Brésil', 'HORS_UE'),
    ('BN', 'Brunei', 'HORS_UE'),
    ('BG', 'Bulgarie', 'UE'),
    ('BF', 'Burkina Faso', 'HORS_UE'),
    ('BI', 'Burundi', 'HORS_UE'),
    ('KH', 'Cambodge', 'HORS_UE'),
    ('CM', 'Cameroun', 'HORS_UE'),
    ('CA', 'Canada', 'HORS_UE'),
    ('CV', 'Cap-vert', 'HORS_UE'),
    ('CL', 'Chili', 'HORS_UE'),
    ('CN', 'Chine', 'HORS_UE'),
    ('CY', 'Chypre', 'UE'),
    ('CO', 'Colombie', 'HORS_UE'),
    ('KM', 'Comores', 'HORS_UE'),
    ('KP', 'Corée du Nord', 'HORS_UE'),
    ('KR', 'Corée du Sud', 'HORS_UE'),
    ('CR', 'Costa Rica', 'HORS_UE'),
    ('CI', 'Côte d''Ivoire', 'HORS_UE'),
    ('HR', 'Croatie', 'UE'),
    ('CU', 'Cuba', 'HORS_UE'),
    ('DK', 'Danemark', 'UE'),
    ('DJ', 'Djibouti', 'HORS_UE'),
    ('DM', 'Dominique', 'HORS_UE'),
    ('EG', 'Égypte', 'HORS_UE'),
    ('AE', 'Émirats Arabes Unis', 'HORS_UE'),
    ('EC', 'Équateur', 'HORS_UE'),
    ('ER', 'Érythrée', 'HORS_UE'),
    ('ES', 'Espagne', 'UE'),
    ('EE', 'Estonie', 'UE'),
    ('FM', 'États Fédérés de Micronésie', 'HORS_UE'),
    ('US', 'États-Unis', 'HORS_UE'),
    ('ET', 'Éthiopie', 'HORS_UE'),
    ('FJ', 'Fidji', 'HORS_UE'),
    ('FI', 'Finlande', 'UE'),
    ('FR', 'France', 'FRANCE'),
    ('GA', 'Gabon', 'HORS_UE'),
    ('GM', 'Gambie', 'HORS_UE'),
    ('GE', 'Géorgie', 'HORS_UE'),
    ('GS', 'Géorgie du Sud et les Îles Sandwich du Sud', 'HORS_UE'),
    ('GH', 'Ghana', 'HORS_UE'),
    ('GI', 'Gibraltar', 'HORS_UE'),
    ('GR', 'Grèce', 'UE'),
    ('GD', 'Grenade', 'HORS_UE'),
    ('GL', 'Groenland', 'HORS_UE'),
    ('GP', 'Guadeloupe', 'HORS_UE'),
    ('GU', 'Guam', 'HORS_UE'),
    ('GT', 'Guatemala', 'HORS_UE'),
    ('GN', 'Guinée', 'HORS_UE'),
    ('GQ', 'Guinée Équatoriale', 'HORS_UE'),
    ('GW', 'Guinée-Bissau', 'HORS_UE'),
    ('GY', 'Guyana', 'HORS_UE'),
    ('GF', 'Guyane Française', 'HORS_UE'),
    ('HT', 'Haïti', 'HORS_UE'),
    ('HN', 'Honduras', 'HORS_UE'),
    ('HK', 'Hong-Kong', 'HORS_UE'),
    ('HU', 'Hongrie', 'UE'),
    ('CX', 'Île Christmas', 'HORS_UE'),
    ('IM', 'Île de Man', 'HORS_UE'),
    ('NF', 'Île Norfolk', 'HORS_UE'),
    ('AX', 'Îles Åland', 'HORS_UE'),
    ('KY', 'Îles Caïmanes', 'HORS_UE'),
    ('CC', 'Îles Cocos (Keeling)', 'HORS_UE'),
    ('CK', 'Îles Cook', 'HORS_UE'),
    ('FO', 'Îles Féroé', 'HORS_UE'),
    ('FK', 'Îles Malouines', 'HORS_UE'),
    ('MP', 'Îles Mariannes du Nord', 'HORS_UE'),
    ('MH', 'Îles Marshall', 'HORS_UE'),
    ('PN', 'Îles Pitcairn', 'HORS_UE'),
    ('SB', 'Îles Salomon', 'HORS_UE'),
    ('TC', 'Îles Turks et Caïques', 'HORS_UE'),
    ('VG', 'Îles Vierges Britanniques', 'HORS_UE'),
    ('VI', 'Îles Vierges des États-Unis', 'HORS_UE'),
    ('IN', 'Inde', 'HORS_UE'),
    ('ID', 'Indonésie', 'HORS_UE'),
    ('IR', 'Iran', 'HORS_UE'),
    ('IQ', 'Iraq', 'HORS_UE'),
    ('IE', 'Irlande', 'UE'),
    ('IS', 'Islande', 'HORS_UE'),
    ('IL', 'Israël', 'HORS_UE'),
    ('IT', 'Italie', 'UE'),
    ('JM', 'Jamaïque', 'HORS_UE'),
    ('JP', 'Japon', 'HORS_UE'),
    ('JO', 'Jordanie', 'HORS_UE'),
    ('KZ', 'Kazakhstan', 'HORS_UE'),
    ('KE', 'Kenya', 'HORS_UE'),
    ('KG', 'Kirghizistan', 'HORS_UE'),
    ('KI', 'Kiribati', 'HORS_UE'),
    ('KW', 'Koweït', 'HORS_UE'),
    ('LA', 'Laos', 'HORS_UE'),
    ('VA', 'Le Vatican', 'HORS_UE'),
    ('LS', 'Lesotho', 'HORS_UE'),
    ('LV', 'Lettonie', 'UE'),
    ('LB', 'Liban', 'HORS_UE'),
    ('LR', 'Libéria', 'HORS_UE'),
    ('LY', 'Libye', 'HORS_UE'),
    ('LI', 'Liechtenstein', 'HORS_UE'),
    ('LT', 'Lituanie', 'UE'),
    ('LU', 'Luxembourg', 'UE'),
    ('MO', 'Macao', 'HORS_UE'),
    ('MG', 'Madagascar', 'HORS_UE'),
    ('MY', 'Malaisie', 'HORS_UE'),
    ('MW', 'Malawi', 'HORS_UE'),
    ('MV', 'Maldives', 'HORS_UE'),
    ('ML', 'Mali', 'HORS_UE'),
    ('MT', 'Malte', 'UE'),
    ('MA', 'Maroc', 'HORS_UE'),
    ('MQ', 'Martinique', 'HORS_UE'),
    ('MU', 'Maurice', 'HORS_UE'),
    ('MR', 'Mauritanie', 'HORS_UE'),
    ('YT', 'Mayotte', 'HORS_UE'),
    ('MX', 'Mexique', 'HORS_UE'),
    ('MD', 'Moldavie', 'HORS_UE'),
    ('MC', 'Monaco', 'HORS_UE'),
    ('MN', 'Mongolie', 'HORS_UE'),
    ('ME', 'Monténégro', 'HORS_UE'),
    ('MS', 'Montserrat', 'HORS_UE'),
    ('MZ', 'Mozambique', 'HORS_UE'),
    ('NA', 'Namibie', 'HORS_UE'),
    ('NR', 'Nauru', 'HORS_UE'),
    ('NP', 'Népal', 'HORS_UE'),
    ('NI', 'Nicaragua', 'HORS_UE'),
    ('NE', 'Niger', 'HORS_UE'),
    ('NG', 'Nigéria', 'HORS_UE'),
    ('NU', 'Niué', 'HORS_UE'),
    ('NO', 'Norvège', 'HORS_UE'),
    ('NC', 'Nouvelle-Calédonie', 'HORS_UE'),
    ('NZ', 'Nouvelle-Zélande', 'HORS_UE'),
    ('OM', 'Oman', 'HORS_UE'),
    ('UG', 'Ouganda', 'HORS_UE'),
    ('UZ', 'Ouzbékistan', 'HORS_UE'),
    ('PK', 'Pakistan', 'HORS_UE'),
    ('PW', 'Palaos', 'HORS_UE'),
    ('PA', 'Panama', 'HORS_UE'),
    ('PG', 'Papouasie-Nouvelle-Guinée', 'HORS_UE'),
    ('PY', 'Paraguay', 'HORS_UE'),
    ('NL', 'Pays-Bas', 'UE'),
    ('PE', 'Pérou', 'HORS_UE'),
    ('PH', 'Philippines', 'HORS_UE'),
    ('PL', 'Pologne', 'UE'),
    ('PF', 'Polynésie Française', 'HORS_UE'),
    ('PR', 'Porto Rico', 'HORS_UE'),
    ('PT', 'Portugal', 'UE'),
    ('QA', 'Qatar', 'HORS_UE'),
    ('CF', 'République Centrafricaine', 'HORS_UE'),
    ('MK', 'République de Macédoine', 'HORS_UE'),
    ('CD', 'République Démocratique du Congo', 'HORS_UE'),
    ('DO', 'République Dominicaine', 'HORS_UE'),
    ('CG', 'République du Congo', 'HORS_UE'),
    ('CZ', 'République Tchèque', 'UE'),
    ('RE', 'Réunion', 'HORS_UE'),
    ('RO', 'Roumanie', 'UE'),
    ('GB', 'Royaume-Uni', 'HORS_UE'),
    ('RU', 'Russie', 'HORS_UE'),
    ('RW', 'Rwanda', 'HORS_UE'),
    ('EH', 'Sahara Occidental', 'HORS_UE'),
    ('KN', 'Saint-Kitts-et-Nevis', 'HORS_UE'),
    ('SM', 'Saint-Marin', 'HORS_UE'),
    ('PM', 'Saint-Pierre-et-Miquelon', 'HORS_UE'),
    ('VC', 'Saint-Vincent-et-les Grenadines', 'HORS_UE'),
    ('SH', 'Sainte-Hélène', 'HORS_UE'),
    ('LC', 'Sainte-Lucie', 'HORS_UE'),
    ('SV', 'Salvador', 'HORS_UE'),
    ('WS', 'Samoa', 'HORS_UE'),
    ('AS', 'Samoa Américaines', 'HORS_UE'),
    ('ST', 'Sao Tomé-et-Principe', 'HORS_UE'),
    ('SN', 'Sénégal', 'HORS_UE'),
    ('RS', 'Serbie', 'HORS_UE'),
    ('SC', 'Seychelles', 'HORS_UE'),
    ('SL', 'Sierra Leone', 'HORS_UE'),
    ('SG', 'Singapour', 'HORS_UE'),
    ('SK', 'Slovaquie', 'UE'),
    ('SI', 'Slovénie', 'UE'),
    ('SO', 'Somalie', 'HORS_UE'),
    ('SD', 'Soudan', 'HORS_UE'),
    ('LK', 'Sri Lanka', 'HORS_UE'),
    ('SE', 'Suède', 'UE'),
    ('CH', 'Suisse', 'HORS_UE'),
    ('SR', 'Suriname', 'HORS_UE'),
    ('SJ', 'Svalbard et Jan Mayen', 'HORS_UE'),
    ('SZ', 'Swaziland', 'HORS_UE'),
    ('SY', 'Syrie', 'HORS_UE'),
    ('TJ', 'Tadjikistan', 'HORS_UE'),
    ('TW', 'Taïwan', 'HORS_UE'),
    ('TZ', 'Tanzanie', 'HORS_UE'),
    ('TD', 'Tchad', 'HORS_UE'),
    ('TF', 'Terres Australes Françaises', 'HORS_UE'),
    ('TH', 'Thaïlande', 'HORS_UE'),
    ('TL', 'Timor Oriental', 'HORS_UE'),
    ('TG', 'Togo', 'HORS_UE'),
    ('TO', 'Tonga', 'HORS_UE'),
    ('TT', 'Trinité-et-Tobago', 'HORS_UE'),
    ('TN', 'Tunisie', 'HORS_UE'),
    ('TM', 'Turkménistan', 'HORS_UE'),
    ('TR', 'Turquie', 'HORS_UE'),
    ('TV', 'Tuvalu', 'HORS_UE'),
    ('UA', 'Ukraine', 'HORS_UE'),
    ('UY', 'Uruguay', 'HORS_UE'),
    ('VU', 'Vanuatu', 'HORS_UE'),
    ('VE', 'Venezuela', 'HORS_UE'),
    ('VN', 'Viet Nam', 'HORS_UE'),
    ('WF', 'Wallis et Futuna', 'HORS_UE'),
    ('YE', 'Yémen', 'HORS_UE'),
    ('ZM', 'Zambie', 'HORS_UE'),
    ('ZW', 'Zimbabwe', 'HORS_UE');

-- Corps de métiers (120 lignes)
INSERT INTO corps_de_metier (libelle) VALUES
    ('3109B - FABRICATION D''AUTRES MEUBLES ET INDUSTRIES CONNEXES DE L''AMEUBLEMENT'),
    ('4322B - Travaux d''installation d''équipements thermiques et de climatisation'),
    ('4329B - AUTRES TRAVAUX D''INSTALLATION (MONTAGE – ASSEMBLAGE - MAINTENANCE ET ENTRETIEN DE STORES, RIDEAUX, VOILAGES, TENTURES MURALES – VOLETS)'),
    ('74.90A (ACTIVITE DES ECONOMISTES DE LA CONSTRUCTION)'),
    ('ACTIVITES D''ARCHITECTURE (7111Z)'),
    ('ACTIVITES DE SECURITE PRIVEE (8010Z)'),
    ('ACTIVITES SPECIALISEES, SCIENTIFIQUES ET TECHNIQUES DIVERSES (7490B)'),
    ('AGENCEMENT DE LIEUX DE VENTE'),
    ('ANALYSES, ESSAIS ET INSPECTIONS TECHNIQUES'),
    ('AUTRES ACTIVITES DE NETTOYAGE N.C.A. (8129B) : CREATION INSTALLATION ENTRETIEN D''AQUARIUM BASSINS ET VIVIERS'),
    ('AUTRES COMMERCES DE DETAIL SPECIALISES DIVERS'),
    ('AUTRES TRAVAUX D''INSTALLATION N.C.A. (4329B)'),
    ('AUTRES TRAVAUX DE FINITION'),
    ('AUTRES TRAVAUX SPÉCIALISÉS DE CONSTRUCTION'),
    ('CAROTTAGE - SCIAGE - DÉCONSTRUCTION'),
    ('CARRELEUR'),
    ('CARROTEUR'),
    ('CHAPE FLUIDE ET ISOLATION THERMIQUE'),
    ('CHARGE D''AFFAIRES'),
    ('CHARPENTE'),
    ('CHAUFFAGE - CLIMATISATION - PLOMBERIE'),
    ('CHAUFFEUR PL'),
    ('COFFRAGE - VENTILATION'),
    ('COFFREUR'),
    ('COMMERCE DE DETAIL DE QUINCAILLERIE, PEINTURES ET VERRES EN PETITES SURFACES (MOINS DE 400 M²) (4752A)'),
    ('COMMERCE DE GROS (COMMERCE INTERENTREPRISES) DE COMPOSANTS ET D''EQUIPEMENTS ELECTRONIQUES ET DE TELECOMMUNICATION (4652Z)'),
    ('COMMERCE DE GROS (COMMERCE INTERENTREPRISES) DE FOURNITURES ET EQUIPEMENTS INDUSTRIELS DIVERS (4669B)'),
    ('COMMERCE DE GROS DE BOIS ET DE MATERIAUX DE CONSTRUCTION'),
    ('COMMERCE DE GROS DE FOURNITURES ET EQUIPEMENTS DIVERS POUR LE COMMERCE ET LES SERVICES'),
    ('COMMERCE DE GROS DE MEUBLES, DE TAPIS ET D''APPAREILS D''ECLAIRAGE'),
    ('CONCEPTION D''ENSEMBLE ET D''ASSEMBLAGE SUR SITE INDUSTRIELS D''EQUIPEMENT DE CONTROLE DE PROCESSUS INDUSTRIELS'),
    ('CONDUCTEUR D''ENGINS'),
    ('CONFINEMENT INDUSTRIEL'),
    ('CONSTRUCTION DE MAISONS INDIVIDUELLES'),
    ('CONSTRUCTION ET RÉNOVATION'),
    ('CUISINISTE'),
    ('CUVELAGE'),
    ('DEMOLISSEUR'),
    ('DEMOLITION - TERRASSEMENT'),
    ('DÉMOLITIONS - FONDATIONS - MAÇONNERIE'),
    ('DERATISEUR'),
    ('DESAMIANTAGE'),
    ('DESIAMANTE'),
    ('DIAGNOSTIC RÉHABILITATION RÉSEAU'),
    ('ELECTRICITE'),
    ('ENDUISEUR'),
    ('ENTREPRISE GÉNÉRALE DU BÂTIMENT'),
    ('ETANCHEITE'),
    ('Fabrication de briques, tuiles et autres produits de construction en terre cuite (2332)'),
    ('FABRICATION DE CHARPENTES ET D''AUTRES MENUISERIES'),
    ('FABRICATION DE SIEGES D''AMEUBLEMENT D''INTERIEUR (3109A)'),
    ('FABRICATION DE STRUCTURES METALLIQUES ET DE PARTIES DE STRUCTURES (2511Z)'),
    ('FABRICATION DE STRUCTURES METALLIQUES ET PARTIES ASSEMBLEES DE STRUCTURES'),
    ('FACADIER'),
    ('FACONNIER DE CHEMINEES'),
    ('FERRAILLEUR'),
    ('FORMATION CONTINUE D''ADULTES (8559A)'),
    ('FOURNITURE ET POSE DE GAINES'),
    ('GRUTIER'),
    ('IMPRIMERIE ET REPRODUCTION D''ENREGISTREMENTS / ACTIVITES DE PRE-PRESSE'),
    ('IMPRIMEUR'),
    ('INGENIERIE, ETUDES TECHNIQUES (7112B)'),
    ('INGENIERIE, ETUDES TECHNIQUES (7112B) & FORAGE D’EAU'),
    ('INSTALLATION D''EQUIPEMENTS ELECTRIQUES, DE MATERIELS ELECTRONIQUES ET OPTIQUES OU D''AUTRES MATERIELS (3320DZ)'),
    ('INSTALLATION DE STRUCTURES METALLIQUES, CHAUDRONNEES ET DE TUYAUTERIE'),
    ('INSTALLATION ET MAINTENANCE DE SPRINKLERS'),
    ('JARDINIER'),
    ('LIVRAISON DE MARCHANDISE (MARBRE etc...)'),
    ('MACON'),
    ('MACONNERIE, PEINTURE, ETANCHEITE'),
    ('MANOEUVRE TRAVAUX PUBLICS'),
    ('MENUISERIE'),
    ('MENUISERIE ALUMINIUM'),
    ('MENUISERIE PVC'),
    ('METALLURGISTE'),
    ('MIROITEUR'),
    ('MONTEUR - ASSEMBLEUR DE MEUBLE'),
    ('MONTEUR ASCENSEUR'),
    ('MONTEUR ECHAFAUDAGE'),
    ('NETTOYAGE - PREOPRETE - HYGIENE'),
    ('NETTOYAGE COURANT DES BATIMENTS'),
    ('PARQUET'),
    ('PAYSAGISTE'),
    ('PEINTURE'),
    ('PEINTURE ET DECORATION D''INTERIEUR'),
    ('PEINTURE ET POSE DE VERRE'),
    ('PLAQUISTE'),
    ('PLATRIER'),
    ('PLOMBIER'),
    ('POSE DE CLOISONS MODULAIRES VITREES'),
    ('POSE ET L''INSTALLATION D''ECHAFAUDAGES, STRUCTURES D''ETAIEMENT, ESCALIERS PREFABRIQUES, COUVERTURES PROVISOIRES ET STRUCTURES TUBULAIRES'),
    ('REVETEMENT SOLS ET MURS'),
    ('SERRURERIE'),
    ('SERVICES RELATIFS AUX BATIMENTS ET AMENAGEMENT PAYSAGER / AUTRES ACTIVITES DE NETTOYAGE DES BATIMENTS ET NETTOYAGE INDUSTRIEL'),
    ('SOUTENEMENT'),
    ('STAFFEUR'),
    ('TAILLE, FAÇONNAGE ET FINISSAGE DE PIERRES (2370Z)'),
    ('TERRASEMENT'),
    ('TRANSFORMATION ET FINITION DE PRODUITS EN PLASTIQUE'),
    ('TRANSPORT'),
    ('TRAVAUX ACROBATIQUE'),
    ('TRAVAUX D''ETANCHEIFICATION (4399A)'),
    ('TRAVAUX D''INSTALLATION D''EAU ET DE GAZ EN TOUS LOCAUX'),
    ('TRAVAUX D''INSTALLATION D''EQUIPEMENTS THERMIQUES ET DE CLIMATISATION'),
    ('TRAVAUX D''INSTALLATION ELECTRIQUE DANS TOUS LOCAUX'),
    ('TRAVAUX D''ISOLATION ET SOLUTION CALORIFUGE DES RESEAUX AEROLIQUES ET HYDROLIQUES, EAU GLACEE/EAU CHAUDE.'),
    ('TRAVAUX D''ISOLATION INDUSTRIELLE ET BATIEMENT, ISOLATION THERMIQUE, FRIGORIFIQUE, PHONIQUE'),
    ('TRAVAUX DE CONSTRUCTION SPECIALISES / AUTRES TRAVAUX SPECIALISES DE CONSTRUCTION'),
    ('TRAVAUX DE CONSTRUCTION SPECIALISES / TRAVAUX DE COUVERTURE PAR ELEMENTS'),
    ('TRAVAUX DE CONSTRUCTION SPECIALISES / TRAVAUX DE MONTAGE DE STRUCTURES METALLIQUES'),
    ('TRAVAUX DE MAÇONNERIE GÉNÉRALE ET GROS OEUVRE DE BÂTIMENT'),
    ('TRAVAUX DE MAÇONNERIE GENERALE ET GROS ŒUVRE DE BATIMENT'),
    ('TRAVAUX DE MENUISERIE BOIS ET PVC'),
    ('TRAVAUX DE MENUISERIE METALLIQUE ET SERRURERIE'),
    ('TRAVAUX DE PEINTURE ET VITRERIE (4334Z)'),
    ('TRAVAUX DE PLATRERIE (4331Z)'),
    ('TRAVAUX DE REVETEMENT DES SOLS ET DES MURS (4333ZZ)'),
    ('TRAVAUX DE SIGNALISATION ROUTIERE DE MARQUAGE AU SOL APPLICATION ET PROTECTION ROUTIERE'),
    ('VENTE ET INSTALLATION DE CHEMINEES'),
    ('VOIRIE ET RESEAU DIVERS');

-- Organismes de contrôle (9 lignes)
INSERT INTO controle_tiers (nom) VALUES
    ('AGENT SCI BERLUGANE'),
    ('BENDIHA Afaf'),
    ('GENDARMERIE'),
    ('INSPECTION DU TRAVAIL'),
    ('Kamal Essajid'),
    ('MAXANT BENJAMIN'),
    ('RENTERO Davy'),
    ('ROBINET Jasone'),
    ('Service de Prévention et de Sécurité Ext.');

-- Fonctions salarié (431 lignes)
INSERT INTO salarie_fonction (libelle) VALUES
    ('ADJOINT LOGISTIQUE'),
    ('ADJOINT RESPONSABLE D''AFFAIRES'),
    ('ADMINISTRATEUR DELEGUE'),
    ('AGENCEMENT'),
    ('AGENT ADMINISTRATIF'),
    ('AGENT COMMERCIAL'),
    ('AGENT D''ENTRETIEN'),
    ('AGENT DE NETTOYAGE'),
    ('AGENT DE PROPRETE'),
    ('AGENT DE SECURITE'),
    ('AGENT DE SECURITE SSIAP1'),
    ('AGENT DE SERVICE CONFIRME'),
    ('AGENT TECHNIQUE'),
    ('AIDE - FOREUR'),
    ('AIDE AGENCEUR'),
    ('AIDE BOISEUR'),
    ('AIDE CALORIFUGEUR'),
    ('AIDE CARRELEUR'),
    ('AIDE CHARPENTIER'),
    ('AIDE CHEF DE CHANTIER'),
    ('AIDE COFFREUR'),
    ('AIDE CONDUCTEUR DE TRAVAUX'),
    ('AIDE CORDISTE'),
    ('AIDE COUVREUR'),
    ('AIDE ELECTRICIEN'),
    ('AIDE ETANCHEUR'),
    ('AIDE FERRAILLEUR'),
    ('AIDE FOREUR'),
    ('AIDE FOREUR-NACELLISTE'),
    ('AIDE GAINEUR'),
    ('AIDE MACON'),
    ('AIDE MARBRIER'),
    ('AIDE MENUISIER'),
    ('AIDE MENUISIER ALUMINIUM'),
    ('AIDE MONTEUR'),
    ('AIDE PAYSAGISTE'),
    ('AIDE PLAQUISTE'),
    ('AIDE PLOMBIER'),
    ('AIDE PLOMBIER CHAUFFAGISTE'),
    ('AIDE POSEUR'),
    ('AIDE SCIEUR'),
    ('AIDE TUYAUTEUR'),
    ('AIDE-MONTEUR D''ECHAFAUDAGES'),
    ('AIDE-STAFFEUR'),
    ('APPLICATEUR RESINE'),
    ('APPRENTI'),
    ('APPRENTI CHARPENTIER'),
    ('APPRENTI CHEF DE CHANTIER'),
    ('APPRENTI COUVREUR'),
    ('APPRENTI ELECTRICIEN'),
    ('APPRENTI MONTEUR'),
    ('APPRENTI PLOMBIER'),
    ('ARCHITECTE'),
    ('ARCHITECTE INTERIEUR'),
    ('ARCHITECTE PROJETEUR'),
    ('ARTISAN'),
    ('ASSISTANT CHARGE D''AFFAIRES'),
    ('ASSISTANT CHEF DE CHANTIER'),
    ('ASSISTANT COFFREUR'),
    ('ASSISTANT CONCUCTEUR DE TRAVAUX'),
    ('ASSISTANT TECHNIQUE'),
    ('ASSISTANT(E) INGENIEUR'),
    ('ASSISTANTE'),
    ('ASSISTANTE ADMINISTRATIVE'),
    ('ASSISTANTE CHEF DE PROJET AGENCEMENT'),
    ('ASSISTANTE DE DIRECTION'),
    ('ASSISTANTE DECORATION'),
    ('ASSISTANTE DIRECTEURS GRANDS PROJETS'),
    ('ASSISTANTE TECHNIQUE GED'),
    ('ASSISTANTE TRAVAUX'),
    ('ASSOCIE GERANT'),
    ('BANCHEUR'),
    ('BARDEUR'),
    ('BOISEUR'),
    ('CADRE'),
    ('CADRE TECHNIQUE'),
    ('CALORIFUGEUR'),
    ('CANALISATEUR'),
    ('CARISTE'),
    ('CAROLIFUGEUR'),
    ('CAROTTEUR'),
    ('CARRELEUR'),
    ('CARRELEUR - PLAQUISTE'),
    ('CARRELEUR-JOINTEUR'),
    ('CARROTTEUR'),
    ('CHAPISTE'),
    ('CHARGE D'' AFFAIRES TOUT CORPS D''ETAT'),
    ('CHARGE D''AFFAIRE'),
    ('CHARGE D''AFFAIRES MAINTENANCE'),
    ('CHARGE DE MISSION'),
    ('CHARGE DE PROJET'),
    ('CHARGEE DE RESSOURCES HUMAINES'),
    ('CHARGER D''OPERATION'),
    ('CHARGEUR SUR CHENILLES'),
    ('CHARPENTIER'),
    ('CHARPENTIER METALLIQUE'),
    ('CHAUDRONNIER'),
    ('CHAUFFEUR'),
    ('CHAUFFEUR - MANIPULATEUR'),
    ('CHAUFFEUR - MANUTENTIONNAIRE'),
    ('CHAUFFEUR GRUTTIER'),
    ('CHAUFFEUR PL'),
    ('CHAUFFEUR-LIVREUR'),
    ('CHEF ADJOINT'),
    ('CHEF D EQUIPE POLISSEUR'),
    ('CHEF D''ATELIER'),
    ('CHEF D''EQUIPE'),
    ('CHEF D''EQUIPE - COFFREUR'),
    ('CHEF D''EQUIPE CALORIFUGEUR'),
    ('CHEF D''EQUIPE DE POSEURS MARBRES'),
    ('CHEF D''EQUIPE ELECTRICIEN'),
    ('CHEF D''EQUIPE LOGISTICIEN'),
    ('CHEF D''EQUIPE MENUISIER ALUMINIUM'),
    ('CHEF D''EQUIPE PAYSAGISTE'),
    ('CHEF D''EQUIPE PLOMBIERS'),
    ('CHEF D''EQUIPE POSEUR'),
    ('CHEF D''EQUIPE QUALITE'),
    ('CHEF D''EQUIPE STAFFEUR'),
    ('CHEF DE CHANTIER'),
    ('CHEF DE CHANTIER CALORIFUGEUR'),
    ('CHEF DE CHANTIER STAGIAIRE'),
    ('CHEF DE MACHINE'),
    ('CHEF DE MANOEUVRE'),
    ('CHEF DE PROJET AGENCEMENT'),
    ('CHEF DE PROJET TECHNIQUE'),
    ('CHEF DE PROJETS'),
    ('CHEF DE SECTEUR'),
    ('CHEF DE SECTEUR REHABI. & RENOV.'),
    ('CHEF ELECTRICIEN'),
    ('CHEF ELECTRO MECANICIEN'),
    ('CHEF FERRAILLEUR'),
    ('CHEF FINITION'),
    ('CHEF LOGISTIQUE'),
    ('CLIMATICIEN'),
    ('CO GERANT'),
    ('CO-GERANT'),
    ('COFFREUR'),
    ('COFFREUR BOISEUR'),
    ('COFFREUR BRANCHEUR'),
    ('COMPAGNON'),
    ('COMPAGNON PROFESSIONNEL'),
    ('COMPAGNON QUALIFIE'),
    ('COMPTABLE'),
    ('CONCEPTEUR CAO ET BIM'),
    ('CONDUCTEUR /CHARGEUR SUR CHENILLES'),
    ('CONDUCTEUR D''ENGIN'),
    ('CONDUCTEUR D''ENGINS DE LEVAGE'),
    ('CONDUCTEUR DE PELLE'),
    ('CONDUCTEUR DE POIDS LOURDS'),
    ('CONDUCTEUR DE TRAVAUX'),
    ('CONDUCTRICE DE TRAVAUX'),
    ('CONSEILLERE EN INSERTION PROFESSIONNELLE'),
    ('CONTREMAITRE'),
    ('CONTROLEUR'),
    ('COORDINATEUR'),
    ('COORDINATEUR PROJET'),
    ('CORDISTE'),
    ('COUVREUR'),
    ('COUVREUR - ZINGUEUR'),
    ('CUISINISTE MONTEUR'),
    ('CUVELEUR'),
    ('DECAPEUR'),
    ('DECORATEUR'),
    ('DECORATRICE'),
    ('DECOUPEUR'),
    ('DEMENAGEUR'),
    ('DEMENTELEUR'),
    ('DEMOLISSEUR'),
    ('DEMOUSTIQUEUR'),
    ('DEPANNEUR ASCENSEUR'),
    ('DESAMIANTEUR'),
    ('DESSINATEUR'),
    ('DESSINATEUR - SUIVI TRAVAUX'),
    ('DESSINATEUR BUREAU D''ETUDE'),
    ('DESSINATEUR-PROJETEUR'),
    ('DIRECTEUR'),
    ('DIRECTEUR ADJOINT'),
    ('DIRECTEUR CPI'),
    ('DIRECTEUR D''AGENCE'),
    ('DIRECTEUR D''EXPLOITATION'),
    ('DIRECTEUR DE PROJET'),
    ('DIRECTEUR DE SYNTHESE'),
    ('DIRECTEUR DE TRAVAUX'),
    ('DIRECTEUR GENERAL'),
    ('DIRECTEUR GENERAL ASSOCIE'),
    ('DIRECTEUR LOGISTIQUE'),
    ('DIRECTEUR OPERATIONNEL'),
    ('DIRECTEUR TECHNIQUE'),
    ('DIRI'),
    ('DIRIGEANT'),
    ('EBENISTE'),
    ('ELAGUEUR'),
    ('ELECTRICIEN'),
    ('ELECTROTECHNICIEN'),
    ('ENCADRANT'),
    ('Enduiseur'),
    ('ESTRACTEUR'),
    ('ETANCHEUR'),
    ('ETUDE - DIAGNOSTIQUEUR DÉSAMIANTAGE'),
    ('ETUDE - DIAGNOSTIQUEUR ELECTRICITE'),
    ('EXPERT'),
    ('FABRICATEUR'),
    ('FACADIER'),
    ('FERRAILLEUR'),
    ('FERRONNIER'),
    ('FINISSEUR'),
    ('FINISSEUR MARBRE'),
    ('FLEURISTE'),
    ('FOREUR'),
    ('FRIGORISTE'),
    ('FUMISTE'),
    ('GAINEUR'),
    ('GARDIEN'),
    ('GARDIEN DE PREVENTION'),
    ('GEOMETRE'),
    ('GERANT'),
    ('GERANT - FOREUR'),
    ('GERANT - FRIGORISTE'),
    ('GERANT CHARPENTIER'),
    ('GERANT ETANCHEUR'),
    ('GESTION DE LA CIRCULATION'),
    ('GESTIONNAIRE COMPTABLE'),
    ('GESTIONNAIRE D''APPROVISIONNEMENT'),
    ('GESTIONNAIRE DOCUMENTAIRE'),
    ('GOUDRONNEUR - ASPHALTEUR'),
    ('GRUTIER'),
    ('HOMME TRAFIC'),
    ('INGENIEUR CIVIL'),
    ('INGENIEUR DE SYNTHESE'),
    ('INGENIEUR ETUDES'),
    ('INGENIEUR GENIE CLIMATIQUE'),
    ('INGENIEUR INTEGRATEUR'),
    ('INGENIEUR SYSTEME'),
    ('INGENIEUR TRAVAUX'),
    ('INJECTEUR - RELEVEUR'),
    ('INSTALLATEUR'),
    ('INSTALLATEUR ASCENSEUR'),
    ('INSTALLATEUR ASCENSORISTE'),
    ('INSTALLATEUR DE CHEMINEE'),
    ('JARDINIER'),
    ('JOINTEUR'),
    ('LAVEUR DE VITRE'),
    ('LIFTIER'),
    ('LIVREUR'),
    ('LOGISTICIEN'),
    ('MACON'),
    ('MACON - VRD'),
    ('MACON COFFREUR'),
    ('MACON PAYSAGISTE'),
    ('MAÇON-ETANCHEUR'),
    ('MAGASINIER'),
    ('MAITRE COMPAGNON'),
    ('MAITRISE D''OUVRAGE'),
    ('MANOEUVRE'),
    ('MANOEUVRE LOGISTIQUE'),
    ('MANUTENTIONNAIRE'),
    ('MARBRIER'),
    ('MARBRIER-CARRELEUR'),
    ('MECANICIEN'),
    ('MENUISIER'),
    ('MENUISIER ALUMINIUM'),
    ('MENUISIER ALUMINIUM GERANT'),
    ('MENUISIER BOIS'),
    ('MENUISIER PARQUETEUR'),
    ('MENUISIER POSEUR'),
    ('METALLIER'),
    ('METREUR'),
    ('METTEUR AU POINT'),
    ('MONTEUR'),
    ('MONTEUR - ASCENSEUR'),
    ('MONTEUR - ASSEMBLEUR DE MEUBLE'),
    ('MONTEUR - EPMR'),
    ('MONTEUR - SAUNA'),
    ('MONTEUR CUISINISTE'),
    ('MONTEUR DE GAINES'),
    ('MONTEUR DE GRUE'),
    ('MONTEUR DE MEUBLE'),
    ('MONTEUR DE VOILAGE'),
    ('MONTEUR ECHAFAUDAGE'),
    ('MONTEUR SPRINKLER'),
    ('MONTEUR-CABLEUR'),
    ('MOUSSISTE'),
    ('NACELLISTE'),
    ('NETTOYEUR'),
    ('OPÉRATEUR DESAMIANTAGE'),
    ('OPERATEUR POMPE BETON STATION'),
    ('OUVRIER D EXECUTION'),
    ('OUVRIER D''EXECUTION'),
    ('OUVRIER METALLIER'),
    ('OUVRIER MOUSSISTE ET CHAPISTE'),
    ('OUVRIER PAYSAGISTE'),
    ('OUVRIER POLYVALENT'),
    ('OUVRIER QUALIFIE'),
    ('OUVRIER ROUTIER'),
    ('OUVRIERE SPECIALISEE'),
    ('PARQUETEUR'),
    ('PAYSAGISTE'),
    ('PEINTRE'),
    ('PEINTRE DECORATEUR'),
    ('PEINTRE ENDUISEUR'),
    ('PEINTRE GERANT'),
    ('PELISTE'),
    ('PHOTOGRAFFE'),
    ('PISCINISTE'),
    ('PLAQUISTE'),
    ('PLAQUISTE-CHAPISTE'),
    ('PLATERIE-PLAQUISTE'),
    ('PLATRIER'),
    ('PLOMBIER'),
    ('PLOMBIER - CHAUFFAGISTE'),
    ('PLOMBIER - CHAUFFAGISTE - SOUDEUR'),
    ('PLOMBIER - TUYAUTEUR'),
    ('POLISSEUR'),
    ('POMPISTE'),
    ('PONCEUR'),
    ('PONCEUR DE BETON'),
    ('PONCEUR DE MARBRE'),
    ('PONTIER SOL'),
    ('POSEUR'),
    ('POSEUR ALUMINIUM'),
    ('POSEUR DE CLOTURES'),
    ('POSEUR DE GAINES'),
    ('POSEUR DE MOSAIQUE'),
    ('POSEUR DE PARQUET'),
    ('POSEUR DE PIERRE'),
    ('POSEUR DE REVETEMENT MINERAL'),
    ('POSEUR FACADES VITREES'),
    ('POSEUR MENUISERIE SERRURERIE'),
    ('POSEUR MIROITIER'),
    ('POSEUR MUR MOBILE'),
    ('POSEUR SOLS SOUPLES'),
    ('POSEUR SPA'),
    ('PRÉPARATEUR DE CHANTIER'),
    ('PRESIDENT'),
    ('PRESSING'),
    ('PREVENTEUR'),
    ('PROGRAMEUR'),
    ('PROJECT MANAGER'),
    ('PROJECTEUR BETON'),
    ('PROJECTEUR SOUFFLEUR'),
    ('PROJETEUR'),
    ('PROTECTION INCENDIE'),
    ('REGLEUR'),
    ('REGLEUR AU RATEAU - TERRASSIER'),
    ('REGLEUR FINISSEUR'),
    ('REPARATEUR MODERNISATION'),
    ('RESPONSABLE'),
    ('RESPONSABLE BUREAU D''ETUDES'),
    ('RESPONSABLE CHANTIER'),
    ('RESPONSABLE CHEF DE POSES'),
    ('RESPONSABLE CLOS-COUVERT'),
    ('RESPONSABLE D''AFFAIRES'),
    ('RESPONSABLE D''AGENCE ADJOINT'),
    ('RESPONSABLE D''EXPLOITATION'),
    ('RESPONSABLE D''EXPLOITATION IAC'),
    ('RESPONSABLE DE CHANTIER'),
    ('RESPONSABLE DE TRAVAUX DE SERRURERIE'),
    ('RESPONSABLE D’EXECUTION'),
    ('RESPONSABLE ESSAI FINAUX'),
    ('RESPONSABLE ETANCHEUR'),
    ('RESPONSABLE ETUDE'),
    ('RESPONSABLE GAINEUR'),
    ('RESPONSABLE INSTALLATEUR'),
    ('RESPONSABLE JURIDIQUE ET ENVIRONNEMENT'),
    ('RESPONSABLE LOGISTIQUE'),
    ('RESPONSABLE MATERIEL'),
    ('RESPONSABLE POSEUR'),
    ('RESPONSABLE PROJETS'),
    ('RESPONSABLE QSE (Quali./Sécu./Environn.)'),
    ('RESPONSABLE QUALITE HYGIENE SECURITE ENVIRONNEMENT'),
    ('RESPONSABLE QUALITE LOGISTIQUE'),
    ('RESPONSABLE SAV'),
    ('RESPONSABLE SYNTHESE'),
    ('RESPONSABLE TECHNIQUE'),
    ('RESPONSABLE TRAVAUX'),
    ('RESTAURATEUR D''ART'),
    ('SCIEUR - CABLEUR'),
    ('SCIEUR - CARROTTEUR'),
    ('SECRETAIRE ADMINISTRATIVE'),
    ('SECRETAIRE DE DIRECTION'),
    ('SECRETARIAT - STANDARD'),
    ('SERRURIER'),
    ('SERRURIER-METALLIER'),
    ('SERVICE TRAVAUX'),
    ('SONDEUR'),
    ('SOUDEUR'),
    ('STAFFEUR'),
    ('STAFFEUR / PLAQUISTE'),
    ('STAGIAIRE'),
    ('STAGIAIRE CONTDUCTEUR DE TRAVAUX'),
    ('STORISTE'),
    ('Supervision'),
    ('TAPISSIER'),
    ('TECH. DE MONTAGE - SÉCURITÉ ELECTRONIQUE'),
    ('TECHNICIEN'),
    ('TECHNICIEN - MISE EN SERVICE'),
    ('TECHNICIEN ACOUSTIQUE'),
    ('TECHNICIEN AIRE DE JEUX'),
    ('TECHNICIEN BUREAU D''ETUDES'),
    ('TECHNICIEN CLIMATISATION'),
    ('TECHNICIEN COMMERCIAL'),
    ('TECHNICIEN CORDISTE'),
    ('TECHNICIEN COURANT FAIBLE'),
    ('TECHNICIEN CVC - CHAUFFAGE VENTILATION CLIMATISATION'),
    ('TECHNICIEN DE CONFINEMENT'),
    ('TECHNICIEN DE MAINTENANCE'),
    ('TECHNICIEN DE MONTAGE'),
    ('TECHNICIEN DE RENFORCEMENT'),
    ('TECHNICIEN DOMOTIQUE'),
    ('TECHNICIEN EN MARBRERIE'),
    ('TECHNICIEN FRIGORISTE'),
    ('TECHNICIEN GROUPE ELECTROGENE'),
    ('TECHNICIEN HYDRAULIQUE'),
    ('TECHNICIEN LUMIERE'),
    ('TECHNICIEN MONTE-CHARGE'),
    ('TECHNICIEN PREVENTIF'),
    ('TECHNICIEN PROGRAMMEUR'),
    ('TECHNICIEN REPARATEUR'),
    ('TECHNICIEN RESEAU'),
    ('TECHNICIEN SPA'),
    ('TECHNICIEN SPECIALISE'),
    ('TERRASEUR'),
    ('TOLIER - CALORIFUGEUR'),
    ('TRACEUR'),
    ('TRAVAUX ACROBATIQUE'),
    ('TUYAUTEUR'),
    ('TUYAUTEUR - SOUDEUR'),
    ('VENDEUR - AGENCEUR'),
    ('VERNISSEUR'),
    ('VISITEUR'),
    ('ZINGUEUR');

-- Types de documents / modèles (106 lignes) — pas de restriction par corps de
-- métier/pays capturée (non affichée dans le listing legacy, aurait nécessité 106 pages de détail)
INSERT INTO type_document (libelle, cible, obligatoire, format, date_debut_validite_requise, date_fin_validite_requise, nb_jours_relance_avant, nb_jours_recurrence, retire_accord_acces) VALUES
    ('(*) LETTRE DE FIN DE MISSION', 'SALARIE', FALSE, 'PDF', FALSE, TRUE, 0, 0, TRUE),
    ('(*) Lettre de fin de mission', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('001 - 1er Agrément ou lettre d''acceptation d''un sous-traitant / intervenant / prestataire validé(e) par le MO (Attention : cette demande ne concerne que le client qui devra nous fournir l''agrément définitif ou l''accord d''intervention)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('001.1 - 2ème Agrément ou lettre d''acceptation d''un sous-traitant / intervenant / prestataire validé(e) par le MO (Attention : cette demande ne concerne que le client qui devra nous fournir l''agrément définitif ou l''accord d''intervention)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('001.2 - Pré-Agrément validé par les parties (1er lot)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('001.3 - Pré-Agrément validé par les parties (2ème lot)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('001.4 : Accord d''intervention de l''entreprise confirmé par le donneur d''Ordre M. POINSIGNON ou ses préposés', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('001.5 : Accord d''intervention de l''entreprise confirmé par le donneur d''Ordre ou ses préposés', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('001.6 - Convention de prêt de Main d''œuvre', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('001.6.1 - Accord et validation de la convention de prêt de main par le client', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('002 - Attestation sur l''Honneur " Déclaration des Sous-traitants "', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('003 - Assurance Décennale datée de - de 6 mois (garantie légale des dommages à l''ouvrage et leurs conséquences durant 10 années après réception des travaux)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('003.1 - Justificatif dispense Assurance Décennale (garantie légale des dommages à l''ouvrage et leurs conséquences durant 10 années après réception des travaux)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('004 - Assurance Responsabilité Civile datée de - de 6 mois (dommages corporels causées aux clients, aux salariés, aux tiers survenant après livraison des travaux effectués ou durant l''exploitation des activités assurées)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('004.1 - Assurance Responsabilité Civile (pièces complémentaires)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('004.2 - Assurance Responsabilité Civile (pièces complémentaires, conditions générales applicables etc..)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('005 - Attestation de régularité des Congés et Intempéries BTP de - de 6 mois (Article L.2141-2 du code de la commande publique)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('005.1 - Attestation sur l''Honneur de ne pas dépendre de la Caisse de congés payés du BTP et régler directement les congés payés à ses salariés', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('006 - Attestation de régularité de cotisations contrat prévoyance (de - de 6 mois)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('007 - Attestation de régularité de cotisations contrat retraite (- de 6 mois)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('008 - Attestation de régularité des déclarations fiscales ( - de 6 mois)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('008.1 - Attestation de régularité des déclarations fiscales " Société Mère " ( - de 6 mois)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('008.2 - Attestation sur l''honneur expédiée par nos services de "non-assujettissement à la TVA"', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('008.3 - Accusé de réception de la déclaration du Chiffre d''affaires de - de 6 mois effectuée auprès de l''URSSAF', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('009 - Attestation Sociale Urssaf / MSA / CSS (cotisation de - de 6 mois)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('009.1 - Attestation sur l''honneur relative à l''absence d''effectifs au sein de l''entreprise expédiée par nos services', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('010 - Extrait KBIS / E.BIS / RCS / RCI (- de 6 mois)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('010.1 - Extrait d''Inscription au Répertoire des Métiers', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('010.1 : Attestation de domiciliation pour les sociétés non établies dans le pays d''intervention', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('011 - Contrat de prestation de services signé entre les parties', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('011.1 - Contrat de Sous-traitance établi entre l''Entreprise Principale et l''entreprise de 1er rang', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('011.2 - Contrat de Sous-traitance établi entre l''Entreprise de 1er et 2ème rang', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('011.3 - Bon de commande signé entre les parties', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('011.3 : Ordre de service émis par le Client', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('012 - PPSPS (obligatoirement daté, tamponné et signé)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('012.1 - PPSPS (Initial) comprenant les mesures sanitaires liées au Covid-19', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('013-Fournir l''ANNEXE 1 expédiée par nos services (doc. listant les pièces à fournir dans le cadre de la lutte contre le travail Illégal)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('014-Fournir l''ANNEXE 2 expédiée par nos services (Attestation sur l''honneur)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('015-Fournir l''ANNEXE 3 expédiée par nos services (Liste des salariés étrangers soumis à autorisation de Travail)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('016 - Attestation sur l''Honneur (établi sur feuille à en-tête) de non recours au paiement direct par le Maitre d''Ouvrage', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('017 - SOCIETE : AUTORISATION D''ASSURER DES ACTIVITES PRIVEES DE PROTECTION DES PERSONNES ET/OU DES BIENS', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('017.1 - GERANT : AUTORISATION D''ASSURER DES ACTIVITES PRIVEES DE PROTECTION DES PERSONNES ET/OU DES BIENS', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('AGREMENT DIRIGEANT (CNAPS)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('ATTESTATION D''ASSURANCE PREVOYANCE (Inarcassa : Cassa Nazionale di Previdenza ed Assistenza per gli Ingegneri ed Archittetti indépendants)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('AUTORISATION D''EMBAUCHAGE : DÉCLARATION DU SALARIE - PAYS UE / HORS UE', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('AUTORISATION D''EMBAUCHAGE/PERMIS DE TRAVAIL ou Fournir la demande d''autorisation initiale/renouv. réceptionnée et tamponnée par le Sce de l''Emploi (Validité 2 mois par nos Sces dans l''attente de l''Auto. définitive)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('AUTORISATION D''EMBAUCHAGE/PERMIS DE TRAVAIL ou Fournir la demande d''autorisation initiale/renouv. réceptionnée et tamponnée par le Sce de l''Emploi (Validité 2 mois par nos Sces dans l''attente de l''Auto. définitive)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('AUTORISATION TEMPORAIRE ACCORDEE PAR LE CLIENT (Motif : société non agréée ou en cours de validation ou Accès visiteur ou absence autorisation de chantier monégasque)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('CARTE BTP (CIBTP - site " https://www.cibtp.fr) : Justificatif de non obligation de détention', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('CARTE BTP ou Attestation Provisoire téléchargeable sur le site " https://www.cibtp.fr "', 'SALARIE', TRUE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('CARTE BTP ou Attestation Provisoire téléchargeable sur le site " https://www.cibtp.fr "', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('CARTE PROFESSIONNELLE AUTORISANT L''EXERCICE DES ACITIVTES PRIVEES DE SECURITE DELIVREE PAR LA CNAPS', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('CARTE PROFESSIONNELLE DU BATIMENT (CCPB Monaco) : Justificatif de non obligation de détention', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('CARTE PROFESSIONNELLE DU BATIMENT (CCPB Monaco) ou Attestation Employeur (à renvoyer à défaut de la Carte) pour accord d''accès temporaire (Validité 2 mois)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('CARTE PROFESSIONNELLE DU BATIMENT (CCPB Monaco) ou Attestation fournie par l''Entreprise Intérimaire (à défaut de la Carte pour accord d''accès temporaire - Validité 2 mois)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('CARTE PROFESSIONNELLE DU BATIMENT (CCPB Monaco) ou Attestation fournie par l''Entreprise Intérimaire ou l''Employeur (à défaut de la Carte pour accord d''accès temporaire - Validité 2 mois)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('CONTRAT D''INTERIM (signé par toutes les parties)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('CONTRAT DE PROFESS. / APPRENTISSAGE', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('CONVENTION DE PRET DE MAIN D''OEUVRE', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('CONVENTION DE STAGE', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('CONVENTION DE STAGE : ASSURANCE SCOLAIRE', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DETACHEMENT : AUTORISATION DE DETACHEMENT DELIVREE PAR LE SCE DE L''EMPLOI MONEGASQUE', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DETACHEMENT : FOURNIR LA DECLARATION DE DETACHEMENT (téléservice SIPSI)', 'SALARIE', TRUE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DETACHEMENT : UE V/FRANCE FOURNIR LE CERTIF. RELATIF A LA LEGISLATION SUR LA SECURITE SOCIALE APPLICABLE (Formulaire portable A1 / DA1 émis par l''INSS, l''INPS italie, CNPP Roumanie etc..)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DETACHEMENT DE +/- de 3 MOIS PORTUGAL V/MONACO : FOURNIR LE FORMULAIRE MODELE RV1021–DGSS (https://www.seg-social.pt)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DETACHEMENT FRANCE V/MONACO : FOURNIR L''AVIS DE MISSION (Formulaire S9203 - détach. - de 3 mois)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DETACHEMENT FRANCE V/MONACO : FOURNIR LE CERTIF. D''ASSUJ. (Formulaire SE138-01 - détach. + de 3 mois)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DETACHEMENT ITALIE V/MONACO : FOURNIR IMPRIMES M/I/C1 ou M/I/C2 VALIDES PAR L’INPS', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DETACHEMENT MONACO V/FRANCE : FOURNIR L''AVIS DE DETACHEMENT (Détach. - de 3 mois) ou LE CERTIF. D''ASSUJ. (Formulaire SE138-01 - Détach. + de 3 mois)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DETACHEMENT MONACO V/FRANCE : FOURNIR L''AVIS DE DETACHEMENT (Détach. - de 3 mois) ou LE CERTIF. D''ASSUJ. (Formulaire SE138-01 - Détach. + de 3 mois)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DPAE - DÉCLA. PRÉALABLE A L''EMBAUCHE (mentionner sur le document la date de fin du CDD, s''il y a lieu)', 'SALARIE', TRUE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DPAE : ATTESTATION EMPLOYEUR (à défaut de DPAE ou validation DPAE)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('DPAE : JUSTIFICATIF COMPTABLE DE DÉCLARATION ET NOMINATIF DU SALARIE (à défaut de DPAE)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('EXTRAIT D''IMMATRICULATION (Entreprise / Artisan de France) : Fournir Extrait KBIS / Justif. d''immatriculation daté(s) de - de 6 mois', 'SALARIE', TRUE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('EXTRAIT D''IMMATRICULATION - 6 MOIS (Ent. UE / HORS UE)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('EXTRAIT D''IMMATRICULATION : Estratto di iscrizione al registro del commercio e delle imprese (datée de - de 6 mois)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('FICHE MEDICALE', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('FORMATION : AIPR', 'SALARIE', TRUE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('FORMATION : CACES® (Article R. 4323-56 du code du travail)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('FORMATION : CACES® - FOURNIR ATTEST. AUTORISATION EMPLOYEUR "AUTORISATION DE CONDUITE" (Ar. R. 4323-56 du C.T)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('FORMATION : HABILITATION ELECTRIQUE (H0B0V)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('FORMATION : SST et/ou PSC', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('FR - Attestation Employeur - COVID-19 (Protection, mesures et formation des salariés)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('FR V/MC - ATTESTATION D''AFFILIA. (Caisse Sociale de Monaco CAMTI) ou ASSURANCE PRIVEE ACCIDENT CORPOREL (- DE 6 MOIS)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('GENDARMERIE : ACCORD D''ACCÈS (BADGE VERT)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('GENDARMERIE : REFUS D''ACCÈS (BADGE VERT)', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('IDENTITÉ : CARTE D’IDENTITÉ / PASSEPORT', 'SALARIE', TRUE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('IDENTITÉ : RÉCÉPISSÉ PROVISOIRE VALIDE DANS L''ATTENTE DU NOUVEAU TITRE DE SEJOUR', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('IDENTITÉ : TITRE DE SÉJOUR / VISA', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('IT. : Attestation de régularité de la situation fiscale de l’entreprise (datée de - de 6 mois)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('IT. : Attestation mentionnant le numéro de tva intracommunautaire (datée de - de 6 mois)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('IT. : Code fiscal au registre des entreprises (Codice fiscale al registro imprese)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('IT. : Estratto di iscrizione al registro del commercio e delle imprese (datée de - de 6 mois)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('IT. : « DURC FISCALE » (Certificato di Sussisteneza - Art. 17 bis – Parag. 5 – Décret Législ. du 09.07.1997 – n° 241)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('IT. : « DURC » (Documento Unico di Regolarità Contributiva), attestant que les cotisations de sécurité sociale dues à l''INPS pour le compte des salariés ont été régulièrement payées', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('IT. : « LUL » (Libro Unico del Lavoro), à présenter à l''INAIL, attestant que les primes pour la couverture des salariés contre les accidents du travail ont été régulièrement payées', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('MC - Attestation Employeur " Accueil et information Covid-19 " (Protection, Gestes barrières, mesures préventives et formation des salariés)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('MC - Autorisation de chantier (Monaco)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('MC - Dispense de l''Autorisation de chantier notifiée par le client (Monaco)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('MC - Intervention Ponctuelle de - 48 heures - Fournir copie de la correspondance d''information de votre intervention tranmise à la Direction de l''Expansion Economique de Monaco', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('PRT - Certidão Permanente (PORTUGAL)', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, FALSE),
    ('PRT. : situação contributiva regularizada perante a Segurança Social.', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('PRT. : situação tributária regularizada, nos termos do artigo 177º-A e/ou nºs 3, 6 e 13 do artigo 169º, ambos do Código de Procedimento e de Processo Tributário (CPPT).', 'ENTREPRISE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('VÉHICULE : CARTE GRISE', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('VÉHICULE : CERTIF. D''ASSURANCE AUTOMOBILE', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE),
    ('VÉHICULE : PERMIS DE CONDUIRE', 'SALARIE', FALSE, 'PDF', TRUE, TRUE, 0, 0, TRUE);

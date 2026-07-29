-- ============================================================================
-- Schéma ADMIN-CONTROL'BTP — version consolidée (remplace l'historique
-- V1..V5 issu des itérations successives, squashé car aucune donnée réelle
-- n'existait encore). Reflète l'état final validé : entités métier,
-- authentification JWT, alignement sur l'analyse de la base legacy,
-- intégrité référentielle complète, et suppression logique.
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
    libelle VARCHAR(150) NOT NULL,
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

CREATE TABLE document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_document_id UUID NOT NULL REFERENCES type_document(id),
    salarie_id UUID REFERENCES salarie(id),
    entreprise_id UUID REFERENCES entreprise(id),
    chantier_id UUID REFERENCES chantier(id),
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
-- Automatisation (schéma préparé — moteur d'exécution non branché)
-- ----------------------------------------------------------------------------

CREATE TABLE regle_automatisation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom VARCHAR(150) NOT NULL,
    evenement_declencheur VARCHAR(100) NOT NULL, -- ex: DOCUMENT_EXPIRE, FIN_MISSION_PROCHE
    condition JSONB NOT NULL DEFAULT '{}'::jsonb, -- ex: {"type_contrat": "CDD", "type_document": "CARTE_PRO"}
    action VARCHAR(50) NOT NULL, -- ex: ENVOI_EMAIL, GENERATION_DOCUMENT, NOTIFICATION
    action_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);

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

ALTER TABLE historique_modification
    ADD CONSTRAINT fk_historique_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id);

ALTER TABLE chantier_utilisateur
    ADD CONSTRAINT fk_chantier_utilisateur_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id);

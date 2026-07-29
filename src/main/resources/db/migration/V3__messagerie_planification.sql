-- ============================================================================
-- Messagerie : planification et automatisation des envois par groupe / événement
-- ============================================================================
-- La table regle_automatisation existait déjà dans le schéma initial sous une
-- forme générique (JSONB condition/action) mais n'était reliée à aucun code
-- Java. On la spécialise ici pour le cas d'usage concret demandé : des règles
-- de relance messagerie déclenchées par un événement métier (expiration d'un
-- document, contrôle de chantier à venir), ciblant un groupe de destinataires
-- ou un destinataire précis.

ALTER TABLE regle_automatisation
    DROP COLUMN condition,
    DROP COLUMN action,
    DROP COLUMN action_config,
    ADD COLUMN nb_jours_avant INT NOT NULL DEFAULT 0,
    ADD COLUMN cible_groupe VARCHAR(20) NOT NULL DEFAULT 'SPECIFIQUE',
    ADD COLUMN destinataire_type VARCHAR(20),
    ADD COLUMN destinataire_id UUID,
    ADD COLUMN sujet VARCHAR(200) NOT NULL DEFAULT '',
    ADD COLUMN contenu TEXT NOT NULL DEFAULT '',
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE regle_automatisation
    ADD CONSTRAINT chk_regle_evenement CHECK (evenement_declencheur IN ('DOCUMENT_EXPIRATION', 'CHANTIER_CONTROLE_A_VENIR')),
    ADD CONSTRAINT chk_regle_cible_groupe CHECK (cible_groupe IN ('TOUS_UTILISATEURS', 'TOUS_CLIENTS', 'TOUTES_ENTREPRISES', 'SPECIFIQUE')),
    ADD CONSTRAINT chk_regle_destinataire_type CHECK (destinataire_type IS NULL OR destinataire_type IN ('CLIENT', 'ENTREPRISE', 'UTILISATEUR')),
    ADD CONSTRAINT chk_regle_specifique CHECK (
        (cible_groupe = 'SPECIFIQUE' AND destinataire_type IS NOT NULL AND destinataire_id IS NOT NULL)
        OR (cible_groupe <> 'SPECIFIQUE' AND destinataire_type IS NULL AND destinataire_id IS NULL)
    );

CREATE TABLE message_planifie (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    regle_id UUID REFERENCES regle_automatisation(id),
    -- id du Document/Chantier ayant déclenché la règle ; sert de clé de dédoublonnage
    -- pour ne pas régénérer le même rappel à chaque scan (voir index unique ci-dessous)
    source_entity_id UUID,
    expediteur_utilisateur_id UUID REFERENCES utilisateur(id),
    cible_groupe VARCHAR(20) NOT NULL CHECK (cible_groupe IN ('TOUS_UTILISATEURS', 'TOUS_CLIENTS', 'TOUTES_ENTREPRISES', 'SPECIFIQUE')),
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

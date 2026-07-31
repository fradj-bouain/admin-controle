-- ============================================================================
-- États de documents : catalogue de motifs de refus (comme le site legacy),
-- utilisé au lieu de texte libre quand on refuse un document.
-- ============================================================================

CREATE TABLE document_etat (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titre VARCHAR(255) NOT NULL,
    par_defaut BOOLEAN NOT NULL DEFAULT FALSE,
    date_expiree BOOLEAN NOT NULL DEFAULT FALSE,
    valide_le_document BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE document ADD COLUMN document_etat_id UUID REFERENCES document_etat(id);

-- Un document d'entreprise peut désormais avoir une instance indépendante par chantier
-- (validation, refus, suppression sur un chantier n'affectent aucun autre chantier) —
-- la colonne document.chantier_id existait déjà (V1) mais n'était jusqu'ici jamais
-- renseignée par aucun écran ; ce n'est qu'un changement d'usage, pas de schéma.
--
-- Reste à représenter : les types de document demandés EN PLUS sur un chantier précis,
-- au-delà des types obligatoires globaux (type_document.obligatoire) qui continuent de
-- s'appliquer partout sans qu'aucune ligne ne soit nécessaire ici.
CREATE TABLE document_chantier_supplementaire (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_document_id UUID NOT NULL REFERENCES type_document(id),
    chantier_id UUID NOT NULL REFERENCES chantier(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_doc_chantier_supp_chantier ON document_chantier_supplementaire(chantier_id);
-- une seule règle active par (type, chantier) ; une ligne supprimée n'occupe plus la place.
CREATE UNIQUE INDEX ux_doc_chantier_supp_actif ON document_chantier_supplementaire(type_document_id, chantier_id)
    WHERE deleted_at IS NULL;

-- La liste "Affectations" (GET /entreprises/affectations) doit pouvoir compter, par lot,
-- les documents déposés pour chaque couple (entreprise, chantier) — recherche jusqu'ici
-- non indexée puisque chantier_id n'était jamais utilisé en pratique sur document.
CREATE INDEX idx_document_entreprise_chantier ON document(entreprise_id, chantier_id);

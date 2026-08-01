-- ============================================================================
-- Checklist de contrôle (accord/refus individuel par salarié, comme le site
-- legacy) + gestion complète des rapports (suppression, historique par
-- chantier). Un contrôle sur site consiste à passer en revue chaque salarié
-- présent et noter son accord/refus d'accès ; le rapport en est ensuite la
-- synthèse (compteurs dérivés de ces entrées).
-- ============================================================================

CREATE TABLE controle_salarie (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    controle_id UUID NOT NULL REFERENCES controle(id),
    salarie_id UUID NOT NULL REFERENCES salarie(id),
    entreprise_id UUID NOT NULL REFERENCES entreprise(id),
    accorde BOOLEAN NOT NULL,
    action_corrective_id UUID REFERENCES action_corrective(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_controle_salarie_controle ON controle_salarie(controle_id);

-- Rapports : archive volontairement sans deleted_at en V1 (voir commentaire),
-- mais un rapport généré par erreur doit pouvoir être supprimé (site legacy).
ALTER TABLE rapport_controle ADD COLUMN deleted_at TIMESTAMPTZ;

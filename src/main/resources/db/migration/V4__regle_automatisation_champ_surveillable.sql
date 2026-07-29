-- ============================================================================
-- Automatisation messagerie : le déclencheur d'une règle n'est plus un enum
-- figé à 2 valeurs mais l'id d'un "champ surveillable" enregistré côté backend
-- (voir ChampSurveillableRegistry) — nouvelle source ajoutable sans migration
-- ni changement de schéma, juste une nouvelle implémentation Java enregistrée.
-- ============================================================================

ALTER TABLE regle_automatisation DROP CONSTRAINT chk_regle_evenement;
ALTER TABLE regle_automatisation RENAME COLUMN evenement_declencheur TO champ_surveillable_id;

ALTER TABLE chantier
    ADD COLUMN recurrence_controles VARCHAR(20)
        CHECK (recurrence_controles IN ('AUCUNE', 'HEBDOMADAIRE', 'MENSUEL', 'TRIMESTRIEL', 'ANNUEL')),
    ADD COLUMN date_prochain_controle DATE;

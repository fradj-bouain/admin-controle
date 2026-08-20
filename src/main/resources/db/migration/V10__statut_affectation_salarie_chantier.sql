-- Aligne affectation_salarie_chantier sur affectation_entreprise_chantier : un statut
-- d'engagement explicite (ACTIF/INACTIF), distinct de statut_acces (qui reste l'accord
-- d'accès au chantier, un axe différent). Jusqu'ici seule date_fin distinguait une
-- affectation active d'une affectation close, sans possibilité de réouverture.
ALTER TABLE affectation_salarie_chantier
    ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'ACTIF';

-- Backfill déterministe : reproduit exactement le comportement actuel (date_fin renseignée
-- = affectation considérée close côté écran) sans changer le sens d'aucune ligne existante.
UPDATE affectation_salarie_chantier
SET statut = 'INACTIF'
WHERE date_fin IS NOT NULL;

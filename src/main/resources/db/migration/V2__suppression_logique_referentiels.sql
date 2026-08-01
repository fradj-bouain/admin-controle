-- ============================================================================
-- Suppression logique pour les tables de référence
-- ============================================================================
-- Ces tables n'avaient volontairement pas de deleted_at (voir commentaire V1 :
-- "les référentiels n'ont volontairement pas cette colonne"), mais l'app
-- expose maintenant un bouton Supprimer sur toutes les listes, y compris ces
-- référentiels — la suppression y reste logique, jamais un vrai DELETE, pour
-- ne pas casser les FK des enregistrements existants qui les référencent
-- (ex: un salarié dont la fonction est supprimée garde sa fonction_id valide).

ALTER TABLE pays ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE corps_de_metier ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE type_salarie ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE type_contrat_salarie ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE salarie_fonction ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE controle_tiers ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE type_document ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE document_etat ADD COLUMN deleted_at TIMESTAMPTZ;

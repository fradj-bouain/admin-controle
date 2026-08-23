-- Téléphone et adresse d'intervention propres à la relation (entreprise, chantier) — complète
-- email_contact (V12) sur le même modèle validé : "chaque chantier peut avoir son propre
-- contact, comme si c'était une nouvelle entreprise sans en être une", sans dupliquer
-- Entreprise (raison sociale/SIRET/informations légales restent une seule ligne, partagée
-- par tous les chantiers de l'entreprise). Les deux colonnes sont optionnelles : null = pas
-- de valeur spécifique à ce chantier, on retombe sur les coordonnées principales de l'entreprise.
ALTER TABLE affectation_entreprise_chantier
    ADD COLUMN telephone_contact VARCHAR(50),
    ADD COLUMN adresse_contact VARCHAR(500);

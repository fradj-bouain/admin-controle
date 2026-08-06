-- Un type de document peut désormais cibler une ZONE de pays (FRANCE / UE / HORS_UE,
-- mêmes valeurs que pays.zone) en plus (ou à la place) d'un pays précis (pays_id) :
-- nécessaire pour rendre le Titre de séjour obligatoire pour tout salarié dont la
-- nationalité est Hors UE, sans avoir à lister chaque pays un par un.
ALTER TABLE type_document ADD COLUMN zone_requise VARCHAR(20);

-- Types déjà présents dans les données de référence scrapées du site legacy (voir V1) :
-- la CIN/passeport est déjà obligatoire pour tous ; le Titre de séjour devient
-- obligatoire spécifiquement pour les salariés de nationalité Hors UE.
UPDATE type_document
SET obligatoire = TRUE, zone_requise = 'HORS_UE'
WHERE libelle = 'IDENTITÉ : TITRE DE SÉJOUR / VISA';

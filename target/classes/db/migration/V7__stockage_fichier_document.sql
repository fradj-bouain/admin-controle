-- Stockage réel du fichier déposé (jusqu'ici, fichier_url ne conservait que le
-- nom du fichier choisi côté client, aucun octet n'était jamais transmis ni
-- stocké côté serveur). chemin_stockage est une clé interne (nom de fichier
-- sur disque, UUID) — jamais exposée telle quelle au frontend, qui passe
-- toujours par GET /documents/{id}/fichier pour lire le contenu.
ALTER TABLE document
    ADD COLUMN nom_fichier_original VARCHAR(255),
    ADD COLUMN type_mime VARCHAR(100),
    ADD COLUMN taille_octets BIGINT,
    ADD COLUMN chemin_stockage VARCHAR(500);

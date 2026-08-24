-- Un message de "demande de document" peut désormais viser PLUSIEURS types de documents à la
-- fois (l'admin sélectionne plusieurs documents manquants et envoie une seule demande groupée,
-- au lieu d'un message par document) — remplace la colonne type_document_id (un seul document)
-- par une table de liaison.
CREATE TABLE message_type_document (
    message_id UUID NOT NULL REFERENCES message(id),
    type_document_id UUID NOT NULL
);

INSERT INTO message_type_document (message_id, type_document_id)
SELECT id, type_document_id FROM message WHERE type_document_id IS NOT NULL;

ALTER TABLE message DROP COLUMN type_document_id;

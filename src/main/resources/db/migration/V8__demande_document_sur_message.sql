-- Lien structuré optionnel entre un message "Demander un document" et le
-- document réellement attendu, pour permettre à l'entreprise destinataire de
-- déposer le fichier directement depuis la consultation du message (au lieu
-- de devoir retrouver elle-même la bonne ligne dans la fiche salarié/entreprise).
-- salarie_id seul quand la demande vise un document propre à un salarié précis ;
-- absent (null) quand la demande vise un document de l'entreprise elle-même
-- (le destinataire du message, déjà porté par destinataire_id, suffit alors).
ALTER TABLE message
    ADD COLUMN type_document_id UUID REFERENCES type_document(id),
    ADD COLUMN salarie_id UUID REFERENCES salarie(id);

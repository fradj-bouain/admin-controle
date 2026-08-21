-- Email de contact propre à la relation (entreprise, chantier) — distinct de l'email
-- principal de l'entreprise (Entreprise.email). N'entraîne pour l'instant aucun envoi
-- réel : "Nouveau message" reste une messagerie interne à l'application (voir
-- MessageService.envoyer, jamais branché sur EmailService/SMTP) — ce champ est stocké
-- et affiché en préparation d'un futur envoi réel, pas encore utilisé pour le routage.
ALTER TABLE affectation_entreprise_chantier
    ADD COLUMN email_contact VARCHAR(255);

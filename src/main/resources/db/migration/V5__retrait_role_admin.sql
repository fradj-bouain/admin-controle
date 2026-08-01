-- Retrait du rôle ADMIN : seul SUPER_ADMIN garde l'accès interne complet.
-- Défensif : reclasse tout compte ADMIN existant en SUPER_ADMIN plutôt que
-- de le laisser orphelin d'un rôle qui n'existe plus côté application.
UPDATE utilisateur_role SET role = 'SUPER_ADMIN' WHERE role = 'ADMIN';

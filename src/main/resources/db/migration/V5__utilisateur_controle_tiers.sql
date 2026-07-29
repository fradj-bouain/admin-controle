-- ============================================================================
-- Rattachement d'un compte Contrôleur à son organisme de contrôle
-- ============================================================================
-- Même principe que entreprise_id / client_id sur utilisateur : un compte de
-- type Contrôleur est scopé à un ControleTiers, et ne voit/gère que les
-- contrôles de cet organisme (voir ScopeAuthorizationService côté API).

ALTER TABLE utilisateur
    ADD COLUMN controle_tiers_id UUID REFERENCES controle_tiers(id);

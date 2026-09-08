-- Nouveaux types de contrat demandés par le client pour la répartition des statistiques
-- salariés (fiche Entreprise) : ajoutés tels quels à côté des valeurs existantes (CDI, CDD,
-- Intérimaire, Travailleur détaché, Apprentissage, Convention de stage), sans les remplacer
-- ni migrer les salariés déjà affectés à ces valeurs — décision explicite du client (voir
-- échange produit), même si certains libellés se recoupent (ex: "Apprentis" vs
-- "Apprentissage" déjà existant).
INSERT INTO type_contrat_salarie (code, libelle) VALUES
    ('INTERIMAIRE_DETACHE', 'Intérimaire détaché'),
    ('APPRENTIS', 'Apprentis'),
    ('STAGIAIRE', 'Stagiaire'),
    ('DIRIGEANT_SALARIE', 'Dirigeant salarié'),
    ('DIRIGEANT_NON_SALARIE', 'Dirigeant non salarié'),
    ('VISITEUR_PONCTUEL', 'Visiteur ponctuel');

-- Un compte Client peut désormais être soit "accès total" (voit tous les chantiers,
-- entreprises, salariés, documents et rapports du Client), soit "responsable de
-- chantier" (voit uniquement les chantiers qui lui sont explicitement assignés via
-- chantier_utilisateur, comme aujourd'hui). Par défaut : responsable de chantier.
ALTER TABLE utilisateur
    ADD COLUMN acces_tous_chantiers BOOLEAN NOT NULL DEFAULT false;

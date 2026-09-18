-- Economie de la ferme : porte-monnaie des eleveurs, valeur des animaux,
-- etat vital (faim, sante), sept nouvelles especes et releve de compte.

-- 1. Le porte-monnaie de l'eleveur -----------------------------------------

alter table eleveur
    add column solde numeric(12, 2) not null default 300.00;

alter table eleveur
    add constraint ck_eleveur_solde check (solde >= 0);

-- 2. Valeur marchande et etat vital des animaux ----------------------------

alter table animal
    add column prix                numeric(10, 2) not null default 0,
    add column quantite_production integer        not null default 0,
    add column sante               integer        not null default 100,
    add column derniere_nourriture timestamp(6) with time zone,
    add column derniere_recolte    timestamp(6) with time zone,
    add column derniere_balade     timestamp(6) with time zone;

-- La production etait portee par deux colonnes propres a l'espece : une seule
-- suffit desormais, l'unite vient de l'espece.
update animal
set quantite_production = coalesce(litres_de_lait_par_jour, oeufs_par_semaine, 0);

update animal
set prix = case espece
               when 'VACHE' then 230.00
               when 'POULE' then 26.00
               else 0.00
           end,
    derniere_nourriture = now();

alter table animal
    drop column litres_de_lait_par_jour,
    drop column oeufs_par_semaine;

alter table animal
    add constraint ck_animal_sante check (sante between 0 and 100),
    add constraint ck_animal_prix check (prix >= 0);

-- 3. Les nouvelles especes -------------------------------------------------

alter table animal
    drop constraint ck_animal_espece;

alter table animal
    add constraint ck_animal_espece check (espece in
        ('VACHE', 'POULE', 'MOUTON', 'CHEVRE', 'COCHON', 'CANARD', 'LAPIN', 'CHEVAL', 'OIE'));

-- 4. Le releve de compte ---------------------------------------------------

create table mouvement (
    id          bigserial      primary key,
    eleveur_id  bigint         not null,
    animal_id   bigint,
    type        varchar(20)    not null,
    montant     numeric(10, 2) not null,
    solde_apres numeric(12, 2) not null,
    libelle     varchar(255)   not null,
    horodatage  timestamp(6) with time zone not null,
    constraint fk_mouvement_eleveur foreign key (eleveur_id) references eleveur (id) on delete cascade,
    constraint fk_mouvement_animal foreign key (animal_id) references animal (id) on delete set null,
    constraint ck_mouvement_type check (type in ('ACHAT', 'VENTE', 'REPAS', 'SOIN', 'RECOLTE', 'PROMENADE'))
);

create index idx_mouvement_eleveur on mouvement (eleveur_id, horodatage desc);

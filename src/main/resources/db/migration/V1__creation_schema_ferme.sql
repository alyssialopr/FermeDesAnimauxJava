-- Schema initial de la ferme : les eleveurs et leurs animaux.

create table eleveur (
    id     bigserial    primary key,
    prenom varchar(255) not null,
    constraint uk_eleveur_prenom unique (prenom)
);

create table animal (
    id                      bigserial    primary key,
    espece                  varchar(20)  not null,
    nom                     varchar(255) not null,
    race                    varchar(255) not null,
    couleur                 varchar(255) not null,
    enclos                  varchar(255) not null,
    etat                    varchar(20)  not null,
    eleveur_id              bigint,
    cree_le                 timestamp(6) with time zone not null,
    -- colonnes propres a une espece (heritage SINGLE_TABLE)
    litres_de_lait_par_jour integer,
    oeufs_par_semaine       integer,
    constraint fk_animal_eleveur foreign key (eleveur_id) references eleveur (id) on delete set null,
    constraint ck_animal_espece check (espece in ('VACHE', 'POULE')),
    constraint ck_animal_etat check (etat in ('LIBRE', 'VENDU', 'MORT', 'DISPARU'))
);

create index idx_animal_eleveur on animal (eleveur_id);
create index idx_animal_etat on animal (etat);
create index idx_animal_espece on animal (espece);

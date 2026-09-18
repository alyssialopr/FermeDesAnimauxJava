-- Authentification des eleveurs et verrous optimistes.

-- 1. Cle d'acces ------------------------------------------------------------
--
-- Seule l'empreinte BCrypt est stockee (60 caracteres, 72 par securite) : la
-- cle en clair n'est affichee qu'une fois, a la creation de l'eleveur.
-- Les eleveurs crees avant cette migration n'ont pas de cle : l'API refuse
-- toute action en leur nom tant qu'ils n'en ont pas.

alter table eleveur
    add column cle_hachee varchar(72);

-- 2. Verrous optimistes -----------------------------------------------------
--
-- Deux requetes simultanees sur le meme animal ou le meme porte-monnaie ne
-- doivent pas pouvoir ecraser le resultat l'une de l'autre (double recolte,
-- double depense).

alter table eleveur
    add column version bigint not null default 0;

alter table animal
    add column version bigint not null default 0;

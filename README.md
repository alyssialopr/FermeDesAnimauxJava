# La Ferme des Animaux

Un jeu de gestion de ferme : des **éleveurs** achètent, vendent, nourrissent,
soignent, promènent et récoltent **neuf espèces** d'animaux, avec de l'argent,
de la faim, de la santé et un relevé de compte.

Le projet est né comme un exercice Java en console (`Main.java` + package `laFerme`).
Il est devenu une **API Spring Boot** persistée dans **PostgreSQL**, avec un **front
Three.js (WebGL)** pour y jouer vraiment — le tout lancé par Docker, en gardant la
logique et le découpage d'origine.

```
navigateur ──► web (nginx + Three.js) ──► api (Spring Boot) ──► db (PostgreSQL)
                   sert le jeu            règles métier          données
                   et relaie /api
```

---

## Démarrage rapide (Docker)

```bash
cp .env.example .env
# renseigner POSTGRES_PASSWORD (ex. openssl rand -hex 24)
docker compose up --build
```

| Service | URL |
|---|---|
| **Le jeu** | **http://localhost:3000** |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Santé | http://localhost:8080/actuator/health |

Trois conteneurs démarrent : `db` (PostgreSQL), `api` (Spring Boot) et `web` (le jeu
servi par nginx, qui relaie aussi `/api` vers l'API — donc aucun problème de CORS).

Au premier démarrage, la base est créée par Flyway et le jeu de démonstration
(les animaux et éleveurs de l'ancien `Main.java`) est inséré si la base est vide.
Pour s'en passer : `FERME_DONNEES_DEMO=false`.

## Démarrage en local (sans Docker pour l'API)

```bash
docker compose up -d db                      # juste PostgreSQL
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ferme
export SPRING_DATASOURCE_USERNAME=ferme
export SPRING_DATASOURCE_PASSWORD=...        # même valeur que dans .env
./mvnw spring-boot:run
```

## Jouer

Ouvre **http://localhost:3000** : la ferme se charge en 3D.

- **Choisis ton éleveur** en haut à gauche (ou crées-en un). Tu démarres avec **300 €**.
- Onglet **Marché** : les animaux sans propriétaire attendent au fond de la prairie.
  Achète-les, ils rejoignent ton enclos.
- **Clique sur un animal** pour ouvrir sa fiche : jauges de faim et de santé,
  valeur, production, et les six actions avec leur prix affiché.
- **Récolter** vend la production et remplit la caisse — mais l'animal se fatigue
  et il faut attendre avant la récolte suivante.
- Un animal **affamé** ou **épuisé** refuse de produire : nourris-le (le fourrage
  se paie) ou appelle le vétérinaire.
- Tu ne peux agir que sur **tes** animaux : sinon l'API répond *Cet animal ne vous
  appartient pas* et le refus s'affiche en rouge dans le journal.
- Onglet **Compte** : le classement des fortunes et ton relevé de compte, ligne
  par ligne.
- **Clic-glisser** pour tourner autour de la ferme, molette pour zoomer, `Échap`
  pour désélectionner.

Rien n'est simulé côté navigateur : chaque action est un appel HTTP à l'API, donc
une écriture en base. Recharge la page (ou ouvre un second onglet avec un autre
éleveur), ta ferme, ton argent et ton relevé sont exactement dans l'état où tu les
as laissés.

### Les espèces

La ferme tourne sur une **horloge accélérée** : les délais se comptent en minutes,
pour qu'une partie tienne en quelques minutes. `GET /api/especes` expose le tableau
complet ; en résumé :

| Espèce | Prix | Production | Par récolte | Délai |
|---|---|---|---|---|
| 🐔 Poule | 26 € | œufs | 5 × 0,60 € | 2 min |
| 🦆 Canard | 28 € | œufs | 4 × 0,75 € | 2 min |
| 🐐 Chèvre | 60 € | lait | 4 × 1,80 € | 3 min |
| 🐇 Lapin | 70 € | lapereaux | 2 × 12 € | 6 min |
| 🐖 Cochon | 110 € | — | s'engraisse : +9 € de valeur par repas | — |
| 🐑 Mouton | 120 € | laine | 3 × 6 € | 6 min |
| 🦢 Oie | 120 € | duvet | 120 × 0,15 € | 6 min |
| 🐄 Vache | 230 € | lait | 18 × 1,50 € | 3 min |
| 🐴 Cheval | 500 € | — | ses promenades rapportent 45 € | 3 min |

Revendre un animal rapporte **90 %** de sa valeur.

### Développer le front seul

```bash
docker compose up -d db api    # l'API et la base
cd front && npm install && npm run dev
```

Vite sert le jeu sur http://localhost:5173 et relaie `/api` vers `localhost:8080`.
Depuis la console du navigateur, `window.ferme` expose l'état du jeu, les objets 3D
et les actions — pratique pour déboguer ou piloter une partie.

## Sécurité

L'API demande une **clé d'éleveur** pour toute action (`Authorization: Bearer
<id>.<clé>`) : la lecture est ouverte, l'écriture non. Une clé valide ne permet
d'agir qu'au nom de son propre éleveur.

```bash
# Lecture : ouverte
curl http://localhost:8080/api/animaux

# Action : la clé est obligatoire
curl -X POST http://localhost:8080/api/eleveurs/1/animaux/1/repas \
     -H "Authorization: Bearer 1.demo-alyssia"
```

Le détail de l'audit, des corrections et des vérifications est dans
**[SECURITE.md](SECURITE.md)** : authentification Spring Security, clés hachées en
BCrypt, verrous optimistes sur l'argent, limitation de débit, en-têtes de sécurité,
base non exposée et conteneurs durcis.

## Documentation de l'API (Swagger)

Deux façons de la consulter :

| | |
|---|---|
| **Sans rien lancer** | ouvrir **`docs/api.html`** dans un navigateur — un simple double-clic |
| Depuis le jeu | http://localhost:3000/doc (la même page, servie par nginx) |
| Avec la stack | http://localhost:8080/swagger-ui.html (le bouton *Try it out* y fonctionne) |

`docs/api.html` est un fichier autonome : Swagger UI et le contrat OpenAPI sont
embarqués dedans, il ne télécharge rien et n'a besoin d'aucun serveur. Le contrat
brut est aussi disponible seul dans `docs/openapi.json`, à importer dans Postman ou
Insomnia.

Pour le régénérer après avoir modifié l'API :

```bash
docker compose up -d           # l'API doit tourner
cd docs && npm install && npm run generer
```

La documentation est générée à partir du code (springdoc-openapi) : c'est la même
source dans les deux cas.

Elle est complète : chaque opération porte un résumé et une description, chaque
champ des requêtes et des réponses est décrit avec un exemple, les énumérations
(`Espece`, `EtatAnimal`) expliquent leurs valeurs, et les codes d'erreur `400`,
`404`, `409` sont documentés avec un exemple de corps `application/problem+json`.
Le bouton *Try it out* permet d'appeler l'API directement depuis la page.

## Tests

```bash
./mvnw test
```

- tests unitaires du domaine (aucune dépendance Spring)
- tranches `@WebMvcTest` pour le contrat HTTP
- parcours d'intégration sur un **PostgreSQL jetable (Testcontainers)** : Docker doit
  tourner, les migrations Flyway et le mapping JPA sont validés pour de vrai

---

## Endpoints

### Animaux — `/api/animaux`

| Méthode | Chemin | Rôle |
|---|---|---|
| `POST` | `/api/animaux` | faire entrer un animal (espèce `VACHE` ou `POULE`, éleveur optionnel) |
| `GET` | `/api/animaux` | lister, filtres `espece`, `etat`, `eleveurId`, `enclos` |
| `GET` | `/api/animaux/{id}` | détail |
| `PATCH` | `/api/animaux/{id}/enclos` | déplacer dans un autre enclos |
| `PATCH` | `/api/animaux/{id}/etat` | déclarer `MORT` ou `DISPARU` |
| `DELETE` | `/api/animaux/{id}` | retirer du registre |

### Éleveurs — `/api/eleveurs`

| Méthode | Chemin | Rôle |
|---|---|---|
| `POST` | `/api/eleveurs` | créer un éleveur (prénom unique) |
| `GET` | `/api/eleveurs` | lister (avec la taille du troupeau) |
| `GET` | `/api/eleveurs/{id}` | détail + troupeau |
| `DELETE` | `/api/eleveurs/{id}` | supprimer (troupeau vide obligatoire) |
| `POST` | `/api/eleveurs/{id}/animaux/{animalId}/achat` | acheter |
| `POST` | `.../vente` | vendre |
| `POST` | `.../repas` | nourrir |
| `POST` | `.../soin` | soigner |
| `POST` | `.../balade` | emmener en balade |
| `POST` | `.../recolte` | récolter le lait ou les œufs |

Les six dernières routes sont exactement les méthodes de l'`Eleveur` d'origine, et
renvoient le message métier qui était auparavant affiché en console :

```bash
curl -X POST http://localhost:8080/api/eleveurs/1/animaux/1/repas
```

```json
{
  "message": "La vache emily a ete nourrie.",
  "animal": { "id": 1, "espece": "VACHE", "nom": "emily", "etat": "LIBRE", "...": "..." }
}
```

### Erreurs

Toutes les erreurs suivent la RFC 7807 :

```json
{
  "title": "Animal non possede",
  "status": 409,
  "detail": "Cet animal ne vous appartient pas",
  "instance": "/api/eleveurs/2/animaux/1/repas",
  "horodatage": "2026-09-18T07:27:13.445Z"
}
```

| Statut | Cas |
|---|---|
| `400` | validation (le détail par champ est dans `champs`) |
| `404` | animal ou éleveur inconnu |
| `409` | règle métier : animal d'un autre éleveur, animal vendu/mort/disparu, prénom déjà pris |

---

## Organisation du code

Le découpage reprend celui du projet de référence (`model`, `repository`, `services`,
`controller`, `config`, `utils`), sous le package historique `laFerme`.

```
src/main/java/laFerme/
├── FermeApplication.java      point d'entrée (ex-Main.java)
├── model/                     Animal (abstrait), Vache, Poule, Eleveur, EtatAnimal, Espece
├── repository/                AnimalRepository, EleveurRepository
├── services/                  AnimalService, EleveurService
├── controller/                AnimalController, EleveurController
├── dto/                       requêtes et réponses exposées par l'API
├── utils/                     mappers et critères de recherche
├── exception/                 exceptions métier + traduction HTTP
└── config/                    OpenAPI, jeu de données de démonstration
src/main/resources/
├── application.yml
└── db/migration/              migrations Flyway

SECURITE.md                    audit de sécurité, corrections et vérifications

docs/                          documentation hors-ligne
├── api.html                   Swagger UI + contrat embarqués (ouvrir tel quel)
├── openapi.json               le contrat seul
└── generer-doc.mjs            régénère les deux depuis l'API

front/                         le jeu (Three.js + Vite, servi par nginx)
├── index.html                 interface 2D posée sur le canvas
├── nginx.conf                 fichiers statiques + relais /api
├── Dockerfile                 build npm puis image nginx
└── src/
    ├── main.js                relie l'API à la scène
    ├── api.js                 client de l'API
    ├── scene/                 décor, modèles 3D et comportement des animaux
    └── ui/                    HUD, fiche animal, journal, styles
```

### Ce qui a été conservé du projet console

- le package `laFerme` et le vocabulaire métier (éleveur, enclos, balade…)
- les cinq actions de l'`Animal` : `nourrir`, `soigner`, `allerEnBalade`, `acheter`, `vendre`
- les états `LIBRE`, `VENDU`, `MORT`, `DISPARU`
- la vérification « **Cet animal ne vous appartient pas** » avant toute action de l'éleveur
- le jeu de données du `Main` (emily, marguerite, nugget, plume, alyssia, killian)

### Les règles du jeu

- chaque éleveur a un **porte-monnaie** persisté ; acheter débite, vendre crédite
- **nourrir** coûte le fourrage, **soigner** coûte le vétérinaire, **récolter**
  vend la production au prix du marché
- la **faim** monte toute seule avec le temps, déduite du dernier repas
- la **santé** baisse à chaque récolte ; les soins et les balades la remontent
- un animal affamé (> 70 %) ou épuisé (< 30 %) refuse de produire
- un **délai** sépare deux récoltes : l'argent ne tombe pas du ciel
- une opération qui dépasse la caisse échoue **en bloc** (409, transaction annulée)
- chaque mouvement d'argent laisse une ligne dans le **relevé de compte**

### Polymorphisme

Les actions de l'`Animal` sont des **méthodes gabarit** : les règles communes
(disponibilité, faim, santé, délais) sont dans la classe abstraite, et chaque
espèce redéfinit ce qui la distingue.

| Méthode redéfinie | Ce qu'en fait l'espèce |
|---|---|
| `cri()` | « Meuh ! », « Cot cot codec ! », « Groin groin ! », « Hiiiii ! »… |
| `messageRecolte()` | la vache **donne** du lait, la poule **pond**, le mouton **se fait tondre**, la lapine **a une portée** |
| `effetDuRepas()` | le **cochon** grossit et prend de la valeur, les autres sont simplement nourris |
| `effetDeLaBalade()` | le **cheval** emmène des promeneurs, les autres se dégourdissent les pattes |
| `valeurAjouteeParRepas()` / `recetteDeLaBalade()` | zéro par défaut, redéfini par le cochon et le cheval |

Les espèces qui ne se récoltent pas ne redéfinissent pas `messageRecolte()` :
elles héritent du refus par défaut. Le service, lui, ne manipule que des
`Animal` — il ne connaît aucune sous-classe.

### Ce qui a été amélioré

- `Animal` était une interface : c'est maintenant une **entité abstraite**, ce qui permet
  de mutualiser les règles et de persister les deux espèces dans une table unique
  (`SINGLE_TABLE`, discriminant `espece`)
- les actions **renvoient leur message** au lieu de l'écrire sur la sortie standard :
  le polymorphisme vache/poule devient visible dans les réponses HTTP
- chaque espèce a une **production** propre (lait, œufs) — l'héritage sert à quelque chose
- les identifiants ne sont plus des compteurs statiques mais délégués à la base
- un animal `VENDU`, `MORT` ou `DISPARU` **refuse toute action** (l'ancien code
  acceptait de nourrir un animal vendu)
- l'`Eleveur` refuse d'acheter l'animal d'un autre, ou un animal mort/disparu
- doublon supprimé : l'ancien `Eleveur` avait `allerEnBalade(animal)` (qui sortait
  l'animal du troupeau) **et** `promener(animal)` ; seul `promener` subsiste
- validation des entrées, erreurs normalisées, documentation OpenAPI, logs, `actuator`
- schéma versionné par Flyway et **validé** par Hibernate (`ddl-auto=validate`)
- recherche filtrée côté base (Specifications) au lieu de filtrer en mémoire
- chargement du troupeau par `EntityGraph` pour éviter le N+1

---

## Choix techniques

| Sujet | Choix |
|---|---|
| Java / Spring Boot | 21 / 4.1.1 |
| Base | PostgreSQL 16, schéma géré par Flyway |
| Héritage JPA | `SINGLE_TABLE` : une table `animal`, colonnes d'espèce nullables |
| Image Docker | multi-étapes, JRE Alpine, utilisateur non root, jar en couches |
| Secrets | aucun identifiant dans le dépôt, tout passe par l'environnement |
| Tests | JUnit 5 + AssertJ + Mockito, Testcontainers pour l'intégration |
| Sécurité | Spring Security (clé d'éleveur, BCrypt), verrous optimistes, nginx (débit, en-têtes) |
| Front | Three.js (WebGL), Vite, aucune dépendance CDN — tout est dans l'image |

## Pistes suivantes

- authentification (un éleveur ne devrait agir que sur son propre compte)
- compteurs de récolte stockés côté API plutôt que dans le navigateur
- temps réel : pousser les changements aux autres joueurs (SSE ou WebSocket)
  plutôt que de rafraîchir toutes les 10 secondes
- pagination et tri sur la liste des animaux
- historique des actions (journal par animal)
- métriques métier exposées via Actuator/Prometheus

# La Ferme des Animaux

Un petit jeu de gestion de ferme : des **éleveurs** achètent, vendent, nourrissent,
soignent et promènent des **vaches** et des **poules**.

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

- **Choisis ton éleveur** en haut à gauche (ou crées-en un).
- **Clique sur un animal** pour ouvrir sa fiche : nourrir, soigner, promener,
  récolter, vendre. Chaque bouton appelle l'API et affiche le message renvoyé.
- Les animaux **sans propriétaire attendent au marché**, au fond : achète-les pour
  les faire entrer dans ton troupeau, ils rejoignent alors ton enclos.
- Tu ne peux agir que sur **tes** animaux : sinon l'API répond *Cet animal ne vous
  appartient pas* et le refus s'affiche en rouge dans le journal.
- **Récolter** remplit les compteurs de lait et d'œufs en haut à droite.
- **Clic-glisser** pour tourner autour de la ferme, molette pour zoomer, `Échap`
  pour désélectionner.

Rien n'est simulé côté navigateur : chaque action est un appel HTTP à l'API, donc
une écriture en base. Recharge la page (ou ouvre un second onglet avec un autre
éleveur), la ferme est exactement dans l'état où tu l'as laissée.

### Développer le front seul

```bash
docker compose up -d db api    # l'API et la base
cd front && npm install && npm run dev
```

Vite sert le jeu sur http://localhost:5173 et relaie `/api` vers `localhost:8080`.
Depuis la console du navigateur, `window.ferme` expose l'état du jeu, les objets 3D
et les actions — pratique pour déboguer ou piloter une partie.

## Documentation de l'API (Swagger)

La documentation est générée à partir du code (springdoc-openapi) et se consulte
sur **http://localhost:8080/swagger-ui.html** — le contrat brut est sur
`/v3/api-docs`.

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
| Front | Three.js (WebGL), Vite, aucune dépendance CDN — tout est dans l'image |

## Pistes suivantes

- authentification (un éleveur ne devrait agir que sur son propre compte)
- compteurs de récolte stockés côté API plutôt que dans le navigateur
- temps réel : pousser les changements aux autres joueurs (SSE ou WebSocket)
  plutôt que de rafraîchir toutes les 10 secondes
- pagination et tri sur la liste des animaux
- historique des actions (journal par animal)
- métriques métier exposées via Actuator/Prometheus

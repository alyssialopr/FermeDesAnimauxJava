# La Ferme des Animaux — API

API REST de gestion d'une ferme : des **éleveurs** achètent, vendent, nourrissent,
soignent et promènent des **vaches** et des **poules**.

Le projet est né comme un exercice Java en console (`Main.java` + package `laFerme`).
Il est désormais exposé en API Spring Boot, persisté dans PostgreSQL et conteneurisé,
en gardant la logique et le découpage d'origine.

---

## Démarrage rapide (Docker)

```bash
cp .env.example .env
# renseigner POSTGRES_PASSWORD (ex. openssl rand -hex 24)
docker compose up --build
```

| Service | URL |
|---|---|
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Santé | http://localhost:8080/actuator/health |

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

## Pistes suivantes

- authentification (un éleveur ne devrait agir que sur son propre compte)
- pagination et tri sur la liste des animaux
- historique des actions (journal par animal)
- métriques métier exposées via Actuator/Prometheus

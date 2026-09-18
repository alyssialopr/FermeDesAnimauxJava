# Audit de sécurité du backend

Audit mené le 18/09/2026 sur l'API Spring Boot de la ferme, puis corrections.
Chaque point ci-dessous a été **constaté** sur la pile qui tourne, pas seulement
lu dans le code.

---

## Ce qui a été trouvé

| # | Constat | Gravité | État |
|---|---|---|---|
| 1 | **Aucune authentification** : n'importe qui pouvait agir au nom de n'importe quel éleveur (`POST /api/eleveurs/1/animaux/1/vente`), vendre son troupeau, vider sa caisse | Critique | Corrigé |
| 2 | **Relevé de compte public** : `GET /api/eleveurs/{id}/mouvements` exposait l'historique financier de tout le monde | Élevé | Corrigé |
| 3 | **Course sur l'argent** : deux requêtes simultanées pouvaient récolter deux fois ou dépenser deux fois le même solde (lecture-modification-écriture sans verrou) | Élevé | Corrigé |
| 4 | **Actuator exposé publiquement** par nginx (`/actuator/**` relayé), avec `show-details: when-authorized` | Moyen | Corrigé |
| 5 | **PostgreSQL publié sur l'hôte** (`5432:5432`), accessible depuis tout le réseau local | Moyen | Corrigé |
| 6 | **Aucune limite de débit** : l'API pouvait être martelée sans frein | Moyen | Corrigé |
| 7 | **Aucun en-tête de sécurité** (CSP, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`…) | Moyen | Corrigé |
| 8 | **Entrées peu contraintes** : un nom d'animal de 255 caractères quelconques était accepté | Faible | Corrigé |
| 9 | **Pas de borne sur les requêtes** (taille d'en-tête, de corps, délais) | Faible | Corrigé |
| 10 | **Conteneurs privilégiés par défaut** : aucune capacité Linux retirée, pas de `no-new-privileges`, pas de limite mémoire | Faible | Corrigé |
| 11 | Un mot de passe utilisateur généré par Spring Security serait apparu dans les logs | Faible | Corrigé |

Points vérifiés **sans anomalie** : injection SQL (tout passe par JPA et les
Specifications, aucune concaténation), *mass assignment* (les entités ne sont
jamais liées à une requête, uniquement des DTO en `record`), fuite de secrets
(identifiants uniquement par variables d'environnement, `.env` non versionné),
fuite de stacktrace (réponses RFC 7807 sans détail interne).

---

## Ce qui a été mis en place

### Authentification et autorisation (Spring Security)

À la création, chaque éleveur reçoit une **clé d'accès** tirée au hasard, renvoyée
**une seule fois**. Le serveur n'en garde qu'une empreinte **BCrypt**.

```
Authorization: Bearer <idEleveur>.<clé>
```

- **Lecture ouverte, écriture authentifiée** : consulter la ferme ne demande rien,
  agir dessus exige la clé de l'éleveur concerné.
- **Autorisation par ressource** : `@PreAuthorize("@securite.estEleveur(#eleveurId)")`
  sur chaque action — une clé valide ne permet pas d'agir au nom d'un autre.
  Présenter la clé de killian sur une route d'alyssia renvoie `403`.
- **Chaîne sans état** : aucun cookie, aucune session, donc pas de CSRF possible ;
  la protection CSRF est désactivée en connaissance de cause.
- **Comparaison à temps constant** : quand l'éleveur n'existe pas, la clé est
  quand même comparée à une empreinte leurre, pour ne pas révéler par le temps de
  réponse quels comptes existent.
- **Refus normalisés** : `401` sans identité, `403` avec une identité insuffisante,
  au format `application/problem+json` comme le reste de l'API — pas de page de
  connexion, pas de détail interne.
- La clé **ne ressort jamais** : `@JsonIgnore` sur l'empreinte, et aucune route ne
  la renvoie après la création.

### Intégrité des données

- **Verrous optimistes** (`@Version`) sur l'éleveur et l'animal : deux opérations
  simultanées ne peuvent plus écraser le même solde ni récolter deux fois. Le
  conflit ressort en `409`.
- **Transactions atomiques** : un achat qui dépasse la caisse annule tout, y
  compris la création de l'animal (vérifié par un test).

### Surface exposée

- `/actuator` **n'est plus relayé** par nginx ; côté API, seul `health` est exposé,
  sans détail (`show-details: never`).
- **PostgreSQL n'est plus publié** sur l'hôte : seule l'API y accède, par le réseau
  interne de compose.
- Les ports publiés le sont sur **`127.0.0.1` uniquement**, plus sur `0.0.0.0`.
- **Aucun traitement CORS** : l'API ne renvoie jamais d'en-tête
  `Access-Control-Allow-Origin`, donc aucun site tiers ne peut lire ses réponses
  depuis un navigateur. Le jeu est servi sur la même origine par nginx.

### Frein et bornes

- **Limitation de débit** nginx : 20 req/s par IP sur `/api`, rafale de 40, `429`
  au-delà (le front affiche un message clair).
- **Limite de connexions** par IP, corps limité à 16 ko, délais de lecture courts.
- Côté Spring : en-têtes limités à 8 ko, formulaires à 64 ko, `connection-timeout`
  à 5 s, téléversement désactivé.

### En-têtes de sécurité

Posés par nginx sur toutes les réponses, et par Spring Security sur l'API :

```
Content-Security-Policy: default-src 'self'; script-src 'self'; object-src 'none';
                         base-uri 'none'; frame-ancestors 'none'; …
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Permissions-Policy: camera=(), microphone=(), geolocation=()
Cross-Origin-Opener-Policy / Resource-Policy: same-origin
```

`server_tokens off` : la version de nginx n'est plus annoncée.

### Validation des entrées

Noms, races, couleurs et enclos : 40 caractères maximum, et un motif qui n'accepte
que lettres, chiffres, espaces, apostrophes et tirets. Un `<script>` dans un nom
est refusé en `400` avec le détail par champ — vérifié par un test.

### Conteneurs

Pour les trois services : `no-new-privileges`, `cap_drop: ALL` (seules les
capacités réellement nécessaires sont rendues), limites mémoire et de processus.
L'API tourne en **lecture seule** avec un `/tmp` en mémoire, en utilisateur non
root, et PostgreSQL est configuré en `scram-sha-256`.

---

## Vérifications

Sur la pile réelle :

| Ce qui est testé | Attendu | Obtenu |
|---|---|---|
| `GET /api/animaux` sans clé | 200 | 200 |
| Action sans clé | 401 | 401 |
| Action avec la clé d'un autre éleveur | 403 | 403 |
| Action avec une mauvaise clé | 401 | 401 |
| Action avec la bonne clé | 200 | 200 |
| `GET /api/eleveurs/{id}/mouvements` sans clé | 401 | 401 |
| La clé apparaît-elle dans une réponse ? | non | non |
| `/actuator/health` via le port du jeu | non relayé | non relayé |
| Port 5432 depuis l'hôte | fermé | fermé |
| 70 requêtes d'affilée | quelques 429 | 3 × 429 |
| `Origin` tiers | aucun en-tête CORS | aucun |

Et dans la suite de tests (`./mvnw test`, 61 tests) : lecture publique, action
refusée sans clé, mauvaise clé, clé d'un autre éleveur, non-fuite de la clé,
entrée invalide refusée avant la base, transaction annulée en cas de fonds
insuffisants.

---

## Ce qui reste, et pourquoi

- **Pas de HTTPS** : la pile tourne en local derrière nginx. En exposition réelle,
  il faudrait un certificat et `Strict-Transport-Security` (l'en-tête n'a pas de
  sens en HTTP).
- **Clés de démonstration connues** (`demo-alyssia`, `demo-killian`) : elles ne
  sont créées que par le jeu de données de démonstration, désactivé dès que
  `ferme.donnees-demo=false`. Un avertissement est écrit dans les logs au
  démarrage.
- **Pas de rotation ni de révocation de clé** : il faut recréer un éleveur. C'est
  suffisant pour un jeu, pas pour un vrai compte.
- **Éleveurs créés avant cette version** : ils n'ont pas de clé et l'API refuse
  toute action en leur nom — volontairement, plutôt que de leur en attribuer une
  par défaut.
- **Limitation de débit par IP** : derrière nginx, toutes les requêtes d'un même
  poste partagent une IP. Suffisant ici, insuffisant pour un service public.

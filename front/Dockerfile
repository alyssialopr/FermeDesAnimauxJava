# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Etape 1 : construction du bundle (Three.js + Vite)
# ---------------------------------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /app

# Les dependances sont installees avant la copie des sources : modifier le jeu
# ne relance pas le telechargement de npm.
COPY package.json package-lock.json ./
RUN npm ci

COPY index.html vite.config.js ./
COPY src ./src
RUN npm run build

# ---------------------------------------------------------------------------
# Etape 2 : service des fichiers statiques + relais vers l'API
# ---------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://localhost/ || exit 1

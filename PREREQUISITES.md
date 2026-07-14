# Prérequis pour lancer l'application en local

## Dépendances système

- **JDK 17+** — le projet cible Java 17 (`pom.xml`), compile aussi avec des JDK plus récents (testé avec JDK 24)
- **Docker Desktop** (inclut Docker Compose) — nécessaire pour lancer la base PostgreSQL locale (`docker compose up -d trading-game-database`)
- **Git**

Maven n'a pas besoin d'être installé séparément : le projet embarque le Maven Wrapper (`mvnw` / `mvnw.cmd`), qui télécharge automatiquement la bonne version de Maven (3.9.11) au premier lancement.

## Extensions VSCode recommandées

- **Extension Pack for Java** (`vscjava.vscode-java-pack`) — support Java (compilation, exécution, debug)
- **Spring Boot Extension Pack** (`vmware.vscode-boot-dev-pack`) — inclut Spring Boot Tools et Spring Boot Dashboard
  - **Spring Boot Dashboard** (`vscjava.vscode-spring-boot-dashboard`) — lancer/arrêter l'app depuis la barre latérale
- **Container Tools** (`ms-azuretools.vscode-containers`) — gérer/visualiser le conteneur Postgres depuis VSCode

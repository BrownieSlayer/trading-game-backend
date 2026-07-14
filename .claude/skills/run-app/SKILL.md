---
name: run-app
description: Launch the trading-game-backend Spring Boot application locally. Use when the user asks to run, start, or launch the backend/app, or to verify it boots after a change.
---

The app is `app.TradingGameBackendApplication`, a Spring Boot 3.5 / Java 17 service exposing only the authentication system (username/password + JWT). It needs a Postgres database; no external API key is required.

1. Start the local Postgres database (only the DB service, not the full stack):
   `docker compose up -d trading-game-database`
   It exposes Postgres on `localhost:5777` (db/user/password `trading`), matching `src/main/resources/application-local.yaml`. The `trading` schema is created by `src/main/resources/db/init_db.sql`, mounted as a Postgres init script.
   Note: the container maps to host port 5777, not the default 5432 — this avoids clashing with any locally-installed Postgres service already bound to 5432.
2. Run the app with the `local` profile via the Maven wrapper:
   - Windows: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"`
   - macOS/Linux: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
3. The API comes up on `http://localhost:8080`.

Alternative profiles: `dev` (`application-dev.yml`, currently empty) and `prod` (`application-prod.yml`) — see `.vscode/launch.json` for the matching IDE run configurations.

To stop the database afterwards: `docker compose down`.

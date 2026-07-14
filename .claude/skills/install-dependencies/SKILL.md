---
name: install-dependencies
description: Install/resolve the Maven dependencies of the trading-game-backend Spring Boot project. Use when the user asks to install, resolve, download, or update dependencies, or when a build fails because dependencies are missing.
---

Use the Maven Wrapper — no global Maven installation is required or expected.

1. From the repo root, run:
   - Windows: `.\mvnw.cmd dependency:resolve`
   - macOS/Linux: `./mvnw dependency:resolve`
2. The wrapper reads `.mvn/wrapper/maven-wrapper.properties` and downloads the pinned Maven distribution (currently 3.9.11) into `~/.m2/wrapper/` on first run — this is expected and only happens once.
3. Confirm success by checking the output ends with `BUILD SUCCESS`.

If `.mvn/wrapper/maven-wrapper.properties` is missing or the wrapper fails with "Cannot start maven from wrapper", the wrapper scripts (`mvnw`, `mvnw.cmd`) are present but the properties file was not committed/restored — recreate it rather than falling back to a system-wide Maven install.

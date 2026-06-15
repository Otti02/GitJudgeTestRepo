# GitJudge Kotlin Demo

Kleines Kotlin-Demo-Projekt fuer Merge-Request- und Code-Review-Tests.

## Stack

- Kotlin + Gradle
- Ktor (HTTP API)
- Exposed (ORM)
- H2 (Datei-DB, lokal)
- Kotlin Test + Ktor Test Host

## Features

- `GET /health` fuer schnellen Smoke-Test
- `GET /todos` listet alle Todos
- `POST /todos` erstellt ein neues Todo
- `PATCH /todos/{id}/done` markiert ein Todo als erledigt

## Starten

```bash
.\gradlew.bat run
```

Die App läuft dann auf:

- `http://localhost:8080`

Tests ausführen:

```bash
.\gradlew.bat test
```

## Voraussetzungen

- JDK 17+ (App wird mit JVM Toolchain 17 gebaut)
- Gradle läuft über den Wrapper (`gradlew` / `gradlew.bat`)
- Gradle 9.4+ unterstützt Java 26 als Gradle-JVM (IDE-Sync mit JDK 26 moeglich)

## Beispiel-Requests

```bash
curl http://localhost:8080/health
```

```bash
curl -X POST http://localhost:8080/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"Review-Pipeline testen","description":"Kleine Demo-Aenderung bauen"}'
```

```bash
curl http://localhost:8080/todos
```

```bash
curl -X PATCH http://localhost:8080/todos/1/done
```

## Datenbank

Standardmaessig wird eine lokale H2-Dateidatenbank verwendet:

- `jdbc:h2:file:./data/gitjudge-demo`

Ueber Umgebungsvariablen kannst du die DB-Konfiguration aendern:

- `DB_JDBC_URL`
- `DB_USER`
- `DB_PASSWORD`

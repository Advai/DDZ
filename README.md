# Dou Dizhu — Spring Boot Server + Engine (Starter)

Multi-module Gradle project: headless `engine` + Spring Boot `server`.

## Build & Run
```
./gradlew build
./gradlew :server:bootRun
```
Create a game:
```
curl -X POST http://localhost:8080/api/games
```
Connect WebSocket (replace {id}):
```
# e.g., with wscat
wscat -c ws://localhost:8080/ws/game/{id}
```

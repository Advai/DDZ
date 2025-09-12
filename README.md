# Dou Dizhu (Starter Monorepo)

This is a minimal **Gradle multi-module** starter focused on the **Java game engine** only.
You can push this repo to GitHub and iterate from here.

## Layout
```
doudizhu/
├─ build.gradle                # root conventions
├─ settings.gradle             # includes :engine
└─ engine/
   ├─ build.gradle
   └─ src/main/java/com/yourco/ddz/engine/...
```

## Quick start
```bash
# from doudizhu/
gradle -v             # ensure Gradle (8+) is installed or use ./gradlew if you add wrapper
./gradlew build       # or: gradle build
./gradlew :engine:test
./gradlew :engine:run # runs DemoMain
```

> If you prefer **IntelliJ IDEA**, just open the `doudizhu` folder and it will import as a Gradle project.

## Next steps
- Replace the placeholder `DemoRules` with real Dou Dizhu rules & scoring.
- Split modules later (e.g., `server`, `protocol`) without touching engine purity.

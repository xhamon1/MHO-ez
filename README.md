# MHO-ez

**MHO guides for noobs**

A fan-made Android app that serves as a build guide for Monster Hunter Outlanders:
pick a weapon and an element, then see recommended armor, skills, and an explanation.

> Not affiliated with Capcom or the game's developers.

## Adding builds
Edit `app/src/main/assets/builds.json`:
```json
{"name":"Build name","weapons":["Great Sword"],"element":"Fire",
 "armor":"Set name","skills":["Effect 1","Effect 2"],"note":"Explanation"}
```
Use `"weapons":["Any"]` if the build works with every weapon.

## Building the APK
- **Automatic:** GitHub repo > *Actions* tab > *Build APK* > download the `MHO-ez-debug-apk` artifact.
- **Manual:** open in Android Studio and click Run.

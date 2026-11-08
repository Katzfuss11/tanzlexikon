# 2 · Die App auf dem Handy starten

Voraussetzung: [1 · Rechner und Handy einrichten](01-einrichtung.md) ist erledigt, und das Handy hängt am USB-Kabel.

## Weg A: mit Android Studio (zum Ausprobieren und Entwickeln)

1. Starte Android Studio.
2. Wähle **Open** und dann den Ordner `C:\Users\<Name>\repos\tanzlexikon`.
   Die Frage „Trust Project?“ beantwortest du mit **Trust Project**.
3. Warte, bis unten rechts der Fortschrittsbalken „Gradle sync“ verschwunden ist. Beim ersten Mal dauert das einige Minuten.
4. Oben in der Werkzeugleiste steht ein Auswahlfeld mit Geräten. Wähle dort dein Handy (es erscheint mit seiner Modellnummer).
5. Klicke auf das grüne **▶ (Run 'app')**.
6. **Geklappt, wenn:** auf dem Handy die App „Tanzlexikon“ startet.

Nach einer Änderung am Code genügt erneut ▶. Deine Daten in der App bleiben dabei erhalten.

## Weg B: ohne Android Studio, über die Kommandozeile

In Git Bash im Repo-Ordner:
```
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew installDebug
```
**Geklappt, wenn:** am Ende `Installed on 1 device` steht. Die App findest du dann im App-Menü des Handys.

## Einen Emulator anlegen (ein virtuelles Handy am PC)

Das ist praktisch, wenn das Handy gerade nicht angeschlossen ist.

1. In Android Studio: **View → Tool Windows → Device Manager**.
2. Klicke auf **+** und dann **Create Virtual Device**. Wähle **Pixel 8** und **Next**, dann das System-Image **API 35** (beim ersten Mal auf den Download-Pfeil klicken) und **Finish**.
3. Wähle den Emulator oben im Geräte-Auswahlfeld aus und klicke auf ▶.

## Die Tests laufen lassen

```
./gradlew testDebugUnitTest
```
**Geklappt, wenn:** `BUILD SUCCESSFUL` erscheint.

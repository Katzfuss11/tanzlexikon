# 3 · Weitergeben und Updates veröffentlichen

Voraussetzung: [2 · Die App auf dem Handy starten](02-app-aufs-handy.md) klappt.

Bisher läuft auf deinem Handy eine **Testversion** (die „Debug-Version“) aus Android Studio. Um die App an andere weiterzugeben, brauchst du eine **richtige Version** (die „Release-Version“). Diese ist mit deinem persönlichen **Schlüssel** unterschrieben. Der Schlüssel ist eine kleine Datei, an der Android erkennt, dass ein Update wirklich von dir stammt.

Abschnitt 1 und 2 machst du **einmal**. Abschnitt 3 bis 6 brauchst du, wann immer du eine Version weitergeben willst.

> **Tipp:** Abschnitt 3, 4 (Schritt 3) und 6 kann auch Claude für dich erledigen. Nur den Schlüssel (Abschnitt 1) legst du selbst an, weil dabei dein Passwort eingegeben wird.

---

## Vorab: Wo gebe ich was ein?

In dieser Anleitung kommen drei Orte vor:

| Wo | Woran du es erkennst |
|---|---|
| **PowerShell** | ein Fenster mit blauem oder schwarzem Hintergrund, in dem du Befehle eintippst |
| **Editor** (Notepad) | das einfache Windows-Schreibprogramm |
| **Handy** | die Tanzlexikon-App oder die Android-Einstellungen |

### PowerShell öffnen

1. Drücke die **Windows-Taste**, tippe `PowerShell` und drücke **Enter**.
2. Es öffnet sich ein Fenster, in dem z. B. `PS C:\Users\<Dein Name>>` steht. Dahinter blinkt der Cursor.

### Einen Befehl eingeben

1. Kopiere den Befehl aus dem grauen Kasten in dieser Anleitung (markieren, **Strg+C**).
2. Klicke in das PowerShell-Fenster und füge ihn mit einem **Rechtsklick** ein (oder **Strg+V**).
3. Drücke **Enter**.
4. Warte, bis wieder `PS C:\…>` mit blinkendem Cursor erscheint. Erst dann kommt der nächste Befehl.

Steht in einem Kasten mehr als eine Zeile, kannst du alle Zeilen zusammen kopieren und einfügen.

### Zu Beginn: in den App-Ordner wechseln

Die meisten Befehle müssen **im Ordner der App** laufen. Gib deshalb nach dem Öffnen von PowerShell immer zuerst diese beiden Zeilen ein:
```
cd "$HOME\repos\tanzlexikon"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```
**Geklappt, wenn:** vorne jetzt `PS C:\Users\<Dein Name>\repos\tanzlexikon>` steht.

Wenn du PowerShell schließt und später neu öffnest, gibst du diese beiden Zeilen wieder ein.

---

## 1. Den Schlüssel einmalig anlegen

Wo: **PowerShell** (die beiden Zeilen aus „Zu Beginn“ hast du schon eingegeben).

Dein Passwort tippst du nur in dein eigenes Fenster und gibst es an niemanden weiter, auch nicht an Claude.

1. Gib ein:
   ```
   New-Item -ItemType Directory -Force "$HOME\tanzlexikon-schluessel"
   & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore "$HOME\tanzlexikon-schluessel\tanzlexikon.jks" -alias tanzlexikon -keyalg RSA -keysize 4096 -validity 10000
   ```
   Die erste Zeile legt einen Ordner `tanzlexikon-schluessel` in deinem Benutzerordner an. Die zweite erzeugt darin den Schlüssel. Der Ordner liegt bewusst **nicht** im App-Ordner, damit der Schlüssel nie im Internet landet.
2. Jetzt stellt das Programm dir Fragen. Beantworte jede und drücke danach **Enter**:
   - **„Keystore-Passwort eingeben“** und danach **„Neues Passwort erneut eingeben“**: Denk dir ein langes Passwort aus und tippe es zweimal ein. **Beim Tippen erscheinen keine Zeichen.** Das ist normal. Schreib das Passwort sofort in deinen Passwortmanager oder auf Papier.
     Verwende im Passwort **keine Umlaute, kein ß und keinen Rückwärtsschrägstrich `\`**.
   - **„Wie lautet Ihr Vor- und Nachname?“**: deinen Namen eintippen.
   - Alle weiteren Fragen (Organisation, Ort, Land …): einfach **Enter** drücken.
   - **„Ist CN=… richtig?“**: `ja` tippen.
3. **Geklappt, wenn:** am Ende eine Zeile mit `[tanzlexikon.jks wird gespeichert]` (oder ähnlich) steht und wieder `PS C:\…>` erscheint.

### Die Passwort-Datei anlegen

Damit das Bauen der App deinen Schlüssel findet, braucht sie eine kleine Textdatei namens `keystore.properties` im App-Ordner. Diese Datei wird nie ins Internet hochgeladen.

1. Wo: **PowerShell**. Gib ein:
   ```
   New-Item keystore.properties -ItemType File; notepad keystore.properties
   ```
   Es öffnet sich der **Editor** mit einer leeren Datei.
2. Wo: **Editor**. Kopiere diese vier Zeilen hinein:
   ```
   storeFile=C:/Users/<Dein Name>/tanzlexikon-schluessel/tanzlexikon.jks
   storePassword=<dein Passwort>
   keyAlias=tanzlexikon
   keyPassword=<dein Passwort>
   ```
3. Ersetze jetzt:
   - `<Dein Name>` durch den Namen deines Windows-Benutzerordners. Das ist der Name, der in PowerShell hinter `C:\Users\` steht.
   - beide `<dein Passwort>` durch dein Passwort aus Schritt 2. Es ist **zweimal dasselbe**.

   Die spitzen Klammern `< >` gehören nicht dazu. Im Pfad stehen **normale Schrägstriche `/`**, keine `\`.
   Beispiel: `storeFile=C:/Users/anna/tanzlexikon-schluessel/tanzlexikon.jks`
4. Speichere mit **Strg+S** und schließe den Editor.

## 2. Den Schlüssel sichern

Wo: **Windows-Explorer** (der normale Dateimanager).

1. Öffne den Explorer, tippe oben in die Adresszeile `%USERPROFILE%\tanzlexikon-schluessel` und drücke **Enter**.
2. Dort liegt die Datei `tanzlexikon.jks`. Kopiere sie an **zwei** sichere Orte, zum Beispiel:
   - auf einen USB-Stick,
   - als Anhang in deinen Passwortmanager oder in einen privaten Cloud-Ordner.
3. Bewahre das Passwort ebenfalls sicher auf, z. B. im Passwortmanager.

**Warum das so wichtig ist:** Ohne diese Datei und das Passwort kannst du nie wieder ein Update für die schon installierten Apps bauen. Alle müssten die App dann löschen und neu installieren und verlören dabei ihre Figuren.

## 3. Die richtige Version bauen

Wo: **PowerShell**, im App-Ordner (siehe „Zu Beginn“).

1. Gib ein:
   ```
   .\gradlew.bat assembleRelease
   ```
   Beim ersten Mal kann das einige Minuten dauern.
   **Geklappt, wenn:** am Ende `BUILD SUCCESSFUL` steht.
2. Prüfe, ob die App mit deinem Schlüssel unterschrieben ist:
   ```
   & "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
   ```
   **Geklappt, wenn:** in der Ausgabe `Signer #1 certificate DN: CN=<dein Name>` steht.
   Steht dort `Android Debug` oder kommt die Meldung, dass die Datei nicht existiert, hat das Bauen die Passwort-Datei nicht gefunden. Prüfe dann in `keystore.properties` den Pfad (normale `/`) und das Passwort.

Die fertige App-Datei liegt jetzt hier:
`C:\Users\<Dein Name>\repos\tanzlexikon\app\build\outputs\apk\release\app-release.apk`

## 4. Auf deinem eigenen Handy umsteigen

Die Testversion und die richtige Version haben verschiedene Schlüssel. Darum kann die eine nicht über die andere installiert werden. Du musst die Testversion vorher löschen, **und dabei gehen ihre Daten verloren**. Halte deshalb genau diese Reihenfolge ein:

1. Wo: **Handy**, in der Tanzlexikon-App. Tippe auf **Einstellungen → Alles exportieren**. Du wählst selbst, wo die Sicherung gespeichert wird, z. B. im Ordner Download oder auf Google Drive. Merke dir den Ort.
2. Wo: **Handy**. Drücke lange auf das App-Symbol und wähle **Deinstallieren**.
3. Verbinde das Handy per USB-Kabel mit dem Rechner. Dann wo: **PowerShell**, im App-Ordner:
   ```
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install app\build\outputs\apk\release\app-release.apk
   ```
   **Geklappt, wenn:** `Success` erscheint.
4. Wo: **Handy**. Öffne die App, tippe auf **Einstellungen → Importieren** und wähle die Sicherung aus Schritt 1.
5. **Geklappt, wenn:** alle Tänze und Figuren mit ihren Videos wieder da sind.

Ab jetzt installierst du auf diesem Handy nur noch die richtige Version. Der grüne ▶-Knopf in Android Studio würde wieder eine Testversion aufspielen wollen.

## 5. Die App an andere weitergeben

1. Wo: **Rechner**. Schicke die Datei `app-release.apk` (Ort siehe Ende von Abschnitt 3) per Messenger oder Mail an die andere Person.
2. Wo: **Handy der anderen Person**:
   1. Die Datei antippen.
   2. Android fragt, ob die App, mit der die Datei geöffnet wurde (z. B. WhatsApp oder „Dateien“), **„Unbekannte Apps installieren“** darf. Den Schalter einschalten, zurückgehen und **Installieren** tippen.
   3. Warnt Google Play Protect vor einer unbekannten App, tippe auf **Trotzdem installieren**.

### Hinweis: Googles Entwicklerprüfung

Google führt nach und nach ein, dass Android-Handys nur noch Apps von **bestätigten Entwicklern** installieren, auch außerhalb des Play Store. Stand November 2026:
- Seit dem 30.09.2026 gilt das nur in Brasilien, Indonesien, Singapur und Thailand. Für Deutschland hat Google „2027“ angekündigt, aber noch kein genaues Datum.
- **Was die andere Person dann tun kann:** In den Entwickleroptionen des Handys einmalig **„Apps von nicht bestätigten Entwicklern zulassen“** einschalten. Danach gilt eine Wartezeit von 24 Stunden. Anschließend lässt sich die App mit einer Warnung installieren. Wird die Einstellung später wieder ausgeschaltet, klappen Updates nicht mehr.
- **Was du tun kannst:** In der [Android Developer Console](https://android.google.com/developerconsole) ein kostenloses Konto für **eingeschränkte Verteilung** anlegen. Es ist für Hobby-Apps auf wenigen Handys gedacht (Google nennt mal 20, mal 50 Geräte). Dort meldest du die App (`io.github.katzfuss11.tanzlexikon`) mit dem Fingerabdruck deines Schlüssels an. Den Fingerabdruck zeigt der Prüfbefehl aus Abschnitt 3 in der Zeile `SHA-256 digest` an. Claude hilft dir gern dabei.
- Installation per USB-Kabel (wie in Abschnitt 4) ist davon nicht betroffen.

Aktuelle Infos: <https://developer.android.com/developer-verification>

## 6. Eine neue Version veröffentlichen

Die App schaut beim Start (und unter **Einstellungen → Nach Updates suchen**) auf GitHub nach, ob es eine neuere Version gibt, und bietet sie dann zum Installieren an. Die Figuren bleiben dabei erhalten.

**Voraussetzung:** Das Projekt auf GitHub ist **öffentlich**. Solange es privat ist, meldet die App „Prüfung nicht möglich“. Ob du es öffentlich machst, entscheidest du. Danach kann jeder den Programmcode sehen, aber nicht deine Figuren oder deinen Schlüssel. Öffentlich machen, wo: **PowerShell**:
```
gh repo edit Katzfuss11/tanzlexikon --visibility public --accept-visibility-change-consequences
```

So veröffentlichst du eine neue Version:

1. **Versionsnummer erhöhen.** Wo: **PowerShell**, im App-Ordner:
   ```
   notepad app\build.gradle.kts
   ```
   Im **Editor** suchst du (mit **Strg+F**) diese beiden Zeilen:
   ```
   versionCode = 1
   versionName = "0.1.0"
   ```
   Erhöhe `versionCode` um 1 (z. B. `1` → `2`) und `versionName` auf die neue Nummer (z. B. `"0.1.0"` → `"0.1.1"`). Speichere mit **Strg+S** und schließe den Editor.
   Ohne höheren `versionCode` lehnt Android das Update ab.
2. **Bauen und prüfen**, wie in Abschnitt 3.
3. **Hochladen.** Wo: **PowerShell**, im App-Ordner. Ersetze `0.1.1` jeweils durch deine neue Nummer und „Was ist neu“ durch eine kurze Beschreibung:
   ```
   git commit -am "Version 0.1.1"
   git push
   gh release create v0.1.1 app\build\outputs\apk\release\app-release.apk --title "Tanzlexikon 0.1.1" --notes "Was ist neu: …"
   ```
   Die Nummer hinter dem `v` muss genau zu `versionName` passen.
4. **Geklappt, wenn:** beim nächsten Start der App auf dem Handy der Hinweis auf die neue Version erscheint, die Installation durchläuft und alle Figuren noch da sind.

## Wenn etwas schiefgeht

- **PowerShell meldet „Unerwartetes Token“ oder „wurde nicht als Name eines Cmdlet … erkannt“:** Der Befehl wurde nicht vollständig kopiert, oder du bist im falschen Ordner. Gib die beiden Zeilen aus „Zu Beginn“ ein und versuche es noch einmal.
- **Das Bauen meldet ein falsches Passwort:** Öffne `keystore.properties` mit `notepad keystore.properties` und prüfe das Passwort auf Tippfehler und Leerzeichen am Zeilenende.
- **Auf dem Handy erscheint „App nicht installiert“ beim Update:** Entweder wurde die neue Version mit einem anderen Schlüssel gebaut, oder der `versionCode` ist nicht höher als vorher. Prüfe beides (Abschnitt 3, Schritt 2, und Abschnitt 6, Schritt 1).
- **Du kommst nicht weiter:** Kopiere die Fehlermeldung aus PowerShell (markieren, Strg+C) und schick sie Claude.

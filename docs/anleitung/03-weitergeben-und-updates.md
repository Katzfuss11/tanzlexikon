# 3 · Weitergeben und Updates veröffentlichen

Voraussetzung: [2 · Die App auf dem Handy starten](02-app-aufs-handy.md) klappt.

Bisher läuft auf dem Handy eine **Debug-Version** aus Android Studio. Um die App an andere weiterzugeben, brauchst du eine **Release-Version**, die mit deinem eigenen Schlüssel unterschrieben (signiert) ist. Android installiert ein Update nur dann über eine vorhandene App, wenn beide mit demselben Schlüssel signiert sind.

Alle Befehle unten laufen in **Git Bash** im Repo-Ordner. Zu Beginn jeder Sitzung:
```
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
```

## 1. Den Signaturschlüssel einmalig anlegen

Den Schlüssel legst du **selbst** an. Die Passwörter tippst du nur in dein eigenes Fenster ein und gibst sie an niemanden weiter, auch nicht an Claude.

1. Lege den Ordner für den Schlüssel an. Er liegt **außerhalb** des Repos, damit der Schlüssel nie auf GitHub landet:
   ```
   mkdir -p ~/tanzlexikon-schluessel
   ```
2. Erzeuge den Schlüssel:
   ```
   "$JAVA_HOME/bin/keytool" -genkeypair -v -keystore ~/tanzlexikon-schluessel/tanzlexikon.jks -alias tanzlexikon -keyalg RSA -keysize 4096 -validity 10000
   ```
   - Zuerst fragt `keytool` zweimal nach einem **Passwort** für die Schlüsseldatei. Beim Tippen erscheinen keine Zeichen, das ist normal. Nimm ein langes Passwort und schreibe es sofort in deinen Passwortmanager.
   - Danach fragt es nach Name, Organisation, Ort usw. Es genügt, bei „Vor- und Nachname“ deinen Namen einzugeben. Die übrigen Fragen bestätigst du mit Enter. Am Ende tippst du `ja`.
   - Falls noch nach einem eigenen Passwort für den Alias gefragt wird: Enter drücken, dann gilt dasselbe Passwort.
3. **Geklappt, wenn:** die Datei `C:\Users\<Dein Name>\tanzlexikon-schluessel\tanzlexikon.jks` existiert.
4. Lege im Repo-Ordner (neben `settings.gradle.kts`) eine Datei `keystore.properties` mit diesem Inhalt an:
   ```
   storeFile=C:/Users/<Dein Name>/tanzlexikon-schluessel/tanzlexikon.jks
   storePassword=<dein Passwort>
   keyAlias=tanzlexikon
   keyPassword=<dein Passwort>
   ```
   Wichtig: Im Pfad stehen **normale Schrägstriche** `/`, keine `\`. Diese Datei steht in `.gitignore` und wird nie committet. Mit `git status` kannst du prüfen, dass sie dort nicht auftaucht.

## 2. Den Schlüssel sichern

Lege die Datei `tanzlexikon.jks` und das Passwort an **zwei** Orten ab, zum Beispiel:
- die `.jks`-Datei auf einem USB-Stick und zusätzlich als Anhang im Passwortmanager,
- das Passwort im Passwortmanager.

**Ohne diesen Schlüssel kannst du nie wieder ein Update für die bereits installierten Apps bauen.** Alle müssten die App dann deinstallieren und verlören dabei ihre Daten.

## 3. Die Release-Version bauen

```
./gradlew assembleRelease
```
**Geklappt, wenn:** `BUILD SUCCESSFUL` erscheint und die Datei `app/build/outputs/apk/release/app-release.apk` existiert.

So prüfst du, dass die APK wirklich mit deinem Schlüssel signiert ist:
```
"$LOCALAPPDATA/Android/Sdk/build-tools/36.0.0/apksigner.bat" verify --print-certs app/build/outputs/apk/release/app-release.apk
```
**Geklappt, wenn:** in der Ausgabe `Signer #1 certificate DN: CN=<dein Name>, …` steht. Heißt es dort `Android Debug` oder entsteht nur eine `app-release-unsigned.apk`, wurde `keystore.properties` nicht gefunden. Prüfe dann Pfad und Dateinamen.

## 4. Von der Debug- zur Release-Version wechseln (dein eigenes Handy)

Debug- und Release-Version haben verschiedene Schlüssel. Deshalb lässt sich die eine nicht über die andere installieren, und **beim Deinstallieren gehen die Daten verloren**. Gehe darum in genau dieser Reihenfolge vor:

1. In der App: **Einstellungen → Alles exportieren**. Du wählst selbst, wo die ZIP-Sicherung gespeichert wird (z. B. Download oder Google Drive). Merke dir den Ort.
2. Die Debug-App deinstallieren (App-Symbol lange drücken → **Deinstallieren**).
3. Die Release-APK installieren, zum Beispiel per Kabel:
   ```
   "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" install app/build/outputs/apk/release/app-release.apk
   ```
4. In der App: **Einstellungen → Importieren** und die Sicherung aus Schritt 1 wählen.
5. **Geklappt, wenn:** alle Tänze und Figuren mit Videos wieder da sind.

Ab jetzt baust du auf diesem Handy nur noch Release-Versionen. Ein ▶ aus Android Studio würde wieder die Debug-Version installieren wollen.

## 5. Die App an andere weitergeben

1. Schicke die Datei `app-release.apk` per Messenger oder Mail.
2. Der Empfänger tippt die Datei auf dem Handy an.
3. Android fragt, ob die App, mit der die Datei geöffnet wurde (z. B. WhatsApp oder Dateien), **„Unbekannte Apps installieren“** darf. Erlauben, zurück, **Installieren**.
4. Google Play Protect warnt eventuell vor einer unbekannten App. Mit **Trotzdem installieren** geht es weiter.

### Hinweis: Googles Entwicklerverifizierung

Google führt schrittweise ein, dass Apps auf zertifizierten Android-Handys nur von **bestätigten Entwicklern** installiert werden dürfen, auch außerhalb des Play Store. Stand November 2026:
- Seit dem 30.09.2026 gilt das nur in Brasilien, Indonesien, Singapur und Thailand. Für die übrigen Länder, auch Deutschland, hat Google „2027“ angekündigt, aber noch kein genaues Datum.
- **Was der Empfänger dann tun kann:** In den Entwickleroptionen gibt es den „erweiterten Ablauf“ („Apps von nicht bestätigten Entwicklern zulassen“). Er wird einmalig eingerichtet und hat eine Wartezeit von 24 Stunden. Danach lassen sich solche Apps mit einer Warnung installieren. Ist der Ablauf später wieder ausgeschaltet, schlagen Updates fehl.
- **Was du tun kannst:** Ein kostenloses Konto für **eingeschränkte Verteilung** in der [Android Developer Console](https://android.google.com/developerconsole) anlegen. Es ist für Hobby-Apps gedacht, die auf wenigen ausdrücklich freigegebenen Geräten laufen. Je nach Google-Seite sind 20 oder 50 Geräte erlaubt. Dort registrierst du den Paketnamen `io.github.katzfuss11.tanzlexikon` mit dem SHA-256-Fingerabdruck deines Schlüssels. `apksigner verify --print-certs` aus Abschnitt 3 zeigt ihn als `SHA-256 digest` an. Das geht nur, solange du den Schlüssel hast (siehe Abschnitt 2).
- Installation per Kabel mit `adb install` ist davon nicht betroffen.

Aktuelle Infos: <https://developer.android.com/developer-verification>

## 6. Eine neue Version veröffentlichen

Die App prüft beim Start (und unter **Einstellungen → Nach Updates suchen**) das neueste Release auf GitHub. Gibt es dort eine höhere Version, bietet sie das Update an.

1. In `app/build.gradle.kts` `versionCode` um 1 erhöhen (z. B. `1` → `2`) und `versionName` anheben (z. B. `"0.1.0"` → `"0.1.1"`). Ohne höheren `versionCode` lehnt Android das Update ab.
2. Release bauen (Abschnitt 3) und committen.
3. Hochladen:
   ```
   git push
   gh release create v0.1.1 app/build/outputs/apk/release/app-release.apk --title "Tanzlexikon 0.1.1" --notes "Was ist neu: …"
   ```
   Der Tag muss zum `versionName` passen (`v` + Version). Am Release muss genau eine `.apk` hängen.
4. **Geklappt, wenn:** beim nächsten Start der App der Update-Hinweis erscheint, die Installation durchläuft und alle Daten erhalten bleiben.

**Wichtig:** Die Update-Prüfung funktioniert nur, wenn das Repo auf GitHub **öffentlich** ist. Bei einem privaten Repo meldet die App „Prüfung nicht möglich“. Öffentlich machen geht so (das entscheidest du selbst, danach kann jeder den Code sehen):
```
gh repo edit Katzfuss11/tanzlexikon --visibility public --accept-visibility-change-consequences
```

## Wenn etwas schiefgeht

- **„App nicht installiert“ beim Update:** Die neue APK wurde mit einem anderen Schlüssel signiert, oder `versionCode` ist nicht höher. Prüfe beides mit `apksigner verify --print-certs` und in `app/build.gradle.kts`.
- **Release-Bau meldet ein falsches Passwort:** Prüfe `keystore.properties` auf Tippfehler und Leerzeichen am Zeilenende.

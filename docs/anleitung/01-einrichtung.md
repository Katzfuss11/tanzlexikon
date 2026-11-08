# 1 · Rechner und Handy einrichten

Diese Schritte machst du **einmal**. Danach kann die App auf deinem Handy gestartet werden.
Bei jedem Schritt steht, woran du erkennst, dass er geklappt hat.

## Schritt 1: Android Studio installieren

Android Studio ist das kostenlose Werkzeug von Google, mit dem Android-Apps gebaut werden.

1. Öffne PowerShell und gib ein:
   ```
   winget install Google.AndroidStudio
   ```
   Bestätige die Windows-Abfrage („Möchten Sie zulassen …“) mit **Ja**.
   *Alternative:* den Installer von https://developer.android.com/studio herunterladen und mit den vorgeschlagenen Einstellungen durchklicken.
2. **Geklappt, wenn:** Im Startmenü steht „Android Studio“.

## Schritt 2: Android Studio einmal starten

1. Starte Android Studio über das Startmenü.
2. Die Frage nach dem Import von Einstellungen beantwortest du mit **„Do not import settings“**.
3. Im Einrichtungsassistenten wählst du **„Standard“** und klickst dich mit **Next** durch. Alle Lizenzen akzeptierst du mit **Accept**, dann **Finish**.
4. Jetzt lädt Android Studio das „Android SDK“ herunter (mehrere GB, das dauert eine Weile).
5. **Geklappt, wenn:** Das Fenster „Welcome to Android Studio“ erscheint. Du kannst Android Studio danach wieder schließen.

## Schritt 3: Entwickleroptionen auf dem Handy einschalten

Das brauchst du, damit der Rechner die App auf dein Handy spielen darf.

1. Auf dem Handy: **Einstellungen → Über das Telefon**.
2. Ganz unten tippst du **7-mal** auf **„Build-Nummer“**. Eventuell fragt das Handy nach deiner PIN.
   **Geklappt, wenn:** die Meldung „Sie sind jetzt Entwickler“ erscheint.
3. Dann gehst du auf **Einstellungen → System → Entwickleroptionen** und schaltest **„USB-Debugging“** ein.

## Schritt 4: Handy anschließen

1. Verbinde das Handy per USB-Kabel mit dem Rechner.
2. Auf dem Handy erscheint „USB-Debugging zulassen?“. Setze den Haken bei **„Von diesem Computer immer zulassen“** und tippe auf **Zulassen**.
   Falls nichts erscheint: Ziehe die Benachrichtigungsleiste herunter, tippe auf die USB-Meldung und wähle „Dateiübertragung“.

## Schritt 5: Prüfen

Gib in PowerShell ein:
```
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```
**Geklappt, wenn:** unter „List of devices attached“ eine Zeile mit einer Kennung und dem Wort **`device`** steht.
Steht dort `unauthorized`, hast du die Abfrage aus Schritt 4 noch nicht bestätigt. Steck das Kabel dann neu ein.

Gib Claude Bescheid, wenn das klappt.

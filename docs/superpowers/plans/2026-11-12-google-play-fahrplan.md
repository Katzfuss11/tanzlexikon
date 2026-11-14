# Fahrplan: Tanzlexikon in Google Play veröffentlichen

**Stand:** 12.11.2026 – Richtung entschieden, Einzelheiten noch offen.

**Ziel:** Die App sollen auch Leute bekommen, die der Entwickler nicht kennt. Dafür hängt in der Tanzschule ein QR-Code (die Erlaubnis liegt vor). Scannen führt in den Play Store, dort genügt ein Tipp auf „Installieren“, ohne Play-Protect-Warnung und mit automatischen Updates.

**Warum Play:** Eine APK von GitHub zeigt Fremden die Warnung mit „Trotzdem installieren“, und ab 2027 kommt für nicht angemeldete Apps noch ein umständlicher Ablauf mit 24 Stunden Wartezeit dazu. Das kostenlose Konto für höchstens 20 Geräte und ein rein geschlossener Test passen nicht zu unbekannten Leuten. F-Droid verlangt, dass zuerst die F-Droid-App installiert wird, und die bringt selbst wieder eine Warnung mit.

**Bleibt:** Die GitHub-Releases mit Selbst-Update laufen als zweiter Weg weiter.

---

## Phase 1 – Entwicklerkonto (Nutzer selbst)

- [ ] Google-Play-Entwicklerkonto anlegen, Typ **privat** (25 $ einmalig, Ausweisprüfung).
- [ ] Bedenken: Der Entwicklername und eine Kontakt-E-Mail werden im Store öffentlich angezeigt. Eventuell eine eigene E-Mail-Adresse dafür anlegen.
- [ ] Warten, bis die Prüfung durch ist (dauert ein paar Tage).

## Phase 2 – App für Play vorbereiten (gemeinsam, eigener Branch)

- [ ] **Zwei Varianten** (product flavors): `play` ohne „Nach Updates suchen“, ohne APK-Download und ohne die Berechtigung `REQUEST_INSTALL_PACKAGES`; `github` wie bisher.
- [ ] Dieselbe `applicationId` (`io.github.katzfuss11.tanzlexikon`) für beide Varianten, damit ein Wechsel von GitHub zu Play ohne Neuinstallation möglich bleibt.
- [x] Keine Galerie-Berechtigungen (`READ_MEDIA_*`), die Play-Richtlinie zu Foto- und Video-Berechtigungen verlangt sonst eine Begründung. Auswahl über den Photo Picker, die Kamera schreibt in einen eigenen Galerie-Eintrag (Branch `ohne-galerie-berechtigung`, 14.11.).
- [ ] Ziel-API auf dem Stand halten, den Play aktuell verlangt (beim Bauen prüfen).
- [ ] App Bundle (`.aab`) für Play bauen, Versionsverlauf/CLAUDE.md-Regeln beachten.

## Phase 3 – Store-Unterlagen

- [ ] **Datenschutzerklärung** als Webseite (z. B. GitHub Pages im Repo). Inhalt: Die App speichert alles nur lokal, sie überträgt nichts, und die Play-Variante fragt auch nicht nach Updates.
- [ ] Formular **Datensicherheit** in der Play Console („keine Daten erhoben/geteilt“).
- [ ] Altersfreigabe-Fragebogen, Zielgruppe, App-Kategorie.
- [ ] Store-Eintrag: Kurz- und Langbeschreibung (Deutsch), Symbol 512×512, Feature-Grafik 1024×500, mindestens 2 Screenshots.

## Phase 4 – App-Signatur

- [ ] Bei der Einrichtung von Play App Signing **den vorhandenen Schlüssel** (`$HOME\tanzlexikon-schluessel\tanzlexikon.jks`) als App-Signaturschlüssel hochladen, statt Google einen neuen erzeugen zu lassen. Nur dann können GitHub-Nutzer später nahtlos auf die Play-Version umsteigen.
- [ ] Vorher eine Sicherungskopie des Schlüssels prüfen und das Vorgehen beim Hochladen gemeinsam Schritt für Schritt durchgehen.

## Phase 5 – Geschlossener Test (Pflicht: mindestens 12 Tester über 14 Tage)

- [ ] Eine Google-Gruppe als Testerliste anlegen; wer beitritt, ist Tester.
- [ ] Erster Aushang in der Tanzschule: „Teste die App vorab“ mit QR-Code auf eine kleine Seite, die den Ablauf erklärt (Gruppe beitreten, dann über den Test-Link installieren).
- [ ] 14 Tage ohne Unterbrechung laufen lassen; den Testkanal nicht pausieren, sonst beginnt die Frist neu.
- [ ] Während des Tests mindestens ein Update ausliefern und Rückmeldungen sammeln (zählt als echte Nutzung).

## Phase 6 – Veröffentlichung

- [ ] In der Play Console die Freischaltung für die Produktion beantragen (Fragen zum Test beantworten).
- [ ] Nach der Freigabe: Produktions-Release, Store-Eintrag öffentlich.
- [ ] Endgültiger QR-Code zeigt auf `https://play.google.com/store/apps/details?id=io.github.katzfuss11.tanzlexikon`.
- [ ] Aushang in der Tanzschule austauschen; README auf GitHub um den Play-Link ergänzen.

## Später (2027)

- [ ] Googles Entwicklerprüfung für Apps außerhalb von Play: Paket und Schlüssel für die GitHub-Variante über die Play Console anmelden, sobald das für Deutschland gilt.

## Zurückgestellt: iPhone-Version

Nicht vor dem Play-Start. Erst entscheiden, wenn klar ist, wie viele Leute nach einer iPhone-Version fragen.
- Voraussetzungen: ein eigener Mac (den Arbeits-Mac nicht verwenden), ein iPhone zum Testen und das Apple Developer Program (99 € pro Jahr).
- Mögliche Wege ohne eigenen Mac: über GitHub Actions bauen (macOS-Rechner, für öffentliche Repos kostenlos) oder einen Mac in der Cloud mieten. Ohne eigenes iPhone wäre das aber Testen im Blindflug.
- Technik: Umbau auf Compose Multiplatform. Neu schreiben müsste man das Abspielen der Videos (ExoPlayer gibt es nur für Android), das Teilen, die Sicherung und die Medienauswahl.
- Verteilung: App Store; vorab TestFlight mit öffentlichem Link (als QR-Code möglich, bis zu 10.000 Tester).

---

## Noch offene Punkte (vor Phase 2 klären)

- Welchen Namen und welche E-Mail-Adresse soll der Store öffentlich zeigen?
- Text und Gestaltung der beiden Aushänge (Testphase und endgültig).
- Wo liegt die Datenschutzerklärung, und braucht sie eine Kontaktadresse?
- Sollen Nutzer der GitHub-Variante aktiv auf Play hingewiesen werden (z. B. ein Hinweis in der App)?

**Zeitrahmen grob:** etwa 3–4 Wochen von der Konto-Anlage bis zum öffentlichen Store-Eintrag, davon mindestens 14 Tage Testphase plus Googles Prüfungen.

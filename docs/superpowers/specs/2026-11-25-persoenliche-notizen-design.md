# Persönliche Notizen – Entwurf (kurz)

Freigegeben am 25.11.2026.

## Wozu

Ein eigener Bereich pro Figur, in dem man festhält, worauf man selbst besser achten muss
(z. B. „linke Hand nicht so hoch“). Das ist privat und soll beim Teilen nie mitgehen.

## Ziele / Nicht-Ziele

- Ziel: ein Textfeld „Meine Notizen“ je Figur, unterhalb von Leader und Follower.
- Ziel: Notizen verlassen das Handy nur über die eigene Sicherung, nie über das Teilen.
- Nicht-Ziel: Medien in den Notizen (ein Textfeld reicht).
- Anti-Ziel: Notizen tauchen irgendwo im geteilten Text oder in einer `.tanzfigur`-Datei auf.

## Ansätze

1. **Viertes Textfeld an der Figur (gewählt).** Neue Spalte `noteText` in `figure`, analog zu
   `leaderText`/`followerText`. Sicherung, Suche und Papierkorb funktionieren mit wenig Aufwand mit.
2. Eigene Tabelle für Notizen – kein Vorteil, da genau eine Notiz pro Figur; mehr Aufwand bei
   Sicherung, Suche, Papierkorb.
3. Notizen außerhalb der Datenbank (eigene Datei) – müssten in der Sicherung extra behandelt
   werden; verworfen.

## Verhalten

- **Anzeige:** Abschnitt „Meine Notizen“ in neutraler Farbe, ohne Schloss und ohne Hinweis
  (Nutzerwunsch 25.11.: die Info ist nur beim Teilen wichtig). Steht nach Follower, vor „Siehe auch“.
  Leer → beim Ansehen ausgeblendet; beim Bearbeiten immer da.
- **Teilen-Dialog:** Hat die Figur eine Notiz, steht unten „„Meine Notizen“ werden nicht mitgeschickt.“
- **Nur Text**, kein „Medium hinzufügen“.
- **Teilen:** weder im geteilten Text noch in der `.tanzfigur`-Datei. Empfangene Figuren haben
  leere Notizen.
- **Sicherung:** Notizen sind enthalten (neues optionales Feld im Archiv, Standard leer). Ältere
  App-Versionen öffnen die Sicherung weiter und ignorieren das Feld (`ignoreUnknownKeys`).
- **Suche:** findet auch Text in Notizen.
- **Papierkorb:** Notiz bleibt beim Löschen/Wiederherstellen erhalten.
- **Datenbank:** Migration 2 → 3 fügt die Spalte mit Standard `''` hinzu.
- **Versionsverlauf:** Punkt im Abschnitt 0.1.4.
- **Tests:** Notiz fehlt beim Teilen (Text und Datei), ist in der Sicherung, überlebt
  Sicherung → Wiederherstellen, wird gefunden, Migration.

## Dem Bauenden überlassen

Genaue Farbtöne, Symbol, Platzhaltertext im Eingabefeld, ob `Section` erweitert wird oder die
Notiz bewusst außerhalb von `Section` bleibt (Medien dürfen ihr nie zugeordnet werden).

Branch `persoenliche-notizen`, zweigt von `ohne-galerie-berechtigung` ab.

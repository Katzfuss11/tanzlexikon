# Tanzlexikon – Hinweise für die Arbeit am Projekt

## Versionsverlauf pflegen

Die App zeigt unter Einstellungen → Über → Versionsverlauf, was sich in jeder Version geändert hat.
Quelle ist `app/src/main/assets/versionsverlauf.md`.

- Jede für Nutzer spürbare Änderung bekommt gleich beim Bauen einen Punkt im obersten Abschnitt –
  in einfachen Worten, aus Sicht von jemandem, der die App benutzt (keine Technikbegriffe).
- Neue Version anfangen: `versionCode`/`versionName` in `app/build.gradle.kts` erhöhen und oben
  einen Abschnitt `## <versionName>` (ohne Datum = „in Arbeit“) anlegen.
- Beim Veröffentlichen: das Datum ergänzen (`## 0.1.3 · 12.11.2026`) und die Punkte als Text
  für das GitHub-Release übernehmen.
- `VersionsverlaufTest` schlägt fehl, wenn der oberste Abschnitt nicht zur `versionName` passt.

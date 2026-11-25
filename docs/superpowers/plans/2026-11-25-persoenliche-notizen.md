# Persönliche Notizen – Bauplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Jede Figur bekommt ein privates Textfeld „Meine Notizen“, das in der Sicherung steckt, aber nie geteilt wird.

**Architecture:** Neue Spalte `noteText` in `figure` (Room-AutoMigration 2 → 3). Die Notiz bleibt bewusst **außerhalb** von `enum Section`, damit ihr nie Medien zugeordnet werden können. Das Archivformat bekommt ein optionales Feld `ArchFigure.noteText`, das nur die Sicherung schreibt und nur `replaceAll` liest.

**Tech Stack:** Kotlin, Jetpack Compose, Room, kotlinx.serialization, Robolectric-Tests.

**Spec:** `docs/superpowers/specs/2026-11-25-persoenliche-notizen-design.md`

## Global Constraints

- Branch `persoenliche-notizen` (zweigt von `ohne-galerie-berechtigung` ab). Version bleibt 0.1.4/5.
- Überschrift „Meine Notizen“, Hinweis „Wird nicht geteilt“, Platzhalter im Eingabefeld „Worauf ich achten will“.
- Notizen nie im geteilten Text, nie in einer `.tanzfigur`-Datei; empfangene Figuren haben immer leere Notiz – auch wenn eine Datei (manipuliert) eine `noteText` enthält.
- Sicherungen ohne `noteText` (ältere Versionen) lassen sich weiter einlesen (Notiz = leer).
- Versionsverlauf-Punkt im Abschnitt `## 0.1.4`, ohne Technikbegriffe (siehe CLAUDE.md).
- Tests: `.\gradlew.bat testDebugUnitTest` (in PowerShell im Repo-Wurzelordner; in Bash `./gradlew` mit `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`).

## Review Focus

1. Eine empfangene `.tanzfigur`, deren JSON `noteText` enthält → Notiz wird beim Übernehmen verworfen (Test in Task 2).
2. Entwurf, der vor dem Update im gespeicherten Zustand lag (ohne `noteText`) → lässt sich weiter öffnen, Notiz leer (Test in Task 3).
3. Figur in den Papierkorb und zurück → Notiz unverändert (Test in Task 1).
4. Ansicht einer Figur, die nur eine Notiz hat → „Diese Figur ist noch leer“ erscheint **nicht**, der Notizbereich schon (Task 3, Handyprüfung).
5. Suche nach einem Wort, das nur in der Notiz steht → Figur wird gefunden (Test in Task 1).

---

### Task 1: Datenbank, Speichern, Suche

**Files:**
- Modify: `app/src/main/java/io/github/katzfuss11/tanzlexikon/data/Entities.kt` (FigureEntity)
- Modify: `app/src/main/java/io/github/katzfuss11/tanzlexikon/data/TanzDatabase.kt` (version 3, `AutoMigration(from = 2, to = 3)`)
- Modify: `app/src/main/java/io/github/katzfuss11/tanzlexikon/data/LexikonDao.kt` (`updateFigureTexts`)
- Modify: `app/src/main/java/io/github/katzfuss11/tanzlexikon/lexikon/LexikonRepository.kt` (`saveFigure`, `search`)
- Create (generiert): `app/schemas/io.github.katzfuss11.tanzlexikon.data.TanzDatabase/3.json`
- Test: `app/src/test/java/.../data/MigrationTest.kt`, `app/src/test/java/.../lexikon/LexikonRepositoryTest.kt`

**Interfaces:**
- Produces: `FigureEntity.noteText: String` mit `@ColumnInfo(defaultValue = "") val noteText: String = ""` (nach `followerText`).
- Produces: `LexikonDao.updateFigureTexts(id: Long, name: String, generalText: String, leaderText: String, followerText: String, noteText: String)`.
- Produces: `LexikonRepository.saveFigure(id, name, generalText, leaderText, followerText, noteText: String, media)` – `noteText` als neuer Parameter direkt vor `media`.

- [ ] **Step 1: Failing tests**
  - `MigrationTest.migrate2To3_keepsAllData_noteEmpty`: Datenbank nach `2.json` anlegen (wie `createVersion1Database`, Hilfsfunktion auf Versionsnummer verallgemeinern), Figur einfügen, `db.version = 2`, über `TanzDatabase.build(fileName = …)` öffnen → Figur unverändert, `noteText == ""`.
  - Bestehender `migrate1To2…`-Test läuft weiter durch bis Version 3.
  - `LexikonRepositoryTest.saveFigure_storesNote`: speichern mit `noteText = "Linke Hand tiefer"` → `observeFigure`/Detail liefert sie.
  - `LexikonRepositoryTest.search_findsWordOnlyInNote`: Notiz „Blickkontakt“, sonst nichts → `search("blickkontakt")` enthält die Figur.
  - `LexikonRepositoryTest.trashAndRestore_keepsNote`: Notiz setzen, `trashFigure`, wiederherstellen → `noteText` unverändert.
- [ ] **Step 2:** `.\gradlew.bat testDebugUnitTest` → die neuen Tests schlagen fehl (Kompilierfehler bzw. FAIL).
- [ ] **Step 3: Implementieren.** Spalte, Version 3 + AutoMigration (Kommentar „2 → 3: Spalte noteText, Standard leer“), DAO-Query um `noteText = :noteText` ergänzen, `saveFigure` reicht durch, `search` nimmt `f.noteText` in die Liste auf. Damit `FigurViewModel` kompiliert, legt dieser Task schon `FigurDraft.noteText` an (siehe Task 3 Interfaces) und reicht es in `save`/`draftOf` durch – nie fest `""` übergeben, sonst löscht Speichern die Notiz.
- [ ] **Step 4:** Tests grün; `3.json` ist entstanden.
- [ ] **Step 5: Commit** `feat: Notizspalte an der Figur (Datenbank 3), Speichern und Suche`

### Task 2: Sicherung ja, Teilen nein

**Files:**
- Modify: `app/src/main/java/.../transfer/ArchiveFormat.kt` (`ArchFigure`)
- Modify: `app/src/main/java/.../lexikon/LexikonRepository.kt` (`snapshot`, `replaceAll`, `insertArchFigure`)
- Prüfen, nicht ändern: `share/FigureShare.kt`, `share/ShareText.kt` (lesen keine Notiz – Tests sichern das ab)
- Test: `transfer/BackupServiceTest.kt`, `share/FigureShareTest.kt`, `share/ShareTextTest.kt`, `lexikon/LexikonRepositoryTest.kt`

**Interfaces:**
- Consumes: `FigureEntity.noteText` (Task 1).
- Produces: `ArchFigure.noteText: String = ""` als letztes Feld, KDoc „Persönliche Notiz. Nur in Sicherungen; beim Empfang einer Figur ignoriert.“

- [ ] **Step 1: Failing tests**
  - `BackupServiceTest.exportThenImport_keepsNote`: Notiz „Nicht ziehen 🙂“ → Export → Import in frische DB → Notiz gleich.
  - `BackupServiceTest.import_oldBackupWithoutLinkFields_works` zusätzlich: importierte Figur hat `noteText == ""`.
  - `FigureShareTest.writeFigureFile_neverContainsNote`: Figur mit Notiz „GEHEIM-123“, `.tanzfigur` schreiben (mit und ohne Medien) → `manifest…figures.single().noteText == ""` **und** das rohe JSON (`tanzlexikon.json`) enthält „GEHEIM-123“ nicht.
  - `ShareTextTest.shareText_neverContainsNote`: `buildShareText` mit Notiz „GEHEIM-123“ → Text enthält sie nicht.
  - `LexikonRepositoryTest.importFigure_ignoresNoteInFile`: `importFigure(danceId, ArchFigure(…, noteText = "fremd"), …)` → neue Figur hat `noteText == ""`.
- [ ] **Step 2:** Tests laufen → Backup- und Import-Test schlagen fehl.
- [ ] **Step 3: Implementieren.** `snapshot` setzt `noteText = f.noteText`. `insertArchFigure` bekommt Parameter `noteText: String`; `replaceAll` übergibt `f.noteText`, `importFigure` übergibt `""`. `FigureShare` bleibt unverändert (Feld hat Standard leer).
- [ ] **Step 4:** Alle Tests grün.
- [ ] **Step 5: Commit** `feat: Notizen in der Sicherung, nie beim Teilen`

### Task 3: Bildschirm „Meine Notizen“ und Versionsverlauf

**Files:**
- Modify: `app/src/main/java/.../ui/figur/FigurViewModel.kt` (Feld und Durchreichen kommen schon aus Task 1; hier nur Tests)
- Modify: `app/src/main/java/.../ui/figur/FigurScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/assets/versionsverlauf.md`
- Test: `app/src/test/java/.../ui/figur/FigurViewModelTest.kt`

**Interfaces:**
- Produces: `FigurDraft.noteText: String = ""` (nach `followerText`, mit Standard, damit alte gespeicherte Entwürfe lesbar bleiben).
- Strings: `notes_title` = „Meine Notizen“, `notes_private` = „Wird nicht geteilt“, `notes_hint` = „Worauf ich achten will“.

- [ ] **Step 1: Failing tests** in `FigurViewModelTest`:
  - `edit_thenSave_storesNote`: bearbeiten, `updateDraft { it.copy(noteText = "Arm tiefer") }`, speichern → Repository hat die Notiz.
  - `oldSavedDraftWithoutNote_isRestored`: gespeicherter Zustand mit Entwurfs-JSON ohne `noteText` → Entwurf wird geladen, `noteText == ""`.
- [ ] **Step 2:** Tests schlagen fehl.
- [ ] **Step 3: Implementieren.**
  - ViewModel: schon in Task 1 erledigt; die Tests hier sichern es ab.
  - Screen: neue Composable `NotesCard(content)` im Stil von `SectionCard`, Farben `surfaceVariant` / `onSurfaceVariant`, Überschrift mit `Icons.Default.Lock` (klein, 16 dp) + `notes_title`, darunter `notes_private` in `labelSmall`. Steht nach den drei Bereichen, vor `SeeAlsoSection`.
  - Bearbeiten: immer sichtbar, `OutlinedTextField` mit `notes_hint`, `minLines = 3`, kein „Medium hinzufügen“.
  - Ansehen: nur wenn `f.noteText.isNotBlank()`. „Figur ist leer“-Hinweis nur, wenn auch die Notiz leer ist.
  - `EmpfangScreen` bleibt unverändert.
  - Versionsverlauf, Punkt unter `## 0.1.4`: „Jede Figur hat jetzt einen Bereich „Meine Notizen“ für das, worauf du selbst achten willst. Er wird beim Teilen nie mitgeschickt, steckt aber in deiner Sicherung.“
- [ ] **Step 4:** Alle Tests grün; `.\gradlew.bat assembleDebug` läuft durch.
- [ ] **Step 5: Handyprüfung** (Debug-App per adb): Notiz schreiben, speichern, ansehen; Figur nur mit Notiz; teilen als Nachricht und als Datei → Notiz fehlt; Hell und Dunkel ansehen.
- [ ] **Step 6: Commit** `feat: Bereich „Meine Notizen“ in der Figur`

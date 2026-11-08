# Tanzlexikon – Bauplan (Implementation Plan)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eine Android-App „Tanzlexikon“, in der man Tanzfiguren nach Kategorie und Tanz ablegt (Texte für Allgemein, Leader und Follower, dazu Videos, Bilder und YouTube-Links), sie mit Papierkorb schützt, sichert, einzeln teilt und über GitHub-Releases aktualisiert.

**Architecture:** Ein App-Modul in Kotlin mit Jetpack Compose. Die Daten liegen in einer Room-Datenbank, und die gesamte Fachlogik (Lösch-Regeln, Papierkorb, Wiederherstellen, Suche) sitzt in einem einzigen `LexikonRepository`, das die Bildschirme über ViewModels nutzen. Medien sind Content-URI-Verweise auf die Galerie. Export, Teilen-Datei und Import nutzen dasselbe ZIP-Format (`ArchiveWriter`/`ArchiveReader`). Pakete sind nach Funktion geschnitten (`data`, `lexikon`, `media`, `transfer`, `share`, `update`, `ui/*`), nicht nach Schichten.

**Tech Stack:** Kotlin 2.1.0, AGP 8.7.3, Gradle 8.11.1, Compose BOM 2024.12.01 (Material 3), Navigation Compose 2.8.5, Room 2.6.1 + KSP 2.1.0-1.0.29, kotlinx-serialization-json 1.7.3, Coil 2.7.0, `sh.calvin.reorderable:reorderable` 2.4.3, Tests: JUnit 4, Robolectric 4.14.1, kotlinx-coroutines-test 1.9.0, Room in-memory. (Das sind erprobte, zueinander passende Versionen. Der Builder darf sie gemeinsam anheben, wenn der Build es verlangt.)

**Spec:** `docs/superpowers/specs/2026-11-08-tanzlexikon-design.md`

## Global Constraints

- `applicationId` und Paket: `io.github.katzfuss11.tanzlexikon`. App-Name: „Tanzlexikon“.
- `minSdk` 26, `targetSdk` 35, `compileSdk` 35. Erste Version: `versionName "0.1.0"`, `versionCode 1`.
- Alle sichtbaren Texte auf Deutsch in `app/src/main/res/values/strings.xml`, keine fest verdrahteten Strings in Compose.
- Pflichtfeld einer Figur ist nur der Name, alles andere ist optional.
- Papierkorb-Frist: genau 30 Tage (`TRASH_RETENTION_MS = 30L * 24 * 60 * 60 * 1000`).
- Die App löscht **nie** Dateien aus der Galerie, sie entfernt nur Verweise.
- Kein Server und kein Konto. Netzwerk nur für YouTube-Vorschaubilder und die Update-Prüfung gegen `https://api.github.com/repos/Katzfuss11/tanzlexikon/releases/latest`.
- Signaturschlüssel und Passwörter liegen **nie** im Repo. `keystore.properties` und `*.jks` stehen in `.gitignore`.
- Der Autor ist Anfänger: Code ohne unnötige Abstraktionen (keine DI-Frameworks, kein Clean-Architecture-Schichtenmodell). Abhängigkeiten werden per Hand in `TanzlexikonApp` erzeugt.
- Testgerät: Android-15-Handy des Autors.
- Gradle-Befehle laufen in Git Bash im Repo-Root mit `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`. Der Testbefehl ist `./gradlew testDebugUnitTest`.

## Review Focus

1. **Galerie-Datei außerhalb der App gelöscht:** Die Figur zeigt „Datei nicht mehr gefunden“, nichts stürzt ab, die Startseite zählt das Medium als fehlend (Test in Task 6).
2. **Wiederherstellen mit anders geschriebenem Tanznamen:** Eine Figur aus „Rumba“, deren Tanz gelöscht und als „rumba“ neu angelegt wurde, landet in „rumba“ und nicht in einem zweiten Tanz (Test in Task 3).
3. **Kaputte oder fremde Datei als `.tanzfigur` geöffnet** (ein beliebiges ZIP, abgeschnittene Datei, ein höheres `formatVersion`): verständliche Fehlermeldung, es wird nichts angelegt (Test in Task 8).
4. **Umlaute, Emojis und lange Texte** („Schulter an Schulter – Übergang 💃“, 5 000 Zeichen) überstehen den Weg Export → Import und Figur-Datei → Vorschau unverändert (Tests in Task 8 und Task 9).
5. **Versions-Tags mit „v“ und zweistelligen Teilen:** `v0.10.0` ist neuer als `0.9.3`. Ohne Netz oder bei HTTP 404 (privates Repo) gibt es beim Start keinen Hinweis und keinen Absturz (Tests in Task 10).

---

## Dateistruktur

```
tanzlexikon/
  settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew(.bat), gradle/wrapper/*
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  app/src/main/res/values/strings.xml, res/xml/backup_rules.xml, res/xml/data_extraction_rules.xml, res/xml/file_paths.xml
  app/src/main/java/io/github/katzfuss11/tanzlexikon/
    TanzlexikonApp.kt          – Application, erzeugt DB/Repository/Services, räumt beim Start den Papierkorb auf
    MainActivity.kt            – Compose-Host, nimmt geteilte .tanzfigur-Dateien entgegen
    data/Entities.kt           – Room-Entities + Enums Section, MediaKind
    data/LexikonDao.kt         – alle Queries
    data/TanzDatabase.kt       – Room-Datenbank, Seed der Grundliste
    data/Seed.kt               – Grundliste (Kategorien/Tänze)
    lexikon/LexikonRepository.kt – Fachlogik (CRUD, Lösch-Regeln, Papierkorb, Wiederherstellen, Suche)
    lexikon/Models.kt          – UI-taugliche Lese-Modelle (DanceSummary, FigureDetail …)
    media/YouTube.kt           – Link → Video-ID, Vorschaubild-URL
    media/MediaChecker.kt      – „Datei noch da?“
    media/GalleryWriter.kt     – Datei in Galerie speichern (mit Duplikat-Erkennung)
    transfer/ArchiveFormat.kt  – serialisierbares Manifest
    transfer/ArchiveWriter.kt, transfer/ArchiveReader.kt – ZIP gestreamt schreiben/lesen
    transfer/BackupService.kt  – kompletter Export/Import
    share/ShareText.kt         – Figur → Nachrichtentext
    share/FigureShare.kt       – Teilen-Intents, .tanzfigur erzeugen
    share/ImportTarget.kt      – Zuordnungsvorschlag für empfangene Figur
    update/Version.kt, update/UpdateChecker.kt, update/UpdateInstaller.kt
    ui/Navigation.kt, ui/theme/Theme.kt
    ui/start/, ui/tanz/, ui/figur/, ui/suche/, ui/papierkorb/, ui/einstellungen/, ui/empfang/  – je Screen.kt + ViewModel.kt
  app/src/test/java/io/github/katzfuss11/tanzlexikon/…  – JVM-Tests (Robolectric wo Android nötig)
  docs/anleitung/01-einrichtung.md, 02-app-aufs-handy.md, 03-weitergeben-und-updates.md
```

---

### Task 0: Einrichtung des Rechners (mit dem Autor)

Diesen Task erledigt der Autor selbst nach der Anleitung. Der Agent schreibt die Anleitung und prüft das Ergebnis.

**Files:**
- Create: `docs/anleitung/01-einrichtung.md`

- [ ] **Step 1: Anleitung schreiben.** Kurze nummerierte Schritte auf Deutsch, je Schritt: was tun und woran man erkennt, dass es geklappt hat:
  1. Android Studio installieren: `winget install Google.AndroidStudio` oder der Download von developer.android.com/studio. Standardpfade beibehalten.
  2. Android Studio einmal starten. Der Einrichtungsassistent („Standard“) lädt das Android SDK nach `%LOCALAPPDATA%\Android\Sdk`.
  3. Auf dem Handy die Entwickleroptionen einschalten: Einstellungen → Über das Telefon → 7× auf „Build-Nummer“ tippen. Dann Einstellungen → System → Entwickleroptionen → „USB-Debugging“ an.
  4. Das Handy per USB anschließen und „USB-Debugging zulassen“ bestätigen (Haken bei „Immer zulassen“).
  5. Prüfen: In PowerShell `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices` zeigt eine Zeile mit `device`.
- [ ] **Step 2: Anleitung für den Autor in VS Code öffnen** und warten, bis er Bescheid gibt.
- [ ] **Step 3: Prüfen**
  Run: `ls "/c/Program Files/Android/Android Studio/jbr/bin/java.exe" && "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" devices`
  Expected: Die Java-Datei existiert, und `adb` listet ein Gerät mit Status `device`.
- [ ] **Step 4: Commit** `docs: Anleitung zur Einrichtung`

### Task 1: Projektgerüst, das auf dem Handy startet

**Files:**
- Create: Gradle-Dateien laut Dateistruktur, `app/build.gradle.kts`, `AndroidManifest.xml`, `MainActivity.kt`, `TanzlexikonApp.kt`, `ui/theme/Theme.kt`, `res/values/strings.xml`, `.gitignore` (ergänzen), `docs/anleitung/02-app-aufs-handy.md`
- Test: `app/src/test/java/.../SmokeTest.kt`

**Interfaces:**
- Produces: `class TanzlexikonApp : Application` (wird in Task 2 erweitert), `MainActivity` mit `setContent { TanzlexikonTheme { … } }`, `@Composable fun TanzlexikonTheme(content: @Composable () -> Unit)`.

- [ ] **Step 1: Gradle-Wrapper erzeugen.** Gradle 8.11.1 (bin-zip) ins Scratchpad laden und entpacken, dann im Repo `gradle wrapper --gradle-version 8.11.1` ausführen (mit `JAVA_HOME` aus den Global Constraints). Danach wird nur noch `./gradlew` benutzt.
- [ ] **Step 2: Build-Dateien anlegen.** Versionskatalog `gradle/libs.versions.toml` mit allen Bibliotheken aus dem Tech Stack. In `app/build.gradle.kts` stehen die Werte aus den Global Constraints sowie `buildFeatures { compose = true; buildConfig = true }` und `testOptions.unitTests.isIncludeAndroidResources = true`. Release-`signingConfig` liest `keystore.properties` aus dem Repo-Root **nur wenn vorhanden** (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`), sonst baut Release unsigniert. `.gitignore` ergänzen: `.gradle/`, `build/`, `local.properties`, `keystore.properties`, `*.jks`, `.idea/`, `*.iml`.
- [ ] **Step 3: Failing test schreiben**

```kotlin
@RunWith(RobolectricTestRunner::class)
class SmokeTest {
    @Test fun appNameIsTanzlexikon() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Tanzlexikon", ctx.getString(R.string.app_name))
    }
}
```

- [ ] **Step 4: Run:** `./gradlew testDebugUnitTest` → Expected: FAIL (`R.string.app_name` fehlt bzw. die Klassen fehlen).
- [ ] **Step 5: Implementieren:** `strings.xml` mit `app_name` = „Tanzlexikon“. `MainActivity` zeigt einen Platzhaltertext `R.string.start_title` („Tanzlexikon“). Manifest mit `android:name=".TanzlexikonApp"`. `Theme.kt` mit Material-3-Farbschema in Hell und Dunkel (dynamische Farben aus).
- [ ] **Step 6: Run:** `./gradlew testDebugUnitTest` → PASS. Danach `./gradlew installDebug` → „Installed on 1 device“.
- [ ] **Step 7: Anleitung `02-app-aufs-handy.md` schreiben:** Projekt in Android Studio öffnen (File → Open → Repo-Ordner), auf den Gradle-Sync warten, Gerät oben auswählen, ▶ drücken. Alternative ohne Android Studio: `./gradlew installDebug`. Zusätzlich: einen Emulator anlegen (Device Manager → Pixel, API 35).
- [ ] **Step 8: Manuelle Prüfung mit dem Autor:** Die App „Tanzlexikon“ startet auf dem Handy und zeigt den Titel.
- [ ] **Step 9: Commit** `feat: Projektgerüst und Anleitung zum Starten`

### Task 2: Datenbank, Grundliste und Auto-Backup

**Files:**
- Create: `data/Entities.kt`, `data/LexikonDao.kt`, `data/TanzDatabase.kt`, `data/Seed.kt`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`
- Modify: `TanzlexikonApp.kt`, `AndroidManifest.xml`
- Test: `data/TanzDatabaseTest.kt`

**Interfaces:**
- Produces:
  - `enum class Section { GENERAL, LEADER, FOLLOWER }`, `enum class MediaKind { VIDEO, IMAGE, YOUTUBE }` (TypeConverter über `name`)
  - `@Entity("category") data class CategoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val position: Int)`
  - `@Entity("dance", foreignKeys = [FK category, onDelete = RESTRICT], indices=[categoryId]) data class DanceEntity(id: Long = 0, categoryId: Long, name: String, position: Int)`
  - `@Entity("figure", FK dance onDelete = SET_NULL) data class FigureEntity(id: Long = 0, danceId: Long?, name: String, position: Int, generalText: String = "", leaderText: String = "", followerText: String = "", trashedAt: Long? = null, trashedDanceName: String? = null, trashedCategoryName: String? = null)`
  - `@Entity("media", FK figure onDelete = CASCADE) data class MediaEntity(id: Long = 0, figureId: Long, section: Section, kind: MediaKind, uri: String, displayName: String?, mimeType: String?, sizeBytes: Long?, position: Int)`. Bei `YOUTUBE` steht in `uri` die Original-URL.
  - `abstract class TanzDatabase : RoomDatabase` mit `abstract fun dao(): LexikonDao` und `companion fun build(context: Context, inMemory: Boolean = false): TanzDatabase`. Der Seed läuft in `RoomDatabase.Callback.onCreate`, und zwar genau einmal.
  - `val SEED: List<Pair<String, List<String>>>` in `Seed.kt` (Reihenfolge = `position`).
  - `TanzlexikonApp.database: TanzDatabase` (lazy).
  - DAO-Methoden legt der Builder nach Bedarf von Task 3 an.

- [ ] **Step 1: Failing test schreiben**

```kotlin
@Test fun freshDatabaseContainsSeedInOrder() = runTest {
    val db = TanzDatabase.build(context, inMemory = true)
    val cats = db.dao().allCategories()          // nach position sortiert
    assertEquals(listOf("Standard", "Latein", "Sonstige"), cats.map { it.name })
    val latein = db.dao().dancesOf(cats[1].id).map { it.name }
    assertEquals(listOf("Cha-Cha-Cha", "Samba", "Rumba", "Paso Doble", "Jive", "Salsa"), latein)
    assertEquals(listOf("Langsamer Walzer", "Tango", "Wiener Walzer", "Slowfox", "Quickstep"),
        db.dao().dancesOf(cats[0].id).map { it.name })
    assertEquals(listOf("Discofox", "Argentinischer Tango"), db.dao().dancesOf(cats[2].id).map { it.name })
}
@Test fun deletingFigureCascadesMediaButDanceDeleteNullsFigure() = runTest { /* Figur mit Medium anlegen; Figur löschen → media leer.
   Zweite Figur in neuem Tanz; Tanz löschen → figure.danceId == null */ }
```

- [ ] **Step 2: Run:** `./gradlew testDebugUnitTest --tests '*TanzDatabaseTest*'` → FAIL
- [ ] **Step 3: Implementieren** wie in den Interfaces. Der Seed schreibt per `db.execSQL` im Callback, damit er synchron in der Erstellungstransaktion liegt.
- [ ] **Step 4: Auto-Backup:** `android:allowBackup="true"`, `android:fullBackupContent="@xml/backup_rules"` (bis API 30) und `android:dataExtractionRules="@xml/data_extraction_rules"` (ab 31). Beide sichern nur `domain="database"` (`tanzlexikon.db`) und `sharedpref`, aber keinen Cache.
- [ ] **Step 5: Run** Tests → PASS
- [ ] **Step 6: Commit** `feat: Datenbank mit Grundliste und Auto-Backup`

### Task 3: LexikonRepository – Lösch-Regeln, Papierkorb, Wiederherstellen, Suche

Reine Logik, vollständig getestet (Room in-memory unter Robolectric). Die Zeit wird als `now: Long` übergeben.

**Files:**
- Create: `lexikon/LexikonRepository.kt`, `lexikon/Models.kt`
- Modify: `data/LexikonDao.kt`, `TanzlexikonApp.kt`
- Test: `lexikon/LexikonRepositoryTest.kt`

**Interfaces:**
- Consumes: Entities/DAO aus Task 2.
- Produces (`class LexikonRepository(private val db: TanzDatabase)`, alle `suspend` außer den Flows):
  - Modelle in `Models.kt`: `data class DanceSummary(val id: Long, val name: String, val figureCount: Int)`, `data class CategoryWithDances(val id: Long, val name: String, val dances: List<DanceSummary>)`, `data class FigureSummary(val id: Long, val name: String, val videoCount: Int, val imageCount: Int, val linkCount: Int)`, `data class FigureDetail(val figure: FigureEntity, val danceName: String, val categoryName: String, val media: List<MediaEntity>)`, `data class TrashedFigure(val id: Long, val name: String, val danceName: String, val trashedAt: Long)`, `data class SearchHit(val figureId: Long?, val danceId: Long, val title: String, val subtitle: String)`, `enum class DeleteResult { DELETED, NOT_EMPTY }`
  - `fun observeStart(): Flow<List<CategoryWithDances>>` (zählt nur Figuren mit `trashedAt == null`)
  - `addCategory(name: String): Long`, `renameCategory(id: Long, name: String)`, `reorderCategories(orderedIds: List<Long>)`, `deleteCategory(id: Long): DeleteResult`
  - `addDance(categoryId: Long, name: String): Long`, `renameDance(id, name)`, `moveDance(id: Long, categoryId: Long)`, `reorderDances(categoryId: Long, orderedIds: List<Long>)`, `deleteDance(id: Long): DeleteResult`
  - `fun observeFigures(danceId: Long): Flow<List<FigureSummary>>`, `addFigure(danceId: Long, name: String): Long` (Position = Ende), `reorderFigures(danceId: Long, orderedIds: List<Long>)`
  - `fun observeFigure(id: Long): Flow<FigureDetail?>`
  - `saveFigure(id: Long, name: String, generalText: String, leaderText: String, followerText: String, media: List<MediaEntity>)`: ersetzt Texte und die komplette Medienliste in **einer** Transaktion (neue Medien `id = 0`)
  - `trashFigure(id: Long, now: Long)`: setzt `trashedAt`, `trashedDanceName` und `trashedCategoryName`
  - `fun observeTrash(): Flow<List<TrashedFigure>>`, `restoreFigure(id: Long): Long` (gibt die `danceId` zurück), `deleteFigurePermanently(id: Long)`, `purgeTrash(now: Long): Int`
  - `findOrCreateDance(categoryName: String, danceName: String): Long`: Abgleich ohne Groß-/Kleinschreibung und mit `trim()`, legt fehlende Kategorie bzw. fehlenden Tanz am Ende an
  - `search(query: String): List<SearchHit>`: Treffer in Tanznamen, Figurennamen und den drei Texten, ohne Groß-/Kleinschreibung, nur nicht gelöschte Figuren. Leere Query → leere Liste.
  - `TanzlexikonApp.repository: LexikonRepository` (lazy). In `onCreate` startet `purgeTrash(System.currentTimeMillis())` auf `Dispatchers.IO`.

- [ ] **Step 1: Failing tests schreiben** (je ein `@Test`):

```kotlin
deleteDance_withActiveFigure_returnsNotEmpty_andKeepsDance()
deleteDance_withOnlyTrashedFigures_deletes()
deleteCategory_withDances_returnsNotEmpty() ; deleteCategory_empty_deletes()
trashFigure_hidesFromDanceAndCounts_showsInTrash()          // observeFigures leer, figureCount 0, observeTrash 1 Eintrag
restore_whenDanceStillExists_returnsSameDance()
restore_afterDanceAndCategoryDeleted_recreatesBothWithOldNames()  // "Latein"/"Rumba" weg → beide neu, Figur darin
restore_matchesExistingDanceIgnoringCase()                   // Rumba gelöscht, "rumba" neu angelegt → Figur landet in "rumba", nur 1 Tanz  (Review Focus 2)
purgeTrash_removesOnlyOlderThan30Days()                      // trashedAt = now - 30 Tage - 1 ms → weg; now - 29 Tage → bleibt; Rückgabe 1
saveFigure_replacesMediaAtomically_andNeverTouchesOthers()
addFigure_onlyNameRequired_appendsAtEnd()
search_findsInNamesAndTexts_caseInsensitive_excludesTrashed() // "fächer" findet Figur "Fächer" und Text "…Fächerposition…"
```

- [ ] **Step 2: Run:** `./gradlew testDebugUnitTest --tests '*LexikonRepositoryTest*'` → FAIL
- [ ] **Step 3: Implementieren.** `restoreFigure`: Ist `danceId != null`, wird nur `trashedAt` und das Folgende geleert. Sonst `findOrCreateDance(trashedCategoryName, trashedDanceName)` und die Figur dort ans Ende hängen.
- [ ] **Step 4: Run** → PASS
- [ ] **Step 5: Commit** `feat: Repository mit Lösch-Regeln, Papierkorb und Suche`

### Task 4: Navigation, Startseite und Suche

**Files:**
- Create: `ui/Navigation.kt`, `ui/start/StartScreen.kt`, `ui/start/StartViewModel.kt`, `ui/suche/SucheScreen.kt`, `ui/suche/SucheViewModel.kt`
- Modify: `MainActivity.kt`, `strings.xml`
- Test: `ui/start/StartViewModelTest.kt`

**Interfaces:**
- Consumes: `LexikonRepository` (Task 3).
- Produces: Routen-Konstanten in `Navigation.kt`: `"start"`, `"tanz/{danceId}"`, `"figur/{figureId}"`, `"suche"`, `"papierkorb"`, `"einstellungen"`, `"empfang"`. Die Ziele der späteren Tasks hängen dort ein, bis dahin zeigen sie einen Platzhalter. ViewModels bekommen das Repository über eine `viewModelFactory { initializer { … (application as TanzlexikonApp).repository } }`.
  - `StartViewModel`: `val state: StateFlow<StartState>` mit `data class StartState(val categories: List<CategoryWithDances>, val missingMediaCount: Int = 0, val message: Int? = null)`. Aktionen `addCategory`, `addDance`, `rename…`, `deleteDance`/`deleteCategory` (bei `NOT_EMPTY` → `message = R.string.delete_dance_not_empty` bzw. `delete_category_not_empty`), `reorder…`, `moveDance`.

- [ ] **Step 1: Failing test:** `deleteDanceWithFigures_setsNotEmptyMessage_andDanceStays()`, `deleteEmptyDance_removesWithoutConfirmation()`.
- [ ] **Step 2: Run** → FAIL
- [ ] **Step 3: Startseite umsetzen** (Entwurf Variante B, Startseite):
  - Oben ein Suchfeld, ein Tipp darauf öffnet „suche“.
  - Liste nach Kategorien mit Überschrift. Jeder Tanz zeigt die Figurenzahl, ein Tipp öffnet `tanz/{id}`.
  - Ein langer Druck auf einen Tanz öffnet das Menü: Umbenennen, In Kategorie verschieben, Löschen. Ein langer Druck auf eine Kategorie: Umbenennen, Löschen.
  - Das Menü oben rechts bietet: Tänze ordnen (Ziehmodus mit `reorderable`, für Kategorien und Tänze), Papierkorb, Einstellungen.
  - Der Plus-Knopf öffnet die Wahl zwischen „Neuer Tanz“ (Name + Kategorie) und „Neue Kategorie“.
  - Meldungen erscheinen als Snackbar.
- [ ] **Step 4: Suche umsetzen:** Das Feld hat sofort den Fokus, die Treffer erscheinen beim Tippen (`search` mit 200 ms Verzögerung). Ein Treffer öffnet die Figur bzw. den Tanz.
- [ ] **Step 5: Run** Tests → PASS, `./gradlew installDebug`
- [ ] **Step 6: Manuelle Prüfung mit dem Autor:** Die Grundliste ist sichtbar. Tanz anlegen, umbenennen, verschieben, ordnen. Einen leeren Tanz löschen, das geht ohne Abfrage.
- [ ] **Step 7: Commit** `feat: Startseite und Suche`

### Task 5: Tanz-Bildschirm und Figur lesen/bearbeiten (Texte), Papierkorb-Bildschirm

**Files:**
- Create: `ui/tanz/TanzScreen.kt`, `ui/tanz/TanzViewModel.kt`, `ui/figur/FigurScreen.kt`, `ui/figur/FigurViewModel.kt`, `ui/papierkorb/PapierkorbScreen.kt`, `ui/papierkorb/PapierkorbViewModel.kt`
- Test: `ui/figur/FigurViewModelTest.kt`, `ui/tanz/MediaSummaryTest.kt`

**Interfaces:**
- Consumes: Repository (Task 3), Routen (Task 4).
- Produces:
  - `fun mediaSummaryText(res: Resources, s: FigureSummary): String` in `TanzScreen.kt` → z. B. „2 Videos“, „1 Video, 1 Link“, „nur Text“ (Plurals in `strings.xml`)
  - `FigurViewModel(repository, figureId)`: `val state: StateFlow<FigurState>`, `data class FigurState(val detail: FigureDetail?, val editing: Boolean, val draft: FigurDraft?)`, `data class FigurDraft(val name: String, val generalText: String, val leaderText: String, val followerText: String, val media: List<MediaEntity>)`. Aktionen: `startEditing()`, `updateDraft(transform: (FigurDraft) -> FigurDraft)`, `cancelEditing()`, `save()` (bei leerem Namen → Fehler `R.string.name_required`, nicht speichern), `trash(now: Long)`.

- [ ] **Step 1: Failing tests:**

```kotlin
cancelEditing_discardsDraft_andDetailUnchanged()
save_withBlankName_keepsEditing_andShowsNameRequired()
save_writesTextsAndMediaInOneGo()
mediaSummaryText_variants()   // (0,0,0) → "nur Text"; (2,0,0) → "2 Videos"; (1,0,1) → "1 Video, 1 Link"; (0,1,0) → "1 Bild"
```

- [ ] **Step 2: Run** → FAIL
- [ ] **Step 3: Tanz-Bildschirm:** Liste der Figuren mit `mediaSummaryText`, Ziehen zum Umsortieren (`reorderable`, Griff rechts). Der Plus-Knopf öffnet einen Dialog, in dem nur der Name einzugeben ist, und danach direkt die neue Figur im Lesemodus. Wischen oder langer Druck → „In den Papierkorb“ ohne Abfrage, mit Snackbar „In den Papierkorb verschoben“ und „Rückgängig“ (= `restoreFigure`).
- [ ] **Step 4: Figur-Bildschirm (Lesen):**
  - Die drei Bereiche stehen untereinander, farbig abgesetzt (Allgemein, Leader, Follower). Ein Bereich ohne Text **und** ohne Medien wird ausgeblendet.
  - In der oberen Leiste stehen Stift (Bearbeiten), Teilen (Platzhalter bis Task 9) und das Menü „In den Papierkorb“.
  - Medien kommen erst in Task 6, hier nur Texte.
- [ ] **Step 5: Figur-Bildschirm (Bearbeiten):** Name und die drei Textfelder, alle Bereiche sichtbar. Oben Abbrechen und Speichern, die Zurück-Taste wirkt wie Abbrechen.
- [ ] **Step 6: Papierkorb-Bildschirm:** Jeder Eintrag zeigt Name, Tanz, Löschdatum und „noch N Tage“ (`30 - (now - trashedAt) / Tag`, mindestens 0). Darauf folgen Wiederherstellen und „Endgültig löschen“; nur hier mit Bestätigungsdialog.
- [ ] **Step 7: Run** Tests → PASS, `./gradlew installDebug`
- [ ] **Step 8: Manuelle Prüfung mit dem Autor:** Figur anlegen und Leader-Text ergänzen. Bearbeitung abbrechen, der Text ist unverändert. Figur löschen und wiederherstellen. Tanz samt Figur löschen und dann die Figur wiederherstellen, der Tanz kommt zurück.
- [ ] **Step 9: Commit** `feat: Tanz-, Figur- und Papierkorb-Bildschirm`

### Task 6: Medien – aufnehmen, auswählen, YouTube, anzeigen, fehlend, neu verknüpfen

**Files:**
- Create: `media/YouTube.kt`, `media/MediaChecker.kt`, `ui/figur/MediaSection.kt`, `ui/figur/MediaPickers.kt`
- Modify: `FigurScreen.kt`, `FigurViewModel.kt`, `StartViewModel.kt`, `StartScreen.kt`, `AndroidManifest.xml`, `strings.xml`
- Test: `media/YouTubeTest.kt`, `media/MediaCheckerTest.kt`, `ui/start/MissingMediaTest.kt`

**Interfaces:**
- Produces:
  - `fun youtubeVideoId(url: String): String?`: akzeptiert `youtube.com/watch?v=`, `youtu.be/`, `youtube.com/shorts/`, `m.youtube.com`, mit und ohne `https://`, zusätzliche Parameter (`&t=42s`, `?si=`). Alles andere ergibt `null`.
  - `fun youtubeThumbnailUrl(id: String) = "https://img.youtube.com/vi/$id/hqdefault.jpg"`
  - `class MediaChecker(private val resolver: ContentResolver) { fun isAvailable(m: MediaEntity): Boolean }`: YouTube ist immer `true`. Sonst `openFileDescriptor(uri, "r")` in `try`, und **jede** Exception bedeutet `false`.
  - `LexikonRepository.allActiveMedia(): List<Pair<MediaEntity, FigureSummaryRef>>` mit `data class FigureSummaryRef(val figureId: Long, val figureName: String, val danceName: String)`. Ergänzt Task 3.
  - `StartState.missingMediaCount` wird beim Start im Hintergrund berechnet. Ein Tipp auf den Hinweis zeigt einen Dialog mit den betroffenen Figuren als Links.
- Berechtigungen im Manifest: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` (maxSdkVersion 32), `INTERNET`.

- [ ] **Step 1: Failing tests:**

```kotlin
youtubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s") == "dQw4w9WgXcQ"
youtubeVideoId("youtu.be/dQw4w9WgXcQ?si=abc") == "dQw4w9WgXcQ"
youtubeVideoId("https://youtube.com/shorts/dQw4w9WgXcQ") == "dQw4w9WgXcQ"
youtubeVideoId("https://vimeo.com/123") == null ; youtubeVideoId("") == null
mediaChecker_missingContentUri_returnsFalse_noCrash()     // content://media/external/video/media/999999  (Review Focus 1)
mediaChecker_youtube_alwaysTrue()
missingMediaCount_countsOnlyActiveFiguresMissingFiles()   // gelöschte Figur zählt nicht
```

- [ ] **Step 2: Run** → FAIL
- [ ] **Step 3: Hinzufügen im Bearbeitungsmodus**, je Bereich ein Knopf „Medium hinzufügen“ mit drei Wegen:
  - **Video aufnehmen:** `ActivityResultContracts.StartActivityForResult` mit `MediaStore.ACTION_VIDEO_CAPTURE` **ohne** `EXTRA_OUTPUT`. Die App übernimmt den zurückgegebenen `data.data`. Vorher fragt sie einmalig `READ_MEDIA_VIDEO` an (bzw. `READ_EXTERNAL_STORAGE` bis API 32), damit der Verweis auch nach einem Neustart lesbar bleibt.
  - **Aus Galerie:** `PickMultipleVisualMedia(ImageAndVideo)`, danach `takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)` in `try`. `kind` ergibt sich aus dem MIME-Typ. `displayName`, `mimeType` und `sizeBytes` liest die App per `OpenableColumns` aus.
  - **YouTube-Link:** Ein Textfeld, das beim Öffnen die Zwischenablage nicht automatisch ausliest. Ist `youtubeVideoId` gleich `null`, erscheint „Das ist kein YouTube-Link“.
- [ ] **Step 4: Anzeigen** in `MediaSection`, als zweispaltiges Raster mit Vorschaubildern (Coil, Video-Frames über `coil-video`). YouTube zeigt das Vorschaubild mit Platzhalter, wenn es nicht lädt. Ein Tipp öffnet `ACTION_VIEW` (Video/Bild mit MIME-Typ und `FLAG_GRANT_READ_URI_PERMISSION`, YouTube mit der URL). Fehlende Dateien zeigen eine Kachel „Datei nicht mehr gefunden“. Im Bearbeitungsmodus bietet sie „Neu verknüpfen“ (Picker, dann `uri`/`displayName`/`sizeBytes` am selben Medium ersetzen). Medien lassen sich im Bearbeitungsmodus entfernen, das betrifft nur den Entwurf.
- [ ] **Step 5: Run** Tests → PASS, `./gradlew installDebug`
- [ ] **Step 6: Manuelle Prüfung mit dem Autor auf dem Handy:**
  - Video mit der Kamera-App aufnehmen, es erscheint in der Figur und in der Galerie.
  - Nach einem Neustart der App ist es weiterhin abspielbar.
  - Ein Bild aus der Galerie hinzufügen und einen YouTube-Link einfügen.
  - Ein Testvideo in der Galerie löschen. Die Kachel zeigt „nicht gefunden“, die Startseite den Hinweis, und „Neu verknüpfen“ funktioniert.
- [ ] **Step 7: Commit** `feat: Medien in Figuren`

### Task 7: Galerie-Schreiber und ZIP-Format

Grundlage für den Export (Task 8) und für die Figur-Datei (Task 9).

**Files:**
- Create: `media/GalleryWriter.kt`, `transfer/ArchiveFormat.kt`, `transfer/ArchiveWriter.kt`, `transfer/ArchiveReader.kt`
- Modify: `AndroidManifest.xml` (`WRITE_EXTERNAL_STORAGE` maxSdkVersion 28)
- Test: `transfer/ArchiveRoundTripTest.kt`, `media/GalleryWriterTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class ArchiveManifest(val formatVersion: Int = 1, val kind: ArchiveKind, val appVersion: String, val categories: List<ArchCategory>)`, `enum class ArchiveKind { BACKUP, FIGURE }`, `ArchCategory(name, position, dances: List<ArchDance>)`, `ArchDance(name, position, figures: List<ArchFigure>)`, `ArchFigure(name, position, generalText, leaderText, followerText, media: List<ArchMedia>)`, `ArchMedia(section: Section, kind: MediaKind, position: Int, youtubeUrl: String? = null, file: String? = null, displayName: String? = null, mimeType: String? = null, sizeBytes: Long? = null)`. `file` ist der Pfad im ZIP: `media/<laufende Nummer>.<endung>`. Fehlende Dateien werden beim Schreiben ohne `file` aufgenommen und bleiben als Verweis „nicht gefunden“ erhalten.
  - `const val MANIFEST_ENTRY = "tanzlexikon.json"`, `const val CURRENT_FORMAT = 1`
  - `class ArchiveWriter { fun write(out: OutputStream, manifest: ArchiveManifest, files: List<ArchiveFile>, onProgress: (done: Long, total: Long) -> Unit) }` mit `data class ArchiveFile(val path: String, val size: Long, val open: () -> InputStream)`. Das Manifest kommt **als erster Eintrag**, danach werden die Dateien mit 64-KB-Puffer gestreamt, ohne sie ganz in den Speicher zu laden. Mediendateien werden ohne Kompression geschrieben (`ZipOutputStream.setLevel(0)` vor den Medien-Einträgen), weil sie ohnehin komprimiert sind.
  - `class ArchiveReader { fun read(input: InputStream, onFile: (path: String, data: InputStream) -> Unit): ArchiveManifest }` wirft `ArchiveException(val reason: ArchiveError)` mit `enum class ArchiveError { NOT_TANZLEXIKON, NEWER_FORMAT, CORRUPT }`: Ist der erste Eintrag nicht `MANIFEST_ENTRY` → `NOT_TANZLEXIKON`, ist `formatVersion > CURRENT_FORMAT` → `NEWER_FORMAT`, bei ZIP- oder JSON-Fehlern → `CORRUPT`. Pfade mit `..` oder einem führenden `/` → `CORRUPT`.
  - `class GalleryWriter(private val resolver: ContentResolver) { fun save(displayName: String, mimeType: String, size: Long, data: InputStream): Uri }`: Zuerst sucht die App im `MediaStore` (Video oder Bild nach MIME-Typ) eine Datei mit gleichem `DISPLAY_NAME` **und** `SIZE`. Wird sie gefunden, gibt die App ihre URI zurück und liest den Stream nicht. Sonst wird sie neu eingefügt: ab API 29 mit `RELATIVE_PATH` = `Movies/Tanzlexikon` bzw. `Pictures/Tanzlexikon` und `IS_PENDING` 1→0, bis API 28 in den öffentlichen Ordner `Movies/Tanzlexikon` bzw. `Pictures/Tanzlexikon`.

- [ ] **Step 1: Failing tests:**

```kotlin
roundTrip_preservesManifestAndFileBytes()          // 2 Dateien (1 KB, 3 MB Zufallsbytes) → gleiche Bytes zurück, onProgress endet bei total
roundTrip_keepsUmlautsEmojiAndLongText()           // generalText = "Schulter an Schulter – Übergang 💃" + "x".repeat(5000)  (Review Focus 4)
read_randomZipWithoutManifest_throwsNotTanzlexikon()
read_truncatedFile_throwsCorrupt()                 // ersten 100 Bytes eines gültigen Archivs
read_formatVersion2_throwsNewerFormat()
read_pathTraversal_throwsCorrupt()                 // Eintrag "../evil"
galleryWriter_sameNameAndSize_returnsExistingUri_withoutReading()   // Robolectric-MediaStore; Stream, der beim Lesen fehlschlägt
```

- [ ] **Step 2: Run** → FAIL
- [ ] **Step 3: Implementieren** wie beschrieben, `Json { ignoreUnknownKeys = true; encodeDefaults = true }`.
- [ ] **Step 4: Run** → PASS
- [ ] **Step 5: Commit** `feat: ZIP-Format und Galerie-Schreiber`

### Task 8: Einstellungen mit Export und Import (kompletter Bestand)

**Files:**
- Create: `transfer/BackupService.kt`, `ui/einstellungen/EinstellungenScreen.kt`, `ui/einstellungen/EinstellungenViewModel.kt`
- Modify: `TanzlexikonApp.kt`, `LexikonRepository.kt`, `Navigation.kt`, `strings.xml`
- Test: `transfer/BackupServiceTest.kt`

**Interfaces:**
- Consumes: Archive*/GalleryWriter (Task 7), Repository, MediaChecker.
- Produces:
  - `LexikonRepository.snapshot(): ArchiveSnapshot` liefert alle Kategorien, Tänze und **nicht gelöschte** Figuren mit Medien. Der Papierkorb wird nicht exportiert, damit gelöschte Figuren auf dem neuen Gerät nicht wieder auftauchen.
  - `LexikonRepository.replaceAll(manifest: ArchiveManifest, uriFor: (ArchMedia) -> String)` leert alle Tabellen und fügt in **einer** Transaktion neu ein. `uriFor` liefert für Dateien die neue Galerie-URI, für YouTube die `youtubeUrl` und für Medien ohne `file` und ohne `youtubeUrl` den Platzhalter `"missing:"`. `MediaChecker` erkennt diesen als fehlend.
  - `class BackupService(context, repository, galleryWriter, mediaChecker)`:
    - `suspend fun export(target: OutputStream, onProgress: (Long, Long) -> Unit)`
    - `suspend fun import(source: () -> InputStream, onProgress: (Long, Long) -> Unit): ImportOutcome`
    - `suspend fun safetyExport(now: Long): Uri`: schreibt nach `Download/Tanzlexikon/tanzlexikon-vor-import-<yyyy-MM-dd-HHmm>.zip` (MediaStore Downloads ab API 29, darunter `getExternalFilesDir`)
    - `fun hasEnoughSpace(required: Long, available: Long): Boolean = available >= required + 50L * 1024 * 1024`
  - Ablauf beim Import, genau in dieser Reihenfolge:
    1. Manifest lesen und prüfen, Fehler → Meldung, nichts ändern.
    2. Platz prüfen (Summe `sizeBytes` gegen `StatFs(Environment.getExternalStorageDirectory())`).
    3. `safetyExport`.
    4. Medien per `GalleryWriter` speichern.
    5. `replaceAll`.
  - Die Datenbank wird erst angefasst, wenn alle Medien gespeichert sind.

- [ ] **Step 1: Failing tests:**

```kotlin
exportThenImport_onFreshDb_restoresStructureTextsAndMedia()    // inkl. Umlaut/Emoji-Text (Review Focus 4)
import_invalidFile_leavesDatabaseUntouched()
import_createsSafetyExportBeforeReplacing()
export_excludesTrashedFigures()
hasEnoughSpace_requires50MbMargin()
```

- [ ] **Step 2: Run** → FAIL
- [ ] **Step 3: BackupService implementieren.** Export und Import laufen als Coroutine im ViewModel auf `Dispatchers.IO`. Der Bildschirm bleibt dabei wach (`FLAG_KEEP_SCREEN_ON`), solange der Vorgang läuft.
- [ ] **Step 4: Einstellungen-Bildschirm** (Route „einstellungen“):
  - Abschnitt Sicherung:
    - „Alles exportieren“ öffnet `CreateDocument("application/zip")` mit dem Vorschlag `tanzlexikon-<datum>.zip`.
    - „Importieren“ öffnet `OpenDocument`. Danach folgt ein **Warndialog** mit dem Titel „Gesamten Datenbestand überschreiben?“ und dem Text, dass alle aktuellen Kategorien, Tänze und Figuren ersetzt werden und vorher automatisch eine Sicherung im Download-Ordner entsteht. Die Knöpfe heißen „Abbrechen“ und „Überschreiben“, wobei „Überschreiben“ rot ist.
    - Während des Vorgangs zeigt die App einen Fortschrittsbalken in Prozent.
  - Abschnitt Papierkorb: Link zu „papierkorb“.
  - Abschnitt Über: installierte Version (`BuildConfig.VERSION_NAME`) und Platz für den Update-Knopf (Task 10).
- [ ] **Step 5: Run** Tests → PASS, `./gradlew installDebug`
- [ ] **Step 6: Manuelle Prüfung mit dem Autor:** Export auf Google Drive. Eine Testfigur ändern, dann importieren. Der alte Stand ist zurück, und die Sicherungsdatei liegt im Download-Ordner.
- [ ] **Step 7: Commit** `feat: Einstellungen mit Export und Import`

### Task 9: Figur teilen und empfangen

**Files:**
- Create: `share/ShareText.kt`, `share/FigureShare.kt`, `share/ImportTarget.kt`, `ui/empfang/EmpfangScreen.kt`, `ui/empfang/EmpfangViewModel.kt`, `res/xml/file_paths.xml`
- Modify: `AndroidManifest.xml`, `MainActivity.kt`, `FigurScreen.kt`, `LexikonRepository.kt`, `Navigation.kt`
- Test: `share/ShareTextTest.kt`, `share/ImportTargetTest.kt`, `ui/empfang/EmpfangViewModelTest.kt`

**Interfaces:**
- Consumes: ArchiveWriter/Reader, GalleryWriter, Repository.
- Produces:
  - `fun buildShareText(d: FigureDetail, labels: ShareLabels): String` mit `data class ShareLabels(val general: String, val leader: String, val follower: String)` aus den Strings. Aufbau:
    - erste Zeile `"<Tanz> – <Figur>"`
    - dann je Bereich mit Text **oder** YouTube-Link ein Block: `"<Label>:\n<Text>"`, darunter die YouTube-URLs des Bereichs, je eine Zeile
    - die Blöcke sind durch eine Leerzeile getrennt, leere Bereiche entfallen
    - am Ende `"\n\nGeteilt mit dem Tanzlexikon"`
  - `class FigureShare(context, repository, archiveWriter)`:
    - `fun shareAsMessage(d: FigureDetail)`: `ACTION_SEND_MULTIPLE` (bzw. `ACTION_SEND` ohne Anhänge) mit `EXTRA_TEXT` und `EXTRA_STREAM` (eigene Videos und Bilder, nur vorhandene), `FLAG_GRANT_READ_URI_PERMISSION`, `Intent.createChooser`
    - `suspend fun shareAsFile(d: FigureDetail)`: erzeugt ein `ArchiveKind.FIGURE`-Archiv mit **genau einer** Kategorie → einem Tanz → einer Figur in `cacheDir/share/<Figurname, Dateisystem-sicher>.tanzfigur`, das per `FileProvider` (`${applicationId}.files`) mit MIME `application/octet-stream` geteilt wird
  - Empfang: Im Manifest hat `MainActivity` Intent-Filter für `ACTION_VIEW` und `ACTION_SEND` mit `mimeType` `application/octet-stream`, `application/zip` und `application/x-zip-compressed` sowie `ACTION_VIEW` mit `pathPattern` `.*\\.tanzfigur` (scheme `content`/`file`). `MainActivity` legt die URI in `TanzlexikonApp.pendingImport: MutableStateFlow<Uri?>` ab, und `Navigation` springt dann nach „empfang“. Eine Datei, deren Manifest nicht `FIGURE` ist, ergibt die Meldung „Das ist keine Tanzfigur-Datei“.
  - `sealed interface ImportTarget { data class Existing(val danceId: Long, val danceName: String, val categoryName: String) : ImportTarget; data class New(val categoryName: String, val danceName: String) : ImportTarget }`
  - `fun proposeTarget(existing: List<CategoryWithDances>, categoryName: String, danceName: String): ImportTarget`: Zuerst wird in **allen** Kategorien ein Tanz mit gleichem Namen gesucht (ohne Groß-/Kleinschreibung, mit `trim`), dann `Existing`. Sonst `New(categoryName, danceName)`, wobei `categoryName` an eine bestehende Kategorie mit gleichem Namen angeglichen wird (Schreibweise des Empfängers).
  - `EmpfangViewModel`: liest das Archiv zunächst **nur das Manifest** (Medien werden erst beim Hinzufügen gelesen, die URI also zweimal geöffnet). Der State umfasst `figure: ArchFigure`, `target: ImportTarget`, `figureName: String` (editierbar), `duplicateName: Boolean` (Zieltanz hat schon eine Figur mit gleichem Namen ohne Groß-/Kleinschreibung), `error: ArchiveError?`. Aktionen: `chooseExisting(danceId)`, `chooseNew(categoryName, danceName)`, `rename(name)`, `add()`. `add()` legt den Zieltanz über `findOrCreateDance` an, speichert die Medien per `GalleryWriter` und hängt die Figur ans Ende. Es **überschreibt nie**.

- [ ] **Step 1: Failing tests:**

```kotlin
shareText_omitsEmptySections_andListsYoutubeUnderItsSection()
shareText_keepsUmlautsAndEmoji()                                 // Review Focus 4
proposeTarget_matchesDanceIgnoringCase_acrossCategories()        // Datei "Latein/rumba" → Existing(Rumba)
proposeTarget_unknownDance_proposesNewInExistingCategorySpelling()  // "latein/Bachata" → New("Latein","Bachata")
empfang_salsaFile_userChoosesCasino_addsFigureToCasino_salsaNotCreated()
empfang_duplicateName_flagsDuplicate_addStillCreatesSecondFigure()
empfang_backupZipOpened_showsNotAFigureError_addsNothing()        // Review Focus 3
empfang_corruptFile_showsError_addsNothing()                      // Review Focus 3
```

- [ ] **Step 2: Run** → FAIL
- [ ] **Step 3: Implementieren.** Ein Tipp auf „Teilen“ in der Figur zeigt das Auswahlblatt „Als Nachricht“ / „Als Tanzlexikon-Datei“. Bei der Datei läuft ein Fortschritt, solange gepackt wird.
- [ ] **Step 4: Empfang-Bildschirm umsetzen** (Route „empfang“):
  - Vorschau der Figur, also Name, Texte und Medienliste mit Typ und Dateiname. Darunter steht „Wird einsortiert in: <Kategorie> › <Tanz>“, bei Bedarf mit dem Zusatz „(neu)“.
  - Der Knopf „Ändern“ öffnet ein Blatt mit den bestehenden Tänzen nach Kategorien und der Option „Neuer Tanz…“ (Name + Kategorie, die Kategorie wählbar oder neu).
  - Bei einem doppelten Namen erscheint ein Hinweistext, und das Namensfeld ist editierbar.
  - Unten „Abbrechen“ und „Hinzufügen“. Danach öffnet sich die neue Figur.
- [ ] **Step 5: Run** Tests → PASS, `./gradlew installDebug`
- [ ] **Step 6: Manuelle Prüfung mit dem Autor:** Eine Figur als Nachricht an sich selbst schicken (WhatsApp oder Signal), mit Text und Video. Dann eine Figur als Datei schicken, aus WhatsApp heraus öffnen, die Vorschau prüfen, den Tanz ändern und hinzufügen. Der echte Test mit dem zweiten Nutzer folgt in Task 11.
- [ ] **Step 7: Commit** `feat: Figuren teilen und empfangen`

### Task 10: Update-Prüfung über GitHub-Releases

**Files:**
- Create: `update/Version.kt`, `update/UpdateChecker.kt`, `update/UpdateInstaller.kt`
- Modify: `MainActivity.kt`/`Navigation.kt` (Hinweis beim Start), `EinstellungenScreen.kt`, `EinstellungenViewModel.kt`, `AndroidManifest.xml` (`REQUEST_INSTALL_PACKAGES`, FileProvider-Pfad `cache/updates`), `file_paths.xml`
- Test: `update/VersionTest.kt`, `update/UpdateCheckerTest.kt`

**Interfaces:**
- Produces:
  - `data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version>` und `fun parseVersion(s: String): Version?`. Ein führendes `v`/`V` wird ignoriert, ebenso ein Suffix nach `-` oder `+`. Fehlende Teile zählen als 0. Unbrauchbares ergibt `null`.
  - `data class Release(val version: Version, val tag: String, val apkUrl: String)`
  - `fun parseLatestRelease(json: String): Release?` liest `tag_name` und das erste Asset mit `name` auf `.apk` (`browser_download_url`). Fehlt ein APK-Asset → `null`.
  - `sealed interface UpdateResult { data class Available(val release: Release) : UpdateResult; data object UpToDate : UpdateResult; data object Failed : UpdateResult }`
  - `class UpdateChecker(private val fetch: suspend (url: String) -> String?, private val current: String = BuildConfig.VERSION_NAME) { suspend fun check(): UpdateResult }`. Der echte `fetch` ist `HttpURLConnection` mit `Accept: application/vnd.github+json`, `User-Agent: Tanzlexikon-App`, 10 s Timeout, und liefert bei Status ≠ 200 oder einer Exception `null` → `Failed`.
  - `class UpdateInstaller(context) { suspend fun downloadAndInstall(release: Release, onProgress: (Long, Long) -> Unit) }`:
    1. Download nach `cacheDir/updates/tanzlexikon-<tag>.apk`.
    2. Fehlt `packageManager.canRequestPackageInstalls()`, öffnet die App `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` für das eigene Paket und erklärt den Schritt in einem Dialog.
    3. Sonst `ACTION_VIEW` mit FileProvider-URI, MIME `application/vnd.android.package-archive` und `FLAG_GRANT_READ_URI_PERMISSION`.
  - Beim Start: einmal pro Prozessstart `check()` im Hintergrund. **Nur** bei `Available` erscheint ein Dialog „Neue Version X verfügbar“ mit „Später“ und „Jetzt herunterladen“. `Failed` und `UpToDate` bleiben still.
  - In den Einstellungen: „Nach Updates suchen“, beliebig oft. Es zeigt „Du hast die neueste Version (X)“, den Dialog von oben oder „Prüfung nicht möglich – keine Verbindung oder Repo nicht öffentlich“.

- [ ] **Step 1: Failing tests:**

```kotlin
parseVersion("v0.10.0") > parseVersion("0.9.3")            // Review Focus 5
parseVersion("1.2") == Version(1,2,0) ; parseVersion("1.2.0-beta") == Version(1,2,0) ; parseVersion("abc") == null
parseLatestRelease(sampleJson) liefert tag "v0.2.0" und die .apk-URL ; ohne apk-Asset → null
check_newerRemote_returnsAvailable() ; check_sameVersion_returnsUpToDate()
check_fetchReturnsNull_returnsFailed_noThrow()                 // offline / 404 privates Repo (Review Focus 5)
check_unparsableTag_returnsFailed()
```

- [ ] **Step 2: Run** → FAIL
- [ ] **Step 3: Implementieren** wie beschrieben.
- [ ] **Step 4: Run** → PASS, `./gradlew installDebug`
- [ ] **Step 5: Manuelle Prüfung mit dem Autor:** Solange das Repo privat ist, erscheint beim Start kein Hinweis. In den Einstellungen meldet „Nach Updates suchen“ „Prüfung nicht möglich …“. (Den echten Update-Durchlauf prüft Task 11, sobald das Repo öffentlich ist.)
- [ ] **Step 6: Commit** `feat: Update-Prüfung über GitHub-Releases`

### Task 11: Signierte Version, Weitergeben, Release (mit dem Autor)

**Files:**
- Create: `docs/anleitung/03-weitergeben-und-updates.md`, `README.md`
- Modify: `app/build.gradle.kts` (nur falls in Task 1 etwas fehlt)

- [ ] **Step 1: Anleitung `03-weitergeben-und-updates.md` schreiben:**
  1. **Signaturschlüssel einmalig anlegen.** Mit `keytool` (aus dem `jbr`) wird `C:\Users\<Name>\tanzlexikon-schluessel\tanzlexikon.jks` (außerhalb des Repos) erzeugt, Alias `tanzlexikon`, 10 000 Tage gültig. Danach `keystore.properties` im Repo-Root mit Pfad und Passwörtern anlegen (diese Datei steht in `.gitignore`).
  2. **Schlüssel sichern.** Die `.jks`-Datei und die Passwörter kommen an zwei Orte, z. B. einen USB-Stick und einen Passwortmanager. Ohne den Schlüssel lassen sich keine Updates über eine bestehende Installation installieren.
  3. **Release bauen:** `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
  4. **An andere weitergeben.** Die APK per Messenger oder Mail schicken. Der Empfänger tippt sie an, erlaubt „Unbekannte Apps installieren“ für die App, mit der er sie geöffnet hat, und installiert.
  5. **Neue Version veröffentlichen.**
     - `versionCode` +1 und `versionName` anheben.
     - Release bauen.
     - `gh release create v<version> app-release.apk --title "Tanzlexikon <version>" --notes "…"`.
     - Hinweis: Die Update-Prüfung in der App funktioniert erst, wenn das Repo öffentlich ist (`gh repo edit Katzfuss11/tanzlexikon --visibility public --accept-visibility-change-consequences`). Das entscheidet der Autor.
  6. **Debug und Release nicht mischen.** Eine aus Android Studio installierte Debug-Version muss deinstalliert werden, bevor die Release-Version drauf kann. Dabei gehen die Daten verloren, deshalb vorher exportieren.
- [ ] **Step 2: README.md schreiben** (kurz): Wozu die App da ist, ein Screenshot-Platzhalter, die Installation über die Releases und Verweise auf die drei Anleitungen.
- [ ] **Step 3: Mit dem Autor durchgehen:** Schlüssel anlegen, `./gradlew assembleRelease`, und `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` (aus `build-tools`) zeigt den Alias-Inhaber → signiert.
- [ ] **Step 4: Manuelle Prüfung mit dem Autor und dem zweiten Nutzer:**
  - Die APK auf dessen Handy installieren.
  - Eine Figur vom Autor an ihn als `.tanzfigur` schicken, öffnen, die Zuordnung ändern und hinzufügen.
  - Eine zurückgeschickte Figur beim Autor empfangen.
  - Falls der Autor das Repo öffentlich macht: Release `v0.1.1` anlegen. Auf beiden Handys erscheint beim Start der Update-Hinweis, die Installation läuft durch, und die Daten bleiben erhalten.
- [ ] **Step 5: Commit** `docs: Weitergeben, Release und README`

---

## Selbstprüfung (erledigt)

- **Spec-Abdeckung:**
  - Struktur und Grundliste: Task 2
  - Lösch-Regeln, Papierkorb und Wiederherstellen: Task 3, Task 5
  - Bildschirme Start, Tanz, Figur, Suche und Papierkorb: Task 4, Task 5
  - Medien, fehlende Medien und Neu-Verknüpfen: Task 6
  - Auto-Backup: Task 2
  - Export/Import mit Warnung, Sicherheits-Export, Galerie-Duplikaten und Fortschritt: Task 7, Task 8
  - Teilen und Empfangen: Task 9
  - Updates: Task 10
  - Anleitungen und Signaturschlüssel: Task 0, Task 1, Task 11
  - Deutsche String-Ressourcen: Global Constraints
- **Eigene Entscheidungen in diesem Plan** (nicht in der Spec):
  - Der Papierkorb wird nicht exportiert.
  - Fehlende Medien bleiben beim Export als Platzhalter erhalten.
  - Für den Sicherheits-Export gilt der Ordner `Download/Tanzlexikon`.
  - Der Platzpuffer beträgt 50 MB.
  - Das Löschen im Tanz-Bildschirm bietet eine Snackbar mit „Rückgängig“.
  - Videos werden für den Verweis auf Kamera-Aufnahmen mit der Berechtigung `READ_MEDIA_VIDEO` gelesen.

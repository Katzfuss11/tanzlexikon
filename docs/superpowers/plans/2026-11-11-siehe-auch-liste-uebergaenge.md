# Siehe auch, Liste aller Figuren, Übergänge – Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Figuren lassen sich verknüpfen (Gruppen über Tänze hinweg, einzelne Verbindungen im selben Tanz, Vorschläge bei gleichem Namen), die Startseite bekommt eine Ansicht „Alle Figuren“, und Bildschirmwechsel gleiten seitlich.

**Architecture:** Neue Spalten `linkGroup` und `askedName` an `figure` sowie eine Tabelle `figure_link` für direkte Verbindungen (Room-Schema 2, AutoMigration). Die ganze Verweis-Logik (Anzeige berechnen, Gruppen zusammenlegen, Vorschläge, Speichern) liegt in `LexikonRepository`, die Bildschirme rufen nur Methoden auf. Die Übergänge stehen zentral im `NavHost`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room 2.8.4 (KSP), Navigation Compose, kotlinx.serialization, Robolectric + JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-11-verweise-liste-uebergaenge-design.md` (baut auf `docs/superpowers/specs/2026-11-08-tanzlexikon-design.md` auf)

## Global Constraints

- minSdk 29, targetSdk 35, compileSdk 37. Keine neuen Bibliotheken außer `androidx.room:room-testing` (nur Test, gleiche Version wie Room), falls für den Migrationstest nötig.
- Alle sichtbaren Texte auf Deutsch, in `res/values/strings.xml`, verständlich ohne Fachbegriffe.
- Bestehende Daten bleiben beim Update unverändert. Am Anfang ist keine Figur verknüpft, und für keine wurde gefragt.
- Die App verknüpft nie etwas ohne Zustimmung.
- „Gleicher Name“: `name.trim()` ohne Unterscheidung von Groß- und Kleinschreibung (`lowercase(Locale.GERMAN)`).
- Archivformat bleibt `formatVersion = 1`. Neue Felder sind optional und haben Standardwerte. Ältere Sicherungen lassen sich weiter lesen.
- Einstellungen des Geräts (Ansicht der Startseite) kommen in die SharedPreferences `einstellungen`, nicht in die Sicherung.
- Tests: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew testDebugUnitTest`. Die bestehenden 90 Tests bleiben grün.
- Commits mit Zeile `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`, ohne Claude-Session-Zeile.

## Review Focus

1. **Update von v0.1.2 mit vorhandenen Figuren und Medien:** Nach der Migration ist alles da, keine Figur verknüpft (Test in Task 1).
2. **Verknüpfte Figur im Papierkorb und Wiederherstellen in einen neu angelegten Tanz:** Sie verschwindet aus „Siehe auch“ und kommt wieder. Liegt sie danach in einem anderen Tanz, ordnet sie sich nach dem *aktuellen* Tanz ein (Test in Task 2).
3. **Sicherung mit Verweisen auf frisches Handy, und alte Sicherung ohne Verweise:** Gruppen, Verbindungen und „schon gefragt“ kommen wieder. Die alte Sicherung importiert ohne Fehler (Test in Task 3).
4. **Entwurf mit geänderten Verweisen, und Android beendet die App:** Die Verweise im Entwurf überstehen das wie Text und Medien (Test in Task 5).
5. **Deutsche Sortierung mit Umlauten und gemischter Schreibweise:** „Ärmel“ steht bei A, „crossbody lead“ und „Crossbody Lead“ stehen zusammen (Test in Task 4).

---

## Dateistruktur

```
app/src/main/java/io/github/katzfuss11/tanzlexikon/
  data/Entities.kt            FigureEntity + linkGroup, askedName; neue FigureLinkEntity
  data/TanzDatabase.kt        version 2, AutoMigration(1, 2), Entity FigureLinkEntity
  data/LexikonDao.kt          Abfragen für Gruppen, Verbindungen, alle Figuren
  lexikon/Models.kt           FigureListItem, FigureLinks, LinkSuggestion, normalizeName()
  lexikon/LexikonRepository.kt  observeAllFigures, observeLinks, linkSuggestion, answerSuggestion, saveLinks; snapshot/replaceAll erweitert
  transfer/ArchiveFormat.kt   ArchFigure + key, linkGroup, askedName; ArchiveManifest + links; ArchLink
  ui/start/StartViewSettings.kt  (neu) Ansicht der Startseite merken
  ui/start/StartScreen.kt, StartViewModel.kt  Umschalt-Symbol, Liste „Alle Figuren“
  ui/figur/FigurViewModel.kt, FigurScreen.kt  „Siehe auch“, Verknüpfen/Lösen, Vorschlag
  ui/figur/SeeAlso.kt          (neu) Abschnitt „Siehe auch“ und Auswahl-Dialog
  ui/tanz/TanzScreen.kt        neue Figur öffnet mit neu=true
  ui/Navigation.kt             Route figur/{id}?neu=, Übergänge
  TanzlexikonApp.kt            startViewSettings
app/schemas/.../2.json         von Room erzeugt, mit committen
```

---

### Task 1: Datenmodell und Migration auf Schema 2

**Files:**
- Modify: `data/Entities.kt`, `data/TanzDatabase.kt`, `app/build.gradle.kts` (nur falls room-testing gebraucht wird), `gradle/libs.versions.toml`
- Create: `app/schemas/io.github.katzfuss11.tanzlexikon.data.TanzDatabase/2.json` (erzeugt KSP)
- Test: `app/src/test/java/io/github/katzfuss11/tanzlexikon/data/MigrationTest.kt`

**Interfaces:**
- Produces:
  - `FigureEntity` bekommt am Ende `val linkGroup: Long? = null` (Gruppe „dieselbe Figur in anderen Tänzen“) und `val askedName: String? = null` (normalisierter Name, für den schon gefragt wurde).
  - `@Entity(tableName = "figure_link", primaryKeys = ["figureA", "figureB"], foreignKeys = [beide → figure.id, onDelete = CASCADE], indices = [Index("figureB")]) data class FigureLinkEntity(val figureA: Long, val figureB: Long)`. Es gilt immer `figureA < figureB`.
  - `TanzDatabase`: `version = 2`, `autoMigrations = [AutoMigration(from = 1, to = 2)]`.

- [ ] **Step 1: Write the failing test** `MigrationTest.migrate1To2_keepsAllData_noLinks()`
  - Legt eine Datenbankdatei mit Schema 1 an. Entweder mit `MigrationTestHelper.createDatabase(name, 1)`, oder (falls der Helper unter Robolectric nicht läuft) mit `SupportSQLiteOpenHelper` und den `createSql`-Anweisungen aus `schemas/.../1.json`, inklusive `room_master_table` mit dem `identityHash` aus 1.json.
  - Fügt ein: Kategorie „Latein“, Tanz „Rumba“, Figur „Fächer“ mit `generalText = "Grundidee 💃"`, ein Medium.
  - Öffnet sie mit `Room.databaseBuilder(context, TanzDatabase::class.java, name)` (Version 2).
  - Assertions: `dao.figure(id)!!.name == "Fächer"`, `generalText == "Grundidee 💃"`, `linkGroup == null`, `askedName == null`, `dao.mediaOf(id).size == 1`, Anzahl Zeilen in `figure_link` == 0.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests '*MigrationTest*'` → FAIL (Spalten oder Tabelle fehlen, Version 1)
- [ ] **Step 3: Implement** die Felder, `FigureLinkEntity` und Version 2 mit AutoMigration. Build erzeugt `2.json`.
- [ ] **Step 4: Run** `./gradlew testDebugUnitTest` → alle grün (90 + 1)
- [ ] **Step 5: Commit** `feat: Datenbank-Schema 2 für Verweise` (inkl. `2.json`)

---

### Task 2: Verweis-Logik im Repository

**Files:**
- Modify: `data/LexikonDao.kt`, `lexikon/Models.kt`, `lexikon/LexikonRepository.kt`
- Test: `app/src/test/java/io/github/katzfuss11/tanzlexikon/lexikon/LinksTest.kt`

**Interfaces:**
- Consumes: Task 1 (`linkGroup`, `askedName`, `FigureLinkEntity`)
- Produces (in `lexikon/Models.kt`, alle `@Serializable`, damit der Entwurf sie sichern kann):
  - `data class FigureListItem(val id: Long, val name: String, val danceId: Long, val danceName: String)`
  - `data class FigureLinks(val otherDances: List<FigureListItem> = emptyList(), val sameDance: List<FigureListItem> = emptyList())`
  - `data class LinkSuggestion(val figureName: String, val candidates: List<FigureListItem>)`
  - `fun normalizeName(name: String): String = name.trim().lowercase(Locale.GERMAN)`
- Produces (in `LexikonRepository`):
  - `fun observeAllFigures(): Flow<List<FigureListItem>>`: aktive Figuren (nicht im Papierkorb, mit Tanz), sortiert mit `java.text.Collator.getInstance(Locale.GERMAN)` (Stärke `SECONDARY`) nach Name, bei gleichem Namen nach Kategorie-Position, Tanz-Position, Figur-Position.
  - `fun observeLinks(figureId: Long): Flow<FigureLinks>`
  - `suspend fun linkSuggestion(figureId: Long): LinkSuggestion?`
  - `suspend fun answerSuggestion(figureId: Long, candidateIds: List<Long>, link: Boolean)`
  - `suspend fun saveLinks(figureId: Long, otherDanceIds: List<Long>, sameDanceIds: List<Long>)`

**Regeln (aus der Spec, bindend):**
- `observeLinks(F)`, nur aktive Partner:
  - `otherDances` = Mitglieder von `F.linkGroup` aus anderen Tänzen ∪ direkte Partner aus anderen Tänzen (ohne Doppel).
  - `sameDance` = direkte Partner im selben Tanz.
  - „Tanz“ ist immer `danceId` *jetzt*. Sortierung jeweils nach Kategorie-, Tanz- und Figur-Position.
- `linkSuggestion(F)`: `null`, wenn `F.askedName == normalizeName(F.name)`. Sonst Kandidaten = aktive Figuren mit gleichem normalisierten Namen, anderem Tanz, nicht in `F.linkGroup` (wenn gesetzt). Keine Kandidaten → `null`.
- `answerSuggestion(F, ids, link)`:
  - Bei `link` werden `F` und alle `ids` in *eine* Gruppe gelegt (vorhandene Gruppen zusammenlegen, sonst neue Nummer = `MAX(linkGroup) + 1`).
  - In beiden Fällen bekommen `F` und alle `ids` `askedName = normalizeName(eigener Name)`. Alles in einer Transaktion.
- `saveLinks(F, other, same)`: Vergleich mit dem aktuellen `observeLinks(F)`, in einer Transaktion.
  - **Neu in `other`:** Gruppe von `F` und Ziel zusammenlegen.
  - **Weg aus `other`:** Ziel bekommt `linkGroup = null`, eine direkte Verbindung `F`–Ziel wird gelöscht, `askedName` für beide gesetzt.
  - **Neu in `same`:** direkte Verbindung anlegen (`min/max` der IDs).
  - **Weg aus `same`:** direkte Verbindung löschen, `askedName` für beide gesetzt.
- Endgültiges Löschen entfernt Verbindungen über CASCADE. Für Gruppen ist nichts zu tun (eine Ein-Mitglied-Gruppe zeigt nichts an).

- [ ] **Step 1: Write the failing tests** in `LinksTest` (Robolectric, In-Memory-DB wie `LexikonRepositoryTest`). `cc(dance)` ist eine Figur „Crossbody Lead“ im jeweiligen Tanz.
  ```kotlin
  suggestion_findsSameNameInOtherDancesOnly()            // Salsa "Crossbody Lead", Rumba "crossbody lead ", Salsa-Doppel "Crossbody Lead", Rumba im Papierkorb
                                                          // → für neue Cha-Cha-Figur: Kandidaten = [Salsa, Salsa-Doppel, Rumba aktiv], nicht der Papierkorb
  suggestion_afterNo_notAskedAgain_fromEitherSide()       // answer(neu, [alt], link=false) → linkSuggestion(neu) == null && linkSuggestion(alt) == null
  suggestion_afterRename_askedAgainForNewName()           // nach answer, saveFigure mit neuem passenden Namen → linkSuggestion != null
  answerYes_groupsAcrossDances_transitively()             // Salsa+Rumba verknüpft, dann Cha-Cha + [Salsa] → observeLinks(Rumba).otherDances.map{it.danceName} == ["Cha-Cha-Cha","Salsa"] (Startseiten-Reihenfolge)
  group_hidesSameDanceMembers_untilDirectlyLinked()       // Spec-Beispiel 1–3: Rumba "CL" zeigt Rumba "CL mit Drehung" erst nach saveLinks(same=[Drehung]); dann sameDance == [Drehung] und umgekehrt
  sameDanceLink_isNotTransitive()                         // Fächer–Hockeystick, Hockeystick–New York → observeLinks(Fächer).sameDance.map{it.name} == ["Hockeystick"]
  saveLinks_removeFromOther_takesEntryOutOfGroup_keepsItsSameDanceLinks()
  saveLinks_removeSame_deletesOnlyThatLink_bothSidesAsked()
  trash_hidesFromSeeAlso_restoreBringsBack_inCurrentDance()  // Review Focus 2: Rumba-Figur im Papierkorb, Tanz "Rumba" gelöscht, Wiederherstellen legt "Rumba" neu an → wieder in otherDances der Salsa-Figur
  purge_removesDirectLinks()                              // deleteFigurePermanently → keine figure_link-Zeile mehr
  allFigures_sortedGerman_thenByDanceOrder()              // Review Focus 5: ["Ärmel","Basic","crossbody lead"(Cha-Cha),"Crossbody Lead"(Salsa),"Zorro"]; Papierkorb fehlt
  ```
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests '*LinksTest*'` → FAIL
- [ ] **Step 3: Implement** die DAO-Abfragen und die fünf Repository-Methoden nach den Regeln oben. Die Anzeige berechnet Kotlin aus einer Flow-Abfrage über alle Figuren-Zeilen (id, name, danceId, Tanzname, Positionen, linkGroup, trashedAt) kombiniert mit einer Flow-Abfrage über `figure_link`. Die Datenmenge ist klein.
- [ ] **Step 4: Run** `./gradlew testDebugUnitTest` → alle grün
- [ ] **Step 5: Commit** `feat: Verweise zwischen Figuren im Repository`

---

### Task 3: Verweise in Sicherung, nicht in geteilten Figuren

**Files:**
- Modify: `transfer/ArchiveFormat.kt`, `lexikon/LexikonRepository.kt` (`snapshot`, `replaceAll`, `insertArchFigure`)
- Test: `transfer/BackupServiceTest.kt`, `share/FigureShareTest.kt`, `ui/empfang/EmpfangViewModelTest.kt`

**Interfaces:**
- Consumes: Task 2 (`observeLinks`, `linkSuggestion`)
- Produces:
  - `ArchFigure` bekommt am Ende `val key: Int? = null, val linkGroup: Int? = null, val askedName: String? = null`.
  - `@Serializable data class ArchLink(val a: Int, val b: Int)`
  - `ArchiveManifest` bekommt `val links: List<ArchLink> = emptyList()`.
  - `snapshot`: Schlüssel 1..n für die exportierten (aktiven) Figuren, Gruppen auf 1..m umnummeriert, Links nur zwischen exportierten Figuren.
  - `replaceAll`: übernimmt Gruppen (neue Nummern), Links (über `key`) und `askedName`. `importFigure` (Empfang) übernimmt diese Felder **nicht**.

- [ ] **Step 1: Write the failing tests**
  ```kotlin
  // BackupServiceTest
  exportThenImport_keepsGroupsLinksAndAskedName()   // Review Focus 3: Salsa+Rumba Gruppe, Rumba Fächer–Hockeystick direkt, answer(no) bei einer Figur
                                                     // → nach Import auf frischer DB: observeLinks gleich (nach Namen verglichen), linkSuggestion der abgelehnten == null
  import_oldBackupWithoutLinkFields_works()          // Manifest-JSON ohne key/linkGroup/askedName/links (wie v0.1.2) → Success, keine Verweise
  // FigureShareTest
  shareAsFile_manifestHasNoLinkData()                // geteilte Figur aus einer Gruppe: figures[0].linkGroup == null, key == null, askedName == null, links.isEmpty()
  // EmpfangViewModelTest
  empfang_addedFigure_hasNoGroupAndIsNotAsked()      // auch wenn die Datei linkGroup/askedName enthielte → neue Figur: linkGroup == null, askedName == null
  ```
- [ ] **Step 2: Run** die drei Testklassen → FAIL
- [ ] **Step 3: Implement** wie oben.
- [ ] **Step 4: Run** `./gradlew testDebugUnitTest` → alle grün
- [ ] **Step 5: Commit** `feat: Verweise in der Sicherung`

---

### Task 4: Startseite mit Ansicht „Alle Figuren“

**Files:**
- Create: `ui/start/StartViewSettings.kt`
- Modify: `TanzlexikonApp.kt`, `ui/start/StartViewModel.kt`, `ui/start/StartScreen.kt`, `res/values/strings.xml`
- Test: `app/src/test/java/io/github/katzfuss11/tanzlexikon/ui/start/StartViewSettingsTest.kt`, `ui/start/StartViewModelTest.kt`

**Interfaces:**
- Consumes: Task 2 (`observeAllFigures()`)
- Produces:
  - `enum class StartView { BY_DANCE, ALL_FIGURES }`
  - `class StartViewSettings(prefs: SharedPreferences) { val view: StateFlow<StartView>; fun set(view: StartView) }`, Schlüssel `"start_ansicht"`, Standard `BY_DANCE`, `companion fun create(context)` mit Prefs `"einstellungen"` (Muster wie `ThemeSettings`).
  - `TanzlexikonApp.startViewSettings` (lazy).
  - `StartViewModel(repo, isAvailable, viewSettings: StartViewSettings? = null)`. `StartState` bekommt `view: StartView = BY_DANCE` und `allFigures: List<FigureListItem> = emptyList()`. Dazu `fun toggleView()`.

- [ ] **Step 1: Write the failing tests**
  ```kotlin
  // StartViewSettingsTest
  default_isByDance()
  set_isRememberedAcrossInstances()                    // neue Instanz mit denselben Prefs → ALL_FIGURES
  // StartViewModelTest
  toggleView_switchesAndExposesSortedFigures()         // toggleView() → state.view == ALL_FIGURES, allFigures.map{it.name} sortiert wie Task 2
  ```
- [ ] **Step 2: Run** → FAIL
- [ ] **Step 3: Implement**
  - Settings und ViewModel wie oben.
  - In `StartScreen`: Ein Symbol in `TopAppBar.actions` vor ⋮ (nicht im Ordnungsmodus). Es zeigt die *andere* Ansicht: bei `BY_DANCE` ein Listen-Symbol mit Beschreibung „Alle Figuren anzeigen“, bei `ALL_FIGURES` ein Symbol für Gruppen mit Beschreibung „Nach Tanz anzeigen“, beide aus dem vorhandenen Icon-Set.
  - Bei `ALL_FIGURES` ersetzt eine `LazyColumn` die Tanzliste. Je Zeile `ListItem(headline = name, supporting = danceName)`, Tipp → `onOpenFigure(id)`.
  - Ist die Liste leer, erscheint der Hinweis „Noch keine Figuren. Öffne einen Tanz und tippe auf +, um die erste anzulegen.“
  - Suchfeld, Hinweis auf fehlende Medien und „+“ bleiben. Der Menüpunkt zum Ordnen schaltet vorher auf `BY_DANCE`.
- [ ] **Step 4: Run** `./gradlew testDebugUnitTest` → grün. Im Emulator umschalten, App neu starten: Ansicht bleibt.
- [ ] **Step 5: Commit** `feat: Startseite zeigt wahlweise alle Figuren`

---

### Task 5: „Siehe auch“ auf der Figurenseite, Verknüpfen, Lösen, Vorschlag

**Files:**
- Create: `ui/figur/SeeAlso.kt`
- Modify: `ui/figur/FigurViewModel.kt`, `ui/figur/FigurScreen.kt`, `ui/tanz/TanzScreen.kt`, `ui/Navigation.kt`, `res/values/strings.xml`
- Test: `ui/figur/FigurViewModelTest.kt`

**Interfaces:**
- Consumes: Task 2 (alle fünf Methoden, `FigureListItem`, `FigureLinks`, `LinkSuggestion`)
- Produces:
  - `FigurDraft` bekommt `val links: FigureLinks = FigureLinks()`. `startEditing()` übernimmt die aktuellen Verweise. `hasChanges` vergleicht auch sie.
  - `FigurState` bekommt `links: FigureLinks = FigureLinks()`, `suggestion: LinkSuggestion? = null` und `linkCandidates: List<FigureListItem> = emptyList()`. Kandidaten sind alle aktiven Figuren außer der Figur selbst und den im Entwurf schon angezeigten, sortiert wie `observeAllFigures`.
  - `FigurViewModel(repo, figureId, saved, workScope, isNew: Boolean = false)`
  - `fun addLink(target: FigureListItem)`: `target.danceId == detail.figure.danceId` → `sameDance`, sonst `otherDances`. Nur im Entwurf.
  - `fun removeLink(target: FigureListItem)`: aus der jeweiligen Liste des Entwurfs.
  - `save()`: `saveFigure` → `saveLinks(id, draft.links.otherDances.ids, draft.links.sameDance.ids)` → `linkSuggestion(id)` in `state.suggestion`.
  - `isNew == true`: beim Start einmal `linkSuggestion(id)`. Die Wiederholung verhindert `askedName`.
  - `fun answerSuggestion(link: Boolean)` (Schließen ohne Antwort = `false`).
  - Route `Routes.FIGUR = "figur/{figureId}?neu={neu}"`, `fun figur(figureId: Long, neu: Boolean = false)`. `TanzScreen` bekommt `onFigureCreated: (Long) -> Unit` (→ `figur(id, neu = true)`). `EmpfangScreen.onAdded` navigiert mit `neu = true`.

- [ ] **Step 1: Write the failing tests** in `FigurViewModelTest`
  ```kotlin
  seeAlso_showsLinksFromRepository()                 // Gruppe Salsa+Rumba → vm(Salsa).state.links.otherDances.map{it.danceName} == ["Rumba"]
  editLinks_onlyAfterSave_cancelDiscards()           // addLink(Hockeystick) → Repo unverändert; cancelEditing → state.links unverändert; erneut + save → sameDance == [Hockeystick]
  removeLink_marksHasChanges()                       // startEditing, removeLink(x) → hasChanges == true
  draftLinks_surviveRecreation()                     // Review Focus 4: addLink, recreated(saved) → draft.links enthält das Ziel
  linkCandidates_excludeSelfAndShown()               // Kandidaten enthalten weder die Figur noch schon verknüpfte; nach addLink verschwindet das Ziel
  save_oldFigure_firstTime_offersSuggestion_once()   // alte Figur "Crossbody Lead" (askedName null), Rumba gleichnamig → nach save: suggestion != null; answer(false); erneut save → suggestion == null
  newFigure_offersSuggestionOnOpen()                 // isNew = true, gleichnamige Figur in anderem Tanz → state.suggestion.candidates.size == 1
  answerYes_linksAndClearsSuggestion()               // answerSuggestion(true) → suggestion == null, links.otherDances.size == 1
  ```
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests '*FigurViewModelTest*'` → FAIL
- [ ] **Step 3: Implement** ViewModel wie oben, dazu die Oberfläche in `SeeAlso.kt`:
  - `SeeAlsoSection(links, editing, onOpen, onRemove, onAdd)` als Karte im Stil von `SectionCard`, Titel „Siehe auch“, Teile „In anderen Tänzen“ und „In diesem Tanz“.
  - Je Eintrag Figurenname mit Tanz darunter, beim Bearbeiten mit Lösen-Knopf (Beschreibung „Verknüpfung lösen“). Beim Bearbeiten gibt es den Knopf „Figur verknüpfen“.
  - Außerhalb des Bearbeitens fehlt die Karte, wenn es keine Verweise gibt. Leere Teile fehlen.
  - `LinkPickerDialog(candidates, onPick, onDismiss)` mit Suchfeld (Name enthält, ohne Groß/Klein) und Liste wie „Alle Figuren“. Ein Tipp wählt aus und schließt.
  - Vorschlags-Dialog: Titel „{Name} gibt es auch in {Tänze, mit „und“ verbunden}.“, Text „Verknüpfen, damit du zwischen ihnen springen kannst?“, Knöpfe „Nicht verknüpfen“ und „Verknüpfen“.
  - Tipp auf einen Eintrag außerhalb des Bearbeitens → `nav.navigate(Routes.figur(id))`.
- [ ] **Step 4: Run** `./gradlew testDebugUnitTest` → grün. Im Emulator:
  - Figur „Crossbody Lead“ in Salsa anlegen, dann in Rumba (Vorschlag → Verknüpfen), dann in Cha-Cha-Cha.
  - Alle drei sehen sich gegenseitig, Sprung und Zurück funktionieren.
  - Rumba „Crossbody Lead mit Drehung“ von Hand verknüpfen und das Spec-Beispiel prüfen.
- [ ] **Step 5: Commit** `feat: Siehe auch – Figuren verknüpfen, lösen und Vorschläge`

---

### Task 6: Seitliche Übergänge

**Files:**
- Modify: `ui/Navigation.kt`

**Interfaces:**
- Consumes: nichts Neues

- [ ] **Step 1: Implement** im `NavHost`:
  - `enterTransition = slideIntoContainer(SlideDirection.Start, tween(300))`, `exitTransition = slideOutOfContainer(SlideDirection.Start, tween(300))`.
  - `popEnterTransition = slideIntoContainer(SlideDirection.End, tween(300))`, `popExitTransition = slideOutOfContainer(SlideDirection.End, tween(300))`.
  - Kein Unit-Test, weil reine Animation.
- [ ] **Step 2: Verify** im Emulator (API 35):
  - Start → Langsamer Walzer → Figur gleitet nach links, Zurück gleitet nach rechts. Gleiches gilt für Suche, Einstellungen, Papierkorb und einen Eintrag in „Siehe auch“.
  - Dann `adb shell settings put global animator_duration_scale 0`: Wechsel ohne Animation. Falls Compose das nicht von selbst beachtet, liest die Dauer `Settings.Global.ANIMATOR_DURATION_SCALE` und nimmt bei 0 `tween(0)`.
  - Danach wieder auf 1 stellen.
- [ ] **Step 3: Run** `./gradlew testDebugUnitTest` → grün
- [ ] **Step 4: Commit** `feat: Bildschirme gleiten seitlich`

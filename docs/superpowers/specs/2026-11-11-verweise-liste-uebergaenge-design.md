# Tanzlexikon – Verweise, Liste aller Figuren, Übergänge

Stand: 11.11.2026, aufbauend auf v0.1.2 (`main`). Grundlage: [2026-11-08-tanzlexikon-design.md](2026-11-08-tanzlexikon-design.md). Was dort steht, gilt weiter, wenn hier nichts anderes steht.

## Wozu

Viele Figuren kommen in mehreren Tänzen vor, zum Beispiel „Crossbody Lead“ in Salsa, Rumba und Cha-Cha-Cha. Der Autor will:

1. **Von einer Figur zur gleichen Figur in einem anderen Tanz springen** (Verweise).
2. **Einen Überblick über alle Figuren bekommen, nach Namen sortiert.** So sieht er, welche Figuren in mehreren Tänzen vorkommen, kann sie einzeln anwählen und Lücken ergänzen.
3. **Beim Navigieren sehen, wohin es geht:** Beim Hineingehen gleitet der Bildschirm nach links weg.

## Ziele

- Verweise zwischen gleichnamigen Figuren verschiedener Tänze, die nur mit Zustimmung des Autors entstehen.
- Eine zweite Ansicht der Startseite, „Alle Figuren“, umschaltbar mit der heutigen Ansicht „Nach Tanz“.
- Seitliche Übergänge zwischen allen Bildschirmen.

## Nicht-Ziele

- **Verknüpfen von Figuren mit verschiedenen Namen** („Cross Body Lead“ mit „Crossbody Lead“). Wer das will, benennt eine Figur um. Dann kommt der Vorschlag.
- **Wischen mit dem Finger zum Vor- oder Zurückgehen.** Es geht nur um die Animation, der Autor hat das ausdrücklich so gesagt.
- **Andere Sortierungen der Liste** (nach Datum, nach Anzahl der Medien und Ähnliches).
- **Gemeinsame Inhalte verknüpfter Figuren.** Jede Figur behält ihre eigenen Texte und Medien. Ein Verweis ist nur ein Sprung.

## Anti-Ziele (das wäre ein Fehlschlag, selbst wenn alles funktioniert)

- **Verweise ohne Zustimmung.** Die App verknüpft nie etwas von selbst, auch nicht beim ersten Start nach dem Update.
- **Nervige Vorschläge.** Eine abgelehnte Verknüpfung kommt für denselben Namen nicht wieder.
- **Das schnelle Anlegen einer Figur nach der Tanzstunde wird umständlicher.** Der Vorschlag ist ein Dialog mit einer Antwort und blockiert sonst nichts.
- **Datenverlust beim Update.** Die neue Datenbankversion muss die vorhandenen Daten unverändert übernehmen.

## Verweise

### Wann zwei Figuren „gleich heißen“

Zwei Namen gelten als gleich, wenn sie nach dem Abschneiden der Leerzeichen am Rand ohne Unterscheidung von Groß- und Kleinschreibung übereinstimmen. „Crossbody Lead“ und „crossbody lead “ sind gleich, „Cross Body Lead“ ist es nicht.

Kandidaten für einen Verweis sind nur Figuren, die
- in einem **anderen Tanz** liegen (Gleichnamige im selben Tanz sind Doppel, keine Verweise),
- **nicht im Papierkorb** sind,
- **noch nicht** mit dieser Figur verknüpft sind.

### Wann die App vorschlägt

Der Vorschlag kommt **nur dann**, wenn es mindestens einen Kandidaten gibt, und zu diesen Zeitpunkten:

1. **Neue Figur angelegt:** direkt nachdem der Name im Dialog „Neue Figur“ bestätigt wurde.
2. **Figur bearbeitet und gespeichert:** nach dem Speichern, wenn für den aktuellen Namen noch nie gefragt wurde. Das deckt beide Fälle ab:
   - **Bestehende Figuren aus der Zeit vor dem Update:** einmal beim nächsten Bearbeiten (Wunsch des Autors).
   - **Umbenannte Figuren:** Passt der neue Name zu anderen Figuren, wird für ihn gefragt.
3. **Geteilte Figur empfangen und hinzugefügt** (Empfang einer `.tanzfigur`). Das zählt wie das Anlegen einer neuen Figur. *(Meine Wahl.)*

**„Einmal“ heißt:** Die App merkt sich pro Figur, für welchen Namen schon gefragt wurde, egal ob die Antwort Ja oder Nein war. Für diesen Namen fragt sie dann nicht noch einmal, selbst wenn später neue gleichnamige Figuren in anderen Tänzen dazukommen. Der Vorschlag erscheint in diesem Fall bei der *neuen* Figur, weil die ja angelegt wird. Ändert sich der Name, wird für den neuen Namen wieder gefragt.

**Die Antwort gilt für alle Figuren im Dialog:** Nach Ja oder Nein gelten die Figur *und alle genannten Kandidaten* als „schon gefragt“ für ihren jeweiligen Namen. Sonst käme eine abgelehnte Verknüpfung von der anderen Seite zurück, sobald eine der älteren Figuren bearbeitet wird. *(Meine Wahl, folgt aus dem Anti-Ziel „nervige Vorschläge“.)*

Wiederherstellen aus einer Sicherung löst keine Vorschläge aus.

### Der Vorschlag

Ein Dialog, zum Beispiel:

> **Crossbody Lead gibt es auch in Salsa und Rumba.**
> Verknüpfen, damit du zwischen ihnen springen kannst?
> [Nicht verknüpfen] [Verknüpfen]

- Genannt werden die Tänze aller Kandidaten. Es gibt eine einzige Entscheidung für alle Kandidaten zusammen. *(Meine Wahl: einfacher als einzelne Häkchen. Wer einzelne nicht will, löst sie danach.)*
- „Verknüpfen“ verbindet die Figur mit allen Kandidaten.
- Schließen des Dialogs ohne Antwort (Zurück, daneben tippen) zählt als „Nicht verknüpfen“. *(Meine Wahl, damit niemand in einer Schleife landet.)*

### Verweise gelten für die ganze Gruppe

Verknüpfte Figuren bilden eine Gruppe. Wird eine Figur mit einer anderen verknüpft, die schon in einer Gruppe ist, wandert sie in diese Gruppe. Gehören die Kandidaten zu verschiedenen Gruppen, werden die Gruppen zusammengelegt. Jede Figur einer Gruppe zeigt alle anderen.

Durch das Zusammenlegen können zwei Figuren aus demselben Tanz in einer Gruppe landen, zum Beispiel zwei gleichnamige Figuren in der Salsa. Das ist erlaubt. „Auch in“ zeigt dann einfach alle anderen Figuren der Gruppe. *(Meine Wahl: seltener Fall, einfache Regel, und man kann lösen.)*

**Umbenennen behält die Verknüpfungen.** Meist korrigiert man damit nur die Schreibweise. Passt die Verknüpfung nicht mehr, löst man sie. Passt der neue Name zu weiteren Figuren, kommt für ihn der Vorschlag, und „Verknüpfen“ legt die Gruppen zusammen. *(Meine Wahl: der Autor hat den Verknüpfungen zugestimmt, sie verschwinden nicht stillschweigend.)*

### Anzeige auf der Figurenseite

Unten auf der Figurenseite, unter den drei Bereichen, steht ein Abschnitt **„Auch in“** mit einem Eintrag pro verknüpfter Figur: der Tanzname, klein darunter der Figurenname, falls er anders geschrieben ist. Ein Tipp öffnet diese Figur mit dem normalen Übergang nach links, und Zurück führt zur vorherigen Figur zurück. Ohne Verknüpfungen fehlt der Abschnitt ganz.

Sortierung der Einträge: in der Reihenfolge der Tänze auf der Startseite. *(Meine Wahl.)*

### Verweis lösen

Beim Bearbeiten einer Figur hat jeder Eintrag in „Auch in“ einen Knopf zum Lösen. Lösen nimmt die Figur *dieses Eintrags* aus der Gruppe, nicht die gerade bearbeitete. Wer bei der Salsa-Figur neben „Rumba“ auf Lösen tippt, meint: „Die Rumba-Figur gehört nicht dazu.“ Die übrigen bleiben untereinander verknüpft. Die herausgenommene Figur gilt als „schon gefragt“ für ihren Namen, damit der Vorschlag nicht sofort wiederkommt. *(Meine Wahl.)* Wie das Entfernen von Medien wird auch das Lösen erst mit „Speichern“ wirksam und mit „Abbrechen“ verworfen. *(Meine Wahl, passt zum übrigen Bearbeiten.)*

Hat jemand einen Vorschlag abgelehnt und will die Verknüpfung doch, benennt er die Figur kurz um und wieder zurück. *(Meine Wahl: kein eigener Knopf „Verknüpfen“, damit es einfach bleibt. Der Builder darf einen vorschlagen, falls er sich ohne Aufwand ergibt.)*

### Papierkorb und Löschen

- Eine Figur im Papierkorb erscheint bei den anderen nicht in „Auch in“. Sie behält aber ihre Gruppe. Wird sie wiederhergestellt, ist sie wieder verknüpft.
- Endgültig gelöschte Figuren (nach 30 Tagen) verschwinden aus der Gruppe. Bleibt nur eine Figur übrig, ist sie nicht mehr verknüpft.
- Ein Tanz lässt sich wie bisher nur löschen, wenn er keine Figuren mehr enthält (außer solchen im Papierkorb). Für die Verweise ändert sich dadurch nichts.

### Sicherung und Teilen

- **Sicherung (Export/Import):** Die Verweise sind enthalten und kommen beim Wiederherstellen wieder. Ebenso die Information, für welchen Namen schon gefragt wurde. Sonst kämen nach einem Handywechsel alle Vorschläge erneut.
- **Ältere Sicherungen** ohne diese Angaben lassen sich weiter importieren. Sie haben dann eben keine Verweise.
- **Eine `.tanzfigur` enthält keine Verweise.** Sie wären auf einem anderen Handy bedeutungslos. Beim Empfänger greift der Vorschlag beim Hinzufügen.
- Eine Sicherung aus v0.1.3 soll von v0.1.2 noch gelesen werden können, wenn das ohne Aufwand geht (unbekannte Felder werden dort ignoriert). Die Verweise gehen dabei verloren. Das ist in Ordnung.

## Liste „Alle Figuren“

### Umschalten

Auf der Startseite sitzt oben in der Leiste, links neben dem Menü (⋮), ein Symbol zum Umschalten zwischen **„Nach Tanz“** (heutige Ansicht) und **„Alle Figuren“**. Das hat der Autor gewählt (Variante B im Entwurf). Das Symbol zeigt jeweils die *andere* Ansicht an und hat eine passende Beschreibung für Bildschirmleser. Die App merkt sich die zuletzt gewählte Ansicht, auch über einen Neustart hinaus. *(Meine Wahl.)* Diese Einstellung gehört wie die Darstellung (hell/dunkel) zu den Einstellungen des Geräts und nicht in die Sicherung.

### Inhalt

- Alle Figuren außer denen im Papierkorb.
- Jede Zeile: der **Figurenname**, darunter **kleiner der Tanz**.
- **Sortiert nach Namen**, ohne Unterscheidung von Groß- und Kleinschreibung und nach deutscher Sortierung (Ä wie A, Ö wie O, Ü wie U). Gleichnamige Figuren stehen untereinander, geordnet in der Reihenfolge der Tänze auf der Startseite.
- Ein Tipp öffnet die Figur.
- Ohne Figuren erscheint ein kurzer Hinweis, wie es sie gibt (so wie heute in einem leeren Tanz).

### Was gleich bleibt

- Das Suchfeld oben bleibt und funktioniert wie bisher.
- Das „+“ unten rechts tut in beiden Ansichten dasselbe wie heute.
- Der Hinweis auf fehlende Mediendateien bleibt in beiden Ansichten sichtbar.
- Verschieben per Ziehen gibt es in der Liste nicht. Die Reihenfolge ist ja alphabetisch.

## Übergänge

- **Vorwärts** (etwas antippen, das tiefer führt: Tanz, Figur, Suche, Einstellungen, Papierkorb, Empfang, Verweis in „Auch in“): Der bisherige Bildschirm gleitet nach links hinaus, der neue kommt von rechts herein.
- **Zurück** (Zurück-Pfeil, Zurück-Geste, Zurück-Taste): umgekehrt.
- Das gilt für alle Wechsel zwischen Bildschirmen. Dialoge und der Vollbild-Player sind keine Bildschirme und bleiben, wie sie sind.
- Dauer etwa 300 ms. *(Meine Wahl, Feinabstimmung dem Builder überlassen.)*
- Hat jemand in den Android-Einstellungen die Animationen ausgeschaltet, soll das respektiert werden.

## Rahmenbedingungen

- **Gleiche Technik wie bisher:** Kotlin, Jetpack Compose, Room, Navigation Compose. Neue Bibliotheken nur, wenn unvermeidlich.
- **Datenbank:** Die neue Version braucht eine Room-Migration von Schema 1 auf 2, mit Test gegen das exportierte `schemas/1.json`. Bestehende Daten bleiben unverändert. Am Anfang ist keine Figur verknüpft, und für keine wurde gefragt.
- **minSdk 29**, Tests mit Robolectric wie bisher. Die bestehenden 90 Tests bleiben grün.
- **Der Autor ist kein Programmierer:** alle Texte auf Deutsch, verständlich, ohne Fachbegriffe.
- **Testen auf echten Geräten:** Der Autor testet auf seinem Handy. Am Ende steht ein Release (v0.1.3), wenn der Autor es freigibt.

## Dem Builder überlassen

- Wie Gruppen und das „schon gefragt für Namen X“ gespeichert werden (Spalten an der Figur, eigene Tabelle, Ähnliches).
- Genaue Formulierungen und Symbole, im Ton der bestehenden App.
- Wie der Abschnitt „Auch in“ aussieht, passend zu den bestehenden Bereichskarten.
- Wie die Übergänge technisch umgesetzt werden.
- Ob die Liste Abschnittsbuchstaben (A, B, C …) bekommt. Nicht verlangt, aber erlaubt, wenn es den Überblick verbessert.

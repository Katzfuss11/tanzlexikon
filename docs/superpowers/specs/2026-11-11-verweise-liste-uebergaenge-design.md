# Tanzlexikon – Siehe auch, Liste aller Figuren, Übergänge

Stand: 11.11.2026, aufbauend auf v0.1.2 (`main`). Grundlage: [2026-11-08-tanzlexikon-design.md](2026-11-08-tanzlexikon-design.md). Was dort steht, gilt weiter, wenn hier nichts anderes steht.

## Wozu

Viele Figuren kommen in mehreren Tänzen vor, zum Beispiel „Crossbody Lead“ in Salsa, Rumba und Cha-Cha-Cha. Der Autor will:

1. **Von einer Figur zu verwandten Figuren springen** (Verweise, angezeigt als „Siehe auch“): zur gleichen Figur in einem anderen Tanz, aber auch zu Figuren, die ähnlich sind oder aufeinander aufbauen, auch im selben Tanz.
2. **Einen Überblick über alle Figuren bekommen, nach Namen sortiert.** So sieht er, welche Figuren in mehreren Tänzen vorkommen, kann sie einzeln anwählen und Lücken ergänzen.
3. **Beim Navigieren sehen, wohin es geht:** Beim Hineingehen gleitet der Bildschirm nach links weg.

## Ziele

- Verweise zwischen Figuren, die nur mit Zustimmung des Autors entstehen: vorgeschlagen bei gleichnamigen Figuren in anderen Tänzen, außerdem jederzeit von Hand zu beliebigen Figuren.
- Eine zweite Ansicht der Startseite, „Alle Figuren“, umschaltbar mit der heutigen Ansicht „Nach Tanz“.
- Seitliche Übergänge zwischen allen Bildschirmen.

## Nicht-Ziele

- **Arten von Verweisen** („baut auf“, „ähnlich“, „gleiche Figur“). Ein Verweis ist einfach ein Verweis, ohne Beschriftung.
- **Vorschläge nach ähnlichen Namen.** Vorgeschlagen wird nur bei gleichem Namen. Alles andere verknüpft der Autor von Hand.
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

Kandidaten für einen **Vorschlag** sind nur Figuren, die
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
- „Verknüpfen“ nimmt die Figur und alle Kandidaten in eine gemeinsame Gruppe auf (siehe unten). Die Kandidaten liegen ja alle in anderen Tänzen.
- Schließen des Dialogs ohne Antwort (Zurück, daneben tippen) zählt als „Nicht verknüpfen“. *(Meine Wahl, damit niemand in einer Schleife landet.)*

### Zwei Arten von Verweisen (Wunsch des Autors)

**Zwischen verschiedenen Tänzen: dieselbe Figur, eine Gruppe.** Ein Verweis zwischen Figuren aus verschiedenen Tänzen bedeutet in der Regel: Das ist dieselbe Figur. Solche Figuren bilden eine Gruppe. Werden Crossbody Lead in Salsa und Rumba verknüpft und später Cha-Cha-Cha mit Salsa, gehören alle drei zusammen. Ist eine der beiden Figuren schon in einer anderen Gruppe, werden die Gruppen zusammengelegt. Das gilt für Vorschläge und für Verweise von Hand, auch bei verschiedenen Namen.

**Im selben Tanz: eine einzelne Verbindung.** Ein Verweis zwischen zwei Figuren desselben Tanzes (etwa „Fächer“ ↔ „Hockeystick“ in der Rumba, ähnliche oder aufeinander aufbauende Figuren) verbindet genau diese zwei. Er setzt sich nicht fort und wirkt nicht auf andere Tänze. Er ist auf beiden Seiten sichtbar.

**Die eine Regel, die beides zusammenhält:** Eine Figur zeigt von ihrer Gruppe nur die Mitglieder aus *anderen* Tänzen. Figuren aus dem eigenen Tanz zeigt sie nur über eine direkte Verbindung. Eine Gruppe darf deshalb beliebig viele Figuren eines Tanzes enthalten (kein Limit, Wunsch des Autors). Beispiel:
1. Crossbody Lead in Salsa, Rumba und Cha-Cha-Cha sind eine Gruppe.
2. Die Salsa-Figur wird außerdem mit Rumba „Crossbody Lead mit Drehung“ verknüpft. Die kommt in die Gruppe. Salsa und Cha-Cha-Cha zeigen jetzt beide Rumba-Figuren. Rumba „Crossbody Lead“ zeigt Salsa und Cha-Cha-Cha, die Drehung aber nicht, weil sie im selben Tanz liegt.
3. Rumba „Crossbody Lead“ ↔ Rumba „Crossbody Lead mit Drehung“ werden direkt verbunden. Jetzt zeigen sich die beiden gegenseitig.

Zwischen zwei Figuren gibt es höchstens einen direkten Verweis. Eine Figur kann nicht mit sich selbst verknüpft werden. Maßgeblich ist immer der Tanz, in dem eine Figur *gerade* liegt. Landet eine Figur durch Wiederherstellen in einem anderen Tanz, ordnen sich ihre Verweise danach neu ein.

**Umbenennen behält die Verweise.** Meist korrigiert man damit nur die Schreibweise, und bei Verweisen von Hand spielt der Name ohnehin keine Rolle. Passt der neue Name zu weiteren Figuren, kommt für ihn der Vorschlag. *(Meine Wahl: der Autor hat den Verweisen zugestimmt, sie verschwinden nicht stillschweigend.)*

### Von Hand verknüpfen (Wunsch des Autors)

Beim Bearbeiten einer Figur gibt es im Abschnitt „Siehe auch“ einen Knopf **„Figur verknüpfen“**. Er öffnet eine Auswahl aller Figuren mit Suchfeld (Suche im Namen). Angezeigt wird jeweils der Figurenname und klein darunter der Tanz, wie in der Liste „Alle Figuren“.
- Wählbar sind alle Figuren außer der Figur selbst, außer schon angezeigten und außer solchen im Papierkorb, also auch Figuren aus demselben Tanz und Figuren mit anderem Namen.
- Eine Figur aus einem anderen Tanz kommt in die Gruppe, eine aus demselben Tanz wird direkt verbunden.
- Ein Tipp auf eine Figur fügt sie dem Entwurf hinzu und schließt die Auswahl. *(Meine Wahl: eine Figur pro Durchgang, einfacher als Mehrfachauswahl.)*
- Wie alle Änderungen beim Bearbeiten wird das erst mit „Speichern“ wirksam und mit „Abbrechen“ verworfen.

### Anzeige auf der Figurenseite

Unten auf der Figurenseite, unter den drei Bereichen, steht ein Abschnitt **„Siehe auch“** mit zwei Teilen:
- **„In anderen Tänzen“:** die Mitglieder der Gruppe aus anderen Tänzen.
- **„In diesem Tanz“:** die direkt verbundenen Figuren aus demselben Tanz.

Jeder Eintrag zeigt den Figurennamen, klein darunter den Tanz. Ein Tipp öffnet diese Figur mit dem normalen Übergang nach links, und Zurück führt zur vorherigen Figur zurück. Leere Teile fehlen. Ohne Verweise fehlt der Abschnitt außerhalb des Bearbeitens ganz.

Sortierung der Einträge: in der Reihenfolge der Tänze auf der Startseite, innerhalb eines Tanzes in der Reihenfolge der Figuren dort. *(Meine Wahl.)*

### Verweis lösen

Beim Bearbeiten einer Figur hat jeder Eintrag in „Siehe auch“ einen Knopf zum Lösen.
- **Unter „In anderen Tänzen“** nimmt Lösen die Figur *dieses Eintrags* aus der Gruppe. Wer bei der Salsa-Figur neben „Rumba“ auf Lösen tippt, meint: „Die Rumba-Figur gehört nicht dazu.“ Die übrigen bleiben eine Gruppe. Ihre Verbindungen im eigenen Tanz behält die herausgenommene Figur.
- **Unter „In diesem Tanz“** verschwindet nur diese eine Verbindung, auf beiden Seiten.

Beide beteiligten Figuren gelten danach als „schon gefragt“ für ihren Namen, damit ein gelöster Verweis zwischen gleichnamigen Figuren nicht wieder vorgeschlagen wird. *(Meine Wahl.)* Wie das Entfernen von Medien wird auch das Lösen erst mit „Speichern“ wirksam und mit „Abbrechen“ verworfen. *(Meine Wahl, passt zum übrigen Bearbeiten.)*

Wer einen Vorschlag abgelehnt hat und die Verknüpfung doch will, nimmt „Figur verknüpfen“.

Die Rückfrage „Änderungen verwerfen?“ berücksichtigt auch geänderte Verweise.

### Papierkorb und Löschen

- Eine Figur im Papierkorb erscheint bei den anderen nicht in „Siehe auch“. Gruppe und Verbindungen behält sie aber. Wird sie wiederhergestellt, ist sie wieder verknüpft.
- Endgültig gelöschte Figuren (nach 30 Tagen) verlieren Gruppe und Verbindungen. Bleibt in einer Gruppe nur eine Figur übrig, ist sie nicht mehr verknüpft.
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

- **Vorwärts** (etwas antippen, das tiefer führt: Tanz, Figur, Suche, Einstellungen, Papierkorb, Empfang, Eintrag in „Siehe auch“): Der bisherige Bildschirm gleitet nach links hinaus, der neue kommt von rechts herein.
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

- Wie Gruppen, direkte Verbindungen und das „schon gefragt für Namen X“ gespeichert werden (Gruppen-Spalte an der Figur, eigene Tabelle für Verbindungen o. Ä.).
- Genaue Formulierungen und Symbole, im Ton der bestehenden App.
- Wie der Abschnitt „Siehe auch“ und die Auswahl beim Verknüpfen aussehen, passend zu den bestehenden Bereichskarten.
- Wie die Übergänge technisch umgesetzt werden.
- Ob die Liste Abschnittsbuchstaben (A, B, C …) bekommt. Nicht verlangt, aber erlaubt, wenn es den Überblick verbessert.

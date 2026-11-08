# Tanzlexikon – Design

Stand: 08.11.2026 · Status: Entwurf, noch nicht freigegeben

## Wozu die App da ist

Das Tanzlexikon ist ein **persönliches Nachschlagewerk für Tanzfiguren** auf dem Android-Handy.

Neue Figuren lernt man in der Tanzschule. Nur aus Videos oder Beschreibungen klappt das kaum, wenn einem die Grundtechnik fehlt. Danach kann man die Figur, aber wer sie nicht oft genug übt, vergisst sie wieder. Die App soll sie schnell ins Gedächtnis zurückholen: Man öffnet die Figur, liest die Schritte, schaut das eigene Video. Sie ersetzt keinen Unterricht, sie ist eine Gedächtnisstütze.

Jeder Nutzer füllt die App mit **eigenen Inhalten**. Es gibt keine fertige Datenbank mit Figuren. Zuerst ist die App nur für den Autor selbst gedacht. Weil keine persönlichen Daten im Code stecken, soll der Code später öffentlich auf GitHub stehen, damit andere ihn nutzen können.

## Ziele

- Tänze nach Kategorien geordnet sehen, darin die gelernten Figuren, darin Beschreibung und Medien.
- Jede Figur hat drei Bereiche: **Allgemein**, **Leader** und **Follower**. Jeder Bereich hat einen eigenen Text und eigene Bilder und Videos. Die meisten Medien landen bei Allgemein, weil Paartänze zusammen gefilmt werden.
- Medien: selbst aufgenommene Videos (der Hauptfall), Bilder und Videos aus der Galerie (z. B. vom Tanzlehrer) und YouTube-Links.
- Schnelles Festhalten direkt nach der Stunde muss gehen. Pflicht ist nur der Name, alles andere lässt sich später ergänzen.
- Was man versehentlich löscht, darf nicht sofort und endgültig weg sein.
- Die Daten überstehen einen Handywechsel.

## Nicht-Ziele (vorerst)

- Kein Lernkurs, keine fertigen Inhalte, kein Teilen mit anderen innerhalb der App.
- Kein Server, kein Benutzerkonto, keine Anmeldung.
- Kein Übungsstand („wie sicher“, „zuletzt geübt“). Das ist eine mögliche spätere Idee.
- Keine Cloud-Synchronisation. Sie ist als **späteres To-do** vorgemerkt.
- Keine Veröffentlichung im Play Store. Installiert wird über Android Studio oder eine APK.

## Anti-Ziele (das wäre ein Fehlschlag, selbst wenn alles funktioniert)

- Ein Fehltipp löscht eine ganze Sammlung.
- Das Anlegen einer Figur ist so umständlich, dass man nach der Stunde darauf verzichtet.
- Beim Nachschlagen ändert man versehentlich etwas.
- Ein neues Handy bedeutet, dass alle Einträge weg sind.

## Rahmenbedingungen

- Der Autor hat **noch nie eine Android-App entwickelt** und kennt weder die Sprachen noch die Werkzeuge. Zum Plan gehört deshalb eine **Schritt-für-Schritt-Anleitung**: Android Studio installieren, Projekt öffnen, App im Emulator und auf dem eigenen Handy starten, APK bauen. Code und Struktur sollen für einen Anfänger nachvollziehbar bleiben, ohne unnötige Abstraktionen.
- Repo: `C:\Users\<Name>\repos\tanzlexikon`, GitHub `Katzfuss11/tanzlexikon` (zunächst privat, später öffentlich).
- Die App-Oberfläche ist auf Deutsch. (Mehrsprachigkeit ist nicht nötig, die Texte sollten aber in String-Ressourcen liegen, damit andere später übersetzen können.)

## Inhalt und Struktur

**Kategorie → Tanz → Figur.**

- **Kategorien** und **Tänze** lassen sich anlegen, umbenennen, umsortieren und löschen. Beim ersten Start bringt die App eine Grundliste mit:
  - Standard: Langsamer Walzer, Tango, Wiener Walzer, Slowfox, Quickstep
  - Latein: Cha-Cha-Cha, Samba, Rumba, Paso Doble, Jive
  - Sonstige: Discofox, Salsa
  - (Die genaue Liste unter „Sonstige“ ist meine Wahl und darf ergänzt werden.)
- Eine **Figur** gehört zu genau einem Tanz. (Meine Wahl: Ein „Hockeystick“ in Rumba und in Cha-Cha sind zwei getrennte Einträge, weil sich die Schritte unterscheiden.)
- Eine Figur besteht aus:
  - **Name** (Pflicht)
  - **Allgemein:** Freitext und Medien
  - **Leader:** Freitext und Medien
  - **Follower:** Freitext und Medien
- Ein **Medium** ist eines von: eigenes Video, Bild, Video aus der Galerie, YouTube-Link. Jedes Medium gehört genau zu einem Bereich (Allgemein, Leader oder Follower).

## Bildschirme

Grobentwürfe: https://claude.ai/artifact/WAZ7vnvjrCyrgLhxH4v22k (entschieden: **Variante B** für die Figur).

1. **Start:** Suchfeld oben. Darunter die Tänze, gruppiert nach Kategorie, jeweils mit der Zahl der Figuren. Ein Plus-Knopf legt einen Tanz oder eine Kategorie an. Über das Menü erreicht man Papierkorb und Export/Import.
2. **Tanz:** Liste der Figuren mit kurzem Hinweis auf die Medien („2 Videos“, „nur Text“). Mit dem Plus-Knopf legt man eine neue Figur an, nur der Name ist Pflicht. Die Reihenfolge entspricht dem Anlegen und lässt sich per Ziehen ändern.
3. **Figur (Lesen):** Eine Seite, auf der die drei Bereiche farbig getrennt **untereinander** stehen: Allgemein, Leader, Follower. Jeder zeigt Medien-Vorschauen und Text. **Leere Bereiche werden ausgeblendet.** Ein Tipp auf ein Video spielt es ab, ein Tipp auf einen YouTube-Link öffnet die YouTube-App bzw. den Browser.
4. **Figur (Bearbeiten):** Den Modus öffnet ein Stift oben rechts. Hier bearbeitet man Name und Texte und kann in jedem Bereich Medien hinzufügen oder entfernen. In diesem Modus erscheinen auch leere Bereiche, damit man sie füllen kann. (Meine Wahl: Lesen und Bearbeiten sind getrennt, damit man beim Nachschlagen nichts versehentlich ändert.)
5. **Suche:** Durchsucht Figuren- und Tanznamen (meine Wahl: auch die Beschreibungstexte).
6. **Papierkorb:** Zeigt gelöschte Figuren mit Löschdatum und verbleibenden Tagen. Man kann sie wiederherstellen oder endgültig löschen.

## Medien hinzufügen

- **Video aufnehmen** direkt aus der App: Die Kamera-App wird aufgerufen, und das Video wird **in der Galerie** gespeichert (z. B. im Album „Tanzlexikon“).
- **Aus Galerie wählen:** Bilder und Videos.
- **YouTube-Link einfügen:** Das Vorschaubild wird geladen, wenn Internet da ist. Ohne Internet erscheint ein Platzhalter.
- Die App speichert nur einen **Verweis** auf die Datei in der Galerie und kopiert sie nicht. Wurde die Datei außerhalb der App gelöscht, zeigt die App einen Hinweis „Datei nicht mehr gefunden“ statt abzustürzen.
- **Entschieden vom Autor:** Eigene Aufnahmen liegen in der normalen Galerie und **nicht** in einem app-eigenen Speicher, damit die übliche Fotosicherung sie mitsichert. Eine Kennzeichnung, die Dateien vor Funktionen wie „Speicherplatz freigeben“ in Google Fotos schützt, gibt es unter Android nicht. Der Ausgleich dafür:
  - **Neu verknüpfen:** Bei einem Medium mit „Datei nicht mehr gefunden“ kann man über den Foto-Auswahldialog die Datei neu wählen. Der Dialog zeigt unter Android 13+ auch Google-Fotos-Inhalte, die nur in der Cloud liegen, und lädt sie dabei herunter. Der Verweis zeigt danach auf die neue Datei.
  - (Meine Wahl) Auf der Startseite erscheint ein dezenter Hinweis, wenn Medien fehlen („3 Medien nicht gefunden“). Ein Tipp darauf listet die betroffenen Figuren.
- (Meine Wahl) Videoaufnahme über die System-Kamera-App ohne vorgegebenen Speicherort. Die Kamera speichert wie gewohnt in der Galerie, und die App merkt sich den zurückgegebenen Verweis. Ein eigenes Album „Tanzlexikon“ ist damit nicht garantiert. Das ist robuster, weil Kamera-Apps mancher Hersteller vorgegebene Speicherorte ignorieren.

## Löschen und Papierkorb

Der Grundsatz lautet: Nichts verschwindet durch einen einzigen Fehltipp.

- **Figuren** wandern beim Löschen in den **Papierkorb**. Eine Sicherheitsabfrage ist dafür nicht nötig. Nach **30 Tagen** werden sie endgültig entfernt. Vorher lassen sie sich wiederherstellen.
- **Tänze** lassen sich **nur löschen, wenn sie keine Figuren enthalten** (Figuren im Papierkorb zählen nicht mit). Dann ist auch keine Sicherheitsabfrage nötig. Ein leerer Tanz ist schnell neu angelegt.
- **Kategorien** lassen sich nur löschen, wenn sie keine Tänze enthalten. (Meine Wahl, analog zu den Tänzen.)
- **Wiederherstellen** einer Figur, deren Tanz (oder Kategorie) inzwischen gelöscht wurde: Tanz und gegebenenfalls Kategorie werden **automatisch mit dem alten Namen neu angelegt**. Gibt es schon einen Tanz mit dem Namen in derselben Kategorie, landet die Figur dort.
- **Mediendateien** in der Galerie werden von der App nie gelöscht, weder beim Entfernen eines Mediums noch beim endgültigen Löschen einer Figur. Weg ist nur der Verweis.
- Das Entfernen eines einzelnen Mediums oder das Leeren eines Textes im Bearbeitungsmodus läuft nicht über den Papierkorb. (Meine Wahl: Das gilt erst beim Speichern, und man kann die Bearbeitung abbrechen.)

## Sicherung

Drei Ebenen ergänzen sich:

1. **Videos und Bilder** liegen in der Galerie und werden über die normale Fotosicherung des Nutzers mitgesichert (z. B. Google Fotos).
2. **Texte und Struktur** (die Datenbank) sichert das **Android Auto-Backup** im Google-Konto. Die Datenbank ist klein, das Limit von 25 MB reicht.
3. **Export/Import:** Ein Knopf erzeugt eine ZIP-Datei mit der Datenbank **und den Mediendateien**, die man irgendwo ablegt (Drive, Rechner). Der Import auf einem neuen Handy stellt alles wieder her, auch wenn die Galerie dort anders aussieht.
   - (Meine Wahl) Der Import **ersetzt** den aktuellen Stand, er führt nichts zusammen. Damit dabei nichts verloren geht, legt die App vorher automatisch einen Export des aktuellen Stands an.
   - (Meine Wahl) Medien aus der ZIP kommen zurück **in die Galerie** (Ordner „Movies/Tanzlexikon“ bzw. „Pictures/Tanzlexikon“), und die Verweise zeigen auf diese neuen Einträge. Gibt es dort schon eine Datei mit gleichem Namen und gleicher Größe, wird sie verwendet statt dupliziert.
   - (Meine Wahl) Die ZIP kann mehrere GB groß werden. Export und Import laufen deshalb **gestreamt mit Fortschrittsanzeige**, und vorher prüft die App, ob genug Speicherplatz frei ist.

Beim Wiederherstellen über Auto-Backup oder auf einem neuen Gerät zeigen Verweise auf Galerie-Dateien eventuell ins Leere. Das gilt auch, wenn die Fotos über Google Fotos zurückgekommen sind, denn die Dateien haben dann neue IDs. Der Export/Import ist deshalb der verlässliche Weg für einen Umzug. Medien, die nicht gefunden werden, werden markiert (siehe oben).

**Später:** automatische Synchronisation mit einer Cloud, z. B. Google Drive.

## Technik (meine Wahl, nicht vom Autor entschieden)

- Kotlin und Jetpack Compose (Material 3)
- Lokale Datenbank: Room
- Medien-Verweise über Android-Content-URIs (MediaStore), mit dauerhaften Leseberechtigungen bei ausgewählten Dateien
- Gerät des Autors: **ein aktuelles Android-15-Handy**. Darauf wird getestet.
- (Meine Wahl) `targetSdk` 35 (Android 15), `minSdk` 26 (Android 8), damit andere die App später auch auf älteren Handys nutzen können. Die Foto-Auswahl läuft über den Android Photo Picker, der über Google Play bis in ältere Versionen zurückgereicht wird.
- Einfache Struktur: ein App-Modul, keine unnötigen Schichten

## Dem Builder überlassen

- Optik (Farben, Schrift, Icons). Die Grobentwürfe zeigen nur den Aufbau.
- Wie die Bearbeitungsmaske im Detail aussieht.
- Die Zeit- und Hintergrundsteuerung für das Leeren des Papierkorbs nach 30 Tagen (es reicht z. B., beim App-Start aufzuräumen).
- Das Format der Export-Datei.
- Tests: Die Logik für Papierkorb, Wiederherstellen und Lösch-Regeln sollte getestet sein.

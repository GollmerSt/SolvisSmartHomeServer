# SolvisSmartHomeServer
## Überblick

Dieses Programnm dient der Anbindung der SolvisMax 6 und 7 mit SolvisControl2 in Kombination der SolvisRemote an Smart-Home-Systeme wie z.B. Fhem, ioBroker, OpenHAB, Indigo.

Es eignet sich nicht für die neueren Anlagen der SolvisMax7 und SolvisBen, welche mit der neuen SolvisControl3 ausgeliefert werden. Bei diesen sollte man zur Smart-Home-System-Anbindung das Modbus-Interface nutzen.

Dieses Programm ist ein eigenständiges Java-Programm, das als Service/Task/Daemon im Hintergrund läuft. Ziel war nicht nur ein Monitoring der Anlage zu ermöglichen sondern auch die Einstellung der wichtigsten Anlageparameter wie z. B. Soll-Temperaturen, Raumabhängigkeit, Anlagemodus.

Die bisher mir bekannten Lösungen liefern nur die Messwerte/Zustände einer Solvis-Anlagen an die SmartHome-Systeme. Dabei wird ein XML-String interpretiert,
der über das WebInterface der SolvisRemote abgefragt wird. Der SolvisSmartHomeServer nutzt ebenfalls diese Schnittstelle.
Eine Steuerung der Anlage mit dieser Schnittstelle ist jedoch nicht möglich.

Erst bei neueren Anlagen (von der Solvis-Control-2-Zentralreglerversion MA205 an) hat die Firma Solvis die Steuermöglichkeit über das Modbus-Interface realisiert. Ältere Anlagen bleiben dabei außen vor.

Bei älteren Anlagen gibt es nur die Möglichkeit der Steuerung über die Web-Oberfläche der SolvisRemote, das jedoch auf rein grafischer Basis arbeitet
(es ist eine Pixelkopie der SolvisControl2 der Anlage). Die Steuerung darüber erfolgt über Maus-Klicks auf bestimmte Koordinaten auf diesem GUI,
die Rückmeldung erfolgt ebenfalls nur grafisch über das GUI.

Eine Liste mit der Kanäle findet sich u.a. bei der [MQTT-Beschreibung](Nhttps://github.com/GollmerSt/SolvisSmartHomeServer/wiki/MQTT-Schnittstelle#aktuell-definierte-channels).

### Funktionsweise der Einstellung der Anlageparameter

Dieses Projekt nutzt diesen Weg. Bei einer Sollwert-Änderung wird mit Hilfe eines OCRs zunächst der aktuelle Wert grafisch aus der Web-Oberfläche ermittelt. Anschließend wird der Wert entsprechend der Anforderung durch entsprechende simulierte Maus-Klicks geändert. Zum Abschluss wird diese Änderung durch das OCR verifiziert.

![Gui](https://raw.githubusercontent.com/GollmerSt/SolvisSmartHomeServer/master/docu/images/Hierarchie/1%20Heizung.png)

So wird bei einer Änderung der Feineinstellung die 0 in den eckigen Klammern im oben angezeigten "Home-Screen" mittels OCR erkannt und abhängig
vom einzustellenden Wert die Plus/Minus-Buttons auf der rechten Seite solange betätigt, bis der richtige Wert zwischen den eckigen Klammern
erscheint. Ist der zu ändernde Wert nicht auf dem "Home-Screen", so wird automatisch der entsprechende Screen angefahren. Das erfolgt z.B.
durch die Buttons auf der linken Seite.

### Features
* Auslesen der Messwerte der Sensoren
* Einstellung der Anlagenparameter wie Temperatur-Sollwerte, Raumabhängigkeiten etc.
* Monitoring der Solvis-Uhr mit entsprechender Nachjustierung.
* Erkennen eines Fehlerzustandes der Anlage. Im Fehlerfall kann optional eine Mail versendet werden.
Wird der Fehler als Fehlerscreen von der Anlage angezeigt, wird an die Mail die Hardcopy des Bildschirmes angehängt.
Hier ein Beispiel einer solchen Fehlermeldung:

   ![Fehlermeldung](https://raw.githubusercontent.com/GollmerSt/SolvisSmartHomeServer/master/testFiles/images/Stoerung%205.png)
* Es werden Anwender und Service-Zugriffe auf den Touchscreen der SolvisControl erkannt und nach beenden die möglicherweise
veränderten Anlagenparameter wieder erneut gelesen.
* Anbindung über MQTT, damit ist das System in SmartHome-Systeme ohne speziellen Client möglich.
Voraussetzung ist nur eine MQTT-Schnittstelle(-Erweiterung) im SmartHome-System sowie ein MQTT-Broker (z.B. Mosquitto). U.U.
bringt ein solcher Broker das Smart-Home-System selber mit.
* Zusätzliche Möglichkeit der Anbindung über eine Client-Server-Verbindung
  * Daten zwischen Server-Client werden im JSON-Format ausgetauscht
  * Es können sich max. 50 Clients mit dem Server verbinden
* Leichte Anpassungsmöglichkeit an vorhandene Anlage über XML-Files. Die XML Schema sind mit enthalten, so dass Anpassung
mittels XML-Editor (z.B. integriert in Eclipse) stark vereinfacht wird

### Voraussetzungen
* Solvis-Heizungsanlage (SolvisMax/SolvisBen) mit einer SolvisControl **2** und SolvisRemote. Bei einer SolvisControl **3** ist der
Einsatz nicht möglich, da das Web-Interface durch das Solvis-Portal ersetzt wurde. Zur Anbindung an ein Smart-Home-System
sollte man dort den Modbus verwenden.
* PC/Raspberry o.ä. mit Linux oder Windows, mit JRE >= 1.8.
* Ein Smart-Home-System wie FHEM, ioBroker, OpenHab o.ä. mit MQTT Schnittstelle.

### Interfaces zu Smart-Home-Systemen
Die Anbindung an das SmartHome-System werden durch zwei verschiedene Interfaces ermöglicht:

1. MQTT-Schnittstelle
1. Eine proprietäre Server-Client-Schnittstelle. Diese Schnittstelle erfordert auf der Smart-Home-System-Seite einen speziellen Client.
Ein solcher existiert bisher nur für das FHEM-SmartHome-System

Vom Funktionsumfang her gibt es zwischen beiden Interfaces keine Unterschiede. Anfangs gab es nur die proprietäre Server-Client-Schnittstelle.
Um bei SmartHome-Systemen nicht auf ein spezielles Modul angewiesen zu sein, wurde die MQTT-Schnittstelle zusätzlich implementiert. Für die
meisten SmartHome-System gibt es für die MQTT-Schnittstelle entsprechende Module.

#### Im Installationspaket sind für folgende SmartHome-Systeme Anpassungen enthalten:
Smart-Home-System | Beschreibung
------------------ | ------------
FHEM | Modul, das auf der Server-Client-Schnittstelle basiert.
ioBroker | Objektliste für das ioBroker-Modul MQTT Client

Weitere sind in Vorbereitung

### Installation
Die genauen Installationsanweisungen enthält die unten angehängte ausführliche Dokumentation. Hier daher nur ein grober Überblick.

Das Release beinhaltet normalerweise immer 2 unterschiedliche Installationspakete:
* Installationspaket für Linux mit einem Makefile, das der Installation dient
* Installationspaket für Windows mit einem Installer

**Vor** der Ausführung der Installationspakete ist es wichtig, dass bereits Java installiert ist, da die Installation selber schon Java-Programme
ausführen. Ich verwende unter Windows die Oracle-Java-Version 1.8, oder die OpenJDK ab 8, aktuell 14. Unter Linux habe ich bisher die Version
ab JDK 8 verwendet, vom Funktionsumfang müsste noch die Version 7 gehen.

Der Ablauf der Installation sollte folgendermaßen ablaufen:

1. Installation von Java
1. Herunterladen und Auspacken des Zip-File-Release in einem Ordner
1. Unter Windows Aufruf des Installers
1. Kopieren des base.xml.new unter dem Namen base.xml
1. Anpassen des base.xml (Solvis-IP, Passwörter, etc.), in einer zukünftigen Version werde ich hier einen Konfigurator noch zur Verfügung stellen
1. Aufruf des Learning (über Startmenü unter Windows oder über das Makefile unter Linux). Ein Learning ist notwendig, da die unterschiedlichen
Screens grafisch erkannt werden. Je nach eingestellter Sprache unterscheiden sich die Screens entsprechend. Auch die Erkennung der Symbole (Standby,
Nacht, Tag etc.) wird hierbei vom Screen gelesen und gemerkt.
1. Start des Servers

Die genaue Vorgehensweise muss man aus der folgenden Dokumentation entnommen werden:

## Ausführliche Dokumentation
Eine umfangreiche Dokumentation der Installation, Interfaces, Arbeitsweise etc. findet sich [in der Wiki](https://github.com/GollmerSt/SolvisSmartHomeServer/wiki).

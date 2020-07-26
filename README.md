# SolvisSmartHomeServer
## Überblick

Dieses Programm ist ein eigenständiges Java-Programm. Es dient der Anbindung der Solvis-Heizungsanlage an verschiedene SmartHome-Systeme, wie FHEM, IoBroker, OpenHab etc.. Ziel war nicht nur ein Monitoring
der Anlage sondern auch die Einstellung der wichtigsten Anlageparameter wie Soll-Temperaturen, Raumabhängigkeit, Anlagemodus usw. zu ermöglichen.

Bisher mir bekannte Lösungen liefern nur die Messwerten/Zuständen der Solvis-Anlagen an die SmartHome-Systeme.
Eine Steuerung der Anlage mit diesen Methoden ist bisher m.W. nicht möglich gewesen.

Erst bei neueren Anlagen hat die Firma Solvis die Steuermöglichkeit über das Modbus-Interface realisiert. Ältere Anlagen bleiben dabei außen vor.

Bei älteren Anlagen gibt es nur die Möglichkeit der Steuerung über die Web-Oberfläche der SolvisRemote, das jedoch auf rein grafischer Basis arbeitet
(es ist eine Pixelkopie der SolvisControl an der Anlage). Die Steuerung darüber erfolgt über Maus-Klicks auf bestimmte Koordinaten auf diesem GUI,
die Rückmeldung erfolgt ebenfalls nur grafisch über das GUI.

### Funktionsweise der Einstellung der Anlageparameter

Dieses Projekt nutzt diesen Weg mittels eines OCRs um die aktuellen Werte grafisch aus der Web-Oberfläche zu ermitteln
und entsprechend dem angeforderten Wert mittels simulierter Maus-Clicks auf die entsprechenden Koordinaten zu änden.

![Gui](https://raw.githubusercontent.com/GollmerSt/SolvisSmartHomeServer/master/docu/images/Hierarchie/1%20Heizung.png)

So wird bei einer Änderung der Feineinstellung die 0 in den eckigen Klammern im oben angezeigten "Home-Screen" erkannt und abhängig vom einzustellenden Wert
die Plus/Minus-Buttons auf der rechten Seite solange betätigt, bis der richtige Wert zwischen den eckigen Klammern erscheint. Ist der zu ändernde Wert nicht
auf dem "Home-Screen", so wird automatisch der entsprechende Screen angefahren. Das erfolgt z.B. durch die Buttons auf der linken Seite.

### Features
* Auslesen der Messwerte der Sensoren
* Einstellung der Anlagenparameter wie Temperatur-Sollwerte, Raumabhängigkeiten etc.
* Monitoring der Solvis-Uhr und mit entsprechender Nachjustierung.
* Erkennen eines Fehlerzustandes der Anlage. Im Fehlerfall kann optional einen Mail versendet werden.
Wird der Fehler als Fehlerscreen von der Anlage angezeigt, wird an die Mail die Hardcopy des Bildschirmes angehängt.
* Es werden Anwender und Service-Zugriffe auf den Touchscreen der SolvisControl erkannt und nach beenden die möglicherweise
veränderten Anlagenparameter wieder erneut gelesen.
* Anbindung über MQTT, damit ist das System in SmartHome-Systeme ohne speziellen Client möglich.
Voraussetzung ist nur eine MQTT-Schnittstelle(-Erweiterung) im SmartHome-System.
* Zusätzliche Anbindung über eine Client-Server-Verbindung, dadurch leichte Anpassung an andere Smarthome-System
* Daten zwischen Server-Client werden im JSON-Format ausgetauscht
* Es können sich max. 50 Clients mit dem Server verbinden
* Leichte Anpassungsmöglichkeit an vorhandene Anlage über XML-Files. Die XML Schema sind mit enthalten, so dass Anpassung mittels XML-Editor (z.B. integriert in Eclipse) stark vereinfacht wird

### Voraussetzungen
* Solvis-Heizungsanlage (SolvisMax/SolvisBen) mit einer SolvisControl2 und SolvisRemote.
* PC/Raspberry o.ä. mit Linux oder Windows, mit JRE >= 1.8.

### Interfaces zu SmartHomeSystemen
Die Anbindung an das SmartHome-System werden durch zwei verschiedene Interfaces ermöglicht:

1. MQTT-Schnittstelle
1. Eine proprietäre Server-Client-Schnittstelle. Diese Schnittstelle erfordert auf der SmartHomeSystem-Seite einen speziellen Client.
Ein solcher existiert bisher nur für das FHEM-SmartHome-System

Vom Funktionsumfang gibt es zwischen beiden Interfaces keinen Unterschied. Anfangs gab es nur die propritäre Server-Client-Schnittstelle.
Um bei SmartHome-Systemen nicht auf ein spezielles Modul angewiesen zu sein, wurde die MQTT-Schnittstelle zusätzlich implementiert. Für die
meisten SmartHome-System gibt es für die MQTT-Schnittstelle entsprechende Module.

Im Installationspaket enthalten sind Anpassungen an die SmartHomeSysteme FHEM und IoBroker. Für FHEM existiert ein Client-Modul, für IOBroker existiert
einen Objektliste, die die notwendigen Objekte definiert, wenn man den IoBroker-MQTT-Client nutzt.

## Ausführliche Dokumentation
Eine umfangreiche Dokumentation der Installion, Interfaces, Arbeitsweise etc. findet sich hier:
[SolvisSmartHomeServer.pdf](https://raw.githubusercontent.com/GollmerSt/SolvisSmartHomeServer/master/docu/SolvisSmartHomeServer.pdf)

## License
This program is licensed under GPL-3.0

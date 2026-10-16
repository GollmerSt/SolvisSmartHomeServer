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
bringt ein Smart-Home-System einen solchen Broker das selber mit.
* Zusätzliche Möglichkeit der Anbindung über eine Client-Server-Verbindung
  * Daten zwischen Server-Client werden im JSON-Format ausgetauscht
  * Es können sich max. 50 Clients mit dem Server verbinden
* Leichte Anpassungsmöglichkeit an vorhandene Anlage über XML-Files. Die XML Schema sind mit enthalten, so dass Anpassung
mittels XML-Editor (z.B. integriert in Eclipse) stark vereinfacht wird. Ab Version 1.03.00 sind für SolvisMax 6 und 7 die
laut der Anlagenschema-Dokumentation von Solvis möglichen Konfigurationen berücksichtigt.

Eine Liste der Kanäle findet sich u.a. im Kapitel [MQTT-Schnittstelle](https://github.com/GollmerSt/SolvisSmartHomeServer/wiki/MQTT-Schnittstelle#aktuell-definierte-channels) bzw. kann ab Version 1.03.00 vom Programm selber passend zur Anlagenkonfiguartion generiert werden. Dies ist [hier](https://github.com/GollmerSt/SolvisSmartHomeServer/wiki/Dokumentation-der-Kanalbelegung) beschrieben.

### Voraussetzungen
* Solvis-Heizungsanlage (SolvisMax6/7) mit einer SolvisControl **2** und SolvisRemote. Bei einer SolvisControl **3** ist der
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

#### Es sind für folgende SmartHome-Systeme Anpassungen vorhanden:
Smart-Home-System | Beschreibung
------------------ | ------------
FHEM | Modul, das auf der Server-Client-Schnittstelle basiert (Teil des Progarmmpaketes).
ioBroker | Objektliste mit Pairing-Script für das ioBroker-Modul MQTT Client (Wird vom Programm konfigurationsspezifisch generiert).
Home Assistant | Automatische MQTT Discovery (siehe unten).

#### Home Assistant – MQTT Auto-Discovery

Die Integration mit [Home Assistant](https://www.home-assistant.io/) erfolgt über den eingebauten **MQTT Auto-Discovery**-Mechanismus.
Beim ersten Verbindungsaufbau zum MQTT-Broker veröffentlicht der Server für jeden Solvis-Kanal eine Konfigurationsnachricht auf dem
Discovery-Topic. Home Assistant erkennt daraufhin alle Entities automatisch, ohne dass eine manuelle Konfiguration erforderlich ist.

##### Aktivierung

In der `base.xml` das Attribut `haDiscoveryPrefix` zum `Mqtt`-Element hinzufügen:

```xml
<tns:Mqtt enable="true"
    brokerUrl="192.168.0.x" port="1883"
    subscribeQoS="1" publishQoS="1"
    idPrefix="SolvisSmartHomeServer"
    topicPrefix="SolvisSmartHomeServer"
    smartHomeId="HomeAssistant"
    haDiscoveryPrefix="homeassistant" />
```

Der Wert `homeassistant` entspricht dem Standard-Discovery-Prefix von Home Assistant und muss nur geändert werden,
wenn dieser in der Home-Assistant-Konfiguration abweichend eingestellt wurde.

##### Voraussetzung

In Home Assistant muss die [MQTT-Integration](https://www.home-assistant.io/integrations/mqtt/) eingerichtet und
mit demselben Broker verbunden sein.

##### Erzeugte Entities

Für jeden Solvis-Kanal wird automatisch der passende Entity-Typ gewählt:

Entity-Typ | Kanäle
---------- | ------
`sensor` | Alle Messwerte (Temperaturen, Laufzeiten, Zähler, …)
`number` | Schreibbare Zahlenwerte (Solltemperaturen, Differenzen, …)
`select` | Betriebsmodi (z.B. Automatik / Tag / Nacht / Standby)
`binary_sensor` | Binäre Zustände (nur lesen)
`switch` | Schreibbare Binärkanäle

Nicht angelegt werden Kanäle ohne Belegung in der aktuellen Anlagenkonfiguration (z.B. `I-2`, `O-4`) sowie
alle Kanäle der SolvisControl-Oberfläche (`C*`), solange das Feature `InteractiveGUIAccess` nicht aktiv ist,
weil der Server sie dann nicht liest.

Die Konfigurationsnachrichten enthalten zusätzlich:

* **Namen** aus `ha-labels.properties` (Begriffe des Solvis-Anschlussplans und der SolvisControl-Bildschirme,
  z.B. `S01` → „Oben“ im Gerät „Speicher“, `C08` → „Absenktemperatur“ im Gerät „Heizkreis 1“). Ohne Eintrag wird der
  Name aus dem Kanalnamen abgeleitet.
* **`unique_id` und `object_id`** aus Unit und Kanal-Id (`sensor.solvis_<unitId>_s01`). Ein Umbenennen des Kanals in der
  `base.xml` ändert damit weder die Entity-Id noch geht die Historie verloren.
* **`entity_category`** — `diagnostic` für Anlagentyp, Systemnummer, Uhrzeit und die Server-Status-Entities, `config` für
  die Installateur-Parameter (`C31`–`C49`).
* **`device_class`** — aus der Einheit abgeleitet (`temperature` für °C, `duration` für s/h, `volume_flow_rate` für l/h,
  `power` für W/kW, `energy` für Wh/kWh, `pressure` für bar, …). Differenzen in K erhalten bewusst keine Klasse, da
  Home Assistant sie sonst als absolute Temperatur umrechnen würde.
* **`state_class`** — `total_increasing` für Zähler, Laufzeiten und Wärmemengen, sonst `measurement`
* **`suggested_display_precision`** — Anzahl Nachkommastellen, abgeleitet aus der konfigurierten Messgenauigkeit des Kanals
* **`value_template`** bei Temperaturfühlern: die Solvis-Werte für offenen bzw. kurzgeschlossenen Fühler (≥ 250 °C, ≤ −35 °C)
  werden als „unbekannt“ gemeldet statt als Messwert.
* **`configuration_url`** — URL der Solvis-Anlage, ermöglicht direkten Aufruf aus der HA-Geräteseite

**Geräte:** Die Entities einer Unit werden entsprechend dem Anlagenschema der SolvisRemote auf mehrere Home-Assistant-Geräte
verteilt, die alle mit dem Hauptgerät „Solvis &lt;unitId&gt;“ verknüpft sind (`via_device`):

Gerät | Inhalt
--- | ---
`Solvis <unitId>` | Außentemperatur, Anlagentyp, Systemnummer, Uhrzeit, Server-Status
`<unitId> Speicher` | Speicherfühler S1, S3, S4, S9, Pufferparameter
`<unitId> Solaranlage` | Kollektor, Vor-/Rücklauf, Druck, Durchfluss, Solarpumpen, Ventile, Wärmemenge, Laufzeiten
`<unitId> Warmwasser` | Warmwasser- und Zirkulationsfühler, Pumpen, Solltemperatur, Nachheizen
`<unitId> Heizkreis 1` … `3` | Vorlauf, Raumfühler, Pumpe, Mischer, Betriebsart, Temperaturen, Heizkurve
`<unitId> Wärmeerzeuger` | Kessel/Brenner, Pelletkessel, Wärmepumpe, Fernwärme: Fühler, Anforderung, Ladepumpe, Störung, Laufzeiten, Starts

Die Zuordnung steht in `ha-groups.properties` und ist kanalgenau, d.h. sie berücksichtigt die Belegung des
jeweiligen Anlagentyps (`S09.Heissgas` gehört zur Wärmepumpe, `S09.Heizungspuffertemperatur_unten` zum Speicher;
`A07.Pumpe_Solar2` zur Solaranlage, `A07.Ladepumpe` zum Wärmeerzeuger). Entity-Ids und `unique_id`s sind von der
Gruppierung unabhängig.

**Anpassung pro Anlage:** Liegt im Datenverzeichnis des Servers (neben `graficData.xml`, in `base.xml` als
`writablePathLinux`/`writeablePathWindows` konfiguriert) eine Datei `ha-groups.properties` oder `ha-labels.properties`,
werden deren Einträge zusätzlich zu den eingebauten geladen und überschreiben diese. Damit lassen sich Namen ändern,
Kanäle anderen Gruppen zuordnen (`S09=waermeerzeuger`), neue Gruppen anlegen (`device.keller=Keller`) oder
Kanäle wieder dem Hauptgerät zuweisen (leerer Wert, `S14=`). Die Dateien sind UTF-8-kodiert; Format siehe die
eingebauten Dateien unter `rsc/de/sgollmer/solvismax/data/`. Änderungen wirken nach einem Neustart des Servers.

Nach dem Verbindungsaufbau löscht der Server verwaiste Discovery-Nachrichten früherer Läufe (umbenannte Unit,
entfallene Kanäle, alte `unique_id`s), damit in Home Assistant keine Geister-Entities zurückbleiben.
Home Assistant verschiebt eine bestehende Entity nicht auf ein anderes Gerät, wenn sich nur die Discovery-Nachricht
ändert. Stellt der Server beim Start fest, dass eine Entity laut der retained Nachricht des vorherigen Laufs zu einem
anderen Gerät gehörte, entfernt er sie kurz und legt sie neu an; Entity-Id und Historie bleiben erhalten, manuell
vergebene Bereiche oder Namen der Entity gehen dabei verloren. Hängen Entities trotzdem noch am alten Gerät
(z.B. nach einem Update, bei dem der Broker die neuen Nachrichten schon kannte), genügt es, das Gerät in Home Assistant
zu löschen: die Entities werden aus den retained Discovery-Nachrichten sofort wieder angelegt, jetzt auf den neuen Geräten.

Zusätzlich werden folgende Statusinformationen als Entities angelegt:

Entity | Beschreibung
------ | ------------
`sensor` Verbindungsstatus | Verbindungszustand zur Anlage (`SOLVIS_CONNECTED`, `POWER_OFF`, …)
`sensor` GUI-Status | Aktueller GUI-Zugriff (`CONTROL_WRITE_ONGOING`, `CONTROL_FINISHED`, …)
`sensor` Anlagenzugriff | Manueller Zugriff an der Anlage (`USER_ACCESS_DETECTED`, …)
`binary_sensor` Schreibzugriff aktiviert | `true` wenn GUI-Steuerung aktiv und nicht blockiert ist

##### Schreibzugriff

Schreibbare Entities (Sollwerte, Modi) senden Befehle über MQTT zurück an den Server, der sie anschließend über
die GUI-Steuerung (OCR + simulierte Klicks) in der SolvisControl umsetzt. Dafür muss in der `base.xml` das Feature
`InteractiveGUIAccess` aktiviert sein:

```xml
<tns:Feature id="InteractiveGUIAccess" value="true" />
```

Ob der Schreibzugriff aktuell möglich ist, zeigt der Binary Sensor **„Schreibzugriff aktiviert"**.

### Anlagenkonfiguration

#### Pelletkessel SolvisLino

Für eine SolvisMax mit Pelletkessel SolvisLino gibt es in der `base.xml` die Hauptheizung `mainHeating="Pellet"`.
Sie entspricht `Extern` (Kesselfühler S14, Anforderung über O-1), bildet aber die Lino-Belegung des Anschlussplans ab:
A12 ist die interne Brenneranforderung der SolvisControl, A14 bleibt unbelegt, und die Störmeldung des Kessels wird als
`binary_sensor` „Störung Pelletkessel“ angelegt (I-3 bei `type="SolvisMax6"`, also Futur; I-1 bei `SolvisMax6PurSolo`, also Solo).

#### Heizkreise ohne Raumfühler

Hat ein Heizkreis keinen Raumfühler (z.B. weil Raumthermostate direkt am Heizkreisverteiler regeln), liefert der
Fühlereingang dauerhaft 0,0 °C und der Parameter „Raumeinfluss“ ist bedeutungslos. Mit den Extensions
`OhneRaumfuehler_HK1`, `OhneRaumfuehler_HK2` und `OhneRaumfuehler_HK3` in der `base.xml` entfallen Raumfühler und
Raumeinfluss des jeweiligen Heizkreises vollständig (kein MQTT-Kanal, keine Entity):

```xml
<tns:Extensions>
    <tns:Extension id="OhneRaumfuehler_HK1" />
    <tns:Extension id="OhneRaumfuehler_HK2" />
</tns:Extensions>
```

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

package de.sgollmer.solvismax.model.transfer;

/**
 * 
 * @author stefa_000
 *
 */

public enum Command {
	/**
	 * Client: Erster Request
	 */
	CONNECT,
	/**
	 * Client: Request nach Unterbrechung der Verbindung
	 */
	RECONNECT,
	/**
	 * Client: Verbindungstrennung (z.B. beenden des Programms)
	 */
	DISCONNECT,
	/**
	 * Client: Verbindungstrennung (z.B. beenden des Programms)
	 */
	SHUTDOWN,
	/**
	 * Client: Das Java-Programm wird beendet, wenn nur noch dieser eine Client
	 * verbunden ist, sonst wie DISCONNECT
	 */
	SET,
	/**
	 * Server: Rückmeldung erste Kontaktierung, mit Client-Id
	 */
	CONNECTED,
	/**
	 * Server: Komplette Datenbeschreibung
	 */
	DATA_DESCRIPTIONS,
	/**
	 * Server: Veränderte Daten, beim Beginn der Übertragung alle Daten
	 */
	MEASUREMENTS,
	/**
	 * Server: Liefert den Verbindungs-Zustand der Anlage, z.B. Connected,
	 * Poweroff
	 */
	STATE, // ...
	CONNECTION_STATE
	
}

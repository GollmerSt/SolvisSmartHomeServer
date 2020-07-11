/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

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
	 * Triggert das Auslesen eines Parameters an, welches nur �ber das GUI
	 * zug�nglich ist Daten werden erst �ber
	 */
	GET,
	/**
	 * Client: Das Java-Programm wird beendet, wenn nur noch dieser eine Client
	 * verbunden ist, sonst wie DISCONNECT
	 */
	SET,
	/**
	 * Server: R�ckmeldung erste Kontaktierung, mit Client-Id
	 */
	CONNECTED,
	/**
	 * Server: Komplette Datenbeschreibung
	 */
	DESCRIPTIONS,
	/**
	 * Server: Ver�nderte Daten, beim Beginn der �bertragung alle Daten
	 */
	MEASUREMENTS,
	/**
	 * Server: Liefert den Verbindungs-Zustand der Client/Server-Verbindung
	 */
	CONNECTION_STATE,
	/**
	 * Server: Liefert den Zustand der Solvis-Anlage
	 */
	SOLVIS_STATE,
	/**
	 * Client: Startet bestimmte Server-Befehle (Backup etc.)
	 */
	SERVER_COMMAND,
}

/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.io.IOException;
import java.io.InputStream;

import de.sgollmer.solvismax.error.JsonException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;

public class ReceivedPackageCreator {

	private static final ILogger logger = LogManager.getInstance().getLogger(ReceivedPackageCreator.class);

	public static ReceivedPackageCreator getInstance() {
		return JsonPackageCreatorHolder.INSTANCE;
	}

	private static class JsonPackageCreatorHolder {

		private static final ReceivedPackageCreator INSTANCE = new ReceivedPackageCreator();
	}

	private JsonPackage toSpecificPackage(final JsonPackage jsonPackage) {
		Command command = jsonPackage.command;

		JsonPackage result = null;

		switch (command) {
			case CONNECT:
				result = new ConnectPackage();
				break;
			case CONNECTED:
				result = new ConnectedPackage();
				break;
			case RECONNECT:
				result = new ReconnectPackage();
				break;
			case DISCONNECT:
				result = new DisconnectPackage();
				break;
			case SET:
				result = new SetPackage();
				break;
			case GET:
				result = new GetPackage();
				break;
			case SERVER_COMMAND:
				result = new ServerCommandPackage();
				break;
			case SELECT_SCREEN:
				result = new SelectScreenPackage();
				break;
			default:
				logger.error("Command <" + command.name() + "> not known");
				return null;
		}
		result.data = jsonPackage.data;
		return result;
	}

	public JsonPackage receive(final InputStream stream, final int timeout) throws IOException, JsonException {
		JsonPackage receivedPackage = new JsonPackage();
		receivedPackage.receive(stream, timeout);
		JsonPackage result = this.toSpecificPackage(receivedPackage);
		result.finish();

		return result;
	}
}

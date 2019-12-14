package de.sgollmer.solvismax.connection.transfer;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.error.JsonError;

public class ReceivedPackageCreator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ReceivedPackageCreator.class);

    public static ReceivedPackageCreator getInstance() {
		return JsonPackageCreatorHolder.INSTANCE;
	}

	private static class JsonPackageCreatorHolder {

		private static final ReceivedPackageCreator INSTANCE = new ReceivedPackageCreator();
	}

	@SuppressWarnings("static-method")
	private JsonPackage toSpecificPackage(JsonPackage jsonPackage) {
		Command command = jsonPackage.command;

		JsonPackage result = null;

		switch (command) {
			case CONNECT:
				result = new ConnectPackage();
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
			default:
				logger.error("Command <" + command.name() + "> not known");
				return null;
		}
		result.data = jsonPackage.data;
		return result;
	}

	public JsonPackage receive(InputStream stream) throws IOException, JsonError {
		JsonPackage receivedPackage = new JsonPackage();
		receivedPackage.receive(stream);
		JsonPackage result = this.toSpecificPackage(receivedPackage);
		result.finish();

		return result;
	}
}

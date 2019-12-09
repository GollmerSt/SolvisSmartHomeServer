package de.sgollmer.solvismax.model.transfer;

import java.io.IOException;
import java.io.InputStream;

import de.sgollmer.solvismax.error.JsonError;

public class ReceivedPackageCreator {

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
			default:
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

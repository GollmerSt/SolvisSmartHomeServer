package de.sgollmer.solvismax.model.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import de.sgollmer.solvismax.error.JsonError;

public class JsonPackage {

	public static final Charset CHARSET = Charset.forName("UTF-8");

	protected Command command;
	protected Frame data;

	public JsonPackage() {
	}
	
	public JsonPackage( Command command, Frame frame ) {
		this.command = command  ;
		this.data = frame ;
	}

	public Frame getFrame() {
		Frame result = new Frame();
		Element element = new Element();
		element.name = command.toString();
		element.value = data;
		result.add(element);
		return result;
	}

	private byte[] createSendData() {

		Frame send = this.getFrame();

		StringBuilder builder = new StringBuilder();

		send.addTo(builder);

		return builder.toString().getBytes(CHARSET);

	}

	public void send(OutputStream stream) throws IOException {
		byte[] sendData = this.createSendData();
		int length = sendData.length;
		byte[] lengthBytes = new byte[3];
		lengthBytes[0] = (byte) (length & 0xff);
		lengthBytes[1] = (byte) (length >> 8 & 0xff);
		lengthBytes[2] = (byte) (length >> 16 & 0xff);
		stream.write(lengthBytes);
		stream.write(sendData);
		stream.flush();
	}

	public void receive(InputStream stream) throws IOException, JsonError {
		byte[] lengthBytes = new byte[3];
		stream.read(lengthBytes);
		int length = lengthBytes[2] << 16 | lengthBytes[1] << 8 | lengthBytes[0];
		byte[] receivedData = new byte[length];
		stream.read(receivedData);
		Frame receivedFrame = new Frame();
		String receivedString = new String(receivedData, CHARSET);
		receivedFrame.from(receivedString, 0);
		if (receivedFrame.size() > 0) {
			Element e = receivedFrame.get(0);
			this.command = Command.valueOf(e.name);
			this.data = (Frame) e.value;
		}
	}

	public void finish() {

	}

	public Command getCommand() {
		return command;
	}
}

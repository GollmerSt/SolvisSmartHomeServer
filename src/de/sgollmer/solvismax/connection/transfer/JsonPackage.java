/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import de.sgollmer.solvismax.connection.ITransferedData;
import de.sgollmer.solvismax.error.JsonError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class JsonPackage implements ITransferedData {

	private static final Charset CHARSET = Charset.forName("UTF-8");

	protected Command command;
	protected Frame data;
	private Solvis solvis = null;

	public JsonPackage() {
	}

	JsonPackage(Command command, Frame frame) {
		this.command = command;
		this.data = frame;
	}

	private Frame getFrame() {
		Frame result = new Frame();
		Element element = new Element();
		element.name = this.command.toString();
		element.value = this.data;
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
		lengthBytes[0] = (byte) (length >> 16 & 0xff);
		lengthBytes[1] = (byte) (length >> 8 & 0xff);
		lengthBytes[2] = (byte) (length & 0xff);
		stream.write(lengthBytes);
		stream.write(sendData);
		stream.flush();
	}

	void receive(InputStream stream, int timeout) throws IOException, JsonError {
		byte[] lengthBytes = new byte[3];
		Helper.read(stream, lengthBytes, timeout);
		int length = lengthBytes[0] << 16 | lengthBytes[1] << 8 | lengthBytes[2];

		byte[] receivedData = new byte[length];
		Helper.read(stream, receivedData, timeout);

		Frame receivedFrame = new Frame();
		String receivedString = new String(receivedData, CHARSET);
		receivedFrame.from(receivedString, 0);
		if (receivedFrame.size() > 0) {
			Element e = receivedFrame.get(0);
			this.command = Command.valueOf(e.name);
			if (e.value instanceof SingleValue) {
				this.data = null;
			} else {
				this.data = (Frame) e.value;
			}
		}
	}

	void finish() {

	}

	@Override
	public Command getCommand() {
		return this.command;
	}

	@Override
	public void setSolvis(Solvis solvis) {
		this.solvis = solvis;
	}

	@Override
	public Solvis getSolvis() {
		return this.solvis;
	}

	@Override
	public String getChannelId() {
		return null;
	}

	@Override
	public SingleData<?> getSingleData() {
		return null;
	}

	@Override
	public String getClientId() {
		return null;
	}
}

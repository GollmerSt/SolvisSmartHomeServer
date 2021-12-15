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

import de.sgollmer.solvismax.connection.IReceivedData;
import de.sgollmer.solvismax.error.JsonException;
import de.sgollmer.solvismax.error.PackageException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class JsonPackage implements IReceivedData {

	private static final Charset CHARSET = Charset.forName("UTF-8");

	protected Command command;
	protected Frame data;
	private Solvis solvis = null;
	private String receivedString = null;

	public JsonPackage() {
	}

	JsonPackage(final Command command, final Frame frame) {
		this.command = command;
		this.data = frame;
	}

	private Frame getFrame() {
		Frame result = new Frame();
		Element element = new Element(this.command.toString(), this.data);
		result.add(element);
		return result;
	}

	private byte[] createSendData() {

		Frame send = this.getFrame();

		StringBuilder builder = new StringBuilder();

		send.addTo(builder);

		return builder.toString().getBytes(CHARSET);

	}

	public void send(final OutputStream stream) throws IOException {
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

	void receive(final InputStream stream, final int timeout) throws IOException, JsonException {
		byte[] lengthBytes = new byte[3];
		Helper.read(stream, lengthBytes, timeout);
		int length = ((int) lengthBytes[0] & 0xff) << 16 | ((int) lengthBytes[1] & 0xff) << 8
				| ((int) lengthBytes[2] & 0xff);

		byte[] receivedData = new byte[length];
		Helper.read(stream, receivedData, timeout);

		Frame receivedFrame = new Frame();
		this.receivedString = new String(receivedData, CHARSET);
		long timeStamp = System.currentTimeMillis();
		receivedFrame.from(this.receivedString, 0, timeStamp);
		if (receivedFrame.size() > 0) {
			Element e = receivedFrame.get(0);
			this.command = Command.valueOf(e.name);
			this.data = e.getValue().getFrame();
		}
	}

	void finish() throws TypeException, PackageException {

	}

	@Override
	public Command getCommand() {
		return this.command;
	}

	@Override
	public void setSolvis(final Solvis solvis) {
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

	protected String getReceivedString() {
		return this.receivedString;
	}
}

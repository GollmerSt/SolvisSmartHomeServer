package de.sgollmer.solvismax.smarthome;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.mqtt.TopicType;
import de.sgollmer.solvismax.connection.mqtt.TopicType.TopicData;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.unit.Unit;

public class Csv {

	private static String HEADER1 = "+-------------------------------------------------------------------------------+"
			+ Constants.CRLF;
	private static String HEADER2 = "|                                                                               |"
			+ Constants.CRLF;

	private final boolean semicolon;

	private Writer writer = null;
	private final File directory;
	private File file = null;
	private final String name;

	public Csv(final boolean semicolon, final File directory, final String name) {
		this.semicolon = semicolon;
		this.directory = directory;
		this.name = name;
	}

	public void open() throws FileNotFoundException {

		FileHelper.mkdir(this.directory);
		this.file = new File(this.directory, this.name);

		try {
			this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.file), "Cp1252"));
		} catch (UnsupportedEncodingException e) {
		}
	}

	public void close() throws IOException {
		this.writer.flush();
		this.writer.close();

		System.out.println("File <" + this.file.getAbsolutePath() + "> created.");
	}

	private String insertInTheMiddle(final String insert) {
		if (insert == null) {
			return HEADER2;
		}

		int textLength = insert.length();
		int maxLength = HEADER2.length() - 4;

		String text;

		if (textLength > maxLength) {
			text = insert.substring(0, maxLength);
			textLength = text.length();
		} else {
			text = insert;
		}

		StringBuilder middle = new StringBuilder();
		middle.append(HEADER2, 0, (HEADER2.length() - textLength) / 2);
		middle.append(text);
		middle.append(HEADER2, middle.length(), HEADER2.length());
		return middle.toString();
	}

	public void outCommentHeader(final Unit unit, final long mask, final String comment) throws IOException {

		if (this.writer == null) {
			throw new IOException("Write file not opened.");
		}

		this.writer.write(HEADER1);
		this.writer.write(HEADER2);

		this.writer.write(insertInTheMiddle("Unit id: " + unit.getId()));
		this.writer.write(insertInTheMiddle("Mask: 0x" + String.format("%016x", mask)));
		this.writer.write(insertInTheMiddle("Admin:" + Boolean.toString(unit.isAdmin())));
		if (comment != null) {
			this.writer.write(insertInTheMiddle(comment));
		}

		this.writer.write(HEADER2);
		this.writer.write(HEADER1);
		this.writer.write(Constants.CRLF);

	}

	public void out(final Solvis solvis, final String[] header) throws IOException {

		StringBuilder line = new StringBuilder();
		for (String colName : header) {
			if (line.length() != 0) {
				line.append(this.semicolon ? ';' : ',');
			}
			line.append(colName);
		}
		this.writer.write(line + Constants.CRLF);
		this.writer.write(Constants.CRLF);

		List<SolvisData> list = new ArrayList<>(solvis.getAllSolvisData().getSolvisDatas());

		list.sort(new Comparator<SolvisData>() {

			@Override
			public int compare(SolvisData o1, SolvisData o2) {
				String id1 = o1.getId();
				String id2 = o2.getId();
				if (id1 != null) {
					return id1.compareTo(id2);
				} else if (id2 != null) {
					return -1;
				} else {
					return 0;
				}
			}
		});

		for (SolvisData data : list) {
			if (data == null) {
				continue;
			}
			line = new StringBuilder();
			for (String colName : header) {
				if (line.length() != 0) {
					line.append(this.semicolon ? ';' : ',');
				}
				String cell = data.getCsvMeta(colName, this.semicolon);
				if (cell != null) {
					line.append(cell);
				}
			}
			line.append(Constants.CRLF);
			this.writer.write(line.toString());
		}
		this.writer.write(Constants.CRLF);
		this.writer.write(Constants.CRLF);
	}

	public void screensOut(final Solvis solvis) throws IOException {
		this.writer.append(HEADER1);
		this.writer.append(HEADER2);
		this.writer.write(insertInTheMiddle("Screens"));
		this.writer.append(HEADER2);
		this.writer.append(HEADER1);
		this.writer.write(Constants.CRLF);
		this.writer.append("screenId;");
		this.writer.write(Constants.CRLF);
		this.writer.write(Constants.CRLF);

		List<Screen> screens = new ArrayList<>();

		for (AbstractScreen screen : solvis.getSolvisDescription().getScreens().getScreens(solvis)) {
			if (screen instanceof Screen) {
				screens.add((Screen) screen);
			}
		}

		screens.sort(new Comparator<Screen>() {

			@Override
			public int compare(Screen o1, Screen o2) {
				return o1.getSortId().compareTo(o2.getSortId());
			}
		});

		for (Screen screen : screens) {
			this.writer.append(screen.getId());
			this.writer.append(';');
			this.writer.append(Constants.CRLF);

		}

		this.writer.write(Constants.CRLF);
		this.writer.write(Constants.CRLF);

	}

	public void mqttTopicsOut(final Instances instances) throws IOException {
		this.writer.append(HEADER1);
		this.writer.append(HEADER2);
		this.writer.write(insertInTheMiddle("Mqtt-Topics"));
		this.writer.append(HEADER2);
		this.writer.append(HEADER1);
		this.writer.write(Constants.CRLF);
		this.writer.append("direction;topic;comment;");
		this.writer.write(Constants.CRLF);
		this.writer.write(Constants.CRLF);

		Collection<TopicData> topics = TopicType.getTopicDatas(instances);

		for (TopicData topic : topics) {
			if (topic.isPublish()) {
				this.writer.append("publish");
			} else {
				this.writer.append("subscribe");
			}
			this.writer.append(';');
			this.writer.append(topic.getTopic());
			this.writer.append(';');
			this.writer.append(topic.getComment());
			this.writer.append(';');
			this.writer.append(Constants.CRLF);

		}

		this.writer.write(Constants.CRLF);
		this.writer.write(Constants.CRLF);

	}

}

package de.sgollmer.solvismax.model.objects.csv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.unit.Unit;

public class Csv {

	private static String HEADER1 = "+-------------------------------------------------------------------------------+";
	private static String HEADER2 = "|                                                                               |";

	private final boolean semicolon;

	public Csv(final boolean semicolon) {
		this.semicolon = semicolon;
	}

	private String insertInTheMiddle(String text) {
		if (text == null) {
			return HEADER2;
		}

		int textLength = text.length();
		int maxLength = HEADER2.length() - 4;

		if (textLength > maxLength) {
			text = text.substring(0, maxLength);
			textLength = text.length();
		}

		StringBuilder middle = new StringBuilder();
		middle.append(HEADER2, 0, (HEADER2.length() - textLength) / 2);
		middle.append(text);
		middle.append(HEADER2, middle.length(), HEADER2.length());
		return middle.toString();
	}

	public void outCommentHeader(Unit unit, long mask, String comment) {

		System.out.println(HEADER1);
		System.out.println(HEADER2);

		System.out.println(insertInTheMiddle("Unit id: " + unit.getId()));
		System.out.println(insertInTheMiddle("Mask: 0x" + String.format("%016x", mask)));
		System.out.println(insertInTheMiddle("Admin:" + Boolean.toString(unit.isAdmin())));
		if (comment != null) {
			System.out.println(insertInTheMiddle(comment));
		}

		System.out.println(HEADER2);
		System.out.println(HEADER1);
		System.out.println();

	}

	public void out(Solvis solvis, String[] header) {

		StringBuilder line = new StringBuilder();
		for (String colName : header) {
			if (line.length() != 0) {
				line.append(this.semicolon ? ';' : ',');
			}
			line.append(colName);
		}
		System.out.println(line);
		System.out.println();

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
			System.out.println(line);
		}
		System.out.println();
		System.out.println();
	}

}

/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.clock;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ClockAdjustment {
	private final boolean enable;
	private final int fineLimitUpper_ms;
	private final int fineLimitLower_ms;
	private final int aproximatlySetAjust_ms;
	private final int burstLength;
	
	public ClockAdjustment() {
		this(true, 5000, -20000, 1000, 0) ; // default: fine adjustment off 
	}

	public ClockAdjustment(boolean enable, int fineLimitUpper_ms, int fineLimitLower_ms, int aproximatlySetAjust_ms,
			int burstLength) {
		this.enable = enable;
		this.fineLimitUpper_ms = fineLimitUpper_ms;
		this.fineLimitLower_ms = fineLimitLower_ms;
		this.aproximatlySetAjust_ms = aproximatlySetAjust_ms;
		this.burstLength = burstLength;
	}

	public boolean isEnable() {
		return enable;
	}

	public int getFineLimitUpper_ms() {
		return fineLimitUpper_ms;
	}

	public int getFineLimitLower_ms() {
		return fineLimitLower_ms;
	}

	public int getAproximatlySetAjust_ms() {
		return aproximatlySetAjust_ms;
	}

	public int getBurstLength() {
		return burstLength;
	}

	public static class Creator extends CreatorByXML<ClockAdjustment> {

		private boolean enable = true;
		private int fineLimitUpper_ms = 5000;
		private int fineLimitLower_ms = -20000;
		private int aproximatlySetAjust_ms = 1000;
		private int burstLength = 5;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "enable":
					this.enable = Boolean.parseBoolean(value);
					break;
				case "fineLimitUpper_ms":
					this.fineLimitUpper_ms = Integer.parseInt(value);
					break;
				case "fineLimitLower_ms":
					this.fineLimitLower_ms = Integer.parseInt(value);
					break;
				case "aproximatlySetAjust_ms":
					this.aproximatlySetAjust_ms = Integer.parseInt(value);
					break;
				case "burstLength":
					this.burstLength = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public ClockAdjustment create() throws XmlError, IOException {
			return new ClockAdjustment(enable, fineLimitUpper_ms, fineLimitLower_ms, aproximatlySetAjust_ms,
					burstLength);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
		}

	}
}

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.AccountInfo;

public class ConnectPackage extends JsonPackage implements AccountInfo {

	public ConnectPackage() {
		this.command = Command.CONNECT;
	}

	private String id = null;
	private String url = null;
	private String account = null;
	private String password = null;

	@Override
	public void finish() {
		Frame frame = this.data;
		for (Element e : frame.elements) {
			String id = e.name;
			if (e.value instanceof SingleValue) {
				SingleValue sv = (SingleValue) e.value;
				String value = sv.getData().toString();
				switch (id) {
					case "Id":
						this.id = value;
						break;
					case "Url":
						this.url = value;
						break;
					case "Account":
						this.account = value;
						break;
					case "Password":
						this.password = value;
				}
			}
		}
		this.data = null;
	}

	public String getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public String getAccount() {
		return account;
	}

	@Override
	public char[] createPassword() {
		return this.password.toCharArray();
	}

	public boolean containsSolvisLogin() {
		return (this.url != null && this.account != null && this.password != null);
	}

}

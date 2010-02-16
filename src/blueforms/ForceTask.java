package blueforms;

import java.util.TimerTask;

public class ForceTask extends TimerTask {
	Client client;

	public ForceTask(Client client) {
		this.client = client;
	}

	public void run() {
		client.debug("f");
		client.failOn();
	}
}

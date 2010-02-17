package blueforms;

import java.util.TimerTask;

public class ForceTask extends TimerTask {
	Client client;

	public ForceTask(Client client) {
		this.client = client;
	}

	public void run() {
		if (client.curState == 1) {
			client.debug("f");
			client.setState(2);
		}
	}
}
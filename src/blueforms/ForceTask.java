package blueforms;

import java.util.TimerTask;

public class ForceTask extends TimerTask {
	Client client;

	public ForceTask(Client client) {
		this.client = client;
	}
	public void run() {
		if (!client.stop && client.onlineTime != 0) {
			if (System.currentTimeMillis() - client.onlineTime > 3000){
				client.failOn();
				client.hardStatic.append("force");
				client.hardAlert.append("force");
			}
		}
	}

}

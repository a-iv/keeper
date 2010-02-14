package blueforms;

import java.util.TimerTask;

public class MonitorTask extends TimerTask {
	Client client;

	public MonitorTask(Client client) {
		this.client = client;
	}

	public void run() {
		// client.hardStatic.append("Таймер запущен");
		this.cancel();
		client.check();
	}
}
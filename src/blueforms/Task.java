package blueforms;

import java.util.TimerTask;

public class Task extends TimerTask {
	Client client;

	public Task(Client client) {
		this.client = client;
	}

	public void run() {
		// client.hardStatic.append("������ �������");
		this.cancel();
		client.check();
	}
}
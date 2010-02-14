package blueforms;

import java.util.TimerTask;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.UUID;

public class MonitorTask extends TimerTask {
	Client client;

	public MonitorTask(Client client) {
		this.client = client;
	}

	public void run() {
		this.cancel();
		int[] args = null;
		UUID[] services = new UUID[1];
		services[0] = new UUID(0x0000);
		try {
			client.agent.searchServices(args, services, client.btDev, client.listener);
		} catch (BluetoothStateException e) {
			client.ShowError("Ошибка при запуске мониторинга");
		}
	}
}
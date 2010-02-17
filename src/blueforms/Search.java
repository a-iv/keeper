package blueforms;

import java.io.IOException;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

public class Search implements DiscoveryListener {
	Client client;
	boolean bluetoothSavedFounded;

	public Search(Client client) {
		this.client = client;
	}

	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
		System.out.println("dd");
		String frendlyNameDevice;
		try {
			frendlyNameDevice = btDevice.getFriendlyName(true);
			client.hardSearch.append(frendlyNameDevice, null);
			client.remoteDevicesFounded.addElement(btDevice);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void inquiryCompleted(int discType) {
		System.out.println("ic");
		if (client.remoteDevicesFounded.isEmpty()) {
			client.hardSearch.append("Устройств не найдено", null);
		} else {
			client.hardSearch.addCommand(client.OKCommand);
		}
		client.hardSearch.addCommand(client.repeatCommand);
		client.hardSearch.addCommand(client.passCommand);
	}

	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
		System.out.println("sd");
		bluetoothSavedFounded = true;
		client.debug("Найден: " + String.valueOf(servRecord.length));
		client.agent.cancelServiceSearch(transID);
	}

	public void serviceSearchCompleted(int transID, int respCode) {
		if (client.curState != 0) {
			client.debug(String.valueOf(respCode));
			client.forceTask.cancel();
			client.debug("c");
			if (respCode == SERVICE_SEARCH_COMPLETED
					|| respCode == SERVICE_SEARCH_NO_RECORDS
					|| respCode == SERVICE_SEARCH_TERMINATED) {
				client.forceTask = new ForceTask(client);
				client.forceTimer.schedule(client.forceTask, 5000);
				client.setState(1);
			} else {
				client.setState(2);
			}
			client.monitor();
		}
	}
}

// http://math.ut.ee/~tec/static/api/BluetoothAPI_JSR82/javax/bluetooth/DiscoveryAgent.html
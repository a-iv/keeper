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
		if (client.remoteDevicesFounded.isEmpty()) {
			client.hardSearch.append("Устройств не найдено", null);
		} else {
			client.hardSearch.addCommand(client.OKCommand);
		}
		client.hardSearch.addCommand(client.repeatCommand);
		client.hardSearch.addCommand(client.passCommand);
	}

	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
		bluetoothSavedFounded = true;
		client.hardStatic.append("Найден: " + String.valueOf(servRecord.length));
		client.agent.cancelServiceSearch(transID);
	}

	public void serviceSearchCompleted(int transID, int respCode) {
		client.onlineTime = System.currentTimeMillis();
		if (respCode == SERVICE_SEARCH_COMPLETED
				|| respCode == SERVICE_SEARCH_NO_RECORDS
				|| respCode == SERVICE_SEARCH_TERMINATED) {
		//if (bluetoothSavedFounded) {
			client.failOff();
			client.hardStatic.append("Окончен: " + String.valueOf(respCode));
			client.hardAlert.append("Окончен: " + String.valueOf(respCode));
		} else {
			client.hardStatic.append("Не найдено: " + String.valueOf(respCode));
			client.hardAlert.append("Не найдено: " + String.valueOf(respCode));
			client.failOn();
		}
		client.monitor();
	}
}

// http://math.ut.ee/~tec/static/api/BluetoothAPI_JSR82/javax/bluetooth/DiscoveryAgent.html
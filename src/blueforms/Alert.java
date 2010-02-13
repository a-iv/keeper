package blueforms;

import java.util.TimerTask;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.control.ToneControl;

public class Alert extends TimerTask{
	Client client;

	public Alert(Client client) {
		this.client = client;
	}

	public void run() {
		try {
			this.cancel();
			Manager.playTone(ToneControl.C4 + 16, 100, 100);
			Manager.playTone(ToneControl.C4 + 11, 100, 100);
			client.fail();
		} catch (MediaException e) {
			e.printStackTrace();
		}
	}

}
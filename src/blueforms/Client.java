package blueforms;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.*;

import java.io.IOException;
import java.util.*;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;

public class Client extends MIDlet implements CommandListener{
	
	/*
	 * bluetooth
	 */
	DiscoveryAgent agent;
	RemoteDevice[] remoteDevicesFounded;
	int bluetoothDevicesFonuded;
	RemoteDevice btDev;
	Search listener;
	Timer timer = new Timer(); // ���������� �������
	TimerTask task; // ������� ��� �������
	TimerTask alert; // ������� ��� ������� 
	
	/*
	 * ���������
	 */
	Display display;

	// ���������� ����
	Form setPwd = new Form("����� ����������!");
	List hardSearch = new List("����� ���������", Choice.IMPLICIT);
	Form hardStatic = new Form("���������� �����������");
	Form hardAbsence = new Form("���������� �� �����������");
	Form passChange = new Form("����� ������");
	Form hardAlert = new Form("����� ��������");
	Form hardError = new Form("���������� ����������");

	// ������ ����������
	String recordStoreName = "password10";
	byte[] pwdbyte; //
	String password; // ������
	RecordStore recordStore = null; // ������
	StringItem passwordError = new StringItem("", ""); // ������ ���
														// ������������ �����
														// ������(passChange)
	StringItem passwordError1 = new StringItem("", ""); // ������ ���
														// ������������ �����
														// ������(setPwd)
	StringItem hardStaticMessage = new StringItem("", ""); // ��������� ��� ���������� � �����������
	StringItem passwordErrorAlert = new StringItem("", "");
	boolean changeForm;

	// ���������� ����� �����
	TextField enterSetPwd = new TextField("������� ������", "", 50,
			TextField.PASSWORD);
	TextField repeatSetPwd = new TextField("��������� ������", "", 50,
			TextField.PASSWORD);
	TextField enterOldChangePwd = new TextField("������� ������ ������", "",
			50, TextField.PASSWORD);
	TextField enterNewChangePwd = new TextField("������� ����� ������", "", 50,
			TextField.PASSWORD);
	TextField repeatNewChangePwd = new TextField("��������� ������", "", 50,
			TextField.PASSWORD);
	TextField enterAlert = new TextField(
			"����� � ����������� ��������, ��� ���������� ��������� ������� ������� ������:",
			"", 50, TextField.PASSWORD);

	// ���������� ������
	Command OKCommand = new Command("OK", Command.OK, 0);
	Command repeatCommand = new Command("���������", Command.HELP, 0);
	Command exitCommand = new Command("�����", Command.EXIT, 2);
	Command refrCommand = new Command("��������", Command.STOP, 2);
	Command breakCommand = new Command("���������", Command.CANCEL, 2);
	Command passCommand = new Command("����� ������", Command.BACK, 2);
	Command cancCommand = new Command("������", Command.CANCEL, 2);

	protected void destroyApp(boolean arg0) {
		// ��������� RecordStore
		try {
			recordStore.closeRecordStore();
		} catch (RecordStoreNotOpenException e) {
			// ��� �������
		} catch (RecordStoreException e) {
			// �����-�� ������ ������
		}
		notifyDestroyed();
	}

	protected void pauseApp() {
	}

	protected void startApp() throws MIDletStateChangeException {
		display = Display.getDisplay(this);
		
		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			agent = localDevice.getDiscoveryAgent();
		} 
		catch (BluetoothStateException e) {
			hardStatic.append("���������� ������������ � bluetooth ����������");
		}
		listener = new Search(this);

		// ��������� ������, ��������� ����(������ � ������ ���)
		setPwd.append(enterSetPwd);
		setPwd.append(repeatSetPwd);
		setPwd.append(passwordError1);
		setPwd.addCommand(OKCommand);
		setPwd.addCommand(exitCommand);
		setPwd.setCommandListener(this);

		// ����� ��������
		hardSearch.addCommand(exitCommand);
		hardSearch.setCommandListener(this);


		// ���������� �����������(�������� �����)
		hardStatic.addCommand(exitCommand);
		hardStatic.addCommand(breakCommand);
		hardStatic.setCommandListener(this);

		// ���������� ���������
		hardAbsence.append("���������� �� �����������");
		hardAbsence.addCommand(exitCommand);
		hardAbsence.addCommand(refrCommand);
		hardAbsence.addCommand(passCommand);
		hardAbsence.setCommandListener(this);

		// ��������� ���������� �� �������
		hardError.append("���������� �� �������, ��������� �����");
		hardError.addCommand(refrCommand);
		hardError.setCommandListener(this);

		// ����� ������
		passChange.append(enterOldChangePwd);
		passChange.append(enterNewChangePwd);
		passChange.append(repeatNewChangePwd);
		passChange.append(passwordError);
		passChange.addCommand(cancCommand);
		passChange.addCommand(OKCommand);
		passChange.setCommandListener(this);

		// ����� ��������
		hardAlert.append(enterAlert);
		hardAlert.append(passwordErrorAlert);
		hardAlert.addCommand(OKCommand);
		hardAlert.setCommandListener(this);

		// ------------------------------------------------------------------

		// �������� RecordStore
		try {
			recordStore = RecordStore.openRecordStore(recordStoreName, false);
		} catch (RecordStoreNotFoundException e) {
			// �� ���������� RS
			try {
				recordStore = RecordStore.openRecordStore(recordStoreName, true);
			} catch (RecordStoreNotFoundException e2) {
				// �� ���������� RS
			} catch (RecordStoreException e2) {
				// �����-�� ������ ������
			}
		} catch (RecordStoreException e) {
			// �����-�� ������ ������
		}
		if (getPassword())
			hardSearchFunction();
		else
			display.setCurrent(setPwd);
	}
	
	public boolean getPassword() {
		// ��������� ������ �� RS
		pwdbyte = null;
		try {
			pwdbyte = recordStore.getRecord(1);
			// byte ��������� � ������, ���������� �� � password
			if (pwdbyte == null)
				password = "";
			else
				password = new String(pwdbyte);
			return true;
		} catch (ArrayIndexOutOfBoundsException e) {
			// ������ �� ���������� � ���������� ������
		} catch (InvalidRecordIDException e) {
			// ������ � ����� ID �� ����������
		} catch (RecordStoreNotOpenException e) {
			// record store ���� �������
		} catch (RecordStoreException e) {
			// ������ ������
		}
		return false;
	}
	
	// �������, ����������� ����� ���������
	public void hardSearchFunction() {
		display.setCurrent(hardSearch);
		hardSearch.removeCommand(repeatCommand);
		hardSearch.removeCommand(OKCommand);
		bluetoothDevicesFonuded = 0;
		remoteDevicesFounded = new RemoteDevice[32];
		/*
		 * BlueSearcher searcher = new BlueSearcher(); 
		 * searcher.start();
		 * String[] str_srv = searcher.getServices(); 
		 * if (str_srv==null) {
		 * hardSearchMessage.setText("��������� �� �������");
		 * hardSearch.addCommand(repeatCommand);
		 * } else { for (int i = 0; i < str_srv.length; i++) { 
		 * hardChoise.append(str_srv[i], null); 
		 * }
		 * display.setCurrent(hardChoise); 
		 * }
		 */
		try {
			agent.startInquiry(DiscoveryAgent.GIAC, listener);
			synchronized (listener) {
				try {
					listener.wait();
				} catch (Exception e) {
				}
			}
		} catch (BluetoothStateException e) {
			e.printStackTrace();
		}
	}
	
	public void monitor() {
		task = new Task(this);
		timer.schedule(task, 1000);
	}
	
	public void check() {
		int[] args = null;
		UUID[] services = new UUID[1];
		services[0] = new UUID(0x0001);
/*		services[0] = new UUID("0100", false);
		services[1] = new UUID("111f", false);
		services[2] = new UUID("1108", false);
		services[3] = new UUID("0003", false);
		services[4] = new UUID("0008", false);
		services[5] = new UUID("1101", false);
		services[6] = new UUID("0001", false);
		*/
		try {
			agent.searchServices(args, services, btDev, listener);
		} catch (BluetoothStateException e) {
			hardStatic.append("������ ��� ������� �����������");
			e.printStackTrace();
		}
		// hardStatic.append("����� �������");
		synchronized (listener) {
			try {
				listener.wait();
			} catch (Exception e) {
			}
		}
		// hardStatic.append("��������� ����������");
	}
	
	public void fail() {
			alert = new Alert(this);
			timer.schedule(alert, 100);
		}
	
	public void comparePass() {
		// ���������� ������
		if (enterOldChangePwd.getString().equals(password)
				&& enterNewChangePwd.getString().equals(
						repeatNewChangePwd.getString())) {
			password = enterNewChangePwd.getString();
			// -----������ ������ � RS-----
			// ��������� pass � byte
			pwdbyte = password.getBytes();
			// ���������� ������ � RS
			try {
				recordStore.setRecord(1, pwdbyte, 0, pwdbyte.length);
			} catch (RecordStoreFullException e) {
				// ������ �� ��������� � ������
			} catch (RecordStoreNotOpenException e) {
				// record store ���� �������
			} catch (RecordStoreException e) {
				// ������ ������
			}
			// ---------------------������ �������----------------------
		} else {
			passwordError.setText("�����������! ��������� �������.");
		}
	}
	
	////////////////////////////////////////
	public void commandAction(Command c, Displayable d) {
		// �������� ������
		// setPwd
		if (display.getCurrent() == setPwd) {
			// OK
			if (c == OKCommand) {
				if (enterSetPwd.getString().equals(repeatSetPwd.getString())) {
					password = enterSetPwd.getString();
					enterSetPwd.setString("");
					repeatSetPwd.setString("");
					// ------------------������ ������ �
					// RecordStore-------------------
					// ��������� pass � byte
					pwdbyte = password.getBytes();
					// ���������� ������ � RS
					try {
						recordStore.addRecord(pwdbyte, 0, pwdbyte.length);
					} catch (RecordStoreFullException e) {
						// ������ �� ��������� � ������
					} catch (RecordStoreNotOpenException e) {
						// record store ���� �������
					} catch (RecordStoreException e) {
						// ������ ������
					}
					// ---------------------������ �������----------------------
					hardSearchFunction();
				} else {
					passwordError1.setText("�����������! ��������� �������.");

				}
			}
		}

		// hardSearch
		else if (display.getCurrent() == hardSearch) {
			// OK (� ������ ������ ��� ������ �������� �� ����������� �
			// ����������)
			if (c == OKCommand) {
				btDev = remoteDevicesFounded[hardSearch.getSelectedIndex()];
				// hardStatic.append("����� ���������: " + String.valueOf(bluetoothDevicesFonuded) + ", ������� � " + String.valueOf(hardSearch.getSelectedIndex()));
				try {
					hardStatic.append("���������� � ����������� " + btDev.getFriendlyName(false) + " �����������");
				} catch (IOException e) {
					e.printStackTrace();
				}
				display.setCurrent(hardStatic);
				hardSearch.deleteAll();
				hardSearch.removeCommand(repeatCommand);
				hardSearch.removeCommand(OKCommand);
				monitor();
			}
			// ������ ������ ���������
			if (c == repeatCommand) {
				hardSearch.deleteAll();
				hardSearchFunction();
			}
			// ����� ������
			if (c == passCommand) {
				changeForm = false;
				passwordError.setText("");
				enterOldChangePwd.setString("");
				enterNewChangePwd.setString("");
				repeatNewChangePwd.setString("");
				display.setCurrent(passChange);
			}
		}
		// hardStatic
		else if (display.getCurrent() == hardStatic) {
			// ������ ����������
			if (c == breakCommand) {
				display.setCurrent(hardAbsence);
			}
		}

		// hardAbsence
		else if (display.getCurrent() == hardAbsence) {
			// ���������� ������ ���������
			if (c == refrCommand)
				hardSearchFunction();
			hardStatic.deleteAll();
			if (c == passCommand) {
				changeForm = true;
				passwordError.setText("");
				enterOldChangePwd.setString("");
				enterNewChangePwd.setString("");
				repeatNewChangePwd.setString("");
				display.setCurrent(passChange);
			}
		}
		// passChange
		else if (display.getCurrent() == passChange) {
			// OK
			if (c == OKCommand) {
				if (changeForm){
				comparePass();
				display.setCurrent(hardAbsence);
			} else{
				comparePass();
				display.setCurrent(hardSearch);
			}
			}
			// ������
			if (c == cancCommand) {
				display.setCurrent(hardStatic);
			}
		}
		// hardAlert
		else if (display.getCurrent() == hardAlert) {
			if (c == OKCommand) {
				if (enterAlert.getString().equals(password)) {
					display.setCurrent(hardAbsence);
					enterAlert.setString("");
				} else {
					passwordErrorAlert.setText("�������� ������");
				}
			}
		}

		/* Exit */if (c == exitCommand) {
			destroyApp(true);
		}

		/*
		 * if (hardChoise.getSelectedIndex() == 0) { hardName =
		 * hardChoise.append(); }
		 */
	}
	// �������� ������ ���������

}
// �������
/*
 * byte[] data; ������(������) ��������� � byte String str = new
 * String("adasdad"); data = str.getBytes(); byte ��������� � ������(���
 * ���������) str = new String(data);
 */
package blueforms;

import javax.microedition.lcdui.*;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.ToneControl;
import javax.microedition.midlet.*;
import javax.microedition.rms.*;

import java.io.IOException;
import java.util.*;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;

public class Client extends MIDlet implements CommandListener {

	/*
	 * bluetooth
	 */
	DiscoveryAgent agent;
	Vector remoteDevicesFounded = new Vector();
	RemoteDevice btDev;
	Search listener;
	Timer monitorTimer = new Timer(); // ���������� ������� ��� �����������
	MonitorTask monitorTask; // ������� ��� �������
	Timer forceTimer = new Timer();
	ForceTask forceTask;

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
	StringItem hardStaticMessage = new StringItem("", ""); // ��������� ���
															// ���������� �
															// �����������
	StringItem passwordErrorAlert = new StringItem("", "");
	boolean changeForm;
	boolean stop = true;
	byte[] NOTS = { ToneControl.VERSION, 1, ToneControl.TEMPO, 30,
			ToneControl.BLOCK_START, 0, ToneControl.C4 + 16, 2,
			ToneControl.C4 + 11, 2, ToneControl.C4 + 16, 2,
			ToneControl.C4 + 11, 2, ToneControl.C4 + 16, 2,
			ToneControl.C4 + 11, 2, ToneControl.C4 + 16, 2,
			ToneControl.C4 + 11, 2, ToneControl.C4 + 16, 2,
			ToneControl.C4 + 11, 2, ToneControl.BLOCK_END, 0,
			ToneControl.PLAY_BLOCK, 0 };
	Player player;
	boolean isFail;
	long onlineTime;
	

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
		hardError.append("Bluetooth �� ������");
		hardError.addCommand(exitCommand);
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

		isFail = false;
		// ------------------------------------------------------------------

		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			agent = localDevice.getDiscoveryAgent();
		} catch (BluetoothStateException e) {
			display.setCurrent(hardError);
			return;
		}
		listener = new Search(this);

		// �������� RecordStore
		try {
			recordStore = RecordStore.openRecordStore(recordStoreName, false);
		} catch (RecordStoreNotFoundException e) {
			// �� ���������� RS
			try {
				recordStore = RecordStore
						.openRecordStore(recordStoreName, true);
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

		forceTask = new ForceTask(this);
		forceTimer.schedule(forceTask, 100, 100);
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

	public void ShowError(String error) {
		Alert alert = new Alert(error);
		alert.setTimeout(2000);
		display.setCurrent(alert);
	}

	/**
	 * �������, ����������� ����� ���������
	 */
	public void hardSearchFunction() {
		hardSearch.deleteAll();
		hardSearch.removeCommand(repeatCommand);
		hardSearch.removeCommand(passCommand);
		hardSearch.removeCommand(OKCommand);
		remoteDevicesFounded.removeAllElements();
		display.setCurrent(hardSearch);
		try {
			agent.startInquiry(DiscoveryAgent.GIAC, listener);
		} catch (BluetoothStateException e) {
			ShowError(e.toString());
		}
	}

	public void monitor() {
		if (!stop){
			monitorTask = new MonitorTask(this);
			monitorTimer.schedule(monitorTask, 100);
		} else
			display.setCurrent(hardAbsence);
	}

	public void failOn() {
		if (!isFail){
			isFail = true;
			display.setCurrent(hardAlert);
			try {
				player = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR);
				player.realize();
				ToneControl tcl = (ToneControl) player.getControl("ToneControl");
				tcl.setSequence(NOTS);
				player.setLoopCount(-1);
				player.start();
			} catch (IOException e) {
				ShowError("IO: " + e.toString());
			} catch (MediaException e) {
				ShowError("Media: " + e.toString());
			}
		}
	}
	
	public void failOff() {
		if (isFail) {
			isFail = false;
			display.setCurrent(hardStatic);
			try {
				player.stop();					
			} catch (MediaException e) {
				ShowError("Media2: " + e.toString());
			}
		}
	}

	public void enterChangePass() {
		passwordError.setText("");
		enterOldChangePwd.setString("");
		enterNewChangePwd.setString("");
		repeatNewChangePwd.setString("");
		display.setCurrent(passChange);
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

	// //////////////////////////////////////
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
					// ������ ������ � RecordStore
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
			// OK (��� ������ �������� �� ����������� �
			// ����������)
			if (c == OKCommand) {
				btDev = (RemoteDevice) remoteDevicesFounded
						.elementAt(hardSearch.getSelectedIndex());
				String name;
				try {
					name = btDev.getFriendlyName(false);
				} catch (IOException e) {
					name = btDev.getBluetoothAddress();
				}
				hardStatic.deleteAll();
				hardStatic.append("���������� � ����������� " + name + " �����������");
				display.setCurrent(hardStatic);
				onlineTime = 0;
				stop = false;
				monitor();
			}
			// ������ ������ ���������
			if (c == repeatCommand) {
				hardSearchFunction();
			}
			// ����� ������
			if (c == passCommand) {
				changeForm = false;
				enterChangePass();
			}
		}
		// hardStatic
		else if (display.getCurrent() == hardStatic) {
			// ������ ����������
			if (c == breakCommand) {
				stop = true;
			}
		}

		// hardAbsence
		else if (display.getCurrent() == hardAbsence) {
			// ���������� ������ ���������
			if (c == refrCommand)
				hardSearchFunction();
			if (c == passCommand) {
				changeForm = true;
				enterChangePass();
			}
		}
		// passChange
		else if (display.getCurrent() == passChange) {
			// OK
			if (c == OKCommand) {
				// TODO: ��������� ��������� ����� ������
				comparePass();
				if (changeForm) {
					display.setCurrent(hardAbsence);
				} else {
					display.setCurrent(hardSearch);
				}
			}
			// ������
			if (c == cancCommand) {
				if (changeForm) {
					display.setCurrent(hardAbsence);
				} else {
					display.setCurrent(hardSearch);
				}
			}
		}
		// hardAlert
		else if (display.getCurrent() == hardAlert) {
			if (c == OKCommand) {
				if (enterAlert.getString().equals(password)) {
					failOff();
					enterAlert.setString("");
					passwordErrorAlert.setText("");
					display.setCurrent(hardAbsence);
				} else {
					passwordErrorAlert.setText("�������� ������");
				}
			}
		}
		// Exit
		if (c == exitCommand) {
			destroyApp(true);
		}
	}
}
// �������
/*
 * byte[] data; ������(������) ��������� � byte String str = new
 * String("adasdad"); data = str.getBytes(); byte ��������� � ������(���
 * ���������) str = new String(data);
 */
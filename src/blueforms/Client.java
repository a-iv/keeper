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
	Timer monitorTimer = new Timer(); // Переменная таймера для мониторинга
	MonitorTask monitorTask; // Задание для таймера
	Timer forceTimer = new Timer();
	ForceTask forceTask = new ForceTask(this);

	/*
	 * Интерфейс
	 */
	Display display;

	// Обьявление форм
	Form setPwd = new Form("Добро пожаловать!");
	List hardSearch = new List("Поиск устройств", Choice.IMPLICIT);
	Form hardStatic = new Form("Соединение установлено");
	Form hardAbsence = new Form("Соединение не установлено");
	Form passChange = new Form("Смена пароля");
	Form hardAlert = new Form("Связь потеряна");
	Form hardError = new Form("Соединение невозможно");

	// Другие переменные
	String recordStoreName = "password10";
	byte[] pwdbyte; //
	String password; // Пароль
	RecordStore recordStore = null; // Пароль
	StringItem passwordError = new StringItem("", ""); // Ошибка при
	// неправильном вводе
	// пароля(passChange)
	StringItem passwordError1 = new StringItem("", ""); // Ошибка при
	// неправильном вводе
	// пароля(setPwd)
	StringItem hardStaticMessage = new StringItem("", ""); // Сообщение при
	// соединении с
	// устройством
	StringItem passwordErrorAlert = new StringItem("", "");
	boolean changeForm;
	byte[] NOTS = { ToneControl.VERSION, 1, ToneControl.TEMPO, 30,
			ToneControl.BLOCK_START, 0, ToneControl.C4 + 16, 2,
			ToneControl.C4 + 11, 2, ToneControl.C4 + 16, 2,
			ToneControl.C4 + 11, 2, ToneControl.C4 + 16, 2,
			ToneControl.C4 + 11, 2, ToneControl.C4 + 16, 2,
			ToneControl.C4 + 11, 2, ToneControl.C4 + 16, 2,
			ToneControl.C4 + 11, 2, ToneControl.BLOCK_END, 0,
			ToneControl.PLAY_BLOCK, 0 };
	Player player = null;
	int curState = 0;
	// long onlineTime;

	// Обьявление полей ввода
	TextField enterSetPwd = new TextField("Введите пароль", "", 50,
			TextField.PASSWORD);
	TextField repeatSetPwd = new TextField("Повторите пароль", "", 50,
			TextField.PASSWORD);
	TextField enterOldChangePwd = new TextField("Введите старый пароль", "",
			50, TextField.PASSWORD);
	TextField enterNewChangePwd = new TextField("Введите новый пароль", "", 50,
			TextField.PASSWORD);
	TextField repeatNewChangePwd = new TextField("Повторите пароль", "", 50,
			TextField.PASSWORD);
	TextField enterAlert = new TextField(
			"Связь с устройством потеряна, для отключения звукового сигнала введите пароль:",
			"", 50, TextField.PASSWORD);

	// Обьявление команд
	Command OKCommand = new Command("OK", Command.OK, 0);
	Command repeatCommand = new Command("Повторить", Command.HELP, 0);
	Command exitCommand = new Command("Выход", Command.EXIT, 2);
	Command refrCommand = new Command("Обновить", Command.STOP, 2);
	Command breakCommand = new Command("Разорвать", Command.CANCEL, 2);
	Command passCommand = new Command("Смена пароля", Command.BACK, 2);
	Command cancCommand = new Command("Отмена", Command.CANCEL, 2);

	public void debug(String text) {
		// hardStatic.append(text);
		// hardAlert.append(text);
	}

	protected void destroyApp(boolean arg0) {
		// Закрываем RecordStore
		try {
			recordStore.closeRecordStore();
		} catch (RecordStoreNotOpenException e) {
			// уже закрыта
		} catch (RecordStoreException e) {
			// какие-то другие ошибки
		}
		notifyDestroyed();
	}

	protected void pauseApp() {
	}

	protected void startApp() throws MIDletStateChangeException {
		display = Display.getDisplay(this);

		// Установка пароля, стартовое окно(только в первый раз)
		setPwd.append(enterSetPwd);
		setPwd.append(repeatSetPwd);
		setPwd.append(passwordError1);
		setPwd.addCommand(OKCommand);
		setPwd.addCommand(exitCommand);
		setPwd.setCommandListener(this);

		// Поиск устойств
		hardSearch.addCommand(exitCommand);
		hardSearch.setCommandListener(this);

		// Соединение установлено(основная форма)
		hardStatic.addCommand(exitCommand);
		hardStatic.addCommand(breakCommand);
		hardStatic.setCommandListener(this);

		// Соединение разорвано
		hardAbsence.append("Соединение не установлено");
		hardAbsence.addCommand(exitCommand);
		hardAbsence.addCommand(refrCommand);
		hardAbsence.addCommand(passCommand);
		hardAbsence.setCommandListener(this);

		// Установка соединения не удалась
		hardError.append("Bluetooth не найден");
		hardError.addCommand(exitCommand);
		hardError.setCommandListener(this);

		// Смена пароля
		passChange.append(enterOldChangePwd);
		passChange.append(enterNewChangePwd);
		passChange.append(repeatNewChangePwd);
		passChange.append(passwordError);
		passChange.addCommand(cancCommand);
		passChange.addCommand(OKCommand);
		passChange.setCommandListener(this);

		// Связь потеряна
		hardAlert.append(enterAlert);
		hardAlert.append(passwordErrorAlert);
		hardAlert.addCommand(OKCommand);
		hardAlert.setCommandListener(this);

		try {
			player = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR);
			player.realize();
			ToneControl tcl = (ToneControl) player.getControl("ToneControl");
			tcl.setSequence(NOTS);
			player.setLoopCount(-1);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (MediaException e1) {
			e1.printStackTrace();
		}

		// ------------------------------------------------------------------

		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			agent = localDevice.getDiscoveryAgent();
		} catch (BluetoothStateException e) {
			display.setCurrent(hardError);
			return;
		}
		listener = new Search(this);

		// Открытие RecordStore
		try {
			recordStore = RecordStore.openRecordStore(recordStoreName, false);
		} catch (RecordStoreNotFoundException e) {
			// не существует RS
			try {
				recordStore = RecordStore
						.openRecordStore(recordStoreName, true);
			} catch (RecordStoreNotFoundException e2) {
				// не существует RS
			} catch (RecordStoreException e2) {
				// какие-то другие ошибки
			}
		} catch (RecordStoreException e) {
			// какие-то другие ошибки
		}
		if (getPassword())
			hardSearchFunction();
		else
			display.setCurrent(setPwd);
	}

	public boolean getPassword() {
		// Считываем пароль из RS
		pwdbyte = null;
		try {
			pwdbyte = recordStore.getRecord(1);
			// byte переводим в строку, записываем ее в password
			if (pwdbyte == null)
				password = "";
			else
				password = new String(pwdbyte);
			return true;
		} catch (ArrayIndexOutOfBoundsException e) {
			// запись не помещается в переданный массив
		} catch (InvalidRecordIDException e) {
			// записи с таким ID не существует
		} catch (RecordStoreNotOpenException e) {
			// record store была закрыта
		} catch (RecordStoreException e) {
			// другие ошибки
		}
		return false;
	}

	public void ShowError(String error) {
		Alert alert = new Alert(error);
		alert.setTimeout(2000);
		display.setCurrent(alert);
	}

	/**
	 * Функция, описывающая поиск устройств
	 */
	public void hardSearchFunction() {
		System.out.println("hardSearchFunction");
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

	public void setState(int state) {
		if (curState == 0 && state == 1) {
			curState = state;
			monitor();
		}
		if (curState == 1 && state == 0) {
			curState = state;
			display.setCurrent(hardAbsence);
		}
		if (curState != 2 && state == 2) {
			curState = state;
			debug("+");
			debug("!");
			display.setCurrent(hardAlert);
			try {
				player.start();
				debug(">");
			} catch (MediaException e) {
				ShowError("Media: " + e.toString());
			}
		}
		if (curState == 2 && state != 2) {
			curState = state;
			debug("!");
			if (state == 1) {
				display.setCurrent(hardStatic);
			} else if (state == 0) {
				display.setCurrent(hardAbsence);
			}
			try {
				player.stop();
			} catch (MediaException e) {
				e.printStackTrace();
			}
			debug("|");
		}
	}

	public void monitor() {
		debug("m");
		if (curState == 1 || curState == 2) {
			monitorTask = new MonitorTask(this);
			monitorTimer.schedule(monitorTask, 100);
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
		// Сравниваем пароли
		if (enterOldChangePwd.getString().equals(password)
				&& enterNewChangePwd.getString().equals(
						repeatNewChangePwd.getString())) {
			password = enterNewChangePwd.getString();
			// -----Запись пароля в RS-----
			// Переводим pass в byte
			pwdbyte = password.getBytes();
			// Записываем пароль в RS
			try {
				recordStore.setRecord(1, pwdbyte, 0, pwdbyte.length);
			} catch (RecordStoreFullException e) {
				// данные не умещаются в памяти
			} catch (RecordStoreNotOpenException e) {
				// record store была закрыта
			} catch (RecordStoreException e) {
				// другие ошибки
			}
			// ---------------------Пароль записан----------------------
		} else {
			passwordError.setText("Неправильно! Повторите попытку.");
		}
	}

	// //////////////////////////////////////
	public void commandAction(Command c, Displayable d) {
		// Описание команд
		// setPwd
		if (display.getCurrent() == setPwd) {
			// OK
			if (c == OKCommand) {
				if (enterSetPwd.getString().equals(repeatSetPwd.getString())) {
					password = enterSetPwd.getString();
					enterSetPwd.setString("");
					repeatSetPwd.setString("");
					// Запись пароля в RecordStore
					// Переводим pass в byte
					pwdbyte = password.getBytes();
					// Записываем пароль в RS
					try {
						recordStore.addRecord(pwdbyte, 0, pwdbyte.length);
					} catch (RecordStoreFullException e) {
						// данные не умещаются в памяти
					} catch (RecordStoreNotOpenException e) {
						// record store была закрыта
					} catch (RecordStoreException e) {
						// другие ошибки
					}
					// ---------------------Пароль записан----------------------
					hardSearchFunction();
				} else {
					passwordError1.setText("Неправильно! Повторите попытку.");
				}
			}
		}

		// hardSearch
		else if (display.getCurrent() == hardSearch) {
			// OK (Эта кнопка отвечает за подключение к
			// устройству)
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
				hardStatic.append("Соединение с устройством " + name
						+ " установлено");
				display.setCurrent(hardStatic);
				setState(1);
			}
			// Повтор поиска устройств
			if (c == repeatCommand) {
				hardSearchFunction();
			}
			// Смена пароля
			if (c == passCommand) {
				changeForm = false;
				enterChangePass();
			}
		}
		// hardStatic
		else if (display.getCurrent() == hardStatic) {
			// Разрыв соединения
			if (c == breakCommand) {
				setState(0);
			}
		}

		// hardAbsence
		else if (display.getCurrent() == hardAbsence) {
			// Повторение поиска устройств
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
				// TODO: проверять результат смены пароля
				comparePass();
				if (changeForm) {
					display.setCurrent(hardAbsence);
				} else {
					display.setCurrent(hardSearch);
				}
			}
			// Отмена
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
					setState(0);
					enterAlert.setString("");
					passwordErrorAlert.setText("");
				} else {
					passwordErrorAlert.setText("Неверный пароль");
				}
			}
		}
		// Exit
		if (c == exitCommand) {
			destroyApp(true);
		}
	}
}
// Памятки
/*
 * byte[] data; строку(пароль) переводим в byte String str = new
 * String("adasdad"); data = str.getBytes(); byte переводим в строку(для
 * сравнения) str = new String(data);
 */
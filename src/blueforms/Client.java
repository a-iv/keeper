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
	Timer timer = new Timer(); // Переменная таймера
	TimerTask task; // Задание для таймера
	TimerTask alert; // Задание для таймера 
	
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
	StringItem hardStaticMessage = new StringItem("", ""); // Сообщение при соединении с устройством
	StringItem passwordErrorAlert = new StringItem("", "");
	boolean changeForm;

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
		
		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			agent = localDevice.getDiscoveryAgent();
		} 
		catch (BluetoothStateException e) {
			hardStatic.append("Невозможно подключиться к bluetooth устройству");
		}
		listener = new Search(this);

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
		hardError.append("Соединение не удалось, повторите поиск");
		hardError.addCommand(refrCommand);
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

		// ------------------------------------------------------------------

		// Открытие RecordStore
		try {
			recordStore = RecordStore.openRecordStore(recordStoreName, false);
		} catch (RecordStoreNotFoundException e) {
			// не существует RS
			try {
				recordStore = RecordStore.openRecordStore(recordStoreName, true);
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
	
	// Функция, описывающая поиск устройств
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
		 * hardSearchMessage.setText("Устройств не найдено");
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
			hardStatic.append("Ошибка при запуске мониторинга");
			e.printStackTrace();
		}
		// hardStatic.append("Поиск запущен");
		synchronized (listener) {
			try {
				listener.wait();
			} catch (Exception e) {
			}
		}
		// hardStatic.append("Дождались завершения");
	}
	
	public void fail() {
			alert = new Alert(this);
			timer.schedule(alert, 100);
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
	
	////////////////////////////////////////
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
					// ------------------Запись пароля в
					// RecordStore-------------------
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
			// OK (В данном случае эта кнопка отвечает за подключение к
			// устройству)
			if (c == OKCommand) {
				btDev = remoteDevicesFounded[hardSearch.getSelectedIndex()];
				// hardStatic.append("Всего устройств: " + String.valueOf(bluetoothDevicesFonuded) + ", выбрано № " + String.valueOf(hardSearch.getSelectedIndex()));
				try {
					hardStatic.append("Соединение с устройством " + btDev.getFriendlyName(false) + " установлено");
				} catch (IOException e) {
					e.printStackTrace();
				}
				display.setCurrent(hardStatic);
				hardSearch.deleteAll();
				hardSearch.removeCommand(repeatCommand);
				hardSearch.removeCommand(OKCommand);
				monitor();
			}
			// Повтор поиска устройств
			if (c == repeatCommand) {
				hardSearch.deleteAll();
				hardSearchFunction();
			}
			// Смена пароля
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
			// Разрыв соединения
			if (c == breakCommand) {
				display.setCurrent(hardAbsence);
			}
		}

		// hardAbsence
		else if (display.getCurrent() == hardAbsence) {
			// Повторение поиска устройств
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
			// Отмена
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
					passwordErrorAlert.setText("Неверный пароль");
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
	// Описание команд закончено

}
// Памятки
/*
 * byte[] data; строку(пароль) переводим в byte String str = new
 * String("adasdad"); data = str.getBytes(); byte переводим в строку(для
 * сравнения) str = new String(data);
 */
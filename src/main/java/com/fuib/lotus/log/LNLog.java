package com.fuib.lotus.log;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;


/**
 * Класс, осуществляющий форматированный вывод в стандартный поток Writer.
 * Может писать в любой во внешний "Witer"-объект либо "в себя". В этом случае необходимо
 * создать класс-наследник, где переопределить функции write(...) и flush()
 * @author Gorobets S.
 */
public class LNLog extends Writer implements lotus.domino.Base {
	/**
	 * это свойство переводим в true только в методе createLogDoc конечных классов-наследников!
	 */
	protected boolean		m_bIsLogOpened = false;
	protected Hashtable<Object, Object>		m_properties = new Hashtable<Object, Object>();		// container for additional properties(attributes) of a class
	protected String 		m_sCategory = null;					// log category (id)
	protected String 		m_sModule = null;					// name of a module who logging being performe
	
	public LNLog(String sCategory, String sModule) {
		m_sCategory = sCategory;
		m_sModule = sModule;
	}
	
	
	public boolean isLogOpened()	{ return m_bIsLogOpened; }
	
	
	// --- close log. In subclasses must be called after all subclass 'closed' operations
	public void close() throws IOException {
		m_bIsLogOpened = false;
	}
	
	
	public void setProperty(Object sAttrName, Object vValue)	{ m_properties.put(sAttrName, vValue); }
	public Object getProperty(Object sAttrName)  				{ return m_properties.get(sAttrName); }
	public boolean hasProperty(Object sAttrName)				{ return m_properties.containsKey(sAttrName); }
	
	
	/**
	 * Вывод с формирование строки по определённому шаблону: "время: имя_агента >> логируемая_строка" <br />
	 * для простого вывода используйте переопределённый в классах-наследниках метод Writer.write(String)
	 */
	public void log(String sText) throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss:   ");
		write(formatter.format(new Date()) + ((m_sModule.length() > 0) ? (m_sModule + " >> ") : "") + sText + "\n");
		flush();
	}
	
	/**
	 * Внимание!!! Если после вызова этого метода дублируется код ошибки в фигурных скобках, то вызывайте вместо этого метода просто метод log!
	 */
	public String logError(int nErr, String sText) throws Exception {
		sText = LogEx.getErrInfo(null, nErr, sText, null);
		log(sText);
		return sText;
	}
	
	
	public void printStackTrace(Throwable te) {
		StackTraceElement[] ste = te.getStackTrace();
		try {
			for (int i = 1; i < ste.length; i++) {
				write(" 	" + ste[i].toString() + "\n");
			}
		}
		catch (IOException e) {
			System.err.println("LNLog.printStackTrace: " + LogEx.getErrInfo(e, false));
			LogEx.printStackTrace(e);
		}
	}
	
	
	public void flush() throws IOException {}
	
	
	/**
	 * Метод требуется для проброса данных при передаче данного объекта лога в PrintWriter, к примеру, в выводе стэка:
	 * 		e.printStackTrace(new PrintWriter(LNLog));
	 * !!! Для работоспособности класса в классах-наследниках должен быть переопределён метод Writer.write(String) !!!
	 */
	public void write(char[] arg0, int arg1, int arg2) throws IOException {
		String sText = new String(arg0, arg1, arg2);
		write(sText);
	}
	
	
	/**
	 * Метод создан для совместимости с классами-наследниками;
	 */
	public void recycle() {
		try {
			close();	// для случая, когда лог по какой-либо причине не был закрыт выше
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Метод создан для совместимости с классами-наследниками
	 * ничего не содержит
	 */
	@SuppressWarnings("unchecked")
	public void recycle(Vector arg0) {}
}


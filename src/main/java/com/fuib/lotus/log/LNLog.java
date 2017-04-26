package com.fuib.lotus.log;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;


/**
 * �����, �������������� ��������������� ����� � ����������� ����� Writer.
 * ����� ������ � ����� �� ������� "Witer"-������ ���� "� ����". � ���� ������ ����������
 * ������� �����-���������, ��� �������������� ������� write(...) � flush()
 * @author Gorobets S.
 */
public class LNLog extends Writer implements lotus.domino.Base {
	/**
	 * ��� �������� ��������� � true ������ � ������ createLogDoc �������� �������-�����������!
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
	 * ����� � ������������ ������ �� ������������ �������: "�����: ���_������ >> ����������_������" <br />
	 * ��� �������� ������ ����������� ��������������� � �������-����������� ����� Writer.write(String)
	 */
	public void log(String sText) throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss:   ");
		write(formatter.format(new Date()) + ((m_sModule.length() > 0) ? (m_sModule + " >> ") : "") + sText + "\n");
		flush();
	}
	
	/**
	 * ��������!!! ���� ����� ������ ����� ������ ����������� ��� ������ � �������� �������, �� ��������� ������ ����� ������ ������ ����� log!
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
	 * ����� ��������� ��� �������� ������ ��� �������� ������� ������� ���� � PrintWriter, � �������, � ������ �����:
	 * 		e.printStackTrace(new PrintWriter(LNLog));
	 * !!! ��� ����������������� ������ � �������-����������� ������ ���� ������������ ����� Writer.write(String) !!!
	 */
	public void write(char[] arg0, int arg1, int arg2) throws IOException {
		String sText = new String(arg0, arg1, arg2);
		write(sText);
	}
	
	
	/**
	 * ����� ������ ��� ������������� � ��������-������������;
	 */
	public void recycle() {
		try {
			close();	// ��� ������, ����� ��� �� �����-���� ������� �� ��� ������ ����
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * ����� ������ ��� ������������� � ��������-������������
	 * ������ �� ��������
	 */
	@SuppressWarnings("unchecked")
	public void recycle(Vector arg0) {}
}


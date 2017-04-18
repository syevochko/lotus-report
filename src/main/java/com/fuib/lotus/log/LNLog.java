package com.fuib.lotus.log;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;


/**
 * Класс, осуществляющий форматированный вывод в стандартный поток Writer. 
 * Может писать в любой во внешний "Witer" объект либо "в себя". В этом случае необходимо 
 * создать класс-наследник, где переопределить функции write(...) и flush()  
 * @author Gorobets S.
 */
public class LNLog extends Writer {
	// --- constants
	public static final int 	LOGTYPE_SINGLE = 1;
	public static final int 	LOGTYPE_APPEND = 2;
	
	// --- variables
	protected Writer		m_out = null;						// output stream (any 'Writer' object)
	protected boolean		m_bIsLogOpened = false;
	protected Hashtable		m_properties = new Hashtable();		// container for additional properties(attributes) of a class	
	protected String 		m_sCategory = null;					// log category (id)
	protected String 		m_sModule = null;					// name of a module who logging being performe
	

	// --- constructors
	public LNLog(Writer a_out, String sCategory, String sModule) {
		if ( a_out != null ) {
			m_out = a_out;
			m_bIsLogOpened = true;		// output to external Writer object --> log opened at this point
		} else
			m_out = this;				// output to internal --> log must be opened explitcly by subclass's method

		m_sCategory = sCategory; 
		m_sModule = sModule;
	}
	
	public LNLog()									{ this(null, "", ""); }
	public LNLog(Writer a_out)						{ this(a_out, "", ""); }
	public LNLog(String sCategory)					{ this(null, sCategory, ""); }
	public LNLog(String sCategory, String sModule)	{ this(null, sCategory, sModule); }	
	
	
	// --- access to private members
	public boolean isLogOpened()	{ return m_bIsLogOpened; }
	

	// --- close log. In subclasses must be called after all subclass 'closed' operations 
	public void close() throws IOException {
		if ( isLogOpened() ) {			
			if ( m_out.hashCode() != this.hashCode() ) {
				m_out.flush();
				m_out.close();
			}
			
			m_bIsLogOpened = false;
		}
	}

	
	public void setProperty(Object sAttrName, Object vValue)	{ m_properties.put(sAttrName, vValue); }
	public Object getProperty(Object sAttrName)  				{ return m_properties.get(sAttrName); }
	public boolean hasProperty(Object sAttrName)				{ return m_properties.containsKey(sAttrName); }
		
	
	// --- 'output' functions
	public void log(String sText) throws Exception {
		if ( isLogOpened() ) {
			SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss:   ");

			m_out.write(formatter.format(new Date()) + (( m_sModule.length() > 0 )?(m_sModule + " >> "):"") + sText + "\n");
			m_out.flush();
		}
	}
	
	
	public void logError(int nErr, String sText) throws Exception {
		log("Ошибка! " + sText + " (код: " + String.valueOf(nErr) + ")");		
	}

	
	// !! must be override in subclasses !!
	public void flush() throws IOException { }
	public void write(char[] arg0, int arg1, int arg2) throws IOException {	}
}


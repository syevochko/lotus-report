package com.fuib.lotus.log;

import java.io.IOException;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Database;
import lotus.domino.Item;
import lotus.domino.NotesException;

/**
 * ѕишет каждую запись в отдельный документ лога
 * (дл€ последующей обработки с помощью LogEntriesMerge)
 * 
 * –анее при создании исп. константа LNDbLog.LOGTYPE_APPEND
 * 
 * @author shubniko
 */
public class LNDbLogEntry extends LNDbLog {
	
	public LNDbLogEntry(String category, String module) {
		super(category, module);
	}
	
	public boolean open(Database db, int nDaysLogExpired) throws Exception {
		return super.open(db, nDaysLogExpired);
	}
	
	
	protected boolean createLogDoc() throws NotesException {
		if (super.createLogDoc()) {
			m_docLog.replaceItemValue("Form", "fmTmpLogEntry").recycle();
			m_bIsLogOpened = true;
		}
		return m_bIsLogOpened;
	}
	
	
	public void write(String sText) throws IOException {
		try {
			if (createLogDoc()) {
				if (sText.endsWith("\n")) sText = sText.substring(0, sText.length() - 1);
				Item itemLog = m_docLog.replaceItemValue("fdEvent", sText);
				if (30000 - itemLog.getValueLength() <= 103) itemLog.setSummary(false);
				Tools.recycleObj(itemLog);
			}
		}
		catch (NotesException ne) {
			throw new IOException(ne.text + " {" + ne.id + "}");
		}
	}
	
	
	public void flush() throws IOException {
		if (isLogOpened()) {
			save();
			
			Tools.recycleObj(m_docLog);
			m_docLog = null;	// обнул€ем и здесь, т.к. в Tools.recycleObj() обнуление не срабатывает (€ сам в шоке...)
			
			super.close();
		}
	}
	
	
	public void close() throws IOException {
		if (isLogOpened()) {
			super.close();
			recycle();
		}
	}
	
}

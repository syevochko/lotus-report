package com.fuib.lotus.log;

import java.io.IOException;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Database;
import lotus.domino.RichTextItem;
import lotus.domino.NotesException;

/**
 * Пишет всё в один документ
 * 
 * Ранее при создании исп. константа LNDbLog.LOGTYPE_SINGLE
 * 
 * @author shubniko
 */
public class LNDbLogSingle extends LNDbLog {
	protected RichTextItem	m_itemLog = null;			// item of lotus document containing log
	
	public LNDbLogSingle(String category, String module) {
		super(category, module);
	}
	
	public boolean open(Database db, int nDaysLogExpired) throws Exception {
		if (super.open(db, nDaysLogExpired))
			return createLogDoc();
		return false;
	}
	
	
	protected boolean createLogDoc() throws NotesException {
		if (super.createLogDoc()) {
			m_docLog.replaceItemValue("Form", "fmLogEntry").recycle();
			m_itemLog = m_docLog.createRichTextItem("fdrEvent");
			m_bIsLogOpened = (m_itemLog != null);
		}
		return m_bIsLogOpened;
	}
	
	
	public String logError(int nErr, String sMsg) throws Exception {
		if (isLogOpened()) {
			sMsg = super.logError(nErr, sMsg);
			Tools.appendItemValue(m_docLog, "fdError", sMsg);
		}
		return sMsg;
	}
	
	
	public void write(String sText) throws IOException {
		if (isLogOpened()) {
			try {
				m_itemLog.appendText(sText);
			}
			catch (NotesException ne) {
				throw new IOException(ne.text + " {" + ne.id + "}");
			}
		}
	}
	
	
	public void close() throws IOException {
		if (isLogOpened()) {
			try {
				m_dtNow.setNow();
				m_docLog.replaceItemValue("fddEnd", m_dtNow).recycle();
				save();
			}
			catch (NotesException ne) {
				throw new IOException(ne.text + " {" + ne.id + "}");
			}
			super.close();
			recycle();
		}
	}
	
	
	public void recycle() {
		Tools.recycleObj(m_itemLog);
		super.recycle();
	}
}

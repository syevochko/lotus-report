package com.fuib.lotus.log;

import java.io.IOException;
import java.io.Writer;

public class LNLogWriter extends LNLog {
	protected Writer m_out = null;						// output stream (any 'Writer' object)
	
	public LNLogWriter(Writer p_out, String sCategory, String sModule) {
		super(sCategory, sModule);
		m_out = p_out;
	}
	
	public LNLogWriter(Writer p_out) {
		this(p_out, "", "");
	}
	
	
	public void write(String sText) throws IOException {
		if (isLogOpened()) {
			m_out.write(sText);
		}
	}
	
	public void flush() throws IOException {
		if (isLogOpened()) {
			m_out.flush();
		}
	}
	
	
	public void close() throws IOException {
		if (isLogOpened()) {
			m_out.close();
			super.close();
		}
	}
	
}

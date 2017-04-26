package com.fuib.lotus.agents;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.fuib.lotus.ws.LNCGI;
import com.fuib.lotus.ws.WCResponse;

import lotus.domino.NotesException;

public class LNHTTPAgent extends LNAgentBase {
	protected LNCGI m_CGI;
	protected WCResponse m_agentResp = null;
	
	public LNHTTPAgent() {
		m_CGI = new LNCGI(m_docContext);
	}

	protected void main() throws NotesException, Exception, Throwable {}
	
	
	public String getAuthUser() throws NotesException {
		String user = m_CGI.getAuthUser();
		if (user.isEmpty()) user = super.getAuthUser();
		return user;
	}
	
	
	protected void setErrResponse(int nErr, String sText) throws Exception {
		if(m_agentResp == null) 
			m_agentResp = new WCResponse("");
		
		m_agentResp.SetErrCode(nErr);
		m_agentResp.SetErrText(sText);
	}
	
	
	@SuppressWarnings("unchecked")
	protected void setResponse(int nRC, Object oData) throws Exception {
		if(m_agentResp == null) 
			m_agentResp = new WCResponse("");

		m_agentResp.SetRespCode(nRC);
		
		if (oData instanceof Map)
			m_agentResp.SetRespTextFromMap((Map) oData);
		if (oData instanceof String[])
			m_agentResp.SetRespTextFromArray((String[]) oData);
		else if (oData instanceof String)
			m_agentResp.SetRespText((String) oData);
	}
	
	
	protected void response() throws UnsupportedEncodingException {
		if(m_agentResp == null) 
			m_agentResp = new WCResponse("");

		outToPW(m_agentResp.GetResult()); 
	}
	
}
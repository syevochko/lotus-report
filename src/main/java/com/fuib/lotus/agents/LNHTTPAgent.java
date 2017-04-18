package com.fuib.lotus.agents;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fuib.lotus.ws.WCResponse;

import lotus.domino.NotesException;

public class LNHTTPAgent extends LNAgentBase {
	// some of the CGI variables. Complete list see 
	// Notes:///8525704A00561E32/855DC7FCFD5FEC9A85256B870069C0AB/784B011BAD9C15F68525704A003F67A9
	public static final String CGI_REMOTE_IP = "Remote_Addr";
	public static final String CGI_QUERYSTRING = "Query_String_Decoded";
	public static final String CGI_REMOTE_HOST = "Remote_Host";
	public static final String CGI_AUTH_USER = "Remote_User";	
	public static final String CGI_COOKIE = "HTTP_Cookie";
	public static final String CGI_HTTPS = "HTTPS";
	public static final String QUERY_PARAM_SEP = "&";
		
	protected HashMap m_param = null;
	protected WCResponse m_agentResp = null;
	

	protected void main() throws NotesException, Exception, Throwable {
		if (this.m_docContext == null) m_docContext = m_agentContext.getDocumentContext();		
	}

	// parse query string (f.e. openagent&param0&param1=value1&param2=value2)to table of parameters
	// param0 = null, param1=value1, param2=value2, ...
	protected void ParseQueryString() throws NotesException, UnsupportedEncodingException {
		String sQueryString = "";
		
		if ( m_docContext != null ) {
			if ( m_param == null ) m_param = new HashMap();

			if ( System.getProperty("java.version").startsWith("1.3") || System.getProperty("java.version").startsWith("1.2") || System.getProperty("java.version").startsWith("1.1"))
				sQueryString = URLDecoder.decode(m_docContext.getItemValueString("Query_String"));
			else
				sQueryString = URLDecoder.decode(m_docContext.getItemValueString("Query_String"), "CP1251");

			
			if ( sQueryString.length() > 0 ) {
				String sPrevKey = null;
				Pattern PARSE_PATTERN = Pattern.compile("([\\w ]+)=(.+)");

				String[] arrStr = sQueryString.split( QUERY_PARAM_SEP );
				for (int i=0; i<arrStr.length; i++)	{
					String s1 = arrStr[i];
					if (s1.indexOf("=")!=-1)	{
						Matcher m1 = PARSE_PATTERN.matcher(s1);
						if ( m1.find() )	{
							sPrevKey = m1.group(1);
							m_param.put(sPrevKey, m1.group(2));
						} else {			// key without value, exmp: &key1=& 
							sPrevKey = s1.substring(0, s1.indexOf("="));
							if (!"".equals(sPrevKey)) m_param.put(sPrevKey, "");	//Ignore value with empty key, exmp: &=val&
						}
					}	else if (sPrevKey!=null && !"".equals(sPrevKey))	{			// this is part of previous value, so we append value to previous element  
						m_param.put(sPrevKey, m_param.get(sPrevKey)+ QUERY_PARAM_SEP + s1);
					}
				}
			}			

		}
	}	
	
	
	protected String getCGI(String sCGI) throws NotesException {
		String sReturn = "";
		
		if ( m_docContext != null && m_docContext.hasItem(sCGI) ) 
			sReturn = m_docContext.getItemValueString(sCGI);

		return sReturn;
	}
	
	
	protected boolean isSSL() throws NotesException 
		{ return getCGI(CGI_HTTPS).equalsIgnoreCase("ON"); }
	
	
	protected void setErrResponse(int nErr, String sText) throws Exception {
		if(m_agentResp==null) 
			m_agentResp = new WCResponse("");
		
		m_agentResp.SetErrCode(nErr);
		m_agentResp.SetErrText(sText);
	}
	
	
	protected void setResponse(int nRC, Object oData) throws Exception {
		if(m_agentResp==null) 
			m_agentResp = new WCResponse("");

		m_agentResp.SetRespCode(nRC);
		
		if ( oData instanceof Map )
			m_agentResp.SetRespTextFromMap((Map)oData);
		if ( oData instanceof String[] )
			m_agentResp.SetRespTextFromArray((String[])oData);
		else if ( oData instanceof String )
			m_agentResp.SetRespText((String)oData);
	}
	
	
	protected void response() throws UnsupportedEncodingException {
		if(m_agentResp==null) 
			m_agentResp = new WCResponse("");

		m_pw.println(m_agentResp.GetResult()); 
	}
	
}
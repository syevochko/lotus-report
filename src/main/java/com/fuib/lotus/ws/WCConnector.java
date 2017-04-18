package com.fuib.lotus.ws;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map.Entry;
import java.net.URLEncoder;

import com.fuib.util.SSLURLAction;
import com.fuib.util.URLAction;

/**
 * @author evochko
 * This is a wrapper around URLAction/SSLURLAction. It can process response with WCResponse class.
 * @see com.fuib.lotus.ws.WCResponse  
 */
public class WCConnector {
	
	public String param_sep = "&";	// разделитель пар параметров и значение в http-запросе
	public String param_set = "=";	// разделитель значений ключ и значение в http-запросе
	
	protected URLAction m_urlact = null;
	protected String m_sURLAddr = "";
	protected WCResponse m_resp = null;
	
	
	/**
	 * @param a_login
	 * @param a_password
	 * @param a_sProtocol - type or the protocol (HTTP | HTTPS) @see com.fuib.util.URLAction
	 * @param method	- type of the request (GET | POST)  @see com.fuib.util.URLAction
	 * @param a_sURLAddr - String www-address 
	 * @param a_urlParams	- HasMap - additional params
	 * @throws MalformedURLException
	 * @throws Exception
	 */
	public WCConnector(String a_sURLAddr) throws MalformedURLException, Exception {
		this(null, null, null, URLAction.METHOD_GET, a_sURLAddr, null);
	}
		
	public WCConnector(String a_login, String a_password, String a_sProtocol, String a_sURLAddr) throws MalformedURLException, Exception {
		this(a_login, a_password, a_sProtocol, URLAction.METHOD_GET, a_sURLAddr, null);
	}

	public WCConnector( String a_login, String a_password, String a_sProtocol, int method, String a_sURLAddr, java.util.HashMap a_urlParams ) throws MalformedURLException, Exception {
		this(a_login, a_password, a_sProtocol, URLAction.METHOD_GET, a_sURLAddr, true, a_urlParams);
	}
	
	public WCConnector( String a_login, String a_password, String a_sProtocol, int method, String a_sURLAddr, boolean bDereferenceDNS, java.util.HashMap a_urlParams ) throws MalformedURLException, Exception {
		String sURLAddr = a_sURLAddr, sParams = "";		
		java.util.Map.Entry entr = null;		
		boolean right_java_version = true;
		
		if ( System.getProperty("java.version").startsWith("1.3") || System.getProperty("java.version").startsWith("1.2") || System.getProperty("java.version").startsWith("1.1"))
			right_java_version = false;
		
		if (a_urlParams!=null)	{
			for (Iterator it = a_urlParams.entrySet().iterator(); it.hasNext(); )	{
				entr = (Entry)it.next();
				// кодируем только значения параметров. Всю строку параметров кодировать нельзя, а то похеряться символы типа / и &
				sParams = sParams + this.param_sep + entr.getKey() + this.param_set + 
				(right_java_version? URLEncoder.encode(entr.getValue().toString(), WCResponse.DEFAULT_CHARSET) : URLEncoder.encode(entr.getValue().toString()) );				
			}
		}		
		
		m_sURLAddr = sURLAddr+sParams;
		
		URL myUrl = new URL(m_sURLAddr);
		if ("https".equals(myUrl.getProtocol()) || 
				(a_sProtocol!=null && (URLAction.HTTPS.equals(a_sProtocol) || "https".equals(a_sProtocol)) ) )	{			
			m_urlact = new SSLURLAction(a_login, a_password, m_sURLAddr, false, bDereferenceDNS);	
		} else	{
			m_urlact = new URLAction(a_login, a_password, a_sProtocol, method, m_sURLAddr, false, bDereferenceDNS);
		}				
	}

	
	public String GetURLAddress()	{
		return m_sURLAddr;		
	}
	
	public WCResponse GetURLContent() throws Exception {
		m_resp = new WCResponse(m_urlact.getURLContent());
		
		return m_resp; 
	}	
	
	
	// some functions from WCResponse, for calling from LS
	public String queryURLContent() throws Exception {
		m_resp = this.GetURLContent();
		
		return m_resp.GetRespText(); 
	}
	
	public String getResponseText() throws Exception 		{ return (m_resp != null)?m_resp.GetRespText():""; }
	public String getResponseError() throws Exception 		{ return (m_resp != null)?m_resp.GetErrText():""; }
	public int getResponseErrorCode() throws Exception 		{ return (m_resp != null)?(int)m_resp.GetErrCode():-1; }
	public int getResponseRC() throws Exception 			{ return (m_resp != null)?(int)m_resp.GetRespCode():-1; }
	
}

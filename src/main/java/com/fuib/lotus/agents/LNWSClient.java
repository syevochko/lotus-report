package com.fuib.lotus.agents;

import java.util.HashMap;
import java.util.Vector;

import com.fuib.lotus.LNEnvironment;
import com.fuib.lotus.log.LNDbLog;
import com.fuib.lotus.ws.WCResponse;

import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import lotus.domino.axis.transport.http.HTTPConstants;

/**
 * Старый класс
 * TODO: унифицировать с классом WFAgent_WSClient!
 * 		Установка LTPA_Token здесь ИСПОЛЬЗУЕТСЯ!
 * 		Т.е. в конфигах для классов-наследников в типе аутентификации необходимо выставить "sso&ltpa"!
 */
public abstract class LNWSClient extends LNHTTPAgent {
	public static final String PROFILE_FORM = "WSClient";
	public static final String ITEM_ENDPOINT = "fdWSEP";
	public static final String ITEM_ISAUTH = "fdIsAuth";
	public static final String ITEM_AUTHTYPE = "fdAuthType";
	public static final String ITEM_LOGIN = "UserID";
	public static final String ITEM_PASSWORD = "Password";
	public static final String ITEM_LOGEXPIRED = "fdnLogExpired";
	
	public static final String ITEM_PARAM = "fdParam";
	public static final String ITEM_PARAMVAL = "fdParamVal";
	public static final String PARAM_LTPA_TOKEN = "LTPA_Token";
	
	public static final String AUTH_SESSION = "sso";
	public static final String AUTH_BASIC = "basic";
	
	private final String CREDENTIALS_VIEWNAME = "credentials";
	private final String ITEM_KEY_CREDENTIAL = "fdCredential";
	
	protected WCResponse m_resp = null;
	protected HashMap<String, Object> m_mapConfig = new HashMap<String, Object>();
	protected String m_sCfgProfileName = null;
	
	protected void main() throws NotesException, Exception, Throwable {
		m_resp = new WCResponse("");
		if (m_agentResp == null) m_agentResp = m_resp;
		
		try {			
			loadConfiguration();
			
			setCustomLog(LNDbLog.LOGTYPE_ENTRY, m_mapConfig.get(ITEM_LOGEXPIRED));
			
			m_CGI.parseQueryString();
						
			ws_call();									// entry point for child classes
		}
		catch (NotesException ne) {
			m_resp.SetErrCode(ne.id);
			m_resp.SetErrText(ne.text);			
			ne.printStackTrace();
			try {
				super.response();
			} catch (Exception err) {
				err.printStackTrace();
				logAction("Возникла ошибка при попытке передачи сообщения об ошибке");
			}
		}
		catch (Throwable ex) {
			m_resp.SetErrText(ex.toString());
			ex.printStackTrace();
			try {
				super.response();
			} catch (Exception err) {
				err.printStackTrace();
				logAction("Возникла ошибка при попытке передачи сообщения об ошибке");
			}
		} finally {
			this.logAction(m_resp.GetResult());
		}
	}
	
	protected void wsclient_init(lotus.domino.websvc.client.Stub ws_stub) throws Exception {
		String sEP = (String) m_mapConfig.get(ITEM_ENDPOINT);
		
		if (sEP != null && sEP.length() > 0)
			ws_stub.setEndpoint(sEP);
			
		String sIsAuth = (String) m_mapConfig.get(ITEM_ISAUTH);
		
		if (sIsAuth != null && sIsAuth.equals("1")) {
			if (((String) m_mapConfig.get(ITEM_AUTHTYPE)).contains(AUTH_SESSION)) {	// session-based authentication			
		        ws_stub._setProperty(lotus.domino.websvc.client.Stub.SESSION_MAINTAIN_PROPERTY, Boolean.TRUE);	// support SSO
		        
		        ws_stub._setProperty(HTTPConstants.HEADER_COOKIE, m_CGI.getLTPAToken((String) m_mapConfig.get(PARAM_LTPA_TOKEN)));
			}
			else 														// authentication based on login/password
				ws_stub.setCredentials((String) m_mapConfig.get(ITEM_LOGIN),
										(String) m_mapConfig.get(ITEM_PASSWORD));
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void loadConfiguration() throws Exception {	
		String sProfileName = ((m_sCfgProfileName != null) ? m_sCfgProfileName : this.m_sAgName);
		
		View viewLookup = m_env.getView(m_env.getDbConfig(), LNEnvironment.FUIBCONFIG_LOOKUPVIEWNAME);		
		
		Document docConfig = viewLookup.getDocumentByKey(PROFILE_FORM + "#" + sProfileName); 
		if (docConfig == null)
			throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found config document for web-service: " + sProfileName);
		
		// read from profile document configuration data				
		m_mapConfig.put(ITEM_ENDPOINT, null);
		m_mapConfig.put(ITEM_ISAUTH, null);
		m_mapConfig.put(ITEM_AUTHTYPE, null);
		m_mapConfig.put(ITEM_LOGEXPIRED, null);		
		m_mapConfig.put(ITEM_KEY_CREDENTIAL, null);
		m_mapConfig.put(ITEM_PARAM, null);
		m_mapConfig.put(ITEM_PARAMVAL, null);
		
		super.initFromDocument(docConfig, m_mapConfig);
		
		// try to get parameter association for ITEM_PARAM (keys) and ITEM_PARAMVAL (values)
		if (m_mapConfig.get(ITEM_PARAM) != null && m_mapConfig.get(ITEM_PARAMVAL) != null) {
			if (m_mapConfig.get(ITEM_PARAM) instanceof Vector && m_mapConfig.get(ITEM_PARAMVAL) instanceof Vector) {
				Vector<String> keys = (Vector<String>) m_mapConfig.get(ITEM_PARAM);
				Vector<Object> vals = (Vector<Object>) m_mapConfig.get(ITEM_PARAMVAL);
				if (keys.size() == vals.size())	{
					for (int i = 0; i < keys.size(); i++) {
						m_mapConfig.put(keys.get(i), vals.get(i));
					}
				}
			}
			else if (m_mapConfig.get(ITEM_PARAM) instanceof String && m_mapConfig.get(ITEM_PARAMVAL) instanceof String)	{
				m_mapConfig.put((String) m_mapConfig.get(ITEM_PARAM), m_mapConfig.get(ITEM_PARAMVAL));
			}
		}
		
		m_mapConfig.put(ITEM_LOGIN, null);
		m_mapConfig.put(ITEM_PASSWORD, null);
		
		if (((String) m_mapConfig.get(ITEM_ISAUTH)).equals("1") && 
				((String) m_mapConfig.get(ITEM_AUTHTYPE)).equals(AUTH_BASIC) &&
				!((String) m_mapConfig.get(ITEM_KEY_CREDENTIAL)).isEmpty()) {
			View viewCredentials = m_env.getView(m_env.getDbConfig(), CREDENTIALS_VIEWNAME);
			
			Document docCredential = viewCredentials.getDocumentByKey((String)m_mapConfig.get(ITEM_KEY_CREDENTIAL), true);
			if (docCredential == null)
				throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found credentials document for web-service: " + sProfileName);
			
			m_mapConfig.put(ITEM_LOGIN, docCredential.getItemValueString(ITEM_LOGIN));
			m_mapConfig.put(ITEM_PASSWORD, docCredential.getItemValueString(ITEM_PASSWORD));
		}
		
		logDebug("Configuration parameters are: ");
//		change value of password to its lengths 
		HashMap<String, Object> tmpMap = new HashMap<String, Object>();
		tmpMap.putAll(m_mapConfig);
		if (tmpMap.get(ITEM_PASSWORD) != null)
			tmpMap.put(ITEM_PASSWORD, String.valueOf(((String)tmpMap.get(ITEM_PASSWORD)).length()));		
		logDebug(tmpMap);
	}
	
	
	/*
	 * First lines of this function MUST be a creation a actual Web service object
	 * and initializing by calling wsclient_init(lotus.domino.websvc.client.Stub ws_stub)  
	 */
	abstract protected void ws_call() throws Exception;
}

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
	
	public final static String COOKIE_LTPA = "LtpaToken=";
	public static final String AUTH_SESSION = "sso";
	public static final String AUTH_BASIC = "basic";
	
	private final String CREDENTIALS_VIEWNAME = "credentials";
	private final String ITEM_KEY_CREDENTIAL = "fdCredential";
	
	private lotus.domino.websvc.client.Stub m_ws_stub = null;
	
	protected WCResponse m_resp = null;
	protected LNEnvironment m_env=null; 
	protected HashMap m_mapConfig = new HashMap();
	protected String m_sCfgProfileName = null;
	
	public String m_sDebugLTPA = null;

	protected void main() throws NotesException, Exception, Throwable {
		super.main();
		
		m_resp = new WCResponse("");
		if ( m_agentResp == null)  m_agentResp = m_resp; 
		try {			
			m_env = new LNEnvironment(m_session);
			
			loadConfiguration();
			
			// init custom log if required
			if ( m_sLogCategory != null ) {
				LNDbLog log = new LNDbLog(m_sLogCategory, m_sAgName);
				
				log.open((m_sLogDb != null)?m_env.getDatabase(m_sLogDb):m_dbCurrent, LNDbLog.LOGTYPE_APPEND);
				log.setProperty(LNDbLog.PROP_EXPIRED, m_mapConfig.get(ITEM_LOGEXPIRED));

				setCustomLog(log);
			}
			
			setLogOption(true, (m_sLogCategory!=null && m_sLogDb != null), false, false);						
			
			ParseQueryString();
						
			ws_call();									// entry point for child classes

		} catch (NotesException ne)	{
			m_resp.SetErrCode(ne.id);
			m_resp.SetErrText(ne.text);			
			ne.printStackTrace();
			try {
				super.response();
			} catch (Exception err) {
				err.printStackTrace();
				logAction("Возникла ошибка при попытке передачи сообщения об ошибке");
			}
		} catch (Throwable ex) {
			m_resp.SetErrText(ex.toString());
			ex.printStackTrace();
			try {
				super.response();
			} catch (Exception err) {
				err.printStackTrace();
				logAction("Возникла ошибка при попытке передачи сообщения об ошибке");
			}
		} finally	{
			this.logAction(m_resp.GetResult());
			m_ws_stub = null;
		}
	}
	
	protected void wsclient_init(lotus.domino.websvc.client.Stub ws_stub) throws Exception {
		String sEP = (String) m_mapConfig.get(ITEM_ENDPOINT);
		
		m_ws_stub = ws_stub; 
		
		if ( sEP != null && sEP.length() > 0 )
			ws_stub.setEndpoint(sEP);
			
		String sIsAuth = (String) m_mapConfig.get(ITEM_ISAUTH);
		
		if ( sIsAuth != null && sIsAuth.equals("1") ) {
			if ( m_mapConfig.get(ITEM_AUTHTYPE).equals(AUTH_SESSION) ) {	// session-based authentication			
		          ws_stub._setProperty(
		        		  lotus.domino.websvc.client.Stub.SESSION_MAINTAIN_PROPERTY, 
		        		  Boolean.TRUE);	// support SSO
		          
		          ws_stub._setProperty(
		        		  HTTPConstants.HEADER_COOKIE, 
		        		  getLTPAToken());
				
			} else 														// authentication based on login/password
				ws_stub.setCredentials((String) m_mapConfig.get(ITEM_LOGIN), 
										(String) m_mapConfig.get(ITEM_PASSWORD));
		}

	}
	
	protected void loadConfiguration() throws Exception {	
		String sProfileName = ((m_sCfgProfileName != null) ? m_sCfgProfileName : this.m_sAgName);
		
		View viewLookup = m_env.getDbView(m_env.getFUIBConfigDB(), LNEnvironment.FUIBCONFIG_LOOKUPVIEWNAME);		
		if ( viewLookup == null )
			throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found view " + LNEnvironment.FUIBCONFIG_LOOKUPVIEWNAME + " in database " + 
					m_env.getFUIBConfigDB().getServer() + "!!" + m_env.getFUIBConfigDB().getFilePath());
		
		Document docConfig =viewLookup.getDocumentByKey(PROFILE_FORM + "#" + sProfileName); 
		if ( docConfig == null )
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
		if (m_mapConfig.get(ITEM_PARAM)!=null && m_mapConfig.get(ITEM_PARAMVAL)!=null)	{
			if (m_mapConfig.get(ITEM_PARAM) instanceof Vector && m_mapConfig.get(ITEM_PARAMVAL) instanceof Vector)	{
				Vector keys = (Vector)m_mapConfig.get(ITEM_PARAM);
				Vector vals = (Vector)m_mapConfig.get(ITEM_PARAMVAL);
				if (keys.size() == vals.size())	{
					for (int i=0; i < keys.size(); i++)	{
						m_mapConfig.put(keys.get(i), vals.get(i));
					}
				}
				
			} else if (m_mapConfig.get(ITEM_PARAM) instanceof String && m_mapConfig.get(ITEM_PARAMVAL) instanceof String)	{
				m_mapConfig.put(m_mapConfig.get(ITEM_PARAM), m_mapConfig.get(ITEM_PARAMVAL));
			}
		}

		m_mapConfig.put(ITEM_LOGIN, null);
		m_mapConfig.put(ITEM_PASSWORD, null);
		
		if ( !((String)m_mapConfig.get(ITEM_ISAUTH)).equals("") && 
				((String)m_mapConfig.get(ITEM_AUTHTYPE)).equals(AUTH_BASIC) &&
				!((String)m_mapConfig.get(ITEM_KEY_CREDENTIAL)).equals("") )	{
			View viewCredentials = m_env.getDbView(m_env.getFUIBConfigDB(), CREDENTIALS_VIEWNAME);
			if ( viewCredentials == null )
				throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found view " + CREDENTIALS_VIEWNAME + " in database " + 
						m_env.getFUIBConfigDB().getServer() + "!!" + m_env.getFUIBConfigDB().getFilePath());

			Document docCredential = viewCredentials.getDocumentByKey( (String)m_mapConfig.get(ITEM_KEY_CREDENTIAL), true);
			if ( docCredential == null )
				throw new NotesException(LNEnvironment.ERR_CUSTOM, "Not found credentials document for web-service: " + sProfileName);
			
			m_mapConfig.put(ITEM_LOGIN, docCredential.getItemValueString(ITEM_LOGIN));
			m_mapConfig.put(ITEM_PASSWORD, docCredential.getItemValueString(ITEM_PASSWORD));
		}
		
				
		logDebug("Configuration parameters are: ");
//		change value of password to its lengths 
		HashMap tmpMap = new HashMap();
		tmpMap.putAll(m_mapConfig);
		if (tmpMap.get(ITEM_PASSWORD)!=null)
			tmpMap.put(ITEM_PASSWORD, String.valueOf(((String)tmpMap.get(ITEM_PASSWORD)).length()));		
		logDebug(tmpMap);
	}
	
	
	
	private String getLTPAToken() throws Exception {
		String sLTPA = null;
		
		if ( m_sDebugLTPA != null && m_sDebugLTPA.length() > 0 ) {
			sLTPA = m_sDebugLTPA;
			
			logDebug("LTPA token found (set directly): " + sLTPA);
		} else {
			String sCookie = getCGI(CGI_COOKIE);
			
	        int nPosStart = sCookie.indexOf(COOKIE_LTPA);
	        int nPosEnd = (nPosStart != -1)?sCookie.indexOf(";", nPosStart):0;
	                
	        sLTPA = (nPosStart != -1)?
	        		sCookie.substring(nPosStart + COOKIE_LTPA.length(), (nPosEnd != -1)?nPosEnd:sCookie.length()):
	            	""; 
	        
	        logDebug("LTPA token found (from cookie): " + sLTPA);
		}
	
        return  COOKIE_LTPA + sLTPA;       
	}
	
	
	/*
	 * First lines of this function MUST be a creation a actual Web service object
	 * and initializing by calling wsclient_init(lotus.domino.websvc.client.Stub ws_stub)  
	 */
	abstract protected void ws_call() throws Exception;
}

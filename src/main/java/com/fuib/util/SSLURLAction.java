package com.fuib.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

/**
 * Class for get content from https conncetion.<br>
 * Class supports https connections, GET request methods, basic and SSO authentication.<br>
 * Also supports dereferencing of host dns (by super class)
 * @see com.fuib.util.URLAction
 * @see #SSLURLAction(String, String, String, boolean)
 * @author evochko
 * после введени€ класса MyURLConnection этот класс не нужен - защищенные соедиени€ может обрабатывать и URLAction 
 * этот класс осталс€ только дл€ совместимости с имеющимс€ кодом
 * @param bDereferenceDNS - –азыменование хоста - примен€лось дл€ получени€ доступных хостов за DNS-алиасом. Ѕыло актуально до внедрени€ аппаратных балансировщиков 
 * 		здесь всегда устанавливать в false.  дело в том, что наши хост hub-domino-web разыменовываетс€ в URLAction во вс€кую ахинею - хосты, которые не соответствуют указанным в сертификате. ѕочему так происходит - надо разбиратьс€
 *  
 */
public class SSLURLAction extends URLAction {	

	public SSLURLAction() {}
	
	public SSLURLAction(String sURL) throws MalformedURLException, IOException {
		this(sURL, false);
	}

	public SSLURLAction(String sURL, boolean bCached) throws MalformedURLException, IOException {
		this(null, null, sURL, bCached, false);
	}

	public SSLURLAction(String sURL, boolean bCached, String sSSOToken) throws MalformedURLException, IOException {
		super(sURL, bCached, sSSOToken);
	}

	public SSLURLAction(String login, String passw, String a_sURL, boolean bCached, boolean bDereferenceDNS) throws MalformedURLException, IOException {
		super(login, passw, URLAction.HTTPS, URLAction.METHOD_GET, a_sURL, bCached, false);				
	}

	public static String static_getURLContentSilent(String login, String passw, String sURL, boolean bCached, boolean bDereferenceDNS) {
		String sContent = null;
		
		try {
			SSLURLAction action = new SSLURLAction(login, passw, sURL, bCached, false);
			action.setHaltHTTPCode(HttpURLConnection.HTTP_NOT_FOUND);
			
			sContent = action.getURLContent();
		} catch (IOException e) {
			e.printStackTrace();
			sContent = "";
		}
		
		return sContent;
	}
	
	public static String static_getURLContent(String login, String passw, String sURL, boolean bCached, boolean bDereferenceDNS) throws MalformedURLException, IOException {
		return (new SSLURLAction(login, passw, sURL, bCached, bDereferenceDNS)).getURLContent();
	}
	

}	// end of class

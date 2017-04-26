package com.fuib.lotus.ws;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fuib.lotus.log.LogEx;

import lotus.domino.Document;
import lotus.domino.NotesException;


public class LNCGI {
	// some of the LNCGI variables. Complete list see 
	// Notes:///8525704A00561E32/855DC7FCFD5FEC9A85256B870069C0AB/784B011BAD9C15F68525704A003F67A9
	public static final String VAR_REMOTE_IP = "Remote_Addr";
	public static final String VAR_QUERYSTRING = "Query_String";
	public static final String VAR_QUERYSTRING_DECODED = "Query_String_Decoded";
	public static final String VAR_REMOTE_HOST = "Remote_Host";
	public static final String VAR_AUTH_IP = "Remote_Addr";
	public static final String VAR_AUTH_USER = "Remote_User";	
	public static final String VAR_HTTPS = "HTTPS";
	public static final String VAR_COOKIE = "HTTP_Cookie";
	
	// Параметры строки QUERY_PARAM
	public static final String QP_SEP = "&";
	public static final String QP_COOKIE_LTPA = "LtpaToken=";
	String m_sTokenLTPA = null;
	
	public static final String ENC_CP1251 = "CP1251";
	public static final String ENC_DEFAULT = ENC_CP1251;
	
	private HashMap<String, String> m_cgi = new HashMap<String, String>();
	private HashMap<String, String> m_param = null;
	
	private Document m_ndContext = null;
	
	
	public LNCGI(Document ndContext) {
		m_ndContext = ndContext;
	}
	
	
	/**
	 * Метод для считывания CGI-переменной по её имени;
	 * полученные значения кэшируются
	 */
	public String getVariable(String sVarName) throws NotesException {
		if (!m_cgi.containsKey(sVarName))
			m_cgi.put(sVarName, m_ndContext.getItemValueString(sVarName));
		return m_cgi.get(sVarName);
	}
	
	
	public String getAuthUser() throws NotesException {
		return getVariable(VAR_AUTH_USER);
	}
	
	
	public boolean isSSL() throws NotesException {
		return getVariable(VAR_HTTPS).equalsIgnoreCase("ON");
	}
	
	
	/**
	 * Разбор строки параметров, используя кодировку по умолчанию (CP1251)
	 */
	public void parseQueryString() throws NotesException, UnsupportedEncodingException {
		parseQueryString(null);
	}
	
	/**
	 * Parse query string (f.e. openagent&param0&param1=value1&param2=value2)to table of parameters
	 * param0 = null, param1=value1, param2=value2, ...
	 */
	public void parseQueryString(String sEncoding) throws NotesException, UnsupportedEncodingException {
		if (m_ndContext != null && m_param == null) {
			m_param = new HashMap<String, String>();
			
			if (sEncoding == null || sEncoding.isEmpty()) sEncoding = ENC_DEFAULT;
			
			String sQueryString = URLDecoder.decode(m_ndContext.getItemValueString(VAR_QUERYSTRING), sEncoding);
			
			//TODO: добавить логирование параметров. Сделать это непросто, придётся делать внешний общий класс для агентов и web-сервисов
			
			if (sQueryString.length() > 0) {
				String sPrevKey = null;
				Pattern PARSE_PATTERN = Pattern.compile("([\\w ]+)=(.+)");

				String[] arrStr = sQueryString.split(QP_SEP);
				for (int i = 0; i < arrStr.length; i++)	{
					String s1 = arrStr[i];
					if (s1.indexOf("=") != -1) {
						Matcher m1 = PARSE_PATTERN.matcher(s1);
						if (m1.find()) {
							sPrevKey = m1.group(1);
							m_param.put(sPrevKey, m1.group(2));
						}
						else {			// key without value, exampe: &key1=& 
							sPrevKey = s1.substring(0, s1.indexOf("="));
							if (!sPrevKey.isEmpty()) m_param.put(sPrevKey, "");	//Ignore value with empty key
						}
					}
					else if (sPrevKey != null && !sPrevKey.isEmpty()) {			// this is part of previous value, so we append value to previous element  
						m_param.put(sPrevKey, m_param.get(sPrevKey) + QP_SEP + s1);
					}
				}
			}
		}
	}
	
	/**
	 * @return true, если QueryString при вызове агента было задано
	 */
	public boolean isQueryStringExist() {
		return (m_param != null);
	}
	
	public HashMap<String, String> getQueryStringParameters() {
		return m_param;
	}
	
	/**
	 * @return строковое значение параметра из QueryString
	 */
	public String getQueryStringParameter(String sParamName) {
		if (m_param != null)
			return m_param.get(sParamName);
		return null;
	}
	
	/**
	 * Получение LTPA-токена;
	 * @param - sDefault нужен, чтобы подставлять токен для отладки, полученный из параметров запуска
	 */
	public String getLTPAToken(String sDefault) throws Exception {
		String sLTPA = null;
		
		String sCookie = getVariable(VAR_COOKIE);
		if (sCookie != null && !sCookie.isEmpty()) {
			int nPosStart = sCookie.indexOf(QP_COOKIE_LTPA);
	        int nPosEnd = (nPosStart != -1) ? sCookie.indexOf(";", nPosStart) : 0;
	        
	        sLTPA = (nPosStart != -1)?
	        		sCookie.substring(nPosStart + QP_COOKIE_LTPA.length(), (nPosEnd != -1) ? nPosEnd : sCookie.length()) : ""; 
	        
//			logDebug("LTPA token found (from cookie): " + sLTPA);
		}
		
        if (sLTPA == null) sLTPA = sDefault;
        if (sLTPA != null) sLTPA = QP_COOKIE_LTPA + sLTPA;
        
        return sLTPA;
	}
	
	
	/**
	 * Проверка параметров вызова web-сервиса
	 * @param vAllow - Vector, который содержит перечень определённых значений, вычитанных из конфигурации сервиса
	 * @param sVarName - имя CGI-параметра web-сервиса
	 * @param sVarDescr - описание параметра
	 * @param bCheckRequired - обязательна ли проверка или нет (если да, то будут генерироваться ошибки при отсутствии значений из конфигурации)
	 * @return
	 * @throws NotesException 
	 */
	public void checkVariable(Vector<String> vAllow, String sVarName, String sVarDescr, boolean bCheckRequired) throws NotesException {
		if (vAllow != null && !vAllow.isEmpty()) {
			String sReal = getVariable(sVarName);
			if (sReal != null) {
				boolean bAllow = false;
				for (int i = 0; i < vAllow.size(); i++) {
					if (sReal.equals(vAllow.get(i))) {
						bAllow = true;
						break;
					}
				}
				if (!bAllow)
					throw new NotesException(LogEx.ERRc1223, "Попытка вызова сервиса с параметром " + sVarDescr + "=<" + sReal + ">, не входящим в список разрешённых!");
			}
			else
				throw new NotesException(LogEx.ERRc1111, "Параметр <" + sVarName + "> сервиса не смог быть получен!");
		}
		else
			if (bCheckRequired) throw new NotesException(LogEx.ERRc1111, "Список разрешённых значений для параметра " + sVarName + " не указан в конфигурации cервиса!");
	}
	
}

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
 * ����� �������� ������ MyURLConnection ���� ����� �� ����� - ���������� ��������� ����� ������������ � URLAction 
 * ���� ����� ������� ������ ��� ������������� � ��������� �����
 * @param bDereferenceDNS - ������������� ����� - ����������� ��� ��������� ��������� ������ �� DNS-�������. ���� ��������� �� ��������� ���������� ��������������� 
 * 		����� ������ ������������� � false.  ���� � ���, ��� ���� ���� hub-domino-web ���������������� � URLAction �� ������ ������ - �����, ������� �� ������������� ��������� � �����������. ������ ��� ���������� - ���� �����������
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

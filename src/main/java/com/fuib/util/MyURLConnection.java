/**
 * 
 */
package com.fuib.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.fuib.lotus.log.LogEx;

import sun.misc.BASE64Encoder;

/**
 * @author evochko
 * Говнокласс - обертка для внедрения поддержки в класс URLAction библиотеки Apache HttpClient
 */
public class MyURLConnection {

	private boolean bUseCaches = false;
	private String sRequestMethod = "GET";
	private String login = null;
	private String passwd = null;
	private HashMap<String, String> headerProperties = new HashMap<String, String>();
	private Proxy proxy = null;
	private URL url = null;
	private int nLastResponseCode = 0;
	private boolean debugOn = false; 

	public MyURLConnection(URL url, Proxy proxy) throws IOException	{
		this.url = url;
		this.proxy = proxy;
	}

	
	public String getLogin() { return login; }

	public void setLogin(String login) { this.login = login; }
	
	protected String getPasswd() { return passwd; }

	public void setPasswd(String passwd) { 	this.passwd = passwd; }
	
	public HashMap<String, String> getHeaderProperties() { return headerProperties; }

	public void setHeaderProperties(HashMap<String, String> headerProperties) { this.headerProperties = headerProperties; }
	
	public URL getUrl() { return url; }

	public void setUrl(URL url) { this.url = url; }	

	public Proxy getProxy() { return proxy; }

	public void setProxy(Proxy proxy) { this.proxy = proxy; }

	public boolean isDebugOn() { return debugOn; }

	public void setDebugOn(boolean debugOn) { this.debugOn = debugOn; }

	public void setUseCaches(boolean bCached)	{ bUseCaches = bCached; }

	public String getRequestMethod() 	{ return sRequestMethod; }

	public void setRequestMethod(String method) 	{ sRequestMethod = method; }

	public void setBasicAuthCredentials( String login, String passw)	{
		this.login = login;
		this.passwd = passw;
	}	

	public void setHTTPHeaderProperty(String sPropName, String sPropValue) {
		headerProperties.put(sPropName, sPropValue);
	}

	public int getLastResponseCode()	{ return nLastResponseCode; }
	
	public void setLastResponseCode(int lastResponseCode) { nLastResponseCode = lastResponseCode; }

	public String getContent() throws IOException {
		HttpURLConnection urlCon = null;
		InputStream streamInput = null;
		String sRetContent = null;
		
		urlCon = getConnection(url, proxy);
		urlCon.setUseCaches(bUseCaches);
		urlCon.setRequestMethod(getRequestMethod());
		
		if (login!=null && !login.equals("") && passwd!=null && !passwd.equals(""))	{
			String credentials = login + ":" + passwd;
			String headerProperty = "Basic "+ new BASE64Encoder().encode(credentials.getBytes());
			setHTTPHeaderProperty("Authorization", headerProperty);			
		} 		
		
		if (headerProperties!=null && !headerProperties.isEmpty()) {
			for (Iterator<String> it = headerProperties.keySet().iterator(); it.hasNext(); ) {
				String key = it.next();
				String val = headerProperties.get(key); 
				if (val != null) {
					urlCon.setRequestProperty(key, val);
				}
			}
		}

		try { 
			streamInput = urlCon.getInputStream();
			nLastResponseCode = urlCon.getResponseCode();
		}
		catch (IOException ioe) {
			if (isDebugOn()) {
				System.err.println(LogEx.getErrInfo(ioe, false));
			}
			
			try {
				nLastResponseCode = urlCon.getResponseCode();
			}
			catch (Exception e) {
				System.err.println(LogEx.getMessage(e));
			}
			
			if (isDebugOn()) {
				System.err.println(this.getClass().getSimpleName() + ".getContent: HTTP rc is: " + nLastResponseCode);
			}
			
			urlCon.disconnect();
			throw new IOException(ioe);
		}			
		
		if (streamInput != null) {
			BufferedReader inputData = new BufferedReader(new InputStreamReader(streamInput));
			StringWriter outData = new StringWriter();
			String sLine; 
			
			while ((sLine = inputData.readLine()) != null)
				outData.write(sLine);
			
			inputData.close();
			sRetContent = outData.toString();
		}

		if (urlCon != null)
			urlCon.disconnect();		

		return sRetContent;
	}

	// create connection
	private HttpURLConnection getConnection(URL url, Proxy proxy) throws IOException {
		HttpURLConnection urlCon = null;

		if ("https".equals(url.getProtocol())) {
			return getSecuredConnection(url, proxy);
		}
		else {
			if (proxy != null) {
				urlCon = (HttpURLConnection) this.getUrl().openConnection(proxy);
				if (isDebugOn())
					System.out.println(this.getClass().getSimpleName() + ": connection to " + urlCon.getURL().toString() + " opened via proxy " + proxy.toString());
			}
			else {
				urlCon = (HttpURLConnection) this.getUrl().openConnection();
				if (isDebugOn())	
					System.out.println(this.getClass().getSimpleName() + ": connection to " + urlCon.getURL().toString() + " opened");
			}
			return urlCon;
		}		
	}

	private HttpsURLConnection getSecuredConnection(URL url, Proxy proxy) throws IOException	{
		HttpsURLConnection urlCon = null;

		if (proxy!=null)	{
			urlCon = (HttpsURLConnection)this.getUrl().openConnection(proxy);
			if (isDebugOn()) 
				System.out.println("getURLConnectionContent: connection to " + urlCon.getURL().toString() + " opened via proxy " + proxy.toString());

		} else 	{
			urlCon = (HttpsURLConnection)this.getUrl().openConnection();
			if (isDebugOn())	
				System.out.println("getURLConnectionContent: connection to " + urlCon.getURL().toString() + " opened");
		}		

		//			new trust manager trusts all certificates
		TrustManager[] trustAllCert = new TrustManager[] {
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}
					public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}
					public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}
				}
		};

		try	{
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCert, new java.security.SecureRandom());            
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());				
		}
		catch (Exception e)	{
			System.err.println(LogEx.getMessage(e));
			throw new IOException(LogEx.getMessage(e));
		}

		//			now set HostnameVerifier for current connection - allow connection to all hosts.
		//			!Lotus client doesn't allow HttpsURLConnection.setDefaultHostnameVerifier
		urlCon.setHostnameVerifier(new HostnameVerifier() { 
			public boolean verify(String string, SSLSession sSLSession) { return true; }
		}
		);

		return urlCon;
	}
}

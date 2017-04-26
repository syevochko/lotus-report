package com.fuib.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Hashtable;
import java.util.Vector;

import com.fuib.lotus.log.LogEx;

import sun.misc.BASE64Encoder;

public class URLAction {
	public static int METHOD_GET = 1; 
	public static int METHOD_POST = 2;
	public static String HTTP = "http://";
	public static String HTTPS = "https://";
	public static final String HEADER_PROP_COOKIE = "Cookie";
	public static final int CACHE_LENGTH=3;
	public static final String HEADER_PROP_AUTH = "Authorization";
	private static String m_sProxyHost = null;
	private static int m_nProxyPort = 0;

	protected MyURLConnection m_urlCon = null;
	protected String m_sHEADER_PROP_AUTH = null;
	protected boolean mDereferenceDNS = true;

	private static Hashtable<String, InetAddress> hashCache = null;				// static cache for success accessed hosts @see setToCache
	private static boolean m_bIsDebug = false;
	private String m_URI =null;
	private InetAddress[] m_vHost = null;	
	private String m_sOrigHost = null;
	private Vector<Integer> m_vHTTPCode_Halt = new Vector<Integer>();


	//	set of constructors
	public URLAction() {}
	public URLAction(String sURL) throws MalformedURLException, IOException 	{ this(sURL, false); }	
	public URLAction(String sURL, boolean bCached) throws MalformedURLException, IOException {
		this(null, null, null, METHOD_GET, sURL, bCached);
	}

	public URLAction(String sURL, boolean bCached, String sSSOToken) throws MalformedURLException, IOException {
		this(null, null, null, METHOD_GET, sURL, bCached);		

		if ( sSSOToken != null )
			setHTTPHeaderProperty(HEADER_PROP_COOKIE, "LtpaToken=" + sSSOToken);
	}

	public URLAction(String login, String passw, String a_sProtocol, int nMethod, String a_sURL, boolean bCached) throws MalformedURLException, IOException {
		this(login, passw, a_sProtocol, nMethod, a_sURL, bCached, true);
	}

	public URLAction(String login, String passw, String a_sProtocol, int nMethod, String a_sURL, boolean bCached, boolean bDereferenceDNS) throws MalformedURLException, IOException {
		URL url = null;
		int ind = 0;

		if (hashCache==null)
			hashCache = new Hashtable<String, InetAddress>();

		String sURLAddr = a_sURL;
		String sProtocol = (a_sProtocol != null && !"".equals(a_sProtocol))?a_sProtocol.toLowerCase():((new URL(sURLAddr)).getProtocol()+"://");

		if (HTTPS.startsWith(sProtocol))
			mDereferenceDNS = false;			// see SSLURLAction description
		else
			mDereferenceDNS = bDereferenceDNS;


		// подменяем при необходимости протокол в a_sURLAddr на указанный a_sProtocol  - вообще это бредятина, потому что как правило нигде явно протокол не передается - только URL - надо бы убрать - но это надо проверить использование кода
		if (!sProtocol.equals("") && !sURLAddr.toLowerCase().startsWith(sProtocol))	{
			ind = sURLAddr.indexOf("://");
			sURLAddr = sProtocol + ((ind==-1)? sURLAddr : sURLAddr.substring(ind+3));
		}

		//		 dereference host name of target url
		URL origURL = new URL(sURLAddr);
		m_sOrigHost = origURL.getHost();

		m_vHost = InetAddress.getAllByName(m_sOrigHost);	// return InetAddress[] - an array of IP addresses and host names, based on m_sOrigHost

		InetAddress inTmp = (InetAddress) hashCache.get(m_sOrigHost);
		if (inTmp!=null)				// try to use hostname from cache 
		{
			Vector vTmp = new Vector();
			for (int i = 0; i < m_vHost.length; i++) {
				vTmp.add(m_vHost[i]);
			}
			vTmp.remove(inTmp);
			vTmp.add(0, inTmp);

			m_vHost = (InetAddress[]) vTmp.toArray(new InetAddress[0]);
		}

		//		replace host name by dereferenced host name
		if ( mDereferenceDNS )	{
			url = new URL(	origURL.getProtocol(), 
					InetAddress.getByName(m_vHost[0].getHostAddress()).getHostName(), 
					origURL.getPort(), 
					origURL.getFile());	
		} else
			url = origURL; 


		// create connection object 
		m_urlCon = openConnection(url);			
		m_urlCon.setUseCaches(bCached);

		if ( nMethod == METHOD_GET || nMethod == METHOD_POST )
			m_urlCon.setRequestMethod((nMethod == METHOD_GET)?"GET":"POST");

		// передаем логин и пароль через http-заголовок - GET запрос
		this.setBasicAuthCredentials( login, passw);

	}

	private MyURLConnection openConnection(URL url) throws IOException {
		MyURLConnection urlCon = null;

		try {
			urlCon = new HttpClientWrapper(url, null);
			if (isDebug()) {
				System.out.println("get url conneection by apache httpclient...");
			}
		} finally {
			if (urlCon == null)	{
				urlCon = new MyURLConnection(url, null);
			}
		}

		if ( m_sProxyHost != null ) {
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(m_sProxyHost, m_nProxyPort));
			urlCon.setProxy(proxy);
		}

		return urlCon;
	}

	synchronized protected  void  setToCache(String sKey, InetAddress inRet ) {

		hashCache.put(sKey, inRet);

		if (hashCache.size()>CACHE_LENGTH)
		{hashCache.clear();
		hashCache.put(sKey, inRet);}
	}


	public void setBasicAuthCredentials( String login, String passw)	{
		if (m_urlCon!=null)	{
			if (login!=null && !login.equals("") && passw!=null && !passw.equals(""))	{
				String credentials = login + ":" + passw;
				m_sHEADER_PROP_AUTH = "Basic "+ new BASE64Encoder().encode(credentials.getBytes());
				setHTTPHeaderProperty(HEADER_PROP_AUTH, m_sHEADER_PROP_AUTH);
			} 
		} else	{
			System.err.println("Connection object not yet defined!");
		}
	}


	public void reconnect2URL(URL url) throws IOException {
		if ( m_urlCon != null ) {
			m_urlCon.setUrl(url);
			System.out.println(this.getClass().getSimpleName() + ".reconnect2URL: " + url.toString());
		}		
	}


	public void reconnect2URL(String sHost) throws IOException {
		if ( m_urlCon != null ) {
			URL oldURL = m_urlCon.getUrl(); 
			URL newURL = new URL(oldURL.getProtocol(), sHost, oldURL.getPort(), oldURL.getFile());

			reconnect2URL(newURL);
		}
	}


	public void setHaltHTTPCode(int nHTTPCode) 	{		
		m_vHTTPCode_Halt.add(new Integer(nHTTPCode)); 
	}


	public void setHaltHTTPCode(int[] arrHTTPCode) {
		for (int i=0; i<arrHTTPCode.length; i++)
			setHaltHTTPCode(arrHTTPCode[i]);		
	}


	public String getURLContent() throws IOException {
		String sURLContent = null;
		Vector<String> vHostName = new Vector<String>(m_vHost.length);	

		// try to connect via given URL		
		try	{
			m_urlCon.setDebugOn(isDebug());
			sURLContent = m_urlCon.getContent();

		}	catch (IOException ioe) {
			sURLContent = null;
			hashCache.remove(m_sOrigHost);
			this.m_URI="";
		}

		if (sURLContent==null && mDereferenceDNS)	{						// try to resolve hostname to different ones
			if ( m_vHTTPCode_Halt.contains(new Integer(m_urlCon.getLastResponseCode())) )	{		// return empty string if http rc ic one of 'halt' codes set by setHalt_HTTPCode previously. For example, 404
				if ( isDebug() ) {
					System.err.println(this.getClass().getSimpleName() + ".getURLContent: Failed with halt error code: " + m_urlCon.getLastResponseCode());
				}
				return "";
			}

			vHostName.add(InetAddress.getByName(m_urlCon.getUrl().getHost()).getCanonicalHostName());			
			hashCache.remove(m_sOrigHost);
			this.m_URI="";

			if ( isDebug() )
				System.err.println(this.getClass().getSimpleName() + ".getURLContent: Failed connect to host: " + (String)vHostName.lastElement());
			

			for (int i = 1; sURLContent==null && i < m_vHost.length; i++)	{
				reconnect2URL(InetAddress.getByName(m_vHost[i].getHostAddress()).getHostName());

				try	{
					sURLContent = m_urlCon.getContent();

				}	catch (IOException ioe) {
					sURLContent = null;
					hashCache.remove(m_sOrigHost);
					this.m_URI="";
					if ( isDebug() ) {
						System.err.println(this.getClass().getSimpleName() +".getURLContent: Failed getInputStream. Error is: " + ioe.getMessage());
						try { System.err.println(this.getClass().getSimpleName() +".getURLContent: HTTP rc is: " + m_urlCon.getLastResponseCode()); } 
						catch (Throwable e) {}
					}
				}


				if ( sURLContent == null ) {
					vHostName.add(InetAddress.getByName(m_urlCon.getUrl().getHost()).getCanonicalHostName());
					hashCache.remove(m_sOrigHost);
					this.m_URI="";
					if ( isDebug() )
						System.err.println(this.getClass().getSimpleName() + ".getURLContent: Failed connect to host: " + (String)vHostName.lastElement());
				}
			}

		}			// sURLContent==null && mDereferenceDNS


		if (sURLContent!=null)	{
			if ( isDebug() )
				System.out.println(this.getClass().getSimpleName() +".getURLContent: Connected to host: " + m_urlCon.getUrl().getHost());
			//if (hashCash.get(m_sOrigHost)==null)
			this.setToCache(m_sOrigHost, InetAddress.getByName(m_urlCon.getUrl().getHost()));
			this.m_URI=m_urlCon.getUrl().toString();

		} else	{
			hashCache.remove(m_sOrigHost);
			this.m_URI="";

			throw new IOException(this.getClass().getSimpleName()+".getURLContent: Cannot connect to host: " + m_sOrigHost + " using host names: " + vHostName.toString() + ". Connection refused.");
		}

		return sURLContent;
	}

	public static String static_getLastSuccessHostName(String sUrl) throws MalformedURLException, IOException {
		URL origURL = new URL(sUrl);

		if (hashCache==null)
			return "";

		InetAddress inHost= (InetAddress)hashCache.get(origURL.getHost());

		return (inHost!=null)? inHost.getHostName():"";	
	}

	public  String getURI() {
		return this.m_URI;
	}

	public static String static_getURLContentSilent(String sURL, boolean bCached) {
		String sContent = null;

		try {
			URLAction action = new URLAction(sURL, bCached);
			action.setHaltHTTPCode(HttpURLConnection.HTTP_NOT_FOUND);
			sContent = action.getURLContent();
		}
		catch (IOException e) {
			System.err.println(LogEx.getMessage(e));
			sContent = "";
		}

		return sContent;
	}


	public static String static_getURLContent(String sURL, boolean bCached) throws MalformedURLException, IOException {
		return (new URLAction(sURL, bCached)).getURLContent();
	}


	public void setHTTPHeaderProperty(String sPropName, String sPropValue) {
		if ( m_urlCon != null )	m_urlCon.setHTTPHeaderProperty(sPropName, sPropValue);
	}


	public static boolean isDebug() 				{ return m_bIsDebug; }	
	public static void setDebug(boolean isDebug) 	{ m_bIsDebug = isDebug; }

	public String getProxyConfiguration() 					{ return m_sProxyHost + ":" + String.valueOf(m_nProxyPort); }
	public static void setProxy(String sHost, int nPort) 	{ m_sProxyHost = sHost; m_nProxyPort = nPort; }
}

package com.fuib.util;

import java.io.IOException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;

public class HttpClientWrapper extends MyURLConnection {

	public HttpClientWrapper(URL url, Proxy proxy) throws IOException {
		super(url, proxy);
	}
	
	public String getContent() throws IOException {
		String sURLContent = null;
		Executor urlExecutor = null;
		Request reqHttp = null;
		Response respHttp = null;
		
		try {
			urlExecutor = org.apache.http.client.fluent.Executor.newInstance();
			reqHttp = org.apache.http.client.fluent.Request.Get(getUrl().toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}

		if (getLogin()!=null && !getLogin().equals("") && getPasswd()!=null && !getPasswd().equals(""))	{
			urlExecutor.auth(getLogin(), getPasswd());
		}

		if (getProxy()!=null)	{
			reqHttp.viaProxy(new org.apache.http.HttpHost(getProxy().toString()));
			if (isDebugOn()) 
				System.out.println(this.getClass().getSimpleName() + ": connection to " + getUrl().toString() + " via proxy " + getProxy().toString());

		} else	{
			if (isDebugOn()) 
				System.out.println(this.getClass().getSimpleName() + ": connection to " + getUrl().toString());

		}

		if (getHeaderProperties()!=null && !getHeaderProperties().isEmpty())	{
			for( Iterator it = getHeaderProperties().keySet().iterator(); it.hasNext();  )	{
				String key = (String)it.next();
				String val = getHeaderProperties().get(key); 
				if (val!=null)	{
					reqHttp.addHeader(key, val);
				}
			}
		}

		
		if ("https".equals(getUrl().getProtocol()))	{			
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
				SSLSocketFactory socketFactory = new SSLSocketFactory(sc);
				Executor.registerScheme(new Scheme("https", getUrl().getPort(), socketFactory));
			} catch (Exception e)	{
				e.printStackTrace();
				return null;
			}
		}
		
		
		try {
			respHttp = urlExecutor.execute(reqHttp);
			sURLContent = respHttp.returnContent().toString();

		} catch(HttpResponseException e)	{	
			setLastResponseCode(e.getStatusCode());
			
			if ( isDebugOn() ) {
				System.err.println(this.getClass().getSimpleName() + ": Failed to connect error is: " + e.getMessage());
				System.err.println(this.getClass().getSimpleName() + ": HTTP rc is: " + e.getStatusCode());
			}

			throw new IOException(e);
			
		} catch (org.apache.http.client.ClientProtocolException e) {
			e.printStackTrace();
		}

		// при возврате пустой строки apache возвращает символы перевода строки (типа окончания потока) - для совместимости с URLAction эта ситуация обрабатывается
		// в противном случае могут быть проблемы с парсингом карточек в формате JSON
		if (sURLContent.matches("\\r\\n")) 
			sURLContent = "";
		
		return sURLContent;
	}

}

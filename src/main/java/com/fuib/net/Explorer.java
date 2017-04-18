package com.fuib.net;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public class Explorer {
	private static final String DNS_INIT_FACTORY = "com.sun.jndi.dns.DnsContextFactory";
	private static final String DNS_DEFAULT_URL = "dns:";
	
	public static final String FUIB_DOMAIN = "fuib.com";
	public static final String DEFAULT_SITE = "dho";

	
	/**
	 * Gets all matching dns records as an Vector.
	 * @param sDomain: domain or host, e.g. oberon.ark.com or oberon.com which you want
	 *               the DNS records. Default value is "fuib.com" 
	 * @param vTypes:  e.g."MX","A" to describe which types of record you want.
	 * @return Vector
	 * @throws NamingException if DNS lookup fails.
	 */
	public static Vector lookupDNS(String sDomain, String[] vTypes) throws NamingException	{
		Vector vResults = new Vector();

		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, DNS_INIT_FACTORY);
		env.put(Context.PROVIDER_URL, DNS_DEFAULT_URL);
				
		DirContext dnsContext = new InitialDirContext(env);		
		Attributes attrs = dnsContext.getAttributes((sDomain != null)?sDomain:FUIB_DOMAIN, vTypes);
		
		for ( Enumeration e = attrs.getAll(); e.hasMoreElements(); )			
			for ( Enumeration a = ((Attribute)e.nextElement()).getAll(); a.hasMoreElements(); )
				vResults.add(a.nextElement());
		
		dnsContext.close();
		
		if ( vResults.size() == 0 )
			System.err.println( "Failed to find any DNS records for domain " + sDomain );
					
		return vResults;
	}
	
	
	/**
	 * Search for LDAP servers at specified site.
	 * @param sDNSserver DNS server, IP address or DNS name 
	 * @param a_sSite  e.g."DHO","DON", ets. If null means "DHO"
	 * @return Vector of dns names of servers
	 * @throws NamingException if DNS lookup fails.
	 */
	public static Vector lookupLDAP(String sDNSserver, String a_sSite) throws NamingException	{
		final String dnsServiceName = "_ldap._tcp";					//Specifies a Global Catalog server			
		final String aDDomain 		= "_sites." + FUIB_DOMAIN; 		//The DNS Zone to look	
		
		String retDomainController = null;
		Vector vResult = new Vector();

		Hashtable dnsEnv = new Hashtable();
		dnsEnv.put(Context.INITIAL_CONTEXT_FACTORY, DNS_INIT_FACTORY);
		dnsEnv.put(Context.PROVIDER_URL, 
				"dns://" + sDNSserver + "/" + ((a_sSite != null)?a_sSite.toLowerCase():DEFAULT_SITE) + "." + aDDomain);

		DirContext dnsContext = new InitialDirContext(dnsEnv);			
		Attributes dnsQueryResult = dnsContext.getAttributes(dnsServiceName, new String[] {"SRV"});

		for (Enumeration e = dnsQueryResult.getAll(); e.hasMoreElements();) {
			for (Enumeration a = ((Attribute) e.nextElement()).getAll(); a.hasMoreElements(); ) {
				StringTokenizer st = new StringTokenizer((String)a.nextElement());
				st.nextToken();
				st.nextToken();
				st.nextToken();
				retDomainController = st.nextToken();
				
				vResult.add(retDomainController.substring(0, retDomainController.length() - 1));
			}
		}

		dnsContext.close();
		
		return vResult;
	}
	
	
	/**
	 * Search for LDAP servers at specified site.
	 * @param a_sSite  e.g."DHO","DON", ets. If null means "DHO"
	 * @return Vector of dns names of servers
	 * @throws NamingException if DNS lookup fails.
	 */
	public static Vector lookupLDAP(String a_sSite) throws NamingException	{		
		String sSite = (a_sSite != null)?a_sSite.toLowerCase():DEFAULT_SITE;
		
		Vector vHosts = lookupDNS(FUIB_DOMAIN, new String[] {"NS"});
		Vector vResults = new Vector();
				
		// removing from 'all' list the entries which not start with 'sSite' parameter
		for (Iterator it = vHosts.iterator(); it.hasNext(); ) {
			String sName = (String)it.next();
			
			if ( sName.startsWith(sSite) )
				vResults.add(sName.endsWith(".")?sName.substring(0, sName.length() - 1):sName);
		}
		
		return vResults;
	}	
	
}

package com.fuib.lotus.ws;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import com.fuib.lotus.utils.Tools;

/**
 * @author evochko
 *
 */
public class WCResponse {
	public static final String DEFAULT_CHARSET = "CP1251";
	
	protected static final String CLOSE_SEP = "/";
	public static final String ERR_CODE_TEG = "<err_code>";
	public static final String ERR_TXT_TEG = "<error>";
	public static final String RESP_CODE_TEG = "<response>";
	public static final String RESP_TXT_TEG = "<resp_code>";
	public static final String LIST_DELIM = ";";
	public static final String LIST_KEY_DELIM = "=";
	public static final String LIST_VALUE_BEGIN = "<v>";
	public static final String LIST_VALUE_END = "</v>";

	
//	public static final String DEF_ENC = "UTF-8";
		
	private String m_sConstructorResp = "";		// строковый параметр, который используется при создании объекта класс 
	private long m_nErrCode = 0;
	private long m_nRespCode = 0;
	private String m_sRespTxt = "";
	private String m_sErrTxt = "";	
	
	/**
	 * класс реализует оберточные функции по установке и возврату результатов работы web-сервиса (ответ, ошибки и их коды)  
	 * @throws UnsupportedEncodingException 
	 * @throws IOException 
	 */
	
	public WCResponse(String a_sResponse) throws UnsupportedEncodingException {
		m_sConstructorResp = a_sResponse;
		String sResponse = "";
		if (a_sResponse!=null && !a_sResponse.equals(""))	{
			sResponse = a_sResponse;
			
			if (sResponse.indexOf(ERR_CODE_TEG)!=-1 && sResponse.indexOf(GetEndTeg(ERR_CODE_TEG))!=-1)	{
				try	{
					m_nErrCode = new Long( sResponse.substring(sResponse.indexOf(ERR_CODE_TEG)+ERR_CODE_TEG.length(), sResponse.indexOf(GetEndTeg(ERR_CODE_TEG)) )).longValue();
				}
				catch (Throwable ex) { }
			}

			if (sResponse.indexOf(RESP_CODE_TEG)!=-1 && sResponse.indexOf(GetEndTeg(RESP_CODE_TEG))!=-1)	{
				try	{
					m_nRespCode = new Long( sResponse.substring(sResponse.indexOf(RESP_CODE_TEG)+RESP_CODE_TEG.length(), sResponse.indexOf(GetEndTeg(RESP_CODE_TEG))) ).longValue();
				}
				catch (Throwable ex) { }
			}

			if (sResponse.indexOf(ERR_TXT_TEG)!=-1 && sResponse.indexOf(GetEndTeg(ERR_TXT_TEG))!=-1)	{
				m_sErrTxt = this.Decode( sResponse.substring(sResponse.indexOf(ERR_TXT_TEG)+ERR_TXT_TEG.length(), sResponse.indexOf(GetEndTeg(ERR_TXT_TEG))) );
			}

			if (sResponse.indexOf(RESP_TXT_TEG)!=-1 && sResponse.indexOf(GetEndTeg(RESP_TXT_TEG))!=-1)	{
				m_sRespTxt = this.Decode( sResponse.substring(sResponse.indexOf(RESP_TXT_TEG)+RESP_TXT_TEG.length(), sResponse.indexOf(GetEndTeg(RESP_TXT_TEG))) );				
			}
		}			
	}
	
	public String Decode(String sTxt) throws UnsupportedEncodingException	{
		if ( System.getProperty("java.version").startsWith("1.3") || System.getProperty("java.version").startsWith("1.2") || System.getProperty("java.version").startsWith("1.1"))
			return URLDecoder.decode(sTxt);
		else
			return URLDecoder.decode(sTxt, DEFAULT_CHARSET);
	}
	
	public String Encode(String sTxt) throws UnsupportedEncodingException 	{
		if ( System.getProperty("java.version").startsWith("1.3") || System.getProperty("java.version").startsWith("1.2") || System.getProperty("java.version").startsWith("1.1"))
			return URLEncoder.encode(sTxt);
		else
			return URLEncoder.encode(sTxt, DEFAULT_CHARSET);
	}
	
	public String GetResult() throws UnsupportedEncodingException {
		String result = (m_nErrCode!=0) ? (ERR_CODE_TEG + new Long(m_nErrCode).toString()+ GetEndTeg(ERR_CODE_TEG) ): "";

		if (m_sErrTxt!=null && m_sErrTxt.length() > 0)	{
			result += ERR_TXT_TEG;

			if (m_sErrTxt.length() > 0)	{
				result += this.Encode(m_sErrTxt);
			}
			
			result += GetEndTeg(ERR_TXT_TEG);
		}


		if (m_nRespCode!=0)	{
			result += RESP_CODE_TEG + new Long(m_nRespCode).toString()+ GetEndTeg(RESP_CODE_TEG);
		}
		
		if (m_sRespTxt!=null && m_sRespTxt.length() > 0)	{
			result += RESP_TXT_TEG;
			
			if (m_sRespTxt.length() > 0)	{
				result += this.Encode(m_sRespTxt);
			}				
			
			result += GetEndTeg(RESP_TXT_TEG);
		}
		
		return result;
	}
	
	
	public static String GetEndTeg( String start_teg )	{
		return start_teg.substring(0, 1) + CLOSE_SEP + start_teg.substring(1, start_teg.length());
	}
	
	public boolean IsEmpty()	{
		return (m_nErrCode==0 && m_nRespCode==0 && m_sRespTxt.equals("") && m_sErrTxt.equals(""));
	}
	
	public boolean IsError()	{ return (m_nErrCode!=0 || !m_sErrTxt.equals("")); }
	
	public void SetErrCode(long err)			{ m_nErrCode = err;	}	
	public long GetErrCode()					{ return m_nErrCode;}
	
	public void SetErrText( String sError ) 	{ m_sErrTxt = sError;}
	public String GetErrText()					{ return m_sErrTxt; }

	public void SetRespText(String sResponse)	{ m_sRespTxt = sResponse; }
	
	public void SetRespTextFromMap(Map map)	{
		String sText = "";
		Entry entry = null;

		if ( map != null && !map.isEmpty() ) {
			for (Iterator it=map.entrySet().iterator(); it.hasNext();) {
				entry = (Entry)it.next();
				if ( sText.length() > 0 )	sText += LIST_DELIM;
				
				sText += entry.getKey() + LIST_KEY_DELIM + LIST_VALUE_BEGIN + entry.getValue().toString() + LIST_VALUE_END;
			}

			this.SetRespText(sText);
		}

	}
	
	
	public void SetRespTextFromArray(String[] arrText)	{
		String sText = "";
		
		if ( arrText != null ) {
			for (int i=0; i<arrText.length; i++) {
				if ( sText.length() > 0 )	sText += LIST_DELIM;
				sText += LIST_VALUE_BEGIN + arrText[i] + LIST_VALUE_END;
			}
			
			this.SetRespText(sText);
		}
	}

	
	public String GetRespText()					{ return m_sRespTxt; }
	public Vector GetRespTextAsVector() {
		Vector vResult = new Vector();
		Vector vTmp = Tools.split(this.GetRespText(), LIST_DELIM);
		String sEntry;
		
		if ( vTmp.size() > 0 ) {
			for (Iterator it = vTmp.iterator(); it.hasNext(); ) {
				sEntry = (String)it.next();
				
				if ( sEntry.startsWith(LIST_VALUE_BEGIN) && sEntry.endsWith(LIST_VALUE_END) )
					vResult.add(sEntry.substring(LIST_VALUE_BEGIN.length(), sEntry.length() - LIST_VALUE_END.length()));
				else
					vResult.add(sEntry);									
			}			
		}		
				
		return vResult; 
	}
	
	public String[] GetRespTextAsArray() {
		return (String[]) GetRespTextAsVector().toArray(new String[0]);
	}
	

	public HashMap GetRespTextAsMap() {
		HashMap vResult = new HashMap();
		Vector vTmp = Tools.split(this.GetRespText(), LIST_DELIM);
		String sEntry, sValue;
		
		int nPos;
		
		if ( vTmp.size() > 0 ) {
			for (Iterator it = vTmp.iterator(); it.hasNext(); ) {								
				sEntry = (String)it.next();
				nPos = sEntry.indexOf(LIST_KEY_DELIM);
								
				if ( nPos != -1 ) {
					sValue = sEntry.substring(nPos + 1);
					
					if ( sValue.startsWith(LIST_VALUE_BEGIN) && sValue.endsWith(LIST_VALUE_END) )
						sValue = sValue.substring(LIST_VALUE_BEGIN.length(), sValue.length() - LIST_VALUE_END.length());
					
					vResult.put(sEntry.substring(0, nPos), sValue); 
				} else
					vResult.put(sEntry, "");
			}			
		}		
				
		return vResult; 
	}
	
	
	public void SetRespCode(long resp_code)	{ m_nRespCode = resp_code; }	
	public long GetRespCode()				{ return m_nRespCode; }
	
	public String GetConstructorResp()		{ return m_sConstructorResp; }
}

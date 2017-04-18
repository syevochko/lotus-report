package com.fuib.lotus.utils;


import java.util.Iterator;
import java.util.Vector;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;
import lotus.domino.Session;

public class Tools {
	static int PRIORITY_HIGH = 1;
	static int PRIORITY_NORMAL = 2;
	static int PRIORITY_LOW = 3;
	static int SHORT_NAME = 1;
	static int FULL_NAME = 2;
	public static int CASE_LOWER = 1;
	public static int CASE_UPPER = 2;


//	==========================  Adding document to system log database	========================
	static public void AddErrorToLog(Session session, Database db, String sWho, String sBriefDescription, RichTextItem itemFullDescription, int nPriority, Vector sAdminNames) throws NotesException 	{
		Document doc = db.createDocument();
		DateTime oDateTime = session.createDateTime("Today");

		oDateTime.setNow();
		doc.replaceItemValue("fdDateTime", oDateTime);
		doc.replaceItemValue("fdWho", sWho);
		doc.replaceItemValue("fdBriefDescription", sBriefDescription);
		doc.replaceItemValue("fdPriority", new Integer(nPriority));
		if ( itemFullDescription != null )	itemFullDescription.copyItemToDocument(doc, "fdFullDescription");
		doc.replaceItemValue("Form", "LogDocument");

		doc.save(true, true, true);

		if ( sAdminNames != null ) {
			Document mailDoc = db.createDocument();
			RichTextItem rtItem = mailDoc.createRichTextItem("Body");

			mailDoc.replaceItemValue("DeliveryPriority", "H");
			mailDoc.replaceItemValue("Importance", "1");
			mailDoc.replaceItemValue("Subject", "Внимание! Произошла критическая ошибка. Подробности смотри в системном логе.");
			rtItem.appendDocLink(doc, "", "Линк к документу лога");

			mailDoc.send(sAdminNames);
		} //if
	} //AddErrorToLog


	static public String TranslateName(String sNotesName, int format, Session session) throws NotesException	{
		Vector vResult; 
		String sName;

		if ( format == FULL_NAME )
			vResult = session.evaluate("ret := @DbLookup(''; ''; '(PLUsers)'; '" + sNotesName + "'; 'Employee_FullName'); @if (@IsError(ret); ' '; ret)");
		else
			vResult = session.evaluate("ret := @DbLookup(''; ''; '(PLUsers)'; '" + sNotesName + "'; 'Employee_ShortName'); @if (@IsError(ret); ' '; ret)");	

		sName = (String)vResult.firstElement();

		return (sName.length() > 1)?sName:sNotesName;
	} //TranslateName


	static public boolean isStringsDiffer(String s1, String s2)	{
		if ( s1 == null )
			return ( s2 != null && s2.length() > 0 );
		else if ( s2 == null )
			return ( s1 != null && s1.length() > 0 );
		else
			return !s1.equals(s2);
	}	//isStringsDiffer

	static public boolean isVectorsDiffer(Vector a_v1, Vector a_v2){
		Vector v1 = a_v1, v2 = a_v2;

		// if vector contains ONE EMPTY  String element it must be treated as EMPTY vector
		if ( v1 != null && !v1.isEmpty() && v1.size() == 1 && v1.firstElement().equals("") ) v1.clear();		
		if ( v2 != null && !v2.isEmpty() && v2.size() == 1 && v2.firstElement().equals("") ) v2.clear();

		if ( v1 == null || v1.isEmpty() )
			return ( v2 != null && !v2.isEmpty() );
		else if ( v2 == null || v2.isEmpty() )
			return ( v1 != null && !v1.isEmpty() );	
		else if ( v1.size() != v2.size() )
			return true;
		else
			for (int i = 0; i < v1.size(); i++)	
				if ( !v1.elementAt(i).equals(v2.elementAt(i)) )	return true;

		return false;
	}	//isVectorsDiffer

	static public int getCharCount(String sText, char cChar) {
		int count = 0;

		for (int i=0; i < sText.length(); i++) {
			if ( sText.charAt(i) == cChar ) count++;
		} //for

		return count;
	} //getCharacterCount


	static public Vector split(String a_sText, String sDelim) {
		Vector vTmp = new Vector();
		int nPosBegin = 0;
		int nPosEnd = 0;

		// get v_accessList			
		nPosEnd = a_sText.indexOf(sDelim, nPosBegin);
		while ( nPosEnd != -1 ) {
			vTmp.add(a_sText.substring(nPosBegin, nPosEnd));

			nPosBegin = nPosEnd + sDelim.length();					
			nPosEnd = a_sText.indexOf(sDelim, nPosBegin);
		}

		vTmp.add(a_sText.substring(nPosBegin));

		return vTmp;
	}

//	Split string into array with delemiters (String MUST NOT has a space symbols!) 
	static public Vector split(String a_sText, String sDelim, int nMinLength) {
		Vector vResult = new Vector();
		String sWord, sText = a_sText.trim();
		int nPosStart = 0, nPosEnd;

		for (int i = 0; i < sDelim.length(); i++) {						// replace all 'delimiters' by 'space'
			sText = sText.replace(sDelim.charAt(i), ' ');
		} //for		

		nPosEnd = sText.indexOf(" ");		
		while (nPosEnd != -1) {
			sWord = sText.substring(nPosStart, nPosEnd);
			if ( sWord.length() >= 1 && sWord.length() >= nMinLength ) 	vResult.addElement(sWord);

			nPosStart = nPosEnd + 1;
			nPosEnd = sText.indexOf(" ", nPosStart);
		} // while

		vResult.addElement(sText.substring(nPosStart));

		return vResult;
	} //Split


	static public String replaceSubstring(String sText, String sFrom, String sTo) {
		String sResult = "";
		int nPosStart = 0, nPosEnd;

		if ( sText == null || sText.equals("") || sFrom == null || sFrom.equals("") || sTo == null || sTo.equals("") || sFrom.equals(sTo) )	return sText;

		nPosEnd = sText.indexOf(sFrom);
		while (nPosEnd != -1) {
			sResult += sText.substring(nPosStart, nPosEnd) + sTo;

			nPosStart = nPosEnd + sFrom.length();
			nPosEnd = sText.indexOf(sFrom, nPosStart);
		} // while		

		sResult += sText.substring(nPosStart);

		return sResult;
	} //replaceSubstring



	public static Vector replace(Vector vSrc, Vector vFrom, Vector vTo) throws Exception {
		Vector vRet = new Vector(vSrc.size());
		Object oSrc;
		int nPos;

		if ( vSrc == null )	return null;
		if ( vFrom == null || vTo == null )		return (Vector)vSrc.clone();		
		if ( vFrom.size() != vTo.size() )
			throw new Exception("replace: Incorrect input data: dimension of 'from' and 'to' list is not equal!");

		for (Iterator it = vSrc.iterator(); it.hasNext(); ) {
			oSrc = it.next();
			nPos = vFrom.indexOf(oSrc);

			vRet.add((nPos != -1)?vTo.get(nPos):oSrc);
		}

		return vRet;
	}

	
	public static String word(String sSrc, int nPos, String sDelim) {
		String sRet = "";
		int nPosBegin = 0, nPosEnd = 0, nCurPos = 0;

		nPosEnd = sSrc.indexOf(sDelim, nPosBegin);
		while ( nPosEnd != -1 && nCurPos < nPos ) {
			sRet = sSrc.substring(nPosBegin, nPosEnd);
			nCurPos++;

			nPosBegin = nPosEnd + sDelim.length();					
			nPosEnd = sSrc.indexOf(sDelim, nPosBegin);
		}

		if ( nCurPos + 1 == nPos )
			sRet = sSrc.substring(nPosBegin);
		else if ( nCurPos + 1 < nPos )
			sRet = "";

		return sRet;
	}

	
	public static Vector word(Vector vSrc, int nPos, String sDelim) {
		Vector vRet = new Vector(vSrc.size());

		for (Iterator it=vSrc.iterator(); it.hasNext(); )  {
			vRet.add(word((String)it.next(), nPos, sDelim));
		}

		return vRet;
	}


	static public String Array2String(Object[] arr) {
		StringBuffer sRes = new StringBuffer("[");

		for (int i = 0; i < arr.length; i++) {
			sRes.append(arr[i]);
			sRes.append(",");
		}

		sRes.deleteCharAt(sRes.length() - 1);
		sRes.append("]");

		return sRes.toString();
	}

	static public Vector CollectionForceCase(Vector a_col, int nCase) {
		Vector col = new Vector(a_col.size());

		for (Iterator it = a_col.iterator(); it.hasNext(); )
			col.add((nCase == CASE_LOWER)?
					((String)it.next()).toLowerCase():
						((String)it.next()).toUpperCase()
			);

		return col;
	}



	static public void recycleLNObj(lotus.domino.Base obj) {
		if ( obj != null ) { 
			try {
				obj.recycle();
			} catch (NotesException e) {
				e.printStackTrace();
			}

			obj = null;
		}
	}


	static public void recycleLNObj(Vector vObj) {
		if ( vObj != null && !vObj.isEmpty() ) { 
			try {
				((lotus.domino.Base)vObj.firstElement()).recycle(vObj);
				vObj.clear();
			} catch (NotesException e) {
				e.printStackTrace();
			}

			vObj = null;
		}
	}


	static public Document getDocumentByUNID(String sUNID, Database dbTrg) throws NotesException {
		Document doc=null;

		try {
			doc = dbTrg.getDocumentByUNID(sUNID);
		} catch(NotesException ne) {
			if ( ne.id != 4091 ) throw ne;												// invalid universal id
		} //try

		return doc;
	}
	
	/**
	 * Проверка на соответствие строковых векторов игнорируя порядок строк
	 * @param pv1
	 * @param pv2
	 * @return
	 */
	static public boolean VectorEqualsIgnoreOrder_String(Vector pv1, Vector pv2) {
		if ((pv1 == null && pv2 != null) || (pv1 != null && pv2 == null)) return false;
		if (pv1 == null && pv2 == null) return true;
		
		for (int nI = 0; nI < pv1.size(); nI++) {
			if (!pv2.contains(pv1.get(nI))) return false;
		}
		return true;
	}
	
	/**
	 * Vector -> String (Аналог скриптового Join)
	 * @param pvSrc
	 * @return
	 */
	static public String Join(Vector pvSrc, String psDelim) {
		String sRes = "";
		if (pvSrc != null && pvSrc.size() > 0) {
			sRes = (String) pvSrc.get(0);
			for (int nI = 1; nI < pvSrc.size(); nI++)
				sRes = sRes + psDelim + pvSrc.get(nI);
		}
		return sRes;
	}
}
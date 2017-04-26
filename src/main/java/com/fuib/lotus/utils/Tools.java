package com.fuib.lotus.utils;


import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.fuib.lotus.log.LogEx;

import lotus.domino.Base;
import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.NotesError;
import lotus.domino.NotesException;
import lotus.domino.Session;

public class Tools {
	static int SHORT_NAME = 1;
	static int FULL_NAME = 2;
	public static int CASE_LOWER = 1;
	public static int CASE_UPPER = 2;
	
	
	static public boolean send(Document ndMemo, boolean bErrGen) throws NotesException {
		try {
			ndMemo.send(false);
			return true;
		}
		catch (NotesException E) {
			switch (E.id) {
			case 4294:
			case 4295:
			case 4000:
			case 4160:
				// записываем инфу об ошибке в лог
				//TODO: БД лога..., формирование инфы о получателях
				//AddErrorToLog("Tools::send", Database dbLog, E.id, E.getLocalizedMessage() + " {" + E.id + "}", RichTextItem itemFullDescription);
				
				if (bErrGen)
					throw E;
				else
					System.err.print("Tools::send: " + LogEx.getErrInfo(E, false));
					System.err.print("Tools::send: получатели: " + ndMemo.getItemValueString("SendTo"));
				break;
			default:
				throw E;	// для непредвиденных (необрабатываемых) ошибок выбрасываем сразу же
			}
		}
		return false;
	}
	
	
	static public boolean save(Document nd, boolean bForce, boolean bErrGen) throws Exception {
		try {
			if (nd.save(bForce, false)) return true;
			if (!bErrGen) return false;
			throw new NotesException(NotesError.NOTES_ERR_DOCNOTSAVED, "Документ не был сохранён, возможно он занят другим процессом");
		}
		catch (NotesException e) {
			String sErrText = LogEx.getMessage(e) + "; UNID: " + nd.getUniversalID();
			Database ndb = null;
			try {
				ndb = nd.getParentDatabase();
				sErrText += " (" + ndb.getFilePath() + ")";
			} catch (Exception ne) {}
			sErrText += ", isValid=" + nd.isValid() + ", isDeleted=" + nd.isDeleted();
			throw new NotesException(e.id, sErrText);
		}
	}
	

	@SuppressWarnings("unchecked")
	static public String TranslateName(String sNotesName, int format, Session session) throws NotesException {
		Vector<String> vResult;
		String sName;

		if (format == FULL_NAME)
			vResult = session.evaluate("ret := @DbLookup(''; ''; '(PLUsers)'; '" + sNotesName + "'; 'Employee_FullName'); @if (@IsError(ret); ' '; ret)");
		else
			vResult = session.evaluate("ret := @DbLookup(''; ''; '(PLUsers)'; '" + sNotesName + "'; 'Employee_ShortName'); @if (@IsError(ret); ' '; ret)");	
		
		sName = vResult.firstElement();
		
		return (sName.length() > 1) ? sName : sNotesName;
	} //TranslateName


	static public boolean isStringsDiffer(String s1, String s2)	{
		if (s1 == null)
			return (s2 != null && s2.length() > 0);
		else if (s2 == null)
			return (s1 != null && s1.length() > 0);
		else
			return !s1.equals(s2);
	}	//isStringsDiffer
	
	
	static public boolean isVectorsDiffer(Vector<Object> a_v1, Vector<Object> a_v2){
		Vector<Object> v1 = a_v1, v2 = a_v2;
		
		// if vector contains ONE EMPTY  String element it must be treated as EMPTY vector
		if (v1 != null && !v1.isEmpty() && v1.size() == 1 && v1.firstElement().equals("")) v1.clear();		
		if (v2 != null && !v2.isEmpty() && v2.size() == 1 && v2.firstElement().equals("")) v2.clear();

		if (v1 == null || v1.isEmpty())
			return (v2 != null && !v2.isEmpty());
		else if (v2 == null || v2.isEmpty())
			return (v1 != null && !v1.isEmpty());	
		else if (v1.size() != v2.size())
			return true;
		else
			for (int i = 0; i < v1.size(); i++)
				if (!v1.elementAt(i).equals(v2.elementAt(i))) return true;
		
		return false;
	}	//isVectorsDiffer
	
	
	static public int getCharCount(String sText, char cChar) {
		int count = 0;

		for (int i=0; i < sText.length(); i++) {
			if ( sText.charAt(i) == cChar ) count++;
		} //for

		return count;
	} //getCharacterCount
	
	
	static public Vector<String> split(String a_sText, String sDelim) {
		Vector<String> vTmp = new Vector<String>();
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
	
	
	/**
	 * Split string into array with delemiters (String MUST NOT has a space symbols!) 
	 */
	static public Vector<String> split(String a_sText, String sDelim, int nMinLength) {
		Vector<String> vResult = new Vector<String>();
		String sWord, sText = a_sText.trim();
		int nPosStart = 0, nPosEnd;
		
		for (int i = 0; i < sDelim.length(); i++) {						// replace all 'delimiters' by 'space'
			sText = sText.replace(sDelim.charAt(i), ' ');
		}
		
		nPosEnd = sText.indexOf(" ");		
		while (nPosEnd != -1) {
			sWord = sText.substring(nPosStart, nPosEnd);
			if (sWord.length() >= 1 && sWord.length() >= nMinLength) vResult.addElement(sWord);

			nPosStart = nPosEnd + 1;
			nPosEnd = sText.indexOf(" ", nPosStart);
		}
		
		vResult.addElement(sText.substring(nPosStart));
		
		return vResult;
	}
	
	
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
	}
	
	
	@SuppressWarnings("unchecked")
	public static Vector replace(Vector<?> vSrc, Vector<?> vFrom, Vector<?> vTo) throws Exception {
		Vector vRet = new Vector(vSrc.size());
		Object oSrc;
		int nPos;
		
		if (vSrc == null) return null;
		if (vFrom == null || vTo == null) return (Vector) vSrc.clone();		
		if (vFrom.size() != vTo.size())
			throw new Exception("replace: Incorrect input data: dimension of 'from' and 'to' list is not equal!");
		
		for (Iterator it = vSrc.iterator(); it.hasNext(); ) {
			oSrc = it.next();
			nPos = vFrom.indexOf(oSrc);
			vRet.add((nPos != -1) ? vTo.get(nPos) : oSrc);
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
	
	
	public static Vector<String> word(Vector<String> vSrc, int nPos, String sDelim) {
		Vector<String> vRet = new Vector<String>(vSrc.size());
		
		for (Iterator<String> it = vSrc.iterator(); it.hasNext(); ) {
			vRet.add(word(it.next(), nPos, sDelim));
		}
		
		return vRet;
	}
	
	
	/**
	 * Возвращает размер строки в кодировке по умолчанию (UTF-8) в байтах
	 */
	public static int getSize(String s) {
		return getSize(s, "UTF-8");
	}
	
	/**
	 * Возвращает размер строки в указанной кодировке в байтах
	 */
	public static int getSize(String s, String sEncoding) {
		int size = 0;
		if (s != null && !s.isEmpty())
			size = s.getBytes(Charset.forName(sEncoding)).length;
		return size;
	}
	
	
	/**
	 * Проверка на соответствие строковых векторов игнорируя порядок строк
	 * @param pv1
	 * @param pv2
	 * @return
	 */
	static public boolean VectorEqualsIgnoreOrder_String(Vector<?> pv1, Vector<?> pv2) {
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
	static public String join(Vector<?> vector, String psDelim) {
		StringBuffer sRes = new StringBuffer("");
		if (vector != null && vector.size() > 0) {
			sRes.append("" + vector.get(0));
			for (int i = 1; i < vector.size(); i++) {
				sRes.append(psDelim);
				sRes.append(vector.get(i));
			}
		}
		return sRes.toString();
	}
	
	static public String join(Object[] arr, String psDelim) {
		StringBuffer sRes = new StringBuffer("");
		if (arr.length > 0) {
			sRes.append(arr[0]);
			for (int i = 1; i < arr.length; i++) {
				sRes.append(psDelim);
				sRes.append(arr[i]);
			}
		}
		return sRes.toString();
	}
	
	static public Vector<?> unique(Vector<?> vctSource) {
		HashMap<Object, Integer> vHash = new HashMap<Object, Integer>();
		for (int n = 0; n < vctSource.size(); n++) {
			vHash.put(vctSource.get(n), n);
		}
		Vector<?> vctResult = new Vector<Object>(vHash.keySet());
		return vctResult;
	}
	
	static public String[] toArray(Vector<?> vector) {
		return vector.toArray(new String[vector.size()]);
	}
	
	static public String Array2String(Object[] arr) {
		return "[" + join(arr, ",") + "]";
	}
	
	
	/**
	 * Быстрый метод округления
	 * @param number - округляемое число
	 * @param scale  - количество знаков после запятой
	 * @return дробное число
	 */
	static public double round(double number, int scale) {
		int pow = 10;
		for (int i = 1; i < scale; i++)
		pow *= 10;
		double tmp = number * pow;
		return (double) (int) ((tmp - (int) tmp) >= 0.5 ? tmp + 1 : tmp) / pow;
	}
	
	
	static public Document getDocumentByUNID(String sUNID, Database dbTrg) throws NotesException {
		Document doc = null;
		try {
			doc = dbTrg.getDocumentByUNID(sUNID);
		}
		catch(NotesException ne) {
			switch (ne.id) {
			case NotesError.NOTES_ERR_BAD_UNID:		// invalid universal id
			case NotesError.NOTES_ERR_NOSUCH_ARG:	// unid is empty
				break;
			default:
				throw ne;												
			}
		}
		return doc;
	}
	
	/**
	 * @return Объект DateTime первой даты, содержащейся в поле с датами <br />
	 * При отсутствии поля не генерирует ошибок
	 * @throws NotesException 
	 */
	static public DateTime getItemValueDateTime(Document doc, String itemName) throws NotesException {
		Item item = null;
		try {
			if (doc.hasItem(itemName)) {
				item = doc.getFirstItem(itemName);
				if (item.getType() == Item.DATETIMES)
					return (DateTime) item.getValues().firstElement();
			}
			return null;
		}
		finally {
			recycleObj(item);
		}
	}
	
	/**
	 * @return Объект DateTime первой даты, содержащейся в поле с датами <br />
	 * Генерирует ошибки, когда поле отсутствует, пустое или имеет не тот тип
	 * @throws NotesException 
	 */
	static public DateTime getItemValueDateTimeE(Document doc, String itemName) throws NotesException {
		Item item = null;
		try {
			if (doc.hasItem(itemName)) {
				item = doc.getFirstItem(itemName);
				if (item.getType() == Item.DATETIMES)
					return (DateTime) item.getValues().firstElement();
				else {
					if (doc.getItemValueString(itemName).isEmpty())
						throw new NotesException(NotesError.NOTES_ERR_NOT_A_DATE_ITEM, "Поле '" + itemName + "', в котором ожидается дата, пусто; документ: " + doc.getUniversalID() + " (" + doc.getParentDatabase().getFilePath() + ")");
					else
						throw new NotesException(NotesError.NOTES_ERR_NOT_A_DATE_ITEM, "Поле '" + itemName + "', в котором ожидается дата, имеет другой тип данных; документ: " + doc.getUniversalID() + " (" + doc.getParentDatabase().getFilePath() + ")");
				}
			}
			else
				throw new NotesException(NotesError.NOTES_ERR_NOT_A_DATE_ITEM, "Поле '" + itemName + "' отсутствует в документе " + doc.getUniversalID() + " (" + doc.getParentDatabase().getFilePath() + ")");
		}
		finally {
			recycleObj(item);
		}
	}
	
	/**
	 * @return Объект Date (toJavaDate) первой даты, содержащейся в поле с датами
	 * @throws NotesException 
	 */
	static public Date getItemValueJavaDate(Document doc, String itemName) throws NotesException {
		Date d = null;
		DateTime dt = getItemValueDateTime(doc, itemName);
		if (dt != null) {
			try {
				d = dt.toJavaDate();
			}
			catch (NotesException e) {
				System.err.println(LogEx.getErrInfo(e, false));
				LogEx.printStackTrace(e);
			}
			recycleObj(dt);
		}
		return d;
	}
	
	
	@SuppressWarnings("unchecked")
	static public void appendItemValue(Document doc, String itemName, Object itemValue) throws NotesException {
		Vector<Object> vct = null;
		try {
			vct = doc.getItemValue(itemName);
			vct.add(itemValue);
			doc.replaceItemValue(itemName, vct).recycle();
		}
		catch (NotesException e) {
			System.err.println(itemName + " = " + "[" + join(doc.getItemValue(itemName), ", ") + "]");
			System.err.println("itemValue = " + itemValue);
			System.err.println("vct = " + "[" + join(vct, ", ") + "]");
			throw e;
		}
	}
	
	/**
	 * Добавление скаляра в существующий item с учётом уникальности
	 */
	@SuppressWarnings("unchecked")
	static public boolean appendItemValueUnique(Document doc, String itemName, Object itemValue) throws NotesException {
		Vector<Object> vct = doc.getItemValue(itemName);
		if (vct.contains(itemValue)) {
			appendItemValue(doc, itemName, itemValue);
			return true;
		}
		return false;
	}
	
	/**
	 * Добавление Vector'а в существующий item с учётом уникальности
	 */
	@SuppressWarnings("unchecked")
	static public boolean appendItemValueUnique(Document doc, String itemName, Vector<?> pvct) throws NotesException {
		Vector<Object> vct = doc.getItemValue(itemName);
		HashMap<Object, Integer> map = new HashMap<Object, Integer>(vct.size() + pvct.size(), 1);
		for (int n = 0; n < vct.size(); n++) {
			map.put(vct.get(n), n);
		}
		for (int n = 0; n < pvct.size(); n++) {
			map.put(pvct.get(n), n);
		}
		if (map.size() != vct.size()) {
			vct.clear();
			vct = new Vector<Object>(map.keySet());
			doc.replaceItemValue(itemName, vct).recycle();
			return true;
		}
		return false;
	}
	
	
	static public Vector<String> collectionForceCase(Vector<String> a_col, int nCase) {
		Vector<String> col = new Vector<String>(a_col.size());
		
		for (Iterator<String> it = a_col.iterator(); it.hasNext(); )
			col.add((nCase == CASE_LOWER)?
					(it.next()).toLowerCase():
						(it.next()).toUpperCase()
			);
		
		return col;
	}
	
	
	static public boolean isItemEmpty(Document doc, String itemName) {
		boolean result = true;
		try {
			if (doc.hasItem(itemName)) {
				Vector<?> vct = doc.getItemValue(itemName);
				if (!vct.isEmpty())
					result = (vct.firstElement().toString().isEmpty());
			}
		}
		catch (NotesException e) {
			System.err.println(LogEx.getErrInfo(e, false));
			LogEx.printStackTrace(e);
		}
		return result;
	}
	
	
	static public void recycleObj(lotus.domino.Base obj) {
		try {
			if (obj != null) obj.recycle();
		}
		catch (NotesException e) {
			if (e.id != 4376) {
				System.err.println(LogEx.getErrInfo(e, false));
				LogEx.printStackTrace(e);
			}
		}
		finally {
			obj = null;
		}
	}
	
	
	static public void recycleObj(Vector<?> vObj) {
		if (vObj != null && !vObj.isEmpty()) {
			for (int i = 0; i < vObj.size(); i++) {
				if (vObj instanceof Base) recycleObj((Base) vObj.get(i));
			}
			vObj.clear();
			vObj = null;
		}
	}
	
}
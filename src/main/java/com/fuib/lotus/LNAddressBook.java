package com.fuib.lotus;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.Name;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;

public class LNAddressBook implements lotus.domino.Base {
	static long FIELD_MAX_SIZE = 15000;									// max size of the field which Notes can support
	static long FIELD_MAX_SUMMARY_SIZE = 15104;							// max size of the field which Notes can support with flag 'SUMMARY'
		
	private Database dbNames = null;
	private View viewGroup = null;
	private View viewGroup2 = null;
	private View viewPerson = null;
	
	protected Session m_session;
	protected Hashtable m_cache = new Hashtable();						// cache for non-lotus objects 

	public LNAddressBook(LNEnvironment env) throws NotesException	{	this(env.getSession()); }
	public LNAddressBook(Session a_session) throws NotesException	{
		m_session = a_session;
		dbNames = m_session.getDatabase(m_session.getServerName(), "names.nsf", false);
		
		if ( dbNames == null ) {
			// look for address book
			for (Iterator e = m_session.getAddressBooks().iterator(); e.hasNext(); ) {
				dbNames = (Database)e.next();
				if ( dbNames.isPublicAddressBook() )	{ 
					dbNames.open(); 
					break; 
				}
				
				dbNames.recycle();
			}
		}
		

		if ( dbNames == null || !dbNames.isOpen() ) throw new NotesException(1, "Cannot find any valid public address book");
		
		if ((viewGroup = dbNames.getView("($Users)")) == null ) throw new NotesException(1, "Cannot get group view by name '($Users)' from database" + dbNames.getFilePath());
		if ((viewGroup2 = dbNames.getView("($Groups)")) == null ) throw new NotesException(1, "Cannot get group view by name '($Groups)' from database" + dbNames.getFilePath());		
	} //LNAddressBook
	
	
	public Database getPAB() {	return dbNames; } 
	
	public View getPersonView() throws NotesException {
		if ( viewPerson == null )
			if ((viewPerson = dbNames.getView("($VIMPeople)")) == null ) 
				throw new NotesException(1, "Cannot get person view by name '($VIMPeople)' from database" + dbNames.getFilePath());
	
		return viewPerson;
	} 
	
	private Document internalGetGroupDocument(String sGroup) throws NotesException	{
		Document docRes = viewGroup.getDocumentByKey(sGroup, true);
		
		if ( docRes == null ) docRes = viewGroup2.getDocumentByKey(sGroup, true);
		
		return docRes;
	} // internalGetGroupDocument


	private boolean removeFromMembers(String a_sName, Document a_docGroup) throws NotesException	{
		Item itemMembers = a_docGroup.getFirstItem("Members");
		boolean bRet = true;
		
		if ( itemMembers.getValues() == null ) return true;
		
		if ( itemMembers.getValues().contains(a_sName) ) {
			Vector vMembers = itemMembers.getValues();	
			
			vMembers.removeElement(a_sName);
			itemMembers.setValues(vMembers);
			
			if ( (itemMembers.getValues() == null) && (a_docGroup.getItemValueString("ListName").indexOf("_ext") != -1) )
				bRet = a_docGroup.remove(true);
			else
				bRet = 	a_docGroup.save();
		}
		else {
			String sExtGrName = a_docGroup.getItemValueString("ListName") + "_ext";
			
			if ( itemMembers.getValues().contains(sExtGrName) ) {
				Document docExt = internalGetGroupDocument(sExtGrName);
				if ( docExt == null ) throw new NotesException(1, "Not found group by name '" + sExtGrName + "' in database " + dbNames.getFilePath());
				
				bRet = removeFromMembers(a_sName, docExt);
				docExt.recycle();
			} //if
		} //if
	
		itemMembers.recycle();	
		return bRet;
	} //removeFromMembers


	private boolean addToMembers(String a_sName, Document a_docGroup) throws NotesException	{		
		Item itemMembers = a_docGroup.getFirstItem("Members");
		boolean bRet;
		
		if ( itemMembers.getValueLength() > FIELD_MAX_SIZE) {
			Document docExt;
			String sExtGrName = a_docGroup.getItemValueString("ListName") + "_ext";
			
			if ( !itemMembers.getValues().contains(sExtGrName) ) {				
				docExt = a_docGroup.getParentDatabase().createDocument();
				a_docGroup.copyAllItems(docExt, true);
				docExt.replaceItemValue("ListName", sExtGrName);
				docExt.replaceItemValue("Members", new String(""));
			
				itemMembers.appendToTextList(sExtGrName);														// add extension group to members of current group
				a_docGroup.save();
			} 
			else {																															// get existed extension group
				if ((docExt = internalGetGroupDocument(sExtGrName)) == null ) throw new NotesException(1, "Not found group by name '" + sExtGrName + "' in database " + dbNames.getFilePath());
			} //if

			bRet = addToMembers(a_sName, docExt);
			
			docExt.recycle();
		}
		else {
			Vector vMemb = itemMembers.getValues();
			if ( vMemb == null ) 
				a_docGroup.replaceItemValue("Members", a_sName);
			else	
				if ( !vMemb.contains(a_sName) ) itemMembers.appendToTextList(a_sName);		
				
			itemMembers.setSummary(itemMembers.getValueLength() < FIELD_MAX_SUMMARY_SIZE);			
			bRet = a_docGroup.save();
		} //if		
		
		itemMembers.recycle();
		return bRet;
	} //addToMembers

	
	public boolean addUserToGroup(String a_sUserName, String a_sGroup) throws NotesException	{
		Document docGroup;
		boolean bRet;
		
		docGroup = internalGetGroupDocument(a_sGroup);
		if ( docGroup != null ) { 
			bRet = addToMembers(a_sUserName, docGroup);											
			docGroup.recycle();			
			
			return bRet;
		}
		
		return false;				
	} //addUserToGroup
	
	
	public boolean addUserToGroup(Vector a_sUserName, String a_sGroup) throws NotesException	{
		Document docGroup;		
		boolean bRet = true;
		
		docGroup = internalGetGroupDocument(a_sGroup);
		if ( docGroup != null )		{
			Enumeration e = a_sUserName.elements();
			
			while (e.hasMoreElements()) bRet &= addToMembers((String)e.nextElement(), docGroup);
			
			docGroup.recycle();
			
			return bRet;
		 }	
		
		return false;										
	} //addUserToGroup		
	
	
	public boolean removeUserFromGroup(String a_sUserName, String a_sGroup) throws NotesException
	{
		Document docGroup;
		boolean bRet;
		
		docGroup = internalGetGroupDocument(a_sGroup);
		if ( docGroup != null )	{ 
			bRet = removeFromMembers(a_sUserName, docGroup);											
			docGroup.recycle();			
			
			return bRet;
		}
		 
		return false;				
	} //removeUserFromGroup
	
	
	public boolean removeUserFromGroup(Vector a_sUserName, String a_sGroup) throws NotesException {
		Document docGroup;		
		boolean bRet = true;
		
		docGroup = internalGetGroupDocument(a_sGroup);
		if ( docGroup != null )	{
			Enumeration e = a_sUserName.elements();
			
			while (e.hasMoreElements()) bRet &= removeFromMembers((String)e.nextElement(), docGroup);
			
			docGroup.recycle();
			
			return bRet;
		 }	
		
		return false;										
	} //removeUserFromGroup


	public Vector getAllUsersFromGroup(String a_sGroup)  throws NotesException	{
		Document docGroup;		
		Vector vRet = null;
				
		docGroup = internalGetGroupDocument(a_sGroup);
		if ( docGroup != null )	{  vRet = docGroup.getItemValue("Members"); docGroup.recycle(); }
		
		return vRet;
	} //getAllUsersFromGroup
	

	public void setGroupMembers(String a_sGroup, Vector a_vUsers)  throws NotesException	{	
		Document docGroup = internalGetGroupDocument(a_sGroup);
		if ( docGroup != null )	 { docGroup.replaceItemValue("Members", a_vUsers); docGroup.save(true); docGroup.recycle(); }
	} //setGroupMembers
	
	
	public Document getGroupDocumentByName(String a_sGroup, boolean a_bCreateIfNotExist)  throws NotesException	{	
		Document doc;
		
		doc = internalGetGroupDocument(a_sGroup);
		if ( doc == null & a_bCreateIfNotExist ) {			// creating new doc
			doc = dbNames.createDocument();
			Document docEveryone = internalGetGroupDocument("Everyone");						
			
			docEveryone.copyAllItems(doc, true);
			docEveryone.recycle();
			
			doc.replaceItemValue("Members", "");
			doc.replaceItemValue("ListDescription", "");
			doc.replaceItemValue("ListCategory", "");
			doc.replaceItemValue("ListName", a_sGroup);			
		} //if
		
		return doc;
	} //getGroupDocumentByName(String)
	
	
	public Document getGroupDocumentByName(Vector a_vGroup, boolean a_bCreateIfNotExist)  throws NotesException	{	
		Document doc = null;
			
		for (int i=0; i < a_vGroup.size() &&(doc == null); i++) 
			doc = internalGetGroupDocument((String)a_vGroup.elementAt(i));
						
		if ( doc == null & a_bCreateIfNotExist ) {			// creating new doc
			doc = dbNames.createDocument();
			Document docEveryone = internalGetGroupDocument("Everyone");						
			
			docEveryone.copyAllItems(doc, true);
			docEveryone.recycle();
			
			doc.replaceItemValue("Members", "");
			doc.replaceItemValue("ListDescription", "");
			doc.replaceItemValue("ListCategory", "");
			doc.replaceItemValue("ListName", a_vGroup);			
		} //if
		
		return doc;
	} //getGroupDocumentByName(Vector)

	
	public Document getUserDocumentbyName(String a_sUserName) throws NotesException {
		Name oUser = m_session.createName(a_sUserName);
		String sUsername = oUser.getAbbreviated();
		
		oUser.recycle();
		
		return getPersonView().getDocumentByKey(sUsername);		
	}
	
	
	public boolean isIncludeNamesList(String a_sName, String a_sGroup) throws NotesException {
		Vector vParam = new Vector(1);
		vParam.add(a_sGroup);
		
		return isIncludeNamesList(a_sName, vParam);
	}
	
	
	public boolean isIncludeNamesList(String a_sName, Vector a_vNamesList) throws NotesException {
		if ( a_vNamesList == null || a_sName == null )		return false;
		
		Name oName = m_session.createName(a_sName);
		String sGroupName = null;
		
		try {			
			if ( a_vNamesList.contains(oName.getCanonical()) )	return true;
			
			for (Iterator col = a_vNamesList.iterator(); col.hasNext(); ) {
				sGroupName = (String)col.next();
				
				if ( sGroupName.length() > 0 && sGroupName.indexOf('/') == -1 ) {		// entry is a names group
					if ( isIncludeNamesList(a_sName, getAllUsersFromGroup(sGroupName)) )	return true;
				}
			}			
		} finally {
			Tools.recycleObj(oName);
	}		
		
		return false;		
	}
	
	
	/*
	 * explore group content to names list. Recurc� is used. Function result is cached
	 */
	public Vector groupMembers(String sGroupName) throws NotesException {
		if ( !m_cache.containsKey(sGroupName) ) {
			Vector vAllMembers = new Vector(), vTmpMembers = null;
			Vector vGroupContent = getAllUsersFromGroup(sGroupName);
			String sName;
			int nPos;
			
			if ( vGroupContent != null ) {
				for (Iterator col = vGroupContent.iterator(); col.hasNext(); ) {
					sName = (String)col.next();
					
					if ( sName.length() > 0 && sName.indexOf('/') == -1 ) {		// entry is a names group
						vTmpMembers = groupMembers(sName);						// result can be null - if group not exist 
						if ( vTmpMembers != null )	vAllMembers.addAll(vTmpMembers);
					} else {													// entry is a person name
						nPos = sName.indexOf('@');
						vAllMembers.add((nPos != -1)?sName.substring(0, nPos):sName);
					}					
				}
			}
			
			if ( vAllMembers.size() > 0 )	m_cache.put(sGroupName, vAllMembers);
			
			return vAllMembers;
		}
		
		System.out.println("Result from cache - sGroupName");
		return (Vector)m_cache.get(sGroupName);			
	}
	
	
	// -----------------------------------------------------------------------------------------------------	
	public void recycle() throws NotesException {
		if ( viewGroup != null) viewGroup.recycle();
		if ( viewGroup2 != null) viewGroup2.recycle();
		if ( viewPerson != null) viewPerson.recycle();
		if ( dbNames != null) dbNames.recycle();		
	}
	
		
	public void recycle(Vector arg0) {
		System.out.println(this.getClass().getName() + "recycle(Vector): do Nothing ...");
	}
	
} //LNAddressBook
	
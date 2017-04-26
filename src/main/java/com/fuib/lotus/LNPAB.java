package com.fuib.lotus;

import java.util.*;

import com.fuib.lotus.log.LogEx;
import com.fuib.lotus.utils.LNIterator;
import com.fuib.lotus.utils.LNObjectList;
import com.fuib.lotus.utils.Tools;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.Item;
import lotus.domino.Name;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewEntryCollection;


public class LNPAB implements lotus.domino.Base {
	static final long FIELD_MAX_SIZE = 15000;							// max size of the field which Notes can support
	static final long FIELD_MAX_SUMMARY_SIZE = 15104;					// max size of the field which Notes can support with flag 'SUMMARY'
	static final String VIEW_PERSON  = "($VIMPeople)";
	static final String VIEW_GROUPS1 = "($Users)";
	static final String VIEW_GROUPS2 = "($Groups)";
	public static final String NAMES_FILENAME = "names.nsf";
	
	public static final String GROUPTYPE_MULTIPURPOSE = "0";
	public static final String GROUPTYPE_MAIL    = "1";
	public static final String GROUPTYPE_ACL     = "2";
	public static final String GROUPTYPE_DENY    = "3";
	public static final String GROUPTYPE_SERVERS = "4";
	
	public static final String ITEM_GROUP_NAME = "ListName";
	public static final String ITEM_GROUP_CATEGORY = "ListCategory";
	public static final String ITEM_GROUP_DESCRIPTION = "ListDescription";
	public static final String ITEM_GROUP_MEMBERS = "Members";
	
	/**
	 * 1-й элемент - это полное Notes-имя
	 */
	public static final String ITEM_USER_FULLNAME = "FullName";
	/**
	 * Логин пользователя в AD
	 */
	public static final String ITEM_USER_SHORTNAME = "ShortName";
	
	protected Session m_session;
	protected Database m_dbNames = null;
	protected LNObjectList m_objStorage = new LNObjectList();											// cache for lotus objects (views)
	protected Hashtable<String, Vector<String>> m_cache = new Hashtable<String, Vector<String>>();		// cache for non-lotus objects
	protected boolean m_bIsDebug = false;
	protected boolean m_bIsAdminServer = false;		// VladSh: параметр не соответствует действительности! На самом деле это bOpenFromAdminServer
	
	
	@SuppressWarnings("unchecked")
	public LNPAB(Session session, boolean bOpenFromAdminServer) throws NotesException {
		m_session = session;
		m_bIsAdminServer = bOpenFromAdminServer;
		m_dbNames = getDatabaseNames(m_session.getAgentContext().getCurrentDatabase().getServer());
		
		if (m_dbNames == null) {
			// look for address book
			LNIterator lnIter = new LNIterator(m_session.getAddressBooks());
			while(lnIter.hasNext()) {
				m_dbNames = (Database) lnIter.next();
				if (m_dbNames.isPublicAddressBook()) {
					m_dbNames.open();
					checkOnFanthomDb(m_dbNames);
					break;
				}
			}
		}
		
		if (m_dbNames == null || !m_dbNames.isOpen())
			throw new NotesException(LogEx.ERRc1223, "LNPAB конструктор: Cannot find any valid Public Address Book");
		
		if (bOpenFromAdminServer) {
			// get server name of admin server of this database
			String sAdminServer = m_dbNames.getACL().getAdministrationServer();
			if (!sAdminServer.equalsIgnoreCase(m_dbNames.getServer())) {
				m_dbNames = getDatabaseNames(sAdminServer);
				
				if (m_dbNames == null || !m_dbNames.isOpen())
					throw new NotesException(LogEx.ERRc1223, "LNPAB конструктор: Cannot open Public Address Book on the server: " + sAdminServer);
			}
		}
		
		if (m_dbNames != null && m_dbNames.isOpen())
			if (m_bIsDebug) System.out.println("LNPAB конструктор: names.nsf открыта на сервере " + m_dbNames.getServer());
	}
	
	
	public Database getPAB() 								{ return m_dbNames; }
	public View getPersonView() throws NotesException 		{ return _getView(VIEW_PERSON); }
	public void setDebugMode(boolean bMode)					{ m_bIsDebug = bMode; }
	public boolean isAdminServer()							{ return m_bIsAdminServer; }
	
	/**
	 * Получает и возвращает адресную книгу при имени names.nsf с определённого сервера
	 */
	private Database getDatabaseNames(String sServer) throws NotesException {
		Database ndb = m_session.getDatabase(sServer, NAMES_FILENAME, false);
		checkOnFanthomDb(ndb);
		return ndb;
	}
	
	/**
	 * Дебаг-метод. Удалить после разбора этой ситуации (ошибки).
	 */
	private void checkOnFanthomDb(Database ndb) throws NotesException {
		if (ndb != null && ndb.getFilePath() == "databasenames.nsf")
			throw new NotesException(1111, "Стэк инициализации фантомной 'databasenames.nsf' определён! (см. ошибку в SynchStaff 'Cannot get group view by name '($Users)' from databasenames.nsf').");
	}
	
	
	protected Document _getGroupDocument(String sGroup) throws NotesException {
		Document docRes = null;
		Document doc = null;
		Document docDel = null;
		DocumentCollection dc = _getView(VIEW_GROUPS1).getAllDocumentsByKey(sGroup, true);
		
		if (dc.getCount() > 0) {
			doc = dc.getFirstDocument();
			while (doc != null) {
				if (isGroupDocument(doc)) {
					docRes = doc;
					break;
				}
				
				docDel = doc;
				doc = dc.getNextDocument(doc);
				Tools.recycleObj(docDel);
			}
		}
		
		if (docRes == null)
			docRes = _getView(VIEW_GROUPS2).getDocumentByKey(sGroup, true);
		
		return docRes;
	}
	
	
	/**
	 * Определяет, является ли переданный член группы именем группы или нет;<br />
	 * здесь реализована самая простая проверка по отсутствию "/"
	 * @return
	 * true  - переданная строка - это имя группы;<br />
	 * false - что-то другое
	 */
	public boolean isGroupMember(String sMember) {
		return (sMember != null && sMember.length() > 0 && sMember.indexOf('/') == -1);
	}
	
	
	/**
	 * Определяет, является ли переданный документ группой
	 */
	private boolean isGroupDocument(Document docRes) throws NotesException {
		if (docRes == null) return false;
		
		return docRes.getItemValueString("Form").equalsIgnoreCase("Group");
	}

	
	public boolean removeFromMembers(Vector<String> a_vUserName, Document docGroup) throws NotesException {
		boolean bRet = false;
		boolean bIsGroupChanged = false;
		
		if (docGroup != null) {
			for (Iterator<String> it = a_vUserName.iterator(); it.hasNext(); )
				bIsGroupChanged |= removeFromMembers(it.next(), docGroup);
			
			bRet = (bIsGroupChanged) ? docGroup.save(false) : true;
		}
		
		return bRet;
	}
	
	@SuppressWarnings("unchecked")
	protected boolean removeFromMembers(String a_sName, Document a_docGroup) throws NotesException	{
		Item itemMembers = a_docGroup.getFirstItem(ITEM_GROUP_MEMBERS);
		Vector<String> vMembers = itemMembers.getValues();
		boolean bIsGroupChanged = false;
		
		if (vMembers == null) return true;
		
		if (vMembers.contains(a_sName)) {
			vMembers.remove(a_sName);
			
			if (vMembers.isEmpty() && a_docGroup.getItemValueString(ITEM_GROUP_NAME).indexOf("_ext") != -1) {
				//a_docGroup.remove(true);
			} else
				itemMembers.setValues(vMembers);
			
			bIsGroupChanged = true;
		}
		else {
			String sExtGrName = a_docGroup.getItemValueString(ITEM_GROUP_NAME) + "_ext";
			
			if (vMembers.contains(sExtGrName)) {
				Document docExt = _getGroupDocument(sExtGrName);
				if (docExt == null)
					throw new NotesException(1, "Not found group by name '" + sExtGrName + "' in database " + m_dbNames.getFilePath());
				
				if (removeFromMembers(a_sName, docExt))	bIsGroupChanged = docExt.save(false);
				Tools.recycleObj(docExt);
			}
		}
		
		return bIsGroupChanged;
	} //removeFromMembers
	
	
	public boolean addToMembers(Vector<String> a_vUserName, Document docGroup) throws NotesException {
		boolean bRet = false;
		boolean bIsGroupChanged = false;
		
		if (docGroup != null) {
			for (Iterator<String> it = a_vUserName.iterator(); it.hasNext(); )
				bIsGroupChanged |= addToMembers(it.next(), docGroup);
			
			bRet = (bIsGroupChanged) ? docGroup.save(false) : true;
		}
		return bRet;
	}
	
	
	@SuppressWarnings("unchecked")
	protected boolean addToMembers(String a_sName, Document a_docGroup) throws NotesException {
		boolean bIsGroupChanged = false;
		Item itemMembers = a_docGroup.getFirstItem(ITEM_GROUP_MEMBERS);
		
		if (itemMembers.getValueLength() > FIELD_MAX_SIZE) {
			Document docExt;
			String sExtGrName = a_docGroup.getItemValueString(ITEM_GROUP_NAME) + "_ext";
			
			if (!itemMembers.getValues().contains(sExtGrName)) {
				docExt = a_docGroup.getParentDatabase().createDocument();
				a_docGroup.copyAllItems(docExt, true);
				docExt.replaceItemValue(ITEM_GROUP_NAME, sExtGrName);
				docExt.replaceItemValue(ITEM_GROUP_MEMBERS, "");
			
				itemMembers.appendToTextList(sExtGrName);							// add extension group to members of current group
			} else {																// get existed extension group
				if ((docExt = _getGroupDocument(sExtGrName)) == null)
					throw new NotesException(1, "Not found group by name '" + sExtGrName + "' in database " + m_dbNames.getFilePath());
			} //if

			if (addToMembers(a_sName, docExt))
				bIsGroupChanged = docExt.save(false);
			
			Tools.recycleObj(docExt);
		}
		else {
			Vector<String> vMemb = itemMembers.getValues();
			
			if (vMemb == null) {
				itemMembers.setValueString(a_sName);
				bIsGroupChanged = true;
			}
			else if (!vMemb.contains(a_sName)) {
				itemMembers.appendToTextList(a_sName);
				bIsGroupChanged = true;
			}
			
			itemMembers.setSummary(itemMembers.getValueLength() < FIELD_MAX_SUMMARY_SIZE);
		}
		
		return bIsGroupChanged;
	} //addToMembers

	
	public boolean addUserToGroup(String a_sUserName, String a_sGroup, boolean bIsCreateIfNotExist, String sGroupType) throws NotesException	{
		boolean bRet = false;
		
		Document docGroup = _getGroupDocument(a_sGroup);
		if (docGroup == null && bIsCreateIfNotExist)
			docGroup = createGroup(a_sGroup, sGroupType);
		
		if (docGroup != null) {
			bRet = (addToMembers(a_sUserName, docGroup)) ? docGroup.save(false) : true;
			Tools.recycleObj(docGroup);
		}
		
		return bRet;
	} //addUserToGroup
	
	
	public boolean addUserToGroup(String a_sUserName, String a_sGroup) throws NotesException {
		return addUserToGroup(a_sUserName, a_sGroup, false, null);
	}
		
	
	public boolean addUserToGroup(Vector<String> a_vUserName, String a_sGroup, boolean bIsCreateIfNotExist, String sGroupType) throws NotesException	{
		Document docGroup = _getGroupDocument(a_sGroup);
		if (docGroup == null && bIsCreateIfNotExist)
			docGroup = createGroup(a_sGroup, sGroupType);
		
		return addToMembers(a_vUserName, docGroup);
	} //addUserToGroup
	
	
	public boolean addUserToGroup(Vector<String> a_vUserName, String a_sGroup) throws NotesException	{
		return addUserToGroup(a_vUserName, a_sGroup, false, null);
	}
	
	
	public boolean removeUserFromGroup(String a_sUserName, String a_sGroup) throws NotesException {
		boolean bRet = false;
		
		Document docGroup = _getGroupDocument(a_sGroup);
		if (docGroup != null)	{
			bRet = (removeFromMembers(a_sUserName, docGroup)) ? docGroup.save(false) : true;
			Tools.recycleObj(docGroup);
		}
		 
		return bRet;
	} //removeUserFromGroup
	
	
	public boolean removeUserFromGroup(Vector<String> a_vUserName, String a_sGroup) throws NotesException {
		boolean bRet = false;
		
		Document docGroup = _getGroupDocument(a_sGroup);
		if (docGroup != null) {
			bRet = (removeFromMembers(a_vUserName, docGroup)) ? docGroup.save(false) : true;
			Tools.recycleObj(docGroup);
		}
		
		return bRet;
	} //removeUserFromGroup

	
	@SuppressWarnings("unchecked")
	public Vector<String> getAllUsersFromGroup(Document docGroup)  throws NotesException {
		Vector<String> vRet;
		if (docGroup != null)
			vRet = docGroup.getItemValue(ITEM_GROUP_MEMBERS);
		else
			vRet = new Vector<String>(0);
		return vRet;
	} //getAllUsersFromGroup
	
	
	public Vector<String> getAllUsersFromGroup(String a_sGroup) throws NotesException {
		Vector<String> vRet;
		Document docGroup = _getGroupDocument(a_sGroup);
		if (docGroup != null) {
			vRet = getAllUsersFromGroup(docGroup);
			Tools.recycleObj(docGroup);
		}
		else
			vRet = new Vector<String>(0);
		return vRet;
	}
	
	/**
	 * Возвращает всех пользователей группы с учётом добавочных групп с суффиксом "_ext".<br /><br />
	 * Предполагается, что в данных группах посторонние группы (кроме "_ext") не могут присутствовать, потому разворачивание здесь не производится
	 * @param sGroupName - начальное имя группы (не содержащее "_ext")
	 * @return
	 * 		- null - группа не найдена;<br />
	 * 		- пустой Vector - группа есть, но пустая;<br />
	 * 		- Vector со значениями.
	 * @throws NotesException
	 */
	@SuppressWarnings("unchecked")
	public Vector<String> getAllUsersFromGroupExt(String sGroupName) throws NotesException {
		Vector<String> vctMembers = null;
		
		Document ndGroup = _getGroupDocument(sGroupName);
		if (ndGroup != null) {
			vctMembers = ndGroup.getItemValue(ITEM_GROUP_MEMBERS);
			if (!vctMembers.isEmpty()) {
				String sGroupNameExt = sGroupName + "_ext";
				int pos = vctMembers.indexOf(sGroupNameExt);
				if (pos != -1) {
					vctMembers.remove(pos);
					Vector<String> vctMembersExt = getAllUsersFromGroupExt(sGroupNameExt);
					if (vctMembersExt != null && !vctMembersExt.isEmpty()) {
						vctMembers.addAll(vctMembersExt);
					}
				}
			}
		}
		
		return vctMembers;
	}
	

	public void setGroupMembers(String a_sGroup, Vector<String> a_vUsers)  throws NotesException {
		Document docGroup = _getGroupDocument(a_sGroup);
		
		if (docGroup != null) {
			docGroup.replaceItemValue(ITEM_GROUP_MEMBERS, a_vUsers);
			docGroup.save(false);
			Tools.recycleObj(docGroup);
		}
	} //setGroupMembers
	
	
	public Document getGroupDocumentByName(String a_sGroup, boolean a_bCreateIfNotExist, String sGroupType)  throws NotesException {
		Document doc = _getGroupDocument(a_sGroup);
		
		if (doc == null && a_bCreateIfNotExist) 			// creating new doc
			doc = createGroup(a_sGroup, sGroupType);
		
		return doc;
	} //getGroupDocumentByName(String)
	
	
	public Document getGroupDocumentByName(Vector<String> a_vGroup, boolean a_bCreateIfNotExist, String sGroupType)  throws NotesException	{
		Document doc = null;
			
		for (int i = 0; i < a_vGroup.size() && (doc == null); i++)
			doc = _getGroupDocument(a_vGroup.elementAt(i));
		
		if (doc == null && a_bCreateIfNotExist)
			doc = createGroup(a_vGroup, sGroupType);
		
		return doc;
	} //getGroupDocumentByName(Vector)

	
	public Document getUserDocumentbyName(String a_sUserName) throws NotesException {
		if (a_sUserName.isEmpty()) return null;
		
		Name oUser = m_session.createName(a_sUserName);
		String sUsername = oUser.getAbbreviated();
		
		Tools.recycleObj(oUser);
		
		return getPersonView().getDocumentByKey(sUsername, true);
	}
	
	/**
	 * Универсальный метод, возвращающий значение (String) любого поля из карточки пользователя
	 * @param sNotesName   - имя пользователя в системе
	 * @param arrItemNames - массив наименований полей:
	 * 			- если первое поле не пусто, то метод вернёт значения первого поля;
	 * 			- если пусто, то будет произведена попытка получения значений из следующих указанных полей
	 * @return
	 * 			- String: значение или пустую строку;<br />
	 * 			- если карточка пользователя отсутствует в АК сервера, то вернёт null.
	 */
	@SuppressWarnings("unchecked")
	public String getUserInfoString(String sNotesName, String[] arrItemNames) throws NotesException {
		String result = null;
		Vector<String> vctResult = (Vector<String>) getUserInfoVector(sNotesName, arrItemNames);
		if (vctResult != null)
			result = (!vctResult.isEmpty()) ? vctResult.get(0) : "";
		return result;
	}
	
	/**
	 * Универсальный метод, возвращающий значения (Vector) любого поля из карточки пользователя
	 * @param sNotesName   - имя пользователя в системе
	 * @param arrItemNames - массив наименований полей:
	 * 			- если первое поле не пусто, то метод вернёт значения первого поля;
	 * 			- если пусто, то будет произведена попытка получения значений из следующих указанных полей
	 * @return
	 * 			- Vector;<br />
	 * 			- если карточка пользователя отсутствует в АК сервера, то вернёт null.
	 */
	public Vector<?> getUserInfoVector(String sNotesName, String[] arrItemNames) throws NotesException {
		Vector<?> vctResult = null;
		Document ndUser = null;
		try {
			ndUser = getUserDocumentbyName(sNotesName);
			if (ndUser == null) return null;
			for (String itemName : arrItemNames) {
				vctResult = ndUser.getItemValue(itemName);
				if (!vctResult.isEmpty()) break;
			}
		}
		finally {
			Tools.recycleObj(ndUser);
		}
		return vctResult;
	}
	
	/**
	 * Возвращает Internet-адрес пользователя по имени в системе<br />
	 * @return
	 * 		- значение или пустую строку;<br />
	 * 		- если карточка пользователя отсутствует в АК сервера, то вернёт null.
	 */
	public String getUserInternetAddress(String sNotesName) throws NotesException {
		return getUserInfoString(sNotesName, new String[]{"InternetAddress", "MailAddress"});
	}
	
	/**
	 * Возвращает документ пользователя по краткому имени (логину)
	 * @throws NotesException
	 */
	public Document getUserDocumentbyShortName(String sLogin) throws NotesException {
		if (sLogin.isEmpty()) return null;
		return _getView(VIEW_GROUPS1).getDocumentByKey(sLogin, true);
	}
	
	
	/**
	 * Возвращает имена групп, в которые непосредственно входит пользователь
	 * @param sUserName - Notes-имя пользователя в любом формате (внутри производится преобразование к Abbreviated)
	 */
	public Vector<String> getAllUserGroupNames(String sUserName) throws NotesException {
		return getAllUserGroupNames(sUserName, null, null);
	}
	
	public Vector<String> getAllUserGroupNames(String sUserName, String sCategory) throws NotesException {
		return getAllUserGroupNames(sUserName, sCategory, null);
	}
	
	/**
	 * Возвращает имена групп пользователя с учётом категории и типа
	 * @param sUserName - Notes-имя пользователя в любом формате (внутри производится преобразование к Abbreviated)
	 */
	public Vector<String> getAllUserGroupNames(String sUserName, String sCategory, String sType) throws NotesException {
		View view = _getView("MemberGroupsEx");
		
		Vector<String> vct = new Vector<String>();
		Name un = m_session.createName(sUserName);
		vct.add(un.getAbbreviated());		// во вьюхе первая колонка именно Abbreviated
		Tools.recycleObj(un);
		if (sCategory != null)	vct.add(sCategory);
		if (sType != null)		vct.add(sType);
		
		Vector<String> vctResult = null;
		
		ViewEntryCollection vec = view.getAllEntriesByKey(vct);
		if (vec.getCount() != 0) {
			vctResult = new Vector<String>(vec.getCount());
			boolean bAutoUpdate = view.isAutoUpdate();
			view.setAutoUpdate(false);
			LNIterator lnIter = new LNIterator(vec, true, false);
			try {
				ViewEntry ve;
				while (lnIter.hasNext()) {
					ve = (ViewEntry) lnIter.next();
					vctResult.add((String) ve.getColumnValues().get(3));
				}
			}
			finally {
				view.setAutoUpdate(bAutoUpdate);
				Tools.recycleObj(lnIter);
			}
		}
		
		return vctResult;
	}
	
	public boolean isIncludeNamesList(String a_sName, String a_sGroup) throws NotesException {
		Vector<String> vParam = new Vector<String>(1);
		vParam.add(a_sGroup);
		
		return isIncludeNamesList(a_sName, vParam);
	}
	
	
	public boolean isIncludeNamesList(String a_sName, Vector<String> a_vNamesList) throws NotesException {
		if (a_vNamesList == null || a_sName == null) return false;
		
		Name oName = m_session.createName(a_sName);
		String sGroupName = null;
		
		if (a_vNamesList.contains(oName.getCanonical())) return true;
		
		for (Iterator<String> col = a_vNamesList.iterator(); col.hasNext(); ) {
			sGroupName = col.next();
			
			if (isGroupMember(sGroupName)) {		// entry is a names group
				if (isIncludeNamesList(a_sName, getAllUsersFromGroup(sGroupName)))	return true;
			}
		}
		
		return false;
	}
	
	
	/*
	 * explore group content to names list. Recurcу is used. Function result is cached
	 */
	public Vector<String> groupMembers(String sGroupName) throws NotesException {
		if (!m_cache.containsKey(sGroupName)) {
			Document docGroup = _getGroupDocument(sGroupName);
			Vector<String> vAllMembers;
			if (docGroup != null) {
				vAllMembers = groupMembers(docGroup);
				Tools.recycleObj(docGroup);
			}
			else
				vAllMembers = new Vector<String>(0);
			return vAllMembers;
		}
		return m_cache.get(sGroupName);
	}
	
	/*
	 * explore group content to names list. Recurcу is used. Function result is cached
	 */
	public Vector<String> groupMembers(Document docGroup) throws NotesException {
		String sGroupName = "";
		Vector<String> vAllMembers = new Vector<String>(0);
		
		if (docGroup != null) {
			sGroupName = docGroup.getItemValueString(ITEM_GROUP_NAME);
			
			if (!m_cache.containsKey(sGroupName)) {
				Vector<String> vTmpMembers = null;
				Vector<String> vGroupContent = getAllUsersFromGroup(docGroup);
				String sName;
				int nPos;
				
				if (vGroupContent != null) {
					for (Iterator<String> col = vGroupContent.iterator(); col.hasNext(); ) {
						sName = col.next();
						
						if (isGroupMember(sName)) {		// entry is a names group
							vTmpMembers = groupMembers(sName);						// result can be null - if group not exist
							if ( vTmpMembers != null )	vAllMembers.addAll(vTmpMembers);
						} else {													// entry is a person name
							nPos = sName.indexOf('@');
							vAllMembers.add((nPos != -1) ? sName.substring(0, nPos) : sName);
						}
					}
				}
				
				if (vAllMembers.size() > 0) m_cache.put(sGroupName, vAllMembers);
				
				return vAllMembers;
			}
			
			return (Vector<String>) m_cache.get(sGroupName);
		}
		else
			return vAllMembers;
	}
	
	
	public Document createGroup(Object oGroupName, String sGroupType) throws NotesException {
		Document docGroup = m_dbNames.createDocument();
		Document docEveryone = _getGroupDocument("Everyone");
		
		docEveryone.copyAllItems(docGroup, true);
		Tools.recycleObj(docEveryone);
		
		docGroup.replaceItemValue(ITEM_GROUP_NAME, oGroupName);
		docGroup.replaceItemValue("GroupType", (sGroupType != null) ? sGroupType : GROUPTYPE_MULTIPURPOSE);
		docGroup.replaceItemValue(ITEM_GROUP_CATEGORY, "");
		docGroup.replaceItemValue(ITEM_GROUP_DESCRIPTION, "");
		docGroup.replaceItemValue(ITEM_GROUP_MEMBERS, "");
		
		return docGroup;
	}
	
	/**
	 * TODO После отлова "NotesException: 'Cannot get group view by name '($Users)' from databasenames.nsf'" удалить этот метод,
	 * перевести на вызов LNEnvironment.getDbView().
	 */
	protected View _getView(String sViewName) throws NotesException {
		String sKey = "view_" + sViewName;
		View oView = (View) m_objStorage.get(sKey);

		if (oView == null) {
			oView = m_dbNames.getView(sViewName);
			if (oView != null)
				m_objStorage.put(sKey, oView);
			else
				throw new NotesException(1, "Cannot get group view by name '" + sViewName + "' from database" + m_dbNames.getFilePath());
		} else
			oView.refresh();
		
		return oView;
	}
	
	
	// -----------------------------------------------------------------------------------------------------
	
	public void recycle() throws NotesException {
		if (m_dbNames != null) m_dbNames.recycle();
		m_objStorage.recycle();
		m_cache.clear();
	}
	
	@SuppressWarnings("unchecked")
	public void recycle(Vector arg0) {
		System.out.println(this.getClass().getName() + "recycle(Vector): do Nothing ...");
	}
	
}
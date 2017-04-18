package com.fuib.lotus;

import java.util.Vector;

import lotus.domino.ACL;
import lotus.domino.ACLEntry;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.NotesException;
import lotus.domino.Session;

public class LNAccess {
	public static final int ACCESS_READ = 0;
	public static final int ACCESS_EDIT = 1;
	
	protected Session m_oSession = null;
	protected LNAddressBook m_oPAB = null;
	protected Vector m_vDocAccList = null;
	
	public LNAccess(Session poSession) throws NotesException {
		this.m_oSession = poSession;
	}
	
	/**
	 * Проверка доступа пользователя к документу
	 *		pbAccEditor = false - на чтение
	 *		pbAccEditor = true - на редактирование
	 * @param pdoc - документ, доступ к которому проверяется
	 * @param psUserName - NN пользователя
	 * @param pbAccEditor - флаг требуемого доступа
	 * @return Флаг наличия доступа
	 * @throws NotesException 
	 */
	public boolean CheckUserAccess2Document(Document pdoc, String psUserName, int pnAccEditor) throws NotesException {
		this.m_vDocAccList = new Vector();

		// Приведение имени в единую форму
		String sUserName = this.m_oSession.createName(psUserName).getAbbreviated();
		
		// Наличие запрашиваемого доступа к базе
		int nUserDbAccLevel = pdoc.getParentDatabase().queryAccess(sUserName);
		if (pnAccEditor == ACCESS_READ && nUserDbAccLevel < ACL.LEVEL_READER) return false;
		else if (pnAccEditor == ACCESS_EDIT && nUserDbAccLevel < ACL.LEVEL_AUTHOR) return false;
		else if (pnAccEditor != ACCESS_EDIT && pnAccEditor != ACCESS_READ) return false;
		
		int nCheckAcc = ACCESS_READ;
		if (pnAccEditor == ACCESS_EDIT) {
			switch (nUserDbAccLevel) {
			case ACL.LEVEL_AUTHOR:
				nCheckAcc = ACCESS_EDIT;
				break;
			}
		}
		
		// Получение списка сущностей имеющих доступ
		this.m_vDocAccList = this.getAccessListFromDocument(pdoc, nCheckAcc);
//System.out.println("LIST: " + this.m_vDocAccList.toString());
		
		// Проверка по ролям пользователя
		Vector vUserDbAccRoles = pdoc.getParentDatabase().queryAccessRoles(sUserName);
		for (int ni = 0; ni < vUserDbAccRoles.size(); ni++) {
			if (this.getLNAB().isIncludeNamesList((String) vUserDbAccRoles.get(ni), this.m_vDocAccList)) {
				// Доступ грантован ролью пользователя - разворачиваем роли в списке
				this.m_vDocAccList = this.replaceRolesWithACLEntries(this.m_vDocAccList, pdoc.getParentDatabase());
				if (this.m_vDocAccList.size() > 0) {
					// уникальный список сущностей
					String sFla = this.m_vDocAccList.toString();
					sFla = sFla.replaceAll("\\[", "@unique(@trim(\"");
					sFla = sFla.replaceAll("\\]", "\"));");
					sFla = sFla.replaceAll(", ", "\":\"");
					this.m_vDocAccList = this.m_oSession.evaluate(sFla);
				}
				return true;
			}
		}
		
		// Проверка пользователя в списке имен (с учетом групп)
		return this.getLNAB().isIncludeNamesList(sUserName, this.m_vDocAccList);
	}
	
	/**
	 * Получение текущего списка доступа (инициализация - в CheckUserAccessToDocument)
	 * @return Текущий список доступа
	 */
	public Vector GetCurrentDocAccList() {
		return this.m_vDocAccList;
	}
	
	
	
	
	/**
	 * Кеширование объекта LNAddressBook
	 * @return LNAddressBook
	 * @throws NotesException
	 */
	private LNAddressBook getLNAB() throws NotesException {
		if (this.m_oPAB == null) {
			this.m_oPAB = new LNAddressBook(this.m_oSession);
		}
		return this.m_oPAB;
	}
	
	/**
	 * Получение списка сущностей имеющих доступ к документу, на основании флага
	 *		pbAccEditor = false - на чтение
	 *		pbAccEditor = true - на редактирование
	 * @param pdoc - документ, с которого вычитывается доступ
	 * @param pbAccEditor - флаг требуемого доступа
	 * @return Список сущностей (уникальные значения)
	 * @throws NotesException 
	 */
	private Vector getAccessListFromDocument(Document pdoc, int pnAccLevel) throws NotesException {
		Vector vFieldsList = new Vector();
		Vector vItems = pdoc.getItems();
		int nICount = vItems.size();
		
		for (int ni = 0; ni < nICount; ni++) {
			Item oItem = (Item) vItems.get(ni);
			if (pnAccLevel == ACCESS_READ && (oItem.getType() == Item.READERS || oItem.getType() == Item.AUTHORS)) {
			// Просмотр
				vFieldsList.add(oItem.getName());
			} else if (pnAccLevel == ACCESS_EDIT && oItem.getType() == Item.AUTHORS) {
			// Редактирование
				vFieldsList.add(oItem.getName());
			}
		}
//System.out.println("FIELDS: " + vFieldsList.toString());
		
		Vector vAccList = new Vector();
		if (vFieldsList.size() > 0) {
			// уникальный список сущностей
			String sFla = vFieldsList.toString();
			sFla = sFla.replaceAll("\\[", "@unique(@trim(");
			sFla = sFla.replaceAll("\\]", "));");
			sFla = sFla.replaceAll(", ", ":");
			vAccList = this.m_oSession.evaluate(sFla, pdoc);
		}

		return vAccList;
	}
	
	/**
	 * "Разворачивание" ролей (Замена в списке ролей на сущности из ACL, у которых данные роли присутствуют)
	 * @param vSrc - Исходный список имен
	 * @param pdb - БД-контекст
	 * @return Адаптированный список
	 * @throws NotesException 
	 */
	private Vector replaceRolesWithACLEntries(Vector vSrc, Database pdb) throws NotesException {
		Vector vRes = new Vector();
		String sEntry = "";
		for (int ni = 0; ni < vSrc.size(); ni++) {
			sEntry = (String) vSrc.get(ni);
			if (sEntry.charAt(0) == '[') {
				vRes.addAll(this.getACLEntriesByRole(sEntry, pdb));
			} else {
				vRes.add(sEntry);
			}
		}
		return vRes;
	}
	
	/**
	 * Получение сущностей из ACL, которым выдана указанная роль
	 * @param psRole - Роль
	 * @param pdb - БД-контекст
	 * @return Список сущностей
	 * @throws NotesException 
	 */
	private Vector getACLEntriesByRole(String psRole, Database pdb) throws NotesException {
		ACL oACL = pdb.getACL();
		ACLEntry oACLEntry = null;
		Vector vRoles = null;
		
		Vector vRes = new Vector();
		oACLEntry = oACL.getFirstEntry();
		while (oACLEntry != null) {
			vRoles = oACLEntry.getRoles();
			if (vRoles.indexOf(psRole) != -1) {
				vRes.add(oACLEntry.getName());
			}
			
			oACLEntry = oACL.getNextEntry(oACLEntry);
		}
		
		return vRes;
	}
}
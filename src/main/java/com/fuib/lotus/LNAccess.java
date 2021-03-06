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
	protected LNPAB m_oPAB = null;
	protected Vector<String> m_vDocAccList = null;
	
	public LNAccess(Session poSession) throws NotesException {
		this.m_oSession = poSession;
	}
	
	/**
	 * ����������� ������� LNPAB
	 * @return LNPAB
	 * @throws NotesException
	 */
	private LNPAB getLNAB() throws NotesException {
		if (this.m_oPAB == null) {
			this.m_oPAB = new LNPAB(this.m_oSession, true);
		}
		return this.m_oPAB;
	}
	
	/**
	 * �������� ������� ������������ � ���������
	 *		pbAccEditor = false - �� ������
	 *		pbAccEditor = true - �� ��������������
	 * @param pdoc - ��������, ������ � �������� �����������
	 * @param psUserName - NN ������������
	 * @param pbAccEditor - ���� ���������� �������
	 * @return ���� ������� �������
	 * @throws NotesException 
	 */
	public boolean CheckUserAccess2Document(Document pdoc, String psUserName, int pnAccEditor) throws NotesException {
		this.m_vDocAccList = new Vector<String>();

		// ���������� ����� � ������ �����
		String sUserName = this.m_oSession.createName(psUserName).getAbbreviated();
		
		// ������� �������������� ������� � ����
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
		
		// ��������� ������ ��������� ������� ������
		this.m_vDocAccList = this.getAccessListFromDocument(pdoc, nCheckAcc);
//System.out.println("LIST: " + this.m_vDocAccList.toString());
		
		// �������� �� ����� ������������
		Vector<String> vUserDbAccRoles = pdoc.getParentDatabase().queryAccessRoles(sUserName);
		for (int ni = 0; ni < vUserDbAccRoles.size(); ni++) {
			if (this.getLNAB().isIncludeNamesList((String) vUserDbAccRoles.get(ni), this.m_vDocAccList)) {
				// ������ ��������� ����� ������������ - ������������� ���� � ������
				this.m_vDocAccList = this.replaceRolesWithACLEntries(this.m_vDocAccList, pdoc.getParentDatabase());
				if (this.m_vDocAccList.size() > 0) {
					// ���������� ������ ���������
					String sFla = this.m_vDocAccList.toString();
					sFla = sFla.replaceAll("\\[", "@unique(@trim(\"");
					sFla = sFla.replaceAll("\\]", "\"));");
					sFla = sFla.replaceAll(", ", "\":\"");
					this.m_vDocAccList = this.m_oSession.evaluate(sFla);
				}
				return true;
			}
		}
		
		// �������� ������������ � ������ ���� (� ������ �����)
		return this.getLNAB().isIncludeNamesList(sUserName, this.m_vDocAccList);
	}
	
	/**
	 * ��������� �������� ������ ������� (������������� - � CheckUserAccessToDocument)
	 * @return ������� ������ �������
	 */
	public Vector<String> GetCurrentDocAccList() {
		return this.m_vDocAccList;
	}
	
	/**
	 * ��������� ������ ��������� ������� ������ � ���������, �� ��������� �����
	 *		pbAccEditor = false - �� ������
	 *		pbAccEditor = true - �� ��������������
	 * @param pdoc - ��������, � �������� ������������ ������
	 * @param pbAccEditor - ���� ���������� �������
	 * @return ������ ��������� (���������� ��������)
	 * @throws NotesException 
	 */
	private Vector<String> getAccessListFromDocument(Document pdoc, int pnAccLevel) throws NotesException {
		Vector<String> vFieldsList = new Vector<String>();
		Vector<Item> vItems = pdoc.getItems();
		int nICount = vItems.size();
		
		for (int ni = 0; ni < nICount; ni++) {
			Item oItem = (Item) vItems.get(ni);
			if (pnAccLevel == ACCESS_READ && (oItem.getType() == Item.READERS || oItem.getType() == Item.AUTHORS)) {
			// ��������
				vFieldsList.add(oItem.getName());
			} else if (pnAccLevel == ACCESS_EDIT && oItem.getType() == Item.AUTHORS) {
			// ��������������
				vFieldsList.add(oItem.getName());
			}
		}
//System.out.println("FIELDS: " + vFieldsList.toString());
		
		Vector<String> vAccList = new Vector<String>();
		if (vFieldsList.size() > 0) {
			// ���������� ������ ���������
			String sFla = vFieldsList.toString();
			sFla = sFla.replaceAll("\\[", "@unique(@trim(");
			sFla = sFla.replaceAll("\\]", "));");
			sFla = sFla.replaceAll(", ", ":");
			vAccList = this.m_oSession.evaluate(sFla, pdoc);
		}

		return vAccList;
	}
	
	/**
	 * "��������������" ����� (������ � ������ ����� �� �������� �� ACL, � ������� ������ ���� ������������)
	 * @param vSrc - �������� ������ ����
	 * @param pdb - ��-��������
	 * @return �������������� ������
	 * @throws NotesException 
	 */
	private Vector<String> replaceRolesWithACLEntries(Vector<String> vSrc, Database pdb) throws NotesException {
		Vector<String> vRes = new Vector<String>();
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
	 * ��������� ��������� �� ACL, ������� ������ ��������� ����
	 * @param psRole - ����
	 * @param pdb - ��-��������
	 * @return ������ ���������
	 * @throws NotesException 
	 */
	private Vector<String> getACLEntriesByRole(String psRole, Database pdb) throws NotesException {
		ACL oACL = pdb.getACL();
		ACLEntry oACLEntry = null;
		Vector<String> vRoles = null;
		
		Vector<String> vRes = new Vector<String>();
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
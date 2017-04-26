package com.fuib.lotus;

import lotus.domino.Document;
import lotus.domino.NotesException;

/**
 * ����� LD2BOW - Lotus Document to Business Object Wrapper
 * 		������� ����� ������� ���������� Lotus ��� �������-��������
 * 		����������� �� ������ ������������ ������
 * 		��� �������� ������� ������������� ������� ������������ ����� IsInitialize
 * @author pashun
 *
 */
public class LD2BOW {
	protected LNEnvironment m_env = null;
	// ����������� ��������
	protected Document m_doc = null;
	// ���� ������� ������������� �������
	protected boolean m_bIsInitialize;
	
	/**
	 * ������������� �� ������� ���������
	 * @param pcore Core ������ ����
	 * @param pdoc Document ������ ���������
	 * @throws NotesException 
	 */
	public LD2BOW(LNEnvironment penv, Document pdoc) throws NotesException {
		m_env = penv;
		if (pdoc != null) {
			m_doc = pdoc;
			m_bIsInitialize = isValidDocument();
		}
	}
	
	public void Recycle() throws NotesException {}
	
	// ========================================================
	// ��������
	/**
	 * ���� ������� �������������
	 */
	public boolean IsInitialize() {
		return m_bIsInitialize;
	}
	
	public Document Doc() {
		return m_doc;
	}
	
	public String UNID() throws NotesException {
		if (m_bIsInitialize) return m_doc.getUniversalID();
		else return "";
	}
	// ��������
	// ========================================================

	/**
	 * ���������� ��������� � �������-������� (���������� ������� ���������)
	 */
	public void Update() throws NotesException {
		m_doc.save(true);
	}
	
	/**
	 * ��������: �������� �� ��������� �������� �������� ����������
	 */
	protected boolean isValidDocument() throws NotesException {
		if (m_doc != null)
			return true;
		return false;
	}
}
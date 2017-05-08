package com.fuib.lotus.agents.report;

import java.io.IOException;

import com.fuib.lotus.agents.LNAgentBase;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;

/**
 * @date Dec 26, 2014
 * @author evochko 
 * @Description ����������� ����� ��� ���������� ������� � ������� Lotus
 * <br> � ����������� ���������� ���������� ��������� ������:
 * <br> <b>addLineToReport</b> - ��� ���������� ������ � �����
 * <br> <b>send</b> - ��� ���������� �������� �� �������� ������ ���������
 * <br> <b>close</b> - ��������� �������� �� �������� � ������� ������������ ��������
 */
public abstract class AbstructReportBuilder {

	private LNAgentBase agentBase = null;
	
	private String sRepName = "";

	protected String sRepTitle = "";

	public String getSRepName() {
		return sRepName;
	}

	public void setSRepName(String repName) {
		sRepName = repName;
	}
	
	public String getRepTitle() {
		return sRepTitle;
	}

	public void setRepTitle(String repTitle) {
		sRepTitle = repTitle;
	}

	public LNAgentBase getAgentBase() {
		return agentBase;
	}

	public void setAgentBase(LNAgentBase agentBase) {
		this.agentBase = agentBase;
	}
	
	/**
	 * @author evochko 
	 * @param sLine - ������ ������
	 * @param doc - ��������, ������� ������������� ���� ������
	 * @Description ���������� ������ � �����
	 */
	public abstract void addLineToReport(String sLine, Document doc) throws IOException, NotesException;
	
	/**
	 * @author evochko 
	 * @param sMemoSubj
	 * @param sAppendText
	 * @param dbForCreateMemo
	 * @return void
	 * @Description ���������� �������� �� �������� ������ ���������
	 */
	public abstract void send(String sMemoSubj, String sAppendText, Database dbForCreateMemo) throws NotesException;
	
	/**
	 * @author evochko 
	 * @Description ��������� �������� �� �������� � ������� ������������ ��������
	 */
	public void close() 	{
		agentBase = null;
	}

}

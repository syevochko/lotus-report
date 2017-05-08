package com.fuib.lotus.agents.report;

import java.io.IOException;

import com.fuib.lotus.agents.LNAgentBase;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;

/**
 * @date Dec 26, 2014
 * @author evochko 
 * @Description абстрактный класс для построения отчетов в агентах Lotus
 * <br> В наследниках необходимо определить следующие методы:
 * <br> <b>addLineToReport</b> - для добавления строки в отчет
 * <br> <b>send</b> - для выполнения действий по доставке отчета адресатам
 * <br> <b>close</b> - финальные действия по закрытию и очистки используемых объектов
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
	 * @param sLine - строка отчета
	 * @param doc - документ, который соответствует этой строке
	 * @Description добавление строки в отчет
	 */
	public abstract void addLineToReport(String sLine, Document doc) throws IOException, NotesException;
	
	/**
	 * @author evochko 
	 * @param sMemoSubj
	 * @param sAppendText
	 * @param dbForCreateMemo
	 * @return void
	 * @Description выполнение действий по доставке отчета адресатам
	 */
	public abstract void send(String sMemoSubj, String sAppendText, Database dbForCreateMemo) throws NotesException;
	
	/**
	 * @author evochko 
	 * @Description финальные действия по закрытию и очистки используемых объектов
	 */
	public void close() 	{
		agentBase = null;
	}

}

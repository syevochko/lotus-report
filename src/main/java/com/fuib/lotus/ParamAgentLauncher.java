package com.fuib.lotus;

import java.util.Vector;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Agent;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.NotesException;

public class ParamAgentLauncher {
	protected Database m_dbAgentHolder = null;
	protected Agent m_Agent = null;
	protected Database m_dbDocParamHolder = null;	
	
	public Document m_docParam = null;
	protected String m_sNoteID = "";
	
	// constructors
	public ParamAgentLauncher()	{}
	public ParamAgentLauncher(Database dbAgentHolder, Database dbDocParamHolder, String sAgentName) throws NotesException	{
		this.setLaunchAgent( dbAgentHolder, sAgentName);
		this.createDocParam( dbDocParamHolder );
	}

	public Database getDBAgentHolder()				{ return m_dbAgentHolder; }
	public Database getDBDocParamHolder()			{ return m_dbDocParamHolder; }

	public void setLaunchAgent ( Database dbHolder, String sAgentName ) throws NotesException	{
		if (sAgentName.equals(""))
			throw new NotesException(1001, "Agent name to launch isn't define/open!");

		m_dbAgentHolder = dbHolder;
		if (m_dbAgentHolder == null || !m_dbAgentHolder.isOpen())
			throw new NotesException(1002, "Database object <dbAgentHolder> isn't define/open!");

		if (m_Agent == null || (m_Agent != null && !m_Agent.getName().toLowerCase().equals(sAgentName.toLowerCase()))) {
			Tools.recycleObj(m_Agent);
			m_Agent = m_dbAgentHolder.getAgent(sAgentName);
		}
	}

	public void createDocParam() throws NotesException	{
		if(m_docParam == null)
			m_docParam = m_dbDocParamHolder.createDocument();
	}
	public void createDocParam( Database dbHolder ) throws NotesException	{
		m_dbDocParamHolder = dbHolder;
		this.createDocParam();
	}
	
	public Document getDocParam()	{ return m_docParam; }	
		
	public void copySourceItemsToDocParam( String[] itNames, Document docSource ) throws NotesException	{
		Item itSrc = null;
		
		this.createDocParam();
		for(int i=0; i<itNames.length; i++)		{
			if( docSource.hasItem(itNames[i]) )	{
				itSrc = docSource.getFirstItem(itNames[i]);
				Tools.recycleObj(m_docParam.copyItem(itSrc));
				Tools.recycleObj(itSrc);
			}
		}
	}
	
	public void replaceItemValue(String sItemName, Object value) throws NotesException	{
		this.createDocParam();
		m_docParam.replaceItemValue(sItemName, value).recycle();
	}
	
	public void appendToTextList(String sItemName, Vector value) throws NotesException	{		
		this.createDocParam();
		if (m_docParam.hasItem(sItemName) && 
				(m_docParam.getItemValue(sItemName).size()>1 || m_docParam.getItemValueString(sItemName).length()!=0)) {
			Item it = m_docParam.getFirstItem(sItemName);
			it.appendToTextList(value);
			Tools.recycleObj(it);
		}
		else
			this.replaceItemValue(sItemName, value);
	}
	
	public void appendToTextList(String sItemName, String value) throws NotesException	{
		Vector v = new Vector();
		v.addElement(value);
		appendToTextList(sItemName, v);
	}
	
	
	public void launchAgent() throws NotesException {
		m_docParam.save(true);
		m_sNoteID = m_docParam.getNoteID();
		Tools.recycleObj(m_docParam);
		m_Agent.runOnServer(m_sNoteID);
		m_docParam = m_dbDocParamHolder.getDocumentByID(m_sNoteID);
	}
	
	public String getLaunchResult(String sResultField) throws NotesException {		
		return (m_docParam.hasItem(sResultField))?m_docParam.getItemValueString(sResultField):"";
	}	
	
	protected void processLaunch()	{
	//TODO	- Your code goes here - redefine this method.
	/*	this is an example:
	 * 

		setLaunchAgent ( dbAgent, "SomeAgent" );
		createDocParam( dbDocParamHolder );
		
		// some actions with m_docParam ( direct use of m_docParam or via getDocParam() method )
		
		LaunchAgent();
		System.out.println(GetLaunchResult("FIELD_WITH_RESULT_OR_ERROR"));		
		
		*/		
	}
	
	public void recycle() throws NotesException	{
		Tools.recycleObj(m_Agent);
		
		if (m_docParam != null)
			m_docParam.remove(true);
		
		m_dbAgentHolder = null;		// clear link on object		
		
		m_dbDocParamHolder = null;
	}
}

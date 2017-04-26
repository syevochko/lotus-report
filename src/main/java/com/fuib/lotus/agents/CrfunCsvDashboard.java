package com.fuib.lotus.agents;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;

import com.fuib.lotus.LNEnvironment;
import com.fuib.lotus.agents.LNAgentCSVFileBase;
import com.fuib.lotus.agents.report.SingleFileReportBuilder;
import com.fuib.lotus.utils.Tools;

public class CrfunCsvDashboard extends LNAgentCSVFileBase {

	private class MySingleFileReportBuilder extends SingleFileReportBuilder	{
		@Override
		public void send(String sMemoSubj, String sAppendText, Database dbForCreateMemo) throws NotesException {
			Document docFile = null;
			RichTextItem rtItem = null;
			File fileCsv = null;
			
			try	{
				setDeleteOnClose(false);
				close();
				setDeleteOnClose(true);
				
				fileCsv = getReportFile();
				
				docFile = dbForCreateMemo.createDocument();
				rtItem = docFile.createRichTextItem("Body");			
				docFile.replaceItemValue("Form", "Memo").recycle();
				docFile.replaceItemValue("From", dbForCreateMemo.getServer()).recycle();		
				docFile.replaceItemValue("Subject", m_sAgName + " - created file: " + fileCsv.getName()).recycle();
				docFile.replaceItemValue("$$DocType", "CSV_FILE").recycle();
				docFile.replaceItemValue("Status", Integer.valueOf(1)).recycle();
				
				rtItem.embedObject(EmbeddedObject.EMBED_ATTACHMENT, "", fileCsv.getAbsolutePath(), "");
				docFile.send(false, "Vasya Pupkin/dho/fuib");
				System.out.print(getRepBuilder().getRepTitle());
				
//				docFile.save(true, true);
				
				logAction(m_sAgName + ": сформирован документ с файлом (" + fileCsv.getName() + ") в базе " + dbForCreateMemo.getFilePath() );
			} catch(NotesException ne)	{
				ne.printStackTrace();
				throw new NotesException(LNEnvironment.ERR_CUSTOM, "Error while create doc in " 
						+ dbForCreateMemo.getFilePath() + " to attach file.");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				Tools.recycleObj(rtItem);
				Tools.recycleObj(docFile);
			}
		}
	}
	
	public CrfunCsvDashboard() {
		setProfileForm("WFAgent");
		bIsFileForSQLImport = true;
		SingleFileReportBuilder repBuilder = new MySingleFileReportBuilder();
		setRepBuilder(repBuilder);
//		m_sCfgProfileName = "CreateCSV_KBPerformanceDashboard";
	}

	@Override
	protected void checkRequiredParams() throws Exception {
		if (!m_mapConfig.containsKey(PARAM_FILE_PREFIX))
			throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <"+PARAM_FILE_PREFIX+"> in agent profile!");

		if (!m_mapConfig.containsKey(PARAM_SELECTION_FORMULA))
			throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <"+PARAM_SELECTION_FORMULA+"> in agent profile!");	
	}
	
	@Override
	protected void loadConfiguration() throws Exception {		
		loadConfiguration(m_env.getDbControl(), LNEnvironment.VIEW_SETTINGS);
	}
		
	@Override
	protected void loadColumnConfiguration() throws Exception {
		super.loadColumnConfiguration();
		getRepBuilder().setRepTitle(getParamCols().getTitle());
		final SimpleDateFormat oFileNameFormatter = new SimpleDateFormat("yyyy-MM-ddHHmmss");
		((SingleFileReportBuilder)getRepBuilder()).setReportFileName(m_mapConfig.get(PARAM_FILE_PREFIX) + oFileNameFormatter.format(new Date()) + ".csv");
	}
	
	@Override
	protected String[] getTargetDbPaths() {
		String[] dbPaths = new String[2];
		try {
			dbPaths[0] = m_env.getWFDbPath("DB_CASH");
			dbPaths[1] = m_env.getWFDbPath("ARCH_WFCASH");
		} catch (NotesException e) {
			e.printStackTrace();
		}
		return dbPaths;
	}

}
package com.fuib.lotus.agents.report;

import com.fuib.lotus.LNEnvironment;
import com.fuib.lotus.agents.report.builder.SingleFileReportBuilder;
import lotus.domino.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CSVDashboardKB extends LNAgentCSVFileBase {

    private class MySingleFileReportBuilder extends SingleFileReportBuilder {
        @Override
        public void send(String sMemoSubj, String sAppendText, Database dbForCreateMemo) throws NotesException {
            Database dbTrg = null;
            Document docFile = null;
            RichTextItem rtItem = null;
            File fileCsv = null;

            try {
                dbTrg = getAgentBase().getLNEnv().getDatabase((String) m_mapConfig.get(PARAM_DBTRG_PATH));

                setDeleteOnClose(false);
                close();
                setDeleteOnClose(true);

                fileCsv = getReportFile();

                docFile = dbTrg.createDocument();
                rtItem = docFile.createRichTextItem("Body");
                docFile.replaceItemValue("Form", "Memo").recycle();
                docFile.replaceItemValue("From", dbTrg.getServer()).recycle();
                docFile.replaceItemValue("Subject", m_sAgName + " - created file: " + fileCsv.getName()).recycle();
                docFile.replaceItemValue("$$DocType", "CSV_FILE").recycle();
                docFile.replaceItemValue("Status", Integer.valueOf(1)).recycle();

                rtItem.embedObject(EmbeddedObject.EMBED_ATTACHMENT, "", fileCsv.getAbsolutePath(), "");
                docFile.save(true, true);

                logAction(m_sAgName + ": сформирован документ с файлом (" + fileCsv.getName() + ") в базе " + dbTrg.getFilePath());

            } catch (NotesException ne) {
                throw new NotesException(LNEnvironment.ERR_CUSTOM, "Error while create doc in " + dbTrg.getFilePath() + " to attach file.");

            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                if (rtItem != null)
                    rtItem.recycle();
                if (docFile != null)
                    docFile.recycle();
                if (dbTrg != null)
                    dbTrg.recycle();
            }

        }
    }

    public CSVDashboardKB() {
        setProfileForm("AgentConfig");
        bIsFileForSQLImport = true;
        setRepBuilder(new MySingleFileReportBuilder());
//		m_sCfgProfileName = "CreateCSV_KBPerformanceDashboard";
    }

    @Override
    protected void checkRequiredParams() throws Exception {
        if (!m_mapConfig.containsKey(PARAM_FILE_PREFIX))
            throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <" + PARAM_FILE_PREFIX + "> in agent profile!");

        if (!m_mapConfig.containsKey(PARAM_SELECTION_FORMULA))
            throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <" + PARAM_SELECTION_FORMULA + "> in agent profile!");
    }

    @Override
    protected void loadColumnConfiguration() throws Exception {
        super.loadColumnConfiguration();

        final SimpleDateFormat oFileNameFormatter = new SimpleDateFormat("yyyy-MM-ddHHmmss");
        ((SingleFileReportBuilder) getRepBuilder()).setReportFileName(m_mapConfig.get(PARAM_FILE_PREFIX) + oFileNameFormatter.format(new Date()) + ".csv");
    }

    @Override
    protected String[] getTargetDbPaths() {
        String[] dbPaths = new String[1];
        try {
            dbPaths[0] = m_dbCurrent.getFilePath();
        } catch (NotesException e) {
            e.printStackTrace();
        }
        return dbPaths;
    }

}
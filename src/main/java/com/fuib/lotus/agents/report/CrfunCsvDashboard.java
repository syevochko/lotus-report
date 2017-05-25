package com.fuib.lotus.agents.report;

import com.fuib.lotus.LNEnvironment;
import com.fuib.lotus.agents.report.params.ParamDocColSet;
import com.fuib.lotus.agents.report.params.ParamDocColumn;
import com.fuib.lotus.agents.report.params.values.AbstractColumnValue;
import com.fuib.lotus.agents.report.builder.SearchDBReportBuilder;
import com.fuib.lotus.agents.report.builder.SingleFileReportBuilder;
import com.fuib.lotus.utils.Tools;
import com.fuib.util.WorkTimeBetweenTwoDates;
import lotus.domino.Base;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CrfunCsvDashboard extends LNAgentCSVFileBase {
    private Map<String, Base> baseMap;
    private WorkTimeBetweenTwoDates datesDiff;
    private List<Database> dbTargets;

    public CrfunCsvDashboard() {
        setProfileForm("WFAgent");
        bIsFileForSQLImport = true;
        SearchDBReportBuilder repBuilder = new SearchDBReportBuilder() {
            @Override
            public String getRepTitle() {
                if (sRepTitle.length() == 0) {
                    StringBuilder sb = new StringBuilder();
                    for (ParamDocColumn col : getParamCols().getColumns()) {
                        sb.append(col.getColDescription()).append(";");
                    }
                    sRepTitle = sb.substring(0, sb.length() - 1);
                }
                return sRepTitle;
            }
            
            public void send(String sMemoSubj, String sAppendText, Database dbForCreateMemo) throws NotesException {
                Document docFile = null;
                RichTextItem rtItem = null;
                File fileCsv = null;

                try {
                    setDeleteOnClose(false);
                    close();
                    setDeleteOnClose(true);

                    fileCsv = getReportFile();

                    docFile = dbForCreateMemo.createDocument();
                    docFile.replaceItemValue("Form", "Memo").recycle();
                    docFile.replaceItemValue("From", dbForCreateMemo.getServer()).recycle();
                    docFile.replaceItemValue("Subject", m_sAgName + " - created file: " + fileCsv.getName()).recycle();
                    docFile.replaceItemValue("$$DocType", "CSV_FILE").recycle();
                    docFile.replaceItemValue("Status", Integer.valueOf(1)).recycle();

                    String searchPathDB = m_env.getWFDbPath("ENV_SEARCH");
                    Database dbSearch = m_env.getDatabase(searchPathDB);
                    Document doc = createSearchResultDoc(dbSearch, fileCsv);
                    rtItem = docFile.createRichTextItem("Body");
                    rtItem.appendText("����� " + fileCsv.getName() + " � ����������� ���������, � ���� ��: ����� ����������: ");
                    rtItem.addNewLine();
                    rtItem.appendText(doc.getNotesURL());
                    docFile.send(false, m_session.getUserName());

                    doc.recycle();
                } catch (NotesException ne) {
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
        };

        setRepBuilder(repBuilder);
        baseMap = new HashMap<String, Base>();
        try {
            datesDiff = new WorkTimeBetweenTwoDates(m_session);
            datesDiff.setInitializeExcludeList(true);
        } catch (NotesException e) {
            e.printStackTrace();
        }
        m_sCfgProfileName = this.getClass().getSimpleName();
        dbTargets = new ArrayList<Database>(2);
    }

    @Override
    protected void process() throws Exception {
        super.process();
        for (Base base : baseMap.values()) {
            Tools.recycleObj(base);
        }
    }

    @Override
    protected void checkRequiredParams() throws Exception {
        if (!m_mapConfig.containsKey(PARAM_FILE_PREFIX))
            throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <" + PARAM_FILE_PREFIX + "> in agent profile!");

        if (!m_mapConfig.containsKey(PARAM_SELECTION_FORMULA))
            throw new NotesException(ERR_REQUIRED_PARAM, "Expected parameter <" + PARAM_SELECTION_FORMULA + "> in agent profile!");
    }

    @Override
    protected void loadConfiguration() throws Exception {
        loadConfiguration(m_env.getDbControl(), LNEnvironment.VIEW_SETTINGS);
    }

    @Override
    protected void loadColumnConfiguration() throws Exception {
        super.loadColumnConfiguration();
        ParamDocColSet colSet = getParamCols();
        List<AbstractColumnValue> colObjList = colSet.getColumnValueObjects();
        for (AbstractColumnValue cv : colObjList) {
            cv.setEnv(m_env);
            cv.setDatesDiff(datesDiff);
            cv.setTargetDatabases(getTargetDBs());
        }

        final SimpleDateFormat oFileNameFormatter = new SimpleDateFormat("yyyy-MM-ddHHmmss");
        ((SingleFileReportBuilder) getRepBuilder()).setReportFileName(m_mapConfig.get(PARAM_FILE_PREFIX) + oFileNameFormatter.format(new Date()));
    }

    private List<Database> getTargetDBs() throws NotesException {
        if (dbTargets.isEmpty()) {
            List<String> dbPaths = new ArrayList<String>(Arrays.asList(getTargetDbPaths()));
            for (String path : dbPaths) {
                Database db = m_env.getDatabase(path);
                dbTargets.add(db);
            }
        }
        return dbTargets;
    }

}
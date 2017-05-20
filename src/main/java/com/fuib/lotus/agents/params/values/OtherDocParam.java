package com.fuib.lotus.agents.params.values;

import com.fuib.lotus.agents.params.ColumnValueFactory;
import com.fuib.lotus.agents.params.ParamDocColSet;
import com.fuib.lotus.utils.Tools;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//#{OtherDocParam doc_formula='@Word(@Subset(fdRelated2;1); "##"; 1)' state_id='CRFVE_OnVerify'}
public class OtherDocParam extends AbstractColumnValue {
    protected static final Pattern VALUES_PATTERN = Pattern.compile("doc_formula='([^']+)'\\s*(.*)");

    protected String docFormula = "";
    protected String partValue = "";

    public OtherDocParam(String value, ParamDocColSet parentColumnSet) {
        super(value, parentColumnSet);
        Matcher m = VALUES_PATTERN.matcher(colValue);
        if (m.matches()) {
            docFormula = m.group(1);
            partValue = m.group(2);
        }
    }

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
    	AbstractColumnValue colObj;
        Document docLinked = getLinkedDoc(doc);
        
        if (docLinked == null) {
            colObj = new DocNotFoundValue("", getParent());
        } else {
            colObj = createNestedColumnObject();
            colObj.setDatesDiff(getDatesDiff());
            colObj.setEnv(getEnv());
        }
        return colObj.getColumnValue(docLinked);
    }

    protected AbstractColumnValue createNestedColumnObject() {
        return ColumnValueFactory.getColumnObject(partValue, getParent());
    }

    protected Document getLinkedDoc(Document doc) throws NotesException {
        Vector vUnid = getEnv().getSession().evaluate(docFormula, doc);
        if (vUnid != null && !vUnid.isEmpty()) {
            String unid = (String) vUnid.get(0);
            Document docLinked = (Document) getParent().getLotusObjectsMap().get(unid);
            if (docLinked == null) {
                for (Database db : targetDatabases) {
                    docLinked = Tools.getDocumentByUNID(unid, db);
                    if (docLinked != null && !docLinked.getItems().isEmpty()) {
                        getParent().getLotusObjectsMap().put(unid, docLinked);
                        return docLinked;
                    }
                }
            } else	{
            	return docLinked;
            }
        }
        return null;
    }
}

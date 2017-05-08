package com.fuib.lotus.agents.params.values;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Database;

public class OtherDocWorkTimeHourDiffValue extends AbstractColumnValue {
    private static final Pattern VALUES_PATTERN = Pattern.compile("doc_formula='([^']+)'\\s*(.*)");
    
    private String docFormula = "";
    private String partValue = "";
    
	public OtherDocWorkTimeHourDiffValue(String value) {
		//#{OtherDocWorkTimeDiffDaysValue doc_formula='@Word(@Subset(fdRelated2;1); "##"; 1)' state_id='CRFUN_Created' log_contains='O=fuib'}
        super(value);
        Matcher m = VALUES_PATTERN.matcher(colValue);
        if (m.matches()) {
        	docFormula = m.group(1);
        	partValue = m.group(2);
        	System.out.println(docFormula);
        	System.out.println(partValue);
        }
	}

	@Override
	public Vector getColumnValue(Document doc) throws NotesException {
		Document docLinked = getLinkedDoc(doc);
		AbstractColumnValue colObj;
		if (docLinked == null)	{
			colObj = new UndefineValue("");
		} else	{
			colObj = new WorkTimeDiffDaysValue(partValue);
			colObj.setBaseMap(getBaseMap());
			colObj.setDatesDiff(getDatesDiff());
			colObj.setEnv(getEnv());
		}
		return colObj.getColumnValue(docLinked);
	}
	
	protected Document getLinkedDoc(Document doc) throws NotesException	{
		Vector vUnid = getEnv().getSession().evaluate(docFormula, doc);
		if (vUnid != null &&  !vUnid.isEmpty())	{
			String unid = (String) vUnid.get(0);
			Document docLinked = (Document) getBaseMap().get(unid);
			if (docLinked == null)	{
				for (Database db : targetDatabases)	{
					docLinked = Tools.getDocumentByUNID(unid, db);
					if (docLinked != null && !docLinked.getItems().isEmpty())	{
						getBaseMap().put(unid, docLinked);
						return docLinked;
					}
				}
			}			
		}		
		return null;
	}
}

package com.fuib.lotus.agents.params;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import lotus.domino.Document;
import lotus.domino.NotesException;

/**
 * @date Dec 20, 2014
 * @author evochko 
 * @Description: обработка параметра профиля агента, который должен вычисляться на контексте целевых документов - для формирования csv-файлов для загрузки в SQL-базы данных
 * добавлено поле  bHasSqlNullIfEmpty
 */
public class ParamDoColumnForSQL extends ParamDocColumn {

	private boolean bHasSqlNullIfEmpty = false;
	
	protected String SQL_NULL = "#NA";
	
	public ParamDoColumnForSQL(String paramValue, String paramDescription, boolean bHasSqlNullIfEmpty) {
		super(paramValue, paramDescription);
		setHasSqlNullIfEmpty(bHasSqlNullIfEmpty);
	}

	public boolean isHasSqlNullIfEmpty() { return bHasSqlNullIfEmpty; }
	public void setHasSqlNullIfEmpty(boolean hasSqlNullIfEmpty) { bHasSqlNullIfEmpty = hasSqlNullIfEmpty; }

	/**
	 * {@link ParamDocColumn#processDocColumn(Document, String, DecimalFormat, SimpleDateFormat)}
	 */
	public String processDocColumn(Document doc, String asValueSep, DecimalFormat amyDecFormat, SimpleDateFormat amyDateFormat) throws NotesException 		{
		String sResult = super.processDocColumn(doc, asValueSep, amyDecFormat, amyDateFormat);
		
		if ( sResult.length()>0 )
			return sResult;
		
		else if ( isHasSqlNullIfEmpty() )
			return SQL_NULL;
		
		else
			return "";			
			
	}
}

package com.fuib.lotus.agents.params;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Vector;

import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * @date Dec 20, 2014
 * @author evochko 
 * @Description: обработка параметра профиля агента, который должен вычисляться на контексте целевых документов 
 */
public class ParamDocColumn 	{
	private boolean bIsFormula = false;
	private String sColValue = "";
	private String sColDescription = "";
	
	protected String VALUE_SEP = ",";
	protected static SimpleDateFormat oColsDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	protected static DecimalFormat oColsDoubleFormatter;
	static	{
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator(".".charAt(0));
		oColsDoubleFormatter = new DecimalFormat("", dfs);
		oColsDoubleFormatter.setGroupingUsed(false);
	}

	public ParamDocColumn(String sParamValue, String sParamDescription) 	{
		String sMyParamValue = sParamValue; 
		bIsFormula = sMyParamValue.startsWith("@", 0) && (sMyParamValue.length()>1);
		if (bIsFormula) sMyParamValue = sMyParamValue.substring(1, sMyParamValue.length());
		sColValue = sMyParamValue;
		sColDescription = sParamDescription;
	}
	
	/**
	 *	getters and setters 
	 */
	public boolean isFormula() { return bIsFormula; }
	public void setIsFormula(boolean isFormula) { bIsFormula = isFormula; }
	public String getColumnValue() { return sColValue; }
	public void setColumnValue(String columnValue) { sColValue = columnValue; }
	public void setColDescription(String sColDescription) { this.sColDescription = sColDescription; }
	public String getColDescription() { return sColDescription!=null?sColDescription:""; }

	/**
	 * Получения значения из документа по заданной колонке 
	 * @param doc - обрабатываемый документ
 	 * @param asValueSep - разделитель мульти-значений в результирующей строке
	 * @param amyDecFormat - форматтер числовых значений с плавающей запятой
	 * @param amyDateFormat - форматтер дат
	 * @return строка, содержащая значение колонки в документе. Мульти-значения преобразуются в одну строку с разделителем asValueSep
	 * @throws NotesException 
	 */
	public String processDocColumn(Document doc, String asValueSep, DecimalFormat amyDecFormat, SimpleDateFormat amyDateFormat) throws NotesException 		{
		String sResult = "";
		Vector v = null;
		
		String sValueSep = (asValueSep==null || asValueSep.length()==0) ? VALUE_SEP : asValueSep;
		DecimalFormat myDecFormat = (amyDecFormat==null) ? oColsDoubleFormatter : amyDecFormat;
		SimpleDateFormat myDateFormat = (amyDateFormat==null) ? oColsDateFormatter : amyDateFormat;
		
		if (doc==null || doc.isDeleted() || !doc.isValid())	{
			return "";	
		}
		
		try	{
			Session sess = doc.getParentDatabase().getParent();
			StringBuffer sb = new StringBuffer();
			
			if (this.isFormula())
				v = sess.evaluate(this.getColumnValue(), doc);
			
			else if ( doc.hasItem(this.getColumnValue()) )
				v = doc.getItemValue(this.getColumnValue());				

			if (v!=null && !v.isEmpty())	{
				
				for(int i=0; i<v.size(); i++)	{
					if (v.get(i) instanceof String)
						sb.append( (String)v.get(i) );
					else if (v.get(i) instanceof Double)
						sb.append( myDecFormat.format( ((Double)v.get(i)).doubleValue() ) );
					else if (v.get(i) instanceof DateTime)
						sb.append( myDateFormat.format( ((DateTime)v.get(i)).toJavaDate() ) );
					
					sb.append(sValueSep);
				}

				sb.setLength(sb.length()-sValueSep.length());
				sResult = sb.toString();
			}
			
		} catch (NotesException e) {
			
			throw new NotesException(e.id, e.toString() + " (process unid: "+doc.getUniversalID()+")");

		} finally	{

		}		

		return sResult;
	}
	
	/**
	 * Получения значения из документа по заданной колонке
	 * @param doc - обрабатываемый документ
	 * @return строка, содержащая значение колонки в документе. Мульти-значения преобразуются в одну строку с разделителем VALUE_SEP
	 * @throws NotesException
	 * {@link #processDocColumn(Document, String, DecimalFormat, SimpleDateFormat)} 
	 */
	public String processDocColumn(Document doc) throws NotesException 		{
		return processDocColumn(doc, null, null, null);
	}
	
}

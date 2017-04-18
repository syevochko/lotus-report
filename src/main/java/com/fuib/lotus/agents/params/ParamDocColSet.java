package com.fuib.lotus.agents.params;

import java.util.HashMap;
import java.util.Vector;

import lotus.domino.Document;
import lotus.domino.NotesException;


/**
 * @date Dec 26, 2014
 * @author evochko 
 * @Description ��������������� ������� ���������� ������� ������ (COL - ������� {@link #PARAM_COL_PREFIX}: COl1, COL2...COL{@link #MAX_COL_COUNT} ),
 * ������� ��������� ������� ������, �� ������ ��������� {@link #createRowByDoc(Document)}
 * <br>������������ ����� {@link ParamDocColumn} 
 */
public class ParamDocColSet 	{

	protected String PARAM_COL_PREFIX = "COL";
	protected int MAX_COL_COUNT = 50;
	protected String COL_SEP = ";";
	protected String PARAM_COLS_HAS_SQLNULL = "COLS_HAS_SQLNULL";
	private String sTitle = "";
	private Vector<ParamDocColumn> vColumns = new Vector<ParamDocColumn>();
	
	public Vector<ParamDocColumn> getColumns() {
		return vColumns;
	}
	
	public void setColumns(Vector<ParamDocColumn> columns) {
		vColumns = columns;
	}

	public void insertColumnAt(ParamDocColumn column, int index)	{
		vColumns.insertElementAt(column, index);
		createTitle();
	}
	
	public void addColumn(ParamDocColumn column)	{
		vColumns.add(column);
		createTitle();
	}
	
	public String getTitle() {
		return sTitle;
	}
	
	private void createTitle()	{
		if (!getColumns().isEmpty())	{
			StringBuffer sbTitle = new StringBuffer("");
			
			for(ParamDocColumn col : getColumns())	{
				sbTitle.append(col.getColDescription()).append(COL_SEP);
			}
			
			sbTitle.setLength(sbTitle.length()-COL_SEP.length());
			sTitle = sbTitle.toString();
		}		
	}
	
	/**
	 * @author evochko 
	 * @param mapAgentParams
	 * @param mapParamsDescr
	 * @param bIsSQLColumns
	 * @return void
	 * @Description ������� ����� �� ���������� ������� ������ - �������� �� mapAgentParams � �� �������� mapParamsDescr �� ���������, ������� ���������� � {@link #PARAM_COL_PREFIX} 
	 * � �������� �� � ����� {@link #vColumns}
	 */
	public void setColumns(HashMap mapAgentParams, HashMap mapParamsDescr, boolean bIsSQLColumns) {
		Vector<ParamDocColumn> columns = new Vector<ParamDocColumn>();		
		
		if (mapAgentParams!=null && !mapAgentParams.isEmpty())	{
			
			String sColsHasSqlNull = "";
			if ( bIsSQLColumns && mapAgentParams.containsKey(PARAM_COLS_HAS_SQLNULL))	{
				sColsHasSqlNull = (String)mapAgentParams.get(PARAM_COLS_HAS_SQLNULL);
				sColsHasSqlNull += "-";		// ��������� ������ - �� �����, ��� ���� ����� � ���������� �� ��������� ���������� ��������� ��� �������� ��������� ������� � ������
			}		
			
			for (int i = 1; i <= MAX_COL_COUNT; i++)	{				
				String sColName = PARAM_COL_PREFIX+i;
				
				if ( mapAgentParams.containsKey(sColName) )	{
					String sColumnVal = (String) mapAgentParams.get(sColName);
					String sColDescr = "";
					
					if (!mapParamsDescr.isEmpty())
						sColDescr = (String)mapParamsDescr.get(sColName);
					
					if (!bIsSQLColumns)	{
						columns.add( new ParamDocColumn(sColumnVal, sColDescr) );
					} else	{
						boolean bHasSqlNullIfEmpty = sColsHasSqlNull.matches(".*"+sColName+"[^0-9].*");
						columns.add( new ParamDoColumnForSQL(sColumnVal, sColDescr, bHasSqlNullIfEmpty) );
					}
				}				
			}
			
			createTitle();
		}
		
		vColumns.clear();
		vColumns.addAll(columns);		
	}
	

	/**
	 * �������� ��������������� ������ �� �������� ��������� �������
	 * <br> � ��������� ����������� ��� ��������� ������� � ������� COL1, COL2 � �.�.
	 * <br> ������� ����������� ��������� COL_SEP
	 * @throws NotesException 
	 * 
	 */
	public String createRowByDoc(Document doc) throws NotesException		{
		
		if (doc==null || doc.isDeleted() || !doc.isValid())	{
			return "";	
		}
		
		StringBuffer strBuff = new StringBuffer();

		if (getColumns().isEmpty())
			throw new NotesException(1001, "Columns are not defined!");
			
		
		for(int i=0; i < getColumns().size(); i++)	{
			String sColValue = normalizeString( ((ParamDocColumn)vColumns.get(i)).processDocColumn(doc) );
			strBuff.append(sColValue).append(COL_SEP);
		}
		
		strBuff.setLength(strBuff.length()-COL_SEP.length());
		return strBuff.toString();
	}
	
	/**
	 * ������ ������������ �������� � ������ - ������������ ������   
	 */
	protected String normalizeString( String sInput )	{
		return sInput.replaceAll( "["+COL_SEP+"\\r\\n]", " ");
	}	
	
}

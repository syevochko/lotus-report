package com.fuib.lotus.agents.params;

import com.fuib.lotus.agents.params.values.AbstractColumnValue;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @author evochko
 *         Dec 26, 2014
 *         ��������������� ������� ���������� ������� ������ (COL - ������� {@link #PARAM_COL_PREFIX}: COl1, COL2...COL{@link #MAX_COL_COUNT} ),
 *         ������� ��������� ������� ������, �� ������ ��������� {@link #createRowByDoc(Document)}
 *         <br>������������ ����� {@link ParamDocColumn}
 */
public class ParamDocColSet {

    private static final String PARAM_COL_PREFIX = "COL";
    private static final int MAX_COL_COUNT = 100;
    private static final String COL_SEP = ";";
    private static final String PARAM_COLS_HAS_SQLNULL = "COLS_HAS_SQLNULL";

    protected String valueSep = ",";
    protected SimpleDateFormat oColsDateFormatter;
    protected DecimalFormat oColsDoubleFormatter;
    protected Map<String, Vector<?>> columnValues;

    private String sTitle = "";
    private Vector<ParamDocColumn> vColumns = new Vector<ParamDocColumn>();

    public ParamDocColSet() {
        columnValues = new HashMap<String, Vector<?>>();
        oColsDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator(",".charAt(0));
        oColsDoubleFormatter = new DecimalFormat("#.###", dfs);
        oColsDoubleFormatter.setGroupingUsed(false);
    }

    public Vector<ParamDocColumn> getColumns() {
        return vColumns;
    }

    public void setColumns(Vector<ParamDocColumn> columns) {
        vColumns = columns;
    }

    public void insertColumnAt(ParamDocColumn column, int index) {
        vColumns.insertElementAt(column, index);
        createTitle();
    }

    public void addColumn(ParamDocColumn column) {
        vColumns.add(column);
        createTitle();
    }

    public String getTitle() {
        return sTitle;
    }

    private void createTitle() {
        if (!getColumns().isEmpty()) {
            StringBuilder sbTitle = new StringBuilder("");

            for (ParamDocColumn col : getColumns()) {
                sbTitle.append(col.getColDescription()).append(COL_SEP);
            }

            sbTitle.setLength(sbTitle.length() - COL_SEP.length());
            sTitle = sbTitle.toString();
        }
    }

    /**
     * @author evochko
     * ������� ����� �� ���������� ������� ������ - �������� �� mapAgentParams � �� �������� mapParamsDescr �� ���������, ������� ���������� � {@link #PARAM_COL_PREFIX}
     * � �������� �� � ����� {@link #vColumns}
     */
    public void setColumns(HashMap mapAgentParams, HashMap mapParamsDescr, boolean bIsSQLColumns) {
        Vector<ParamDocColumn> columns = new Vector<ParamDocColumn>();

        if (mapAgentParams != null && !mapAgentParams.isEmpty()) {

            String sColsHasSqlNull = "";
            if (bIsSQLColumns && mapAgentParams.containsKey(PARAM_COLS_HAS_SQLNULL)) {
                sColsHasSqlNull = (String) mapAgentParams.get(PARAM_COLS_HAS_SQLNULL);
                sColsHasSqlNull += "-";        // ��������� ������ - �� �����, ��� ���� ����� � ���������� �� ��������� ���������� ��������� ��� �������� ��������� ������� � ������
            }

            for (int i = 1; i <= MAX_COL_COUNT; i++) {
                String sColName = PARAM_COL_PREFIX + i;

                if (mapAgentParams.containsKey(sColName)) {
                    String sColumnVal = (String) mapAgentParams.get(sColName);
                    String sColDescr = "";

                    if (!mapParamsDescr.isEmpty())
                        sColDescr = (String) mapParamsDescr.get(sColName);

                    if (!bIsSQLColumns) {
                        columns.add(new ParamDocColumn(sColName, sColumnVal, sColDescr, columnValues));
                    } else {
                        boolean bHasSqlNullIfEmpty = sColsHasSqlNull.matches(".*" + sColName + "[^0-9].*");
                        columns.add(new ParamDoColumnForSQL(sColName, sColumnVal, sColDescr, bHasSqlNullIfEmpty, columnValues));
                    }
                }
            }

            createTitle();
        }

        vColumns.clear();
        vColumns.addAll(columns);
    }

    public List<AbstractColumnValue> getColumnValueObjects() {
        List<AbstractColumnValue> columnValues = new ArrayList<AbstractColumnValue>(vColumns.size());
        for (ParamDocColumn p : vColumns) {
            columnValues.add(p.getColumnValueObj());
        }
        return columnValues;
    }


    /**
     * �������� ��������������� ������ �� �������� ��������� �������
     * <br> � ��������� ����������� ��� ��������� ������� � ������� COL1, COL2 � �.�.
     * <br> ������� ����������� ��������� COL_SEP
     */
    public String createRowByDoc(Document doc) throws NotesException {
        columnValues.clear();

        if (doc == null || doc.isDeleted() || !doc.isValid()) {
            return "";
        }

        StringBuilder strBuff = new StringBuilder();

        if (getColumns().isEmpty())
            throw new NotesException(1001, "Columns are not defined!");


        for (int i = 0; i < getColumns().size(); i++) {
            String sColValue = normalizeString(processDocColumn(doc, vColumns.get(i)));
            strBuff.append(sColValue).append(COL_SEP);
        }

        strBuff.setLength(strBuff.length() - COL_SEP.length());
        return strBuff.toString();
    }

    /**
     * ������ ������������ �������� � ������ - ������������ ������
     */
    protected String normalizeString(String sInput) {
        return sInput.replaceAll("[" + COL_SEP + "\\r\\n]", " ");
    }

    /**
     * ��������� �������� �� ��������� �� �������� �������
     *
     * @param doc - �������������� ��������
     * @return ������, ���������� �������� ������� � ���������. ������-�������� ������������� � ���� ������ � ������������ asValueSep
     */
    protected String processDocColumn(Document doc, ParamDocColumn paramColumn) throws NotesException {
        Vector v;

        if (doc == null || doc.isDeleted() || !doc.isValid()) {
            return "";
        }

        try {
            StringBuilder sb = new StringBuilder();
            v = paramColumn.processDocColumn(doc);
            columnValues.put(paramColumn.getColName(), v);

            if (v != null && !v.isEmpty()) {
                for (int i = 0; i < v.size(); i++) {
                    if (v.get(i) instanceof String)
                        sb.append((String) v.get(i));
                    else if (v.get(i) instanceof Double)
                        sb.append(oColsDoubleFormatter.format(((Double) v.get(i)).doubleValue()));
                    else if (v.get(i) instanceof Integer)
                        sb.append(((Integer) v.get(i)).intValue());
                    else if (v.get(i) instanceof Long)
                        sb.append(((Long) v.get(i)).longValue());
                    else if (v.get(i) instanceof DateTime)
                        sb.append(oColsDateFormatter.format(((DateTime) v.get(i)).toJavaDate()));

                    sb.append(valueSep);
                }

                sb.setLength(sb.length() - valueSep.length());
                return sb.toString();
            }

        } catch (NotesException e) {
            throw new NotesException(e.id, e.toString() + " (process unid: " + doc.getUniversalID() + ")");

        }
        return "";
    }
}

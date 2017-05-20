package com.fuib.lotus.agents.params;

import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Map;
import java.util.Vector;

/**
 * Dec 20, 2014
 *
 * @author evochko
 *         ��������� ��������� ������� ������, ������� ������ ����������� �� ��������� ������� ���������� - ��� ������������ csv-������ ��� �������� � SQL-���� ������
 *         ��������� ����  bHasSqlNullIfEmpty
 */
public class ParamDoColumnForSQL extends ParamDocColumn {

    private boolean bHasSqlNullIfEmpty = false;

    private String SQL_NULL = "#NA";

    @Deprecated
    public ParamDoColumnForSQL(String paramValue, String paramDescription, boolean bHasSqlNullIfEmpty) {
        this(null, paramValue, paramDescription, bHasSqlNullIfEmpty, null);
    }

    public ParamDoColumnForSQL(String colName, String sParamValue, String sParamDescription, boolean bHasSqlNullIfEmpty, Map<String, Vector<?>> columnsValues) {
        super(colName, sParamValue, sParamDescription, columnsValues);
        this.bHasSqlNullIfEmpty = bHasSqlNullIfEmpty;
    }

    public boolean isHasSqlNullIfEmpty() {
        return bHasSqlNullIfEmpty;
    }

    public void setHasSqlNullIfEmpty(boolean hasSqlNullIfEmpty) {
        bHasSqlNullIfEmpty = hasSqlNullIfEmpty;
    }

    @SuppressWarnings("rawtypes,unchecked")
    @Override
    public Vector processDocColumn(Document doc) throws NotesException {
        Vector vResult = super.processDocColumn(doc);

        if (vResult == null || vResult.isEmpty() || String.valueOf(vResult.get(0)).length() == 0) {
            vResult = new Vector(1);
            if (isHasSqlNullIfEmpty()) {
                vResult.add(SQL_NULL);
            } else {
                vResult.add("");
            }
        }

        return vResult;
    }
}

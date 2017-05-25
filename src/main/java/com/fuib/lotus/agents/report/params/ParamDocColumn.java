package com.fuib.lotus.agents.report.params;

import com.fuib.lotus.agents.report.params.values.AbstractColumnValue;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

/**
 * @author evochko
 * Dec 20, 2014
 * обработка параметра профиля агента, который должен вычисляться на контексте целевых документов
 */
public class ParamDocColumn {
    private boolean bIsFormula = false;
    protected final String sColName;
    protected String sColValue = "";
    protected String sColDescription;

    protected AbstractColumnValue columnValueObj;

    /**
     * It's a constuctor for column value.
     * If
     */
    @Deprecated
    public ParamDocColumn(String sParamValue, String sParamDescription) {
        this(null, sParamValue, sParamDescription, null);
    }

    public ParamDocColumn(String colName, String sParamValue, String sParamDescription, ParamDocColSet parentColumnSet) {
        sColName = colName;
        sColValue = sParamValue;
        sColDescription = sParamDescription;
        columnValueObj = ColumnValueFactory.getColumnObject(sColValue, parentColumnSet);
    }

    /**
     * getters and setters
     */
    public boolean isFormula() {
        return bIsFormula;
    }

    public void setIsFormula(boolean isFormula) {
        bIsFormula = isFormula;
    }

    public String getColumnValue() {
        return sColValue;
    }

    public void setColumnValue(String sColumnValue, ParamDocColSet parentColumnSet) {
        sColValue = sColumnValue;
        columnValueObj = ColumnValueFactory.getColumnObject(sColValue, parentColumnSet);
    }

    public AbstractColumnValue getColumnValueObj() {
        return columnValueObj;
    }


    public void setColDescription(String sColDescription) {
        this.sColDescription = sColDescription;
    }

    public String getColDescription() {
        return sColDescription != null ? sColDescription : "";
    }

    public String getColName() {
        return sColName;
    }

    /**
     * Получения значения из документа по заданной колонке
     *
     * @param doc - обрабатываемый документ
     * @return строка, содержащая значение колонки в документе. Мульти-значения преобразуются в одну строку с разделителем VALUE_SEP
     */
    public Vector processDocColumn(Document doc) throws NotesException {
        return getColumnValueObj().getColumnValue(doc);
    }

}

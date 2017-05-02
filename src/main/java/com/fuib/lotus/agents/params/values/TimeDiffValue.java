package com.fuib.lotus.agents.params.values;

import com.fuib.lotus.LNEnvironment;
import lotus.domino.Document;
import lotus.domino.NotesException;

import java.util.Vector;

/**
 * ield Name: %TransactionLog
 * Data Type: Text List
 * Data Length: 849 bytes
 * Seq Num: 24
 * Dup Item ID: 0
 * Field Flags:
 * <p>
 * "17.04.2017 16:22:03  CRFUN_Created  CN=Sergey Yevochko/OU=dho/O=fuib  ��: �������  [������]"
 * "17.04.2017 16:23:19  CRFUN_Underwriter1  �����������  ToProcess  ��: ������ ������� - ���� 1  � ������ ��"
 * "17.04.2017 16:23:29  CRFUN_Underwriter1  CN=Sergey Yevochko/OU=dho/O=fuib  ToProcess  ��: ������ ������� - ���� 1  � ������ ��"
 * "17.04.2017 16:28:09  CRFUN_CmplxCoord  �����������  ToCoordination  ������������ ��1  �� ������������ ��1"
 * "17.04.2017 16:29:47  CRFUN_CmplxCoord  �����������  Accept  ������������ ��1  ����������� ��1"
 * "17.04.2017 16:29:47  CRFUN_KK1Processed  CN=Sergey Yevochko/OU=dho/O=fuib  Accept  ��: ���� ��1 ����������  ����������� ��1"
 * <p>
 * ield Name: %StateLog
 * Data Type: Text List
 * Data Length: 447 bytes
 * Seq Num: 24
 * Dup Item ID: 0
 * Field Flags:
 * <p>
 * "1;CRFUN_Created;CN=Sergey Yevochko/OU=dho/O=fuib;;-;17.04.2017 16:22:03"
 * "2;CRFUN_Underwriter1;CN=Sergey Yevochko/OU=dho/O=fuib;CN=Sergey Yevochko/OU=dho/O=fuib;-;17.04.2017 16:23:29"
 * "3;CRFUN_CmplxCoord;;CN=Sergey Yevochko/OU=dho/O=fuib;-;17.04.2017 16:28:09"
 * "4;CRFUN_CmplxCoord;;CN=Sergey Yevochko/OU=dho/O=fuib;-;17.04.2017 16:29:47"
 * "5;CRFUN_KK1Processed;CN=Sergey Yevochko/OU=dho/O=fuib;CN=Sergey Yevochko/OU=dho/O=fuib;-;17.04.2017 16:29:47"
 * <p>
 * <p>
 * Field Name: %EXECINFO
 * Data Type: Text
 * Data Length: 116 bytes
 * Seq Num: 23
 * Dup Item ID: 0
 * Field Flags: SUMMARY
 * <p>
 * "CN=Sergey Yevochko/OU=dho/O=fuib#������ ������ ���������##Ĳ�\DHO\�����\����\���"
 */
public class TimeDiffValue extends AbstractBracesValue {
    public TimeDiffValue(String value, LNEnvironment environment) {
        super(value, environment);
    }

    @Override
    public Vector getColumnValue(Document doc) throws NotesException {
        String stateID = colValue;
        Vector tranLog = doc.getItemValue("%TransactionLog");
        for (int i = 0; i < tranLog.size(); i++) {

        }

        Vector v = new Vector(1);
        v.add(1000);
        return v;
    }
}

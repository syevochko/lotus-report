package com.fuib.lotus;

import lotus.domino.*;
import java.util.*;

public class MVField
{
  static long FIELD_MAX_SIZE = 20188;

  private String m_sFieldName;
  private String m_sCurFieldNameWrite;
	private long m_nCurLength;
	private int m_nFieldsCount;
  private Document m_doc;
  private Item m_itemCurFieldNameWrite;

  public MVField(Document a_doc, String a_sFieldName) throws NotesException
  {
		if ( a_doc == null ) throw new NotesException(1, "MVField: New: Input parameter is nothing");

		m_doc = a_doc;
		m_nFieldsCount = 0;
		m_sFieldName = a_sFieldName;
		m_sCurFieldNameWrite = a_sFieldName;

		if ( m_doc.hasItem(m_sCurFieldNameWrite) )
    {
			m_nCurLength = ListSize(m_doc.getItemValue(m_sCurFieldNameWrite));

			while ( m_doc.hasItem(m_sFieldName + "$" + Integer.toString(m_nFieldsCount + 1)) )
      {
				m_sCurFieldNameWrite = m_sFieldName + "$" + Integer.toString(++m_nFieldsCount);
				m_nCurLength = ListSize(m_doc.getItemValue(m_sCurFieldNameWrite));
			}
		}
  }


  public void appendToTextList(String sValue) throws NotesException
  {
		if ( sValue != null && sValue.length() > 0 )
    {
			if ( m_itemCurFieldNameWrite == null ) m_itemCurFieldNameWrite = m_doc.replaceItemValue(m_sCurFieldNameWrite, "");
			if ( m_nCurLength + sValue.length() >= FIELD_MAX_SIZE )
      {
				m_sCurFieldNameWrite = m_sFieldName + "$" + Integer.toString(++m_nFieldsCount);
				m_nCurLength = sValue.length();
				m_itemCurFieldNameWrite = m_doc.replaceItemValue(m_sCurFieldNameWrite, "");
      }
			else
				m_nCurLength = m_nCurLength + sValue.length();

			m_itemCurFieldNameWrite.appendToTextList(sValue);
		}
	}


  public Vector GetEnumeration() throws NotesException
  {
    Vector vRet = null;

		if ( m_doc.hasItem(m_sFieldName) )
    {
			vRet = m_doc.getItemValue(m_sFieldName);

			if ( m_nFieldsCount > 0 )
				for ( int i=1; i <= m_nFieldsCount; i++ )
          for (Enumeration e = m_doc.getItemValue(m_sFieldName + "$" + i).elements(); e.hasMoreElements() ;)
            vRet.addElement(e.nextElement());
    } //if

    return vRet;
	}


  public long ListSize(Vector list)
  {
    long nRet = 0;

    for (Enumeration e = list.elements(); e.hasMoreElements() ;)
      nRet += ((String)e.nextElement()).length();

    return nRet;
  }
}


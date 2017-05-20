package com.fuib.lotus.agents.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;

/**
 * @date Dec 28, 2014
 * @author evochko 
 * @Description ����� ��� ������������ ������ ��������� ������ �� ������ ����������
 * <br> ������������ ������ �������� �������� ������ ����� ��� ������ - �������� ����� �������� ������ ��� ������ ������ close()
 * <br> !!! ����������� �������� ����� close() ��� �������� ����� ������ � ��� �������� (���� ����������� {@link #isDeleteOnClose()} )
 */
public class SingleFileReportBuilder extends AbstructFileReportBuilder {

	private String sReportFileName = "";				// �������� ����� ������
	private File fReport = null;
	private OutputStreamWriter fw = null;				// writer ������ - 
	private boolean bIsDeleteOnClose = true;

	public boolean isDeleteOnClose() {		return bIsDeleteOnClose;	}
	public void setDeleteOnClose(boolean isDeleteOnClose) {		this.bIsDeleteOnClose = isDeleteOnClose;	}
	public String getReportFileName() {		return sReportFileName;		}
	public void setReportFileName(String fileName) {	sReportFileName = fileName;		}
	
	@Override
	public void addLineToReport(String line, Document doc) throws IOException, NotesException {
		StringBuffer buff = new StringBuffer(line);
		buff.append(FILE_LINE_SEP);
		
		if (fw==null)	{
			fw = new OutputStreamWriter( new FileOutputStream(getReportFile()), Charset.defaultCharset() );
			if (getRepTitle().length() > 0)	{
				buff.insert(0, FILE_LINE_SEP);
				buff.insert(0, getRepTitle());
			}
		}
		
		try {
			fw.write(buff.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		super.close();
		
		try {
			
			if (fw!=null)
				fw.close();
			
			if (fReport!=null && isDeleteOnClose())
				fReport.delete();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void send(String sMemoSubj, String sAppendText, Database dbForCreateMemo) throws NotesException {
		// TODO Auto-generated method stub
	}

	public File getReportFile() throws IOException		{
		if (fReport==null)	{
			if (getReportFileName().length()>0)	{
				fReport = createFile( getReportFileName() );				
			} else	{				
				fReport = createFile("sfrb");
			}
			
			return fReport;
			
		} else	{
			return fReport;
		}
	}

	/**
	 * @author evochko 
	 * @Description ����������� �� ��� ������, ���� ������ ���� ������� close() ��� ������������� ������
	 */
	protected void finalize()	throws Exception 	{
		close();
	}
	
}

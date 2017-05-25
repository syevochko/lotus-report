package com.fuib.lotus.agents.report.builder;

import java.io.File;
import java.io.IOException;

import com.fuib.lotus.utils.Tools;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;

/**
 * @date Dec 26, 2014
 * @author evochko 
 * @Description абстрактный класс для построения файловых отчетов в агентах Lotus
 */
public abstract class AbstructFileReportBuilder extends AbstructReportBuilder {

	protected String sFileExt = ".csv";	
	protected String FILE_LINE_SEP = "\r";
	protected String TEMP_CATALOG = System.getProperty("java.io.tmpdir");
	protected String sChildCatalog = "";

	public String getChildCatalog() {	return sChildCatalog;	}
	public void setChildCatalog(String childCatalog) {		sChildCatalog = childCatalog;	}
	public String getFileExt() {	return sFileExt;	}
	public void setFileExt(String fileExt) {	sFileExt = fileExt;		}

	public void setRepTitle(String repTitle) {
		if (repTitle!=null && repTitle.length()>0)
			super.setRepTitle(repTitle + FILE_LINE_SEP);
	}

	/**
	 * @author evochko 
	 * @param sFileName 	- имя файла
	 * @return File - файл-отчет для заданного пользователя и подкаталога
	 * @Description: Метод формирует полный путь для создания файла-отчета по пользователю. В случае необходимости создаются подкаталоги.
	 * Если файл с таким именем уже существует, то выполняется его удаление  
	 * <br>Формат: <временный каталог ОС>/sChildCatalog/<sFileName>.csv
	 */
	public File createFile(String sFileName) throws IOException  	{

		File fileDir = new File(TEMP_CATALOG, getChildCatalog());
		if (!fileDir.exists() && !fileDir.mkdirs() )
			throw new IOException("Can't create directory for upload document files by path: " + fileDir.getAbsolutePath());

		File fileRet = new File(fileDir, sFileName.replaceAll( "[ \\r\\n]", "_") + getFileExt());
		if (fileRet.exists())
			fileRet.delete();

		return fileRet; 		
	}

	/**
	 * @Description Отправка файла-отчета заданному пользователю
	 * @author evochko 
	 * @param sTo - адресат отчета
	 * @param sFileName - путь к файлу отчета
	 * @param sSubj - тема письма
	 * @param sAppendText - дополнительный текст в письме
	 * @throws NotesException 
	 */
	public static void sendReport( String sTo, String sFileName, String sSubj, String sAppendText, boolean isDeleteAfterSend, Database dbForCreateMemo) throws NotesException 	{

		Document memo = null;
		RichTextItem rtf = null;
		
		if (sTo==null || sTo.trim().length()==0)
			return;
		
		try	{
			memo = dbForCreateMemo.createDocument();
			memo.replaceItemValue("Form", "Memo").recycle();
			memo.replaceItemValue("Subject", sSubj).recycle();
			memo.replaceItemValue("SendTo", sTo).recycle();

			rtf = memo.createRichTextItem("Body");
			if (sAppendText!=null && sAppendText.length()>0)	{
				rtf.appendText(sAppendText);
				rtf.addNewLine();
			}
			
			if (sFileName!=null && sFileName.length()>0)	{				
				rtf.embedObject(EmbeddedObject.EMBED_ATTACHMENT, null, sFileName, sFileName.substring(sFileName.lastIndexOf("\\")+1));
			}
			
			try {
				memo.send();
			} catch (NotesException e) {
				System.err.println(e.text + " - can't send problem report to " + sTo);
			}
			
			if (isDeleteAfterSend)	{
				File fuser = new File(sFileName);
				if (fuser.exists())	{
					fuser.delete();	
				}				
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			Tools.recycleObj(rtf);
			Tools.recycleObj(memo);
		}
	}

}

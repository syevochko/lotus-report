package com.fuib.lotus.agents.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.Map.Entry;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;

/**
 * @date Dec 28, 2014
 * @author evochko 
 * @Description класс для формирования файловых отчетов для многих пользователей
 * <br> пользователи вычисляются на основе документов, которые отбираются в отчет.
 * <br> Особенностью формирования отчетов является открытие/закрытие файлов при каждом добавлении строки отчета.
 * <br> То есть операции записи в отчеты являются достаточно медленными, однако обеспечивается безопасность работы.
 * <br> !!! обязательно вызывать метод close() для удаления файловых отчетов с файловой системы
 */
public class MultiUserFileReportBuilder extends AbstructFileReportBuilder {

	protected HashMap<String, File> repFiles = new HashMap<String, File>();

	public HashMap<String, File> getRepFiles() {
		return repFiles;
	}

	@Override
	public void addLineToReport(String line, Document doc) throws NotesException, IOException {
		if (!isDocValid(doc))
			return;
		
		Vector<String> repUsers = getUsersByDoc(doc);
		
		if (repUsers!=null && !repUsers.isEmpty())	{
			StringBuffer buff = new StringBuffer(line);
			buff.append(FILE_LINE_SEP);
			
			
			for(String user : repUsers)	{
				if (user.length()>0)	{
					
					boolean isNewFile = false;					
					File userFile = null;
					
					if (repFiles.containsKey(user))	{
						userFile = repFiles.get(user);
					} else	{
						userFile = createFile(getRepNameByUser(user));
						repFiles.put(user, userFile);
						isNewFile = true;
					}
					
					FileWriter first = new FileWriter(userFile, true);
					try {
						if (isNewFile && getRepTitle().length()>0)	{
							first.write(getRepTitle());	
						}
						
						first.write(buff.toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
					first.close();
				}
			}
			
		}
	}

	public void send(String sMemoSubj, String sAppendText, Database dbForCreateMemo) throws NotesException {
		if (!repFiles.isEmpty())	{
			for(Entry<String, File> entry : repFiles.entrySet())	{
				MultiUserFileReportBuilder.sendReport( entry.getKey(), entry.getValue().getAbsolutePath(), sMemoSubj, sAppendText, true, dbForCreateMemo);
				
				try {
					if (getAgentBase()!=null)	{
						getAgentBase().logAction("report " + entry.getValue().getAbsolutePath() + " send to " + entry.getKey());	
					}					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * @author evochko 
	 * @Description !!! Выполняется удаление всех созданных файлов отчетов
	 */
	@Override
	public void close() {
		super.close();
		
		if (!repFiles.isEmpty())	{
			for(Entry<String, File> entr : repFiles.entrySet() )	{
				if (entr.getValue().exists())
					entr.getValue().delete();
			}				
			
			repFiles.clear();
		}
	}

	@SuppressWarnings("unchecked")
	public Vector<String> getUsersByDoc(Document doc) throws NotesException {
		return doc.getItemValue("%Executors");
	}
	
	public String getRepNameByUser(String user) {
		return user.replaceAll("[=/]", "_");
	}
	
	protected boolean isDocValid(Document doc)	{
		try {
			return (doc!=null && !doc.isDeleted() && doc.isValid());
		} catch (NotesException e) {
			e.printStackTrace();
			return false;
		}
	}
	
}

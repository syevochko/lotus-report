package com.fuib.lotus;

import java.util.Vector;

import com.fuib.lotus.log.LogEx;
import com.fuib.lotus.utils.LNIterator;
import com.fuib.lotus.utils.LNObjectList;
import com.fuib.lotus.utils.Tools;

import lotus.domino.AgentContext;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.NotesError;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewEntryCollection;
import lotus.domino.ViewEntry;

import lotus.domino.Name;

public class LNEnvironment implements lotus.domino.Base {
	public static final int ERR_CUSTOM = 1;
	public static final int ERR_DB_NOT_OPEN = 6000;
	
	public static final String CONFIG_NAME = "Configuration";
	public static final String CONFIG_KEY = "AdminUser";
	
	public static final String ENV_LOG = "ENV_LOG";
	public static final String ENV_REF = "ENV_REF";
	public static final String ENV_CONTROL = "ENV_CONTROL";
	public static final String ENV_CONFIG = "ENV_CONFIG";
	public static final String ENV_RESOURCE = "ENV_RESOURCE";
	
	private static final String ENV_CONTROL_ITEM = "fdSystemDatabaseName";
	
	public static final String FUIBCONFIG_DBNAME = "fuibconfig";
	
	public static final String FUIBCONFIG_LOOKUPVIEWNAME = "(lookupConfig)";
	public static final String VIEW_SETTINGS = "(lookupFlowOrState)";
	
	public static final String CLUSTER_HUB = "HUB-CLUSTER";
	
	protected LNObjectList m_storage = new LNObjectList();
	
	protected Session m_session = null;
	protected AgentContext m_agentContext = null;
	protected Database m_dbCurrent = null;
	private Database m_dbLog = null;
	private Database m_dbRef = null;
	// только для getServerWebURL
	final public static String DOMAIN_SUFFIX = ".fuib.com";
	protected LNPAB m_oPAB = null;
	
	
	public LNEnvironment(Session a_session) throws NotesException {
		m_session = a_session;								// no requirement to recycle this objects -
		m_agentContext = m_session.getAgentContext();		// they will be recycled later by calling procedure or class object
		m_dbCurrent = m_agentContext.getCurrentDatabase();
	}
	
	public Session getSession()							{ return m_session; }
	public String getServer() throws NotesException 	{ return m_dbCurrent.getServer(); }
	public Database getCurrentDatabase() throws NotesException 	{ return m_dbCurrent; }
	
	
	/**
	 * При вызове без параметра bOpenFromAdminServer = true;
	 * 		хотя должно быть наоборот! И вообще я бы удалил этот метод, чтобы исключить всякие неожиданности...
	 */
	public LNPAB getAB() throws NotesException {
		return getAB(true);
	}
	
	public LNPAB getAB(boolean bOpenFromAdminServer) throws NotesException {
		if (m_oPAB == null)
			m_oPAB = new LNPAB(m_session, bOpenFromAdminServer);
		return m_oPAB;
	}
	
	
	public Database getDatabase(String sDbPath) throws NotesException {
		return getDatabase(null, sDbPath);
	}
	
	
	public Database getDatabase(String a_sServer, String sDbPath) throws NotesException {
		Database db = null;
		
		if (sDbPath == null || sDbPath.isEmpty())
			throw new NotesException(NotesError.NOTES_ERR_NODBNAME, "Невозможно подключиться к БД, т.к. путь к ней не задан");
		
		String sServer = (a_sServer != null && !sDbPath.isEmpty()) ? a_sServer : getServer();
		String sKey = sServer + "!!" + sDbPath;
		db = (Database) m_storage.get(sKey);
		
		if (db == null) {
			db = m_session.getDatabase(sServer, sDbPath, false);
			if (db != null) {
				if (!db.isOpen())
					throw new NotesException(NotesError.NOTES_ERR_DATABASE_NOTOPEN, "Невозможно подключиться к БД <" + sServer + "!!" + sDbPath + ">");
				m_storage.put(sKey, db);
			}
			else
				throw new NotesException(NotesError.NOTES_ERR_DATABASE_MISSING, "БД <" + sServer + "!!" + sDbPath + "> не найдена");
		}
		
		return db;
	}
	
	/**
	 * Возвращает объект первой доступной БД кластера
	 * @param sCluster - имя кластера для перебора БД
	 * @param sDbPath - полный путь к БД
	 */
	public Database getDatabaseCluster(String sCluster, String sDbPath) throws NotesException {
		Vector<String> vct = new Vector<String>();
		vct = getClusterServers(sCluster);
		if (vct != null) {
			String sServer = getAbbreviated(getServer());
			if (vct.contains(sServer)) {
				// проверяем, если текущий сервер есть в списке, то сразу же его АК и возвращаем, т.к. уже подключились к ней
				return m_oPAB.getPAB();
			}
			// пробуем взять оставшиеся БД
			for (String sServer2 : vct) {
				try {
					Database db = getDatabase(sServer2, sDbPath);
					return db;
				} catch (NotesException e) {}
			}
		}
		
		throw new NotesException(NotesError.NOTES_ERR_DATABASE_NOTOPEN, "Невозможно подключиться к БД <" + sDbPath + "> кластера <" + sCluster + ">");
	}
	
	
	/**
	 * Возвращает Vector abbreviated-имён серверов указанного кластера
	 */
	@SuppressWarnings("unchecked")
	public Vector<String> getClusterServers(String sCluster) throws NotesException {
		if (sCluster == null || sCluster.isEmpty())
			throw new NotesException(LogEx.ERRc1111, "Имя кластера, на котором необходимо открыть БД, не задано!");
		
		getAB(false);
		
		Vector<String> vct = new Vector<String>(1);
		vct.add(sCluster);
		
		return (Vector<String>) dbLookup(m_oPAB.getPAB(), "Clusters", vct, 2);
	}
	
	
	public Database openByReplicaID(String sServerName, String sReplicaID) throws NotesException {
		Database db = null;
		
		if (sReplicaID == null || sReplicaID.isEmpty())
			throw new NotesException(NotesError.NOTES_ERR_BAD_REPLICA_LIST, "Невозможно подключиться к БД, т.к. ReplicaID не задана");
		
		String sKey = sServerName + "!!" + sReplicaID;
		db = (Database) m_storage.get(sKey);
		
		if (db == null) {
			db = m_session.getDatabase(null, null);
			db.openByReplicaID(sServerName, sReplicaID);
			if (db != null && db.isOpen())
				m_storage.put(sKey, db);
			else
				throw new NotesException(ERR_DB_NOT_OPEN, "Невозможно подключиться к БД <" + sServerName + "!!" + sReplicaID + ">");
		}
		
		return db;
	}
	
	
	/**
	 * ПУМБ-Справочник; ранее - getFUIBMainDB
	 */
	public Database getDbRef() throws NotesException {
		if (m_dbRef == null)
			m_dbRef = getWFDatabase("fdRefDatabaseName", ENV_REF);
		return m_dbRef;
	}
	
	/**
	 * Получение БД "Конфигурация АС"
	 */
	public Database getDbConfig() throws NotesException {
		if (m_dbCurrent.getFileName().indexOf(FUIBCONFIG_DBNAME) != -1)
			return m_dbCurrent;
		return getWFDatabase("fdGlobalConfig", ENV_CONFIG);
	}
	
	/**
	 * Получение БД "Центр управления ДО"
	 */
	public Database getDbControl() throws NotesException {
		return getWFDatabase(ENV_CONTROL_ITEM, ENV_CONTROL);
	}
	
	/**
	 * Получение WF-лога
	 */
	public Database getDbLog() throws NotesException {
		if (m_dbLog == null)
			m_dbLog = getWFDatabase("fdLogDatabaseName", ENV_LOG);
		return m_dbLog;
	}
	
	/**
	 * Получение БД окружения по ID в топологии
	 */
	public Database getWFDatabase(String sID) throws NotesException {
		String sDbPath = getWFDbPath(sID);
		if (sDbPath.isEmpty())
			throw new NotesException(NotesError.NOTES_ERR_NODBNAME, "Невозможно подключиться к БД по ID='" + sID + "', т.к. путь к ней не указан в профайле 'Configuration' (AdminUser)");
		Database db = null;
		try {
			db = getDatabase(null, sDbPath);
		}
		catch (NotesException e) {
			if (e.id == NotesError.NOTES_ERR_NODBNAME)
				throw new NotesException(NotesError.NOTES_ERR_NODBNAME, "Невозможно подключиться к БД по ID='" + sID + "'");
			else
				throw(e);
		}
		return db;
	}
	
	/**
	 * Сначала пытаемся взять путь из спец. item'а, затем по ID
	 * @param sItemName - имя поля, в котором может храниться путь к нужной БД
	 * @param sID - идентификатор БД в топологии
	 */
	protected Database getWFDatabase(String sItemName, String sID) throws NotesException {
		Database db = null;
		Document docProfile = getProfileConfig();
		String sDbPath = "";
		if (docProfile.hasItem(sItemName)) {
			try {
				sDbPath = docProfile.getItemValueString(sItemName);
				if (!sDbPath.isEmpty()) {
					db = getDatabase(sDbPath);
				}
			}
			catch (NotesException e) {
				try {
					db = getWFDatabase(sID);
				}
				catch (NotesException e2) {
					throw e;
				}
			}
		}
		else
			db = getWFDatabase(sID);
		return db;
	}
	
	
	public String getWFDbPath(String sID) throws NotesException {
		Document docProfile = getProfileConfig();
		return getWFDbPath(docProfile, sID);
	}
	
	
	public String getWFDbPath(Document docProfile, String sID) throws NotesException {
		return (sID != ENV_CONTROL) ?
				(String) m_session.evaluate("@Trim(@Right(fdEnv; '" + sID.toUpperCase() + "#'))", docProfile).firstElement() :
				docProfile.getItemValueString(ENV_CONTROL_ITEM);
	}
	
	
	/*
	 * get databade object from pointed profile from pointed item name (name of function is obsolete)
	 */
//	public Database getDB(String sProfileName, String sKey, String sItemName) throws NotesException {
//		return getDB(m_dbCurrent, sProfileName, sKey, sItemName);
//	}
	
	
//	public Database getDB(Database db, String sProfileName, String sKey, String sItemName) throws NotesException {
//		String sDbPath = getProfileItemString(db, sProfileName, sKey, sItemName);
//		return getDatabase(sDbPath);
//	}
	
	
	public Document getProfileConfig() throws NotesException {
		return getProfileConfig(m_dbCurrent);
	}
	
	public Document getProfileConfig(Database db) throws NotesException {
		return getProfile(db, CONFIG_NAME, CONFIG_KEY);
	}
	
	/**
	 * get profile, which describe WF topology
	 */
	public Document getProfileGlobal() throws NotesException {
		return getProfile(getWFDatabase(ENV_CONTROL), "GlobalConfiguration", "");
	}
	
	/**
	 * get profile, which describe WF topology
	 */
	public Document getProfileTopology() throws NotesException {
		final String TOPOLOGY_FORM = "AddArchives";
		
		Document docTopology = getProfile(getWFDatabase(ENV_CONTROL), TOPOLOGY_FORM, CONFIG_KEY);
		
		if (!TOPOLOGY_FORM.equalsIgnoreCase(docTopology.getItemValueString("Form"))) {
			docTopology.remove(true);
			Tools.recycleObj(docTopology);
			throw new NotesException(ERR_CUSTOM, "Профайл 'AddArchives' (AdminUser) отсутствует в базе <" + getServer() + "!!" + this.getWFDatabase(ENV_CONTROL).getFilePath() + ">");
		}
				
		return docTopology;
	}
	
	
	public Document getProfile(Database db, String sProfileName, String sProfileKey) throws NotesException {
		String sKey = db.getFilePath() + "!!" + sProfileName + "_" + sProfileKey;
		Document docProfile = (Document) m_storage.get(sKey);
		if (docProfile == null) {
			try {
				docProfile = db.getProfileDocument(sProfileName, sProfileKey);
			}
			catch (NotesException e) {
				throw new NotesException(LogEx.getID(e), LogEx.getMessage(e) + ": " + sKey);
			}
			m_storage.put(sKey, docProfile);
		}
		return docProfile;
	}
	
	
	public String getProfileItemString(Database db, String sProfileName, String sKey, String sItemName) throws NotesException {
		Document docProfile = getProfile(m_dbCurrent, sProfileName, sKey);
		return getProfileItemString(docProfile, sItemName);
	}
	
	public String getProfileItemString(Document docProfile, String sItemName) throws NotesException {
		String result = null;
		if (docProfile.hasItem(sItemName)) {
			result = docProfile.getItemValueString(sItemName);
		}
//		else
//			throw new NotesException(LogEx.ERRc1222, "В профайле '" + sProfileName + " (" + sKey + ")' базы " + db.getFilePath() + " отсутствует поле '" + sItemName + "'");
		return result;
	}
	
	
	public View getView(Database db, String sViewName) throws NotesException {
		if (db == null)
			throw new NotesException(NotesError.NOTES_ERR_DATABASE_MISSING, "LNEnvironment.getView: Некорректные входные параметры - объект БД == null");
		if (!db.isOpen())
			throw new NotesException(NotesError.NOTES_ERR_DATABASE_NOTOPEN, "LNEnvironment.getView: Некорректные входные параметры - объект БД не открыт");
		if (sViewName == null || sViewName.length() == 0)
			throw new NotesException(NotesError.NOTES_ERR_NOVIEWNAME, "LNEnvironment.getView: Некорректные входные параметры - имя представления не задано");
		
		String sKey = db.getFilePath() + "!!" + sViewName;
		View view = (View) m_storage.get(sKey);
		
		if (view == null) {
			view = db.getView(sViewName);
			if (view != null) {
				viewRefresh(view);
				m_storage.put(sKey, view);
			}
			else
				throw new NotesException(NotesError.NOTES_ERR_VIEW_MISSING, "Не найдено представление '" + sViewName + "' в базе " + m_dbCurrent.getFilePath());
		}
		
		return view;
	}
	
	public View getView(String sID, String sViewName) throws NotesException {
		return getView(getWFDatabase(sID), sViewName);
	}
	
	public View getViewSettings() throws NotesException {
		return getView(getWFDatabase(ENV_CONTROL), VIEW_SETTINGS);
	}
	
	public void viewRefresh(View view) throws NotesException {
		boolean bAutoUpdate = view.isAutoUpdate();
		view.setAutoUpdate(true);
		view.refresh();
		view.setAutoUpdate(bAutoUpdate);
	}
	
	
	public Vector<?> dbLookup(Database ndb, String sViewName, Vector<?> vctCategory, int iReturnColNumber) throws NotesException {
		if (iReturnColNumber < 1)
			throw new NotesException(LogEx.ERRc1111, "Номер колонки (" + iReturnColNumber + ") вида " + sViewName + ", из которой необходимо получить данные, не может быть < 1!");
		
		View nvClusters = getView(m_oPAB.getPAB(), "Clusters");
		ViewEntryCollection nvec = nvClusters.getAllEntriesByKey(vctCategory, true);
		if (nvec.getCount() != 0) {
			Vector<Object> vct = new Vector<Object>(nvec.getCount());
			LNIterator lnIter = null;
			try {
				lnIter = new LNIterator(nvec, true, false);
				while (lnIter.hasNext()) {
					ViewEntry nve = (ViewEntry) lnIter.next();
					vct.add(nve.getColumnValues().get(iReturnColNumber - 1));
				}
				return vct;
			}
			finally {
				Tools.recycleObj(lnIter);
			}
		}
		return null;
	}
	
	
	/**
	 * Обёртка вокруг Database.FTSearch с генерацией правильного кода ошибки
	 * @param sCriteria - формула для FT-поиска
	 * return: всегда DocumentCollection или выпадет по Exception или, возможно, по какой-то необрабатываемой NotesException
	 */
	public DocumentCollection ftSearch(Database db, String sCriteria) throws NotesException {
		try {
			return db.FTSearch(sCriteria);
		}
		catch (NotesException e) {
			switch (e.id) {
			case 4005:		//FTSearch -> Notes error: The full text index needs to be rebuilt
				String sErrText = LogEx.getMessage(e) + " {" + e.id + "}";
				System.err.println(sErrText);
				LogEx.sendWarningMemo(LogEx.ERRc1223, sErrText);
				break;
			default:
				throw e;
			}
		}
		return db.createDocumentCollection();
	}
	
	
	/**
	 * @author gorobets
	 * @param bIsSSL - true - получить https url, false - получить http url
	 * @param bIsCluster - true - получить url на балансировщик, false - получить url на текущий сервер
	 * @return
	 * @return String имя сервера в формате: http(s)://<имя хоста>[:<имя порта>]
	 * @throws NotesException
	 * @Description: Получение корректной web ссылки на текущий сервер с учётом настроек HTTP задачи текущего сервера
	 *
	 * ValdSh: метод был в одном из экземпляров этого класса, а в одном не было; оставлено для возможной совместимости;
	 * 			по идее надо проверить, если он нигде не используется, то удалить или сохранить старую версию в svn и удалить
	 */
	public String getServerWebURL(boolean bIsSSL, boolean bIsCluster) throws NotesException {
		Document docServer = null;
		Name oCurServerName = null;
		String sURL = "";
		
		try {
			if (m_oPAB == null) m_oPAB = new LNPAB(m_session, false);

			oCurServerName = m_session.createName(getServer());
			docServer = getView(m_oPAB.getPAB(), "($Servers)").getDocumentByKey(oCurServerName.getCanonical());
			
			if (docServer == null) throw new NotesException(ERR_CUSTOM, "Не найдены в names.nsf настройки текущего сервера: " + getServer());
			
			String sProtocol = ( (bIsSSL)?"https":"http" );
			String sHost = null;

			if (bIsCluster) {
				sHost = docServer.getItemValueString("HTTP_HostName");
				if (sHost.isEmpty()) sHost = docServer.getItemValueString("ICMHostname");
				if (sHost.isEmpty()) sHost = oCurServerName.getCommon();
				
				if (!sHost.isEmpty() && sHost.toLowerCase().indexOf(DOMAIN_SUFFIX) == -1)
					sHost += DOMAIN_SUFFIX;
				
				sURL = sProtocol + "://" + sHost;			// порты в этом случае стандартные, поэтому их не указываем
			}
			else {
				sHost = oCurServerName.getCommon() + DOMAIN_SUFFIX;
				
				// порты берём из настроек запуска HTTP задачи сервера
				int nPort = docServer.getItemValueInteger(( (bIsSSL)?"HTTP_SSLPort":"HTTP_Port" ));

				sURL = sProtocol + "://" + sHost + ( ((nPort != 80) && (nPort != 443))?(":" + String.valueOf(nPort)):"" );
			}
		} finally {
			Tools.recycleObj(docServer);
			Tools.recycleObj(oCurServerName);
		}
		
		return sURL;
	}
	
	
	public String getAbbreviated(String sName) throws NotesException {
		Name name = null;
		try {
			name = m_session.createName(sName);
			return name.getAbbreviated();
		}
		finally {
			Tools.recycleObj(name);
		}
	}
	
	
	public void putObj(Object key, lotus.domino.Base data) 	{ m_storage.put(key, data); }
	public lotus.domino.Base getObj(Object key) 			{ return (lotus.domino.Base)m_storage.get(key); }
	
	
	public void recycle() {
		m_storage.recycle();
		try {
			if (m_oPAB != null) m_oPAB.recycle();
		}
		catch (NotesException e) {
			e.printStackTrace();
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public void recycle(Vector arg0) throws NotesException {}
	
}

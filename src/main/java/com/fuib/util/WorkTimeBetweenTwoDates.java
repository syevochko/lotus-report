package com.fuib.util;
import java.text.*;
import java.util.*;

import lotus.domino.*;

public class WorkTimeBetweenTwoDates {
	int nStartDay = 9; //время начала рабочего дня
	int nEndDay = 18; // время окончания рабочего дня
	int nDayDurationMinutes = 480; //продолжительность рабочего дня
	int nBreakTimeStart = 13; // время начала обеденного перерыва (в часах)
	int nBreakTimeEnd = 14; //время окончания обеденного перерыва (в часах)
	private Vector<Calendar> arrExcludeList;
	private Session session = null;
	private Database curDB = null;
	private Boolean bInitializeExcludeList = true;
	
	/**
	 *  Переопределение default настроек рабочего дня
	 * @param nStartDay - время начала рабочего дня
	 * @param nEndDay - время окончания рабочего дня
	 * @param nDayDurationMinutes - продолжительность рабочего дня
	 * @param nBreakTimeStart - время начала обеденного перерыва (в часах)
	 * @param nBreakTimeEnd - время окончания обеденного перерыва (в часах)
	 * @throws NotesException 
	 * 
	 */
	public WorkTimeBetweenTwoDates(int nStartDay, int nEndDay, int nDayDurationMinutes, int nBreakTimeStart, int nBreakTimeEnd, Session aSession) throws NotesException{
		this.nStartDay = nStartDay;
		this.nEndDay = nEndDay;
		this.nDayDurationMinutes = nDayDurationMinutes;
		this.nBreakTimeStart = nBreakTimeStart;
		this.nBreakTimeEnd = nBreakTimeEnd;
		
		session = aSession;
		AgentContext agentContext = session.getAgentContext();
		curDB = agentContext.getCurrentDatabase();
	}
	
	public  WorkTimeBetweenTwoDates(Session aSession) throws NotesException{
		if ( aSession != null ) {
			session = aSession;
			AgentContext agentContext = session.getAgentContext();
			curDB = agentContext.getCurrentDatabase();
		} else
			bInitializeExcludeList = false;
	}
	
	public  WorkTimeBetweenTwoDates(){
		bInitializeExcludeList = false;
	}

	
	public Boolean getInitializeExcludeList() {
		return bInitializeExcludeList;
	}
	
	public void setInitializeExcludeList(Boolean initializeExcludeList) {
		bInitializeExcludeList = initializeExcludeList;
	}

	/**
	 * @author ershov
	 * Преобразование даты в формате "dd.MM.yyyy" в объект Calendar
	 * @param sDate  
	 * @return 
	 * ParseException
	 */
	public static Calendar DateToCalendar(String sDate) throws ParseException{
		Calendar cal = null;
		Date tmpDate = null;
		sDate = sDate.replace("[", "");
		sDate = sDate.replace("]", "");
		sDate = sDate.replace("/", ".");

		DateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
		//DateFormat formatter = new SimpleDateFormat();
		tmpDate = (Date)formatter.parse(sDate); 
		cal = Calendar.getInstance();
		cal.setTime(tmpDate);

		return cal;
	}
	
	/**
	 * @Description Преобразование объекта Date в объект Calendar
	 * @param date<Date>
	 * @return Calendar
	 * @throws ParseException
	 * @author ershov
	 */
	public static Calendar DateToCalendar(Date date) throws ParseException { 
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}

	/**
	 * Инициализация списка дат-исключений
	 * @param a_bInitializeExcludeList<Boolean> - если true, то запрашивается список праздников из names.nsf
	 * @throws Exception
	 */
	void InitializeExcludeList(Boolean a_bInitializeExcludeList) throws Exception {
		arrExcludeList = null;
		if (a_bInitializeExcludeList && session != null){
			Database dbNames = null;
			View adrBookView = null;
			DocumentCollection dc = null;
			Document doc = null, docRecycle = null;;

			try {
				// получение списка дат-исключений
				dbNames = session.getDatabase(curDB.getServer(), "names.nsf");

				adrBookView = dbNames.getView("Holidays");
				if (adrBookView == null) throw new Exception("adrBookView is NULL!!");

				dc =  adrBookView.getAllDocumentsByKey("Ukraine");			

				//при повторном вызове очищаем предыдущий список дат-исключений			
				if (arrExcludeList != null)		ClearExcludeList();
				arrExcludeList = new Vector<Calendar>();

				if (dc != null){
					if (dc.getCount()>0){
						doc = dc.getFirstDocument();
						while(doc != null){
							// заполняем список дат-исключений данными, полученными из справочника names.nsf
							//1.получаем данные из документа, преобразовывая их в строку
							//2. преобразовываем строку в объект типа Calendar
							AddDateToExcludeList(DateToCalendar(doc.getItemValue("RepeatStartDate").toString()));

							docRecycle = doc;
							doc = dc.getNextDocument(doc);
							docRecycle.recycle();
							docRecycle = null;
						}
					}
				}
			}
			finally {
				if (doc != null)			{ doc.recycle(); doc = null; }
				if (docRecycle != null)		{ docRecycle.recycle(); docRecycle = null; }
				if (dc != null)				{ dc.recycle(); dc = null; }
				if (adrBookView != null)	{ adrBookView.recycle(); adrBookView = null; }
				if (dbNames != null)		{ dbNames.recycle(); dbNames = null; }
			}
		}
		else {
			arrExcludeList = new Vector<Calendar>();
		}
	}
	
	/**
	 * Lобавление в список дат-исключений нового значения  
	 * @param DT<Date>
	 * @throws ParseException
	 */
	void AddDateToExcludeList(Date DT) throws ParseException{
		if (arrExcludeList == null) arrExcludeList = new Vector<Calendar>();
		arrExcludeList.add(DateToCalendar(DT));
	}
	
	/**
	 * 	Добавление в список дат-исключений новой даты - входной параметр объект типа Calendar
	 * @param DT
	 */
	void AddDateToExcludeList(Calendar DT){
		if (arrExcludeList == null) arrExcludeList = new Vector<Calendar>();
		arrExcludeList.add(DT);
	}
	
	/**
	 * Добавление в список дат-исключений новой даты
	 * @param DT<String>
	 * @throws ParseException
	 */
	void AddDateToExcludeList(String sDT) throws ParseException{
		Calendar cal = DateToCalendar(sDT);
		if (arrExcludeList == null) arrExcludeList = new Vector<Calendar>();
		arrExcludeList.add(cal);
	}
	
	/**
	 * Очистка списка дат-исключений
	 */
	void ClearExcludeList(){
		arrExcludeList.removeAllElements();
	}
	
	/**
	 * Проверка вхождение заданной даты в список исключений
	 * @param lDate<Calendar>
	 * @return
	 * @throws Exception
	 */
	boolean CheckDateInExcludeList(Calendar lDate) throws Exception{
		// если список исключений пуст, то инициализируем его в зависимости от параметра bInitializeExcludeList
		if (arrExcludeList == null) {InitializeExcludeList(bInitializeExcludeList);}
		String sDate = "";

		try {
			String sValue = "";
			sValue = lDate.get(Calendar.DATE)  + "." + lDate.get(Calendar.MONTH);
			for(Calendar calendarItem: arrExcludeList){
				//+1 прибавляется в месяцу потому, что месяц при добавлении на 1 единицу меньше, чем нужно...х.з. почему, но баг-фикс помогает.
				sDate = calendarItem.get(Calendar.DATE)  + "." + (calendarItem.get(Calendar.MONTH));
				if (sDate.equals(sValue)){
					return true;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
	
	/**
	 * @param lDTStart<String> стартовая дата
	 * @param lDTEnd<String> конечная дата
	 * @param a_bInitializeExcludeList<Boolean> если true, то инициализируется список дат-исключений из names.nsf
	 * @return возвращает количество рабочих минут между двумя датами
	 * @throws Exception
	 */
	public int GetWorkTimeBetweenTwoDates(String lDTStart, String lDTEnd, Boolean a_bInitializeExcludeList) throws Exception{		
		return GetWorkTimeBetweenTwoDates(DateToCalendar(lDTStart),DateToCalendar(lDTEnd), a_bInitializeExcludeList);
	}
	/**
	 * @param lDTStart<Date> стартовая дата
	 * @param lDTEnd<Date> конечная дата
	 * @param a_bInitializeExcludeList<Boolean> если true, то инициализируется список дат-исключений из names.nsf
	 * @return возвращает количество рабочих минут между двумя датами
	 * @throws Exception
	 */
	public int GetWorkTimeBetweenTwoDates(Date lDTStart, Date lDTEnd, Boolean a_bInitializeExcludeList) throws Exception{		
		return GetWorkTimeBetweenTwoDates(DateToCalendar(lDTStart),DateToCalendar(lDTEnd), a_bInitializeExcludeList);
	}
	
	/**
	 * @param lDTStart<Calendar> стартовая дата
	 * @param lDTEnd<Calendar> конечная дата
	 * @param a_bInitializeExcludeList<Boolean> если true, то инициализируется список дат-исключений из names.nsf
	 * @return возвращает количество рабочих минут между двумя датами
	 * ОСОБЕННОСТИ РЕАЛИЗАЦИИ
	 * Общее рабочее время между двумя датами вычисляется так:
	 * 1. Вычисляется ЦЕЛОЕ количество рабочих дней между двумя датами (ИТОГОВОЕ), дата и время стартовой/конечной дат при этом не учитывается.
	 * Пример: 
	 * StartDate = 22/09/2014
	 * EndDate = 25/09/2014
	 * Количество ЦЕЛЫХ РАБОЧИХ ДНЕЙ - 2*8*60 (23,24)
	 * 2. Количество рабочих минут в стартовую дату: считается количество рабочих минут С текущего времени ДО окончания рабочего дня 
	 * 3. Количество рабочих минут в конечную дату: считается количество рабочих минут С текущего времени ДО окончания рабочего дня
	 * @throws Exception
	 */
	public int GetWorkTimeBetweenTwoDates(Calendar lDTStart, Calendar lDTEnd, Boolean a_bInitializeExcludeList) throws Exception{
		bInitializeExcludeList = a_bInitializeExcludeList;

		int totalWorkTime = 0;
		
		int n1 = GetWorkingDaysBetweenTwoDates(lDTStart, lDTEnd)*8*60;//1. Вычисляется ЦЕЛОЕ количество рабочих дней между двумя датами (В МИНУТАХ), дата и время стартовой/конечной дат при этом не учитывается.
		int n2 = GetDayWorkTime(lDTStart); //2. Количество рабочих минут в стартовую дату: считается количество рабочих минут С текущего времени ДО окончания рабочего дня
		//=======================================================================
		//3. Количество рабочих минут в конечную дату: считается количество рабочих минут С текущего времени ДО окончания рабочего дня
		int n3 = 0;//Количество минут в работе в конечную дату
		//если конечная и начальные даты не совпадают, то вычисляем время в работе в конечной дате
		if (lDTStart.get(Calendar.DAY_OF_MONTH) != lDTEnd.get(Calendar.DAY_OF_MONTH) || lDTStart.get(Calendar.MONTH) != lDTEnd.get(Calendar.MONTH) || lDTStart.get(Calendar.YEAR) != lDTEnd.get(Calendar.YEAR)){
			// если период попадает в рабочие часы, вычисляем сколько прошло с начала дня до времени lDTEnd
			if(GetDayWorkTime(lDTEnd)>0){
				n3 = nDayDurationMinutes - GetDayWorkTime(lDTEnd);	
			} else if( !isDTWeekend(lDTEnd) && lDTEnd.get(Calendar.HOUR_OF_DAY) >= nEndDay){//иначе если конечное время выходит за рабочие рамки - считаем что это целый день рабочего времени  
				n3 = nDayDurationMinutes;
			}
			
			totalWorkTime =  n1 + n2 + n3;
		} else	{		//даты совпадают
			n3 = GetDayWorkTime(lDTEnd);
			totalWorkTime =  n1 + n2 - n3;			
		}
		
		//=======================================================================
		return totalWorkTime;
	}
	
	private boolean isDTWeekend(Calendar DT)		{
		return (DT.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
				DT.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);
	}
	
	/**
	 * вычисление времени в минутах, которое прошло со времени, указанного в объекте DT, до окончания рабочего дня. 
	 * @param DT<Calendar> 
	 * @return время в минутах, которое прошло со времени, указанного в объекте DT, до окончания рабочего дня.
	 * @throws Exception
	 */
	public int GetDayWorkTime(Calendar DT) throws Exception{
		int nStartDayWorkTime = 0;

		// если Дата НЕ ВХОДИТ В СПИСОК ИСКЛЮЧЕНИЙ, то вычисляем время работы
		if ( DT.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && DT.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && !CheckDateInExcludeList(DT) ) {
			//- вычислить время нахождения документа в работе в переданную дату
			//если документ поступил в работу ДО начала дня, смещаем время начала работы над документом
			if ( DT.get(Calendar.HOUR_OF_DAY) < nStartDay){
				DT.set(DT.get(Calendar.YEAR), DT.get(Calendar.MONTH), DT.get(Calendar.DATE), nStartDay, 0);
				
			}else if(DT.get(Calendar.HOUR_OF_DAY) > nEndDay){//если документ поступил в работу ПОСЛЕ окончания рабочего дня, то устанавливаем время начала работ в 18-00
				DT.set(DT.get(Calendar.YEAR), DT.get(Calendar.MONTH), DT.get(Calendar.DATE), nEndDay, 0);
			}
			
			// если время начала/окончания работы попадает в обеденный перерыв, то устанавливаем время в начало обеденного перерыва
			if ( DT.get(Calendar.HOUR_OF_DAY) == nBreakTimeStart ){
				DT.set(DT.get(Calendar.YEAR), DT.get(Calendar.MONTH), DT.get(Calendar.DATE), nBreakTimeEnd, 0);
			}
			
			// если время поступления в работу не выходит за рамки рабочего дня, вычисляем время нахождения в работе
			if ( DT.get(Calendar.HOUR_OF_DAY) >= nStartDay && DT.get(Calendar.HOUR_OF_DAY) < nEndDay){
				nStartDayWorkTime = nEndDay*60 - DT.get(Calendar.HOUR_OF_DAY)*60 - DT.get(Calendar.MINUTE);//время в минутах
				// если время работы больше 4 часов, то вычитаем время перерыва: 
				// если больше 5 часов, вычитаем 1 час перерыва из общего времени работы 
				if (nStartDayWorkTime>300){
					nStartDayWorkTime -= 60;
				}
			}
		}
		return nStartDayWorkTime;
	}
	
	/**
	 * функция возвращает ЦЕЛОЕ количество рабочих дней (в минутах), прошедших между двумя датами.
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws Exception
	 */
	public int GetWorkingDaysBetweenTwoDates(Calendar startDate, Calendar endDate) throws Exception {
		Calendar lStartCalendar = (Calendar) startDate.clone();
		Calendar lEndCalendar = (Calendar) endDate.clone();
		int workDays = 0;
		// если стартовая и конечная даты совпадают, то 
		if (lStartCalendar.get(Calendar.YEAR) == lEndCalendar.get(Calendar.YEAR) &&  lStartCalendar.get(Calendar.MONTH) == lEndCalendar.get(Calendar.MONTH) && lStartCalendar.get(Calendar.DATE) == lEndCalendar.get(Calendar.DATE)) {
			return 0;
		}
		
		// НЕ УЧИТЫВАЕМ НАЧАЛЬНУЮ ДАТУ
		lStartCalendar.set(lStartCalendar.get(Calendar.YEAR), lStartCalendar.get(Calendar.MONTH), lStartCalendar.get(Calendar.DATE) + 1, nStartDay, 0);
		lStartCalendar.setTime(lStartCalendar.getTime());        

		// НЕ УЧИТЫВАЕМ КОНЕЧНУЮ ДАТУ
		lEndCalendar.set(lEndCalendar.get(Calendar.YEAR), lEndCalendar.get(Calendar.MONTH), lEndCalendar.get(Calendar.DATE) - 1, nEndDay, 0);
		lEndCalendar.setTime(lEndCalendar.getTime());

		if (lStartCalendar.getTimeInMillis() > lEndCalendar.getTimeInMillis()) {
			lStartCalendar.setTime(lEndCalendar.getTime());
			lEndCalendar.setTime(lStartCalendar.getTime());
		}
		//=======================================================================================
		//вычисление количества рабочих дней между двумя датами
		while (lStartCalendar.getTimeInMillis() < lEndCalendar.getTimeInMillis()){
			if(!CheckDateInExcludeList(lStartCalendar)){
				if (lStartCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && lStartCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
					workDays++;
				}
			}
			lStartCalendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		// возвращаем целое количество дней, которые документ находился в работе 
		return workDays;
	}
	
	// ================== Статические методы ============================
	// Рабочее время между датами, заданные строкой в формате: "dd.MM.yyyy HH:mm", без учёта праздников, заданных в names.nsf
	public static int GetWorkTimeFromString(String sStartDate, String sEndDate) throws Exception{		
		DateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		
		Calendar dtStart = Calendar.getInstance();
		dtStart.setTime(formatter.parse(sStartDate));
		
		Calendar dtEnd = Calendar.getInstance();
		dtEnd.setTime(formatter.parse(sEndDate));		
		
		return new WorkTimeBetweenTwoDates().GetWorkTimeBetweenTwoDates(dtStart, dtEnd, false);
	}
	
}
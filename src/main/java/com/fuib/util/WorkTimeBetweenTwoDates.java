package com.fuib.util;
import java.text.*;
import java.util.*;

import lotus.domino.*;

public class WorkTimeBetweenTwoDates {
	int nStartDay = 9; //����� ������ �������� ���
	int nEndDay = 18; // ����� ��������� �������� ���
	int nDayDurationMinutes = 480; //����������������� �������� ���
	int nBreakTimeStart = 13; // ����� ������ ���������� �������� (� �����)
	int nBreakTimeEnd = 14; //����� ��������� ���������� �������� (� �����)
	private Vector<Calendar> arrExcludeList;
	private Session session = null;
	private Database curDB = null;
	private Boolean bInitializeExcludeList = true;
	
	/**
	 *  ��������������� default �������� �������� ���
	 * @param nStartDay - ����� ������ �������� ���
	 * @param nEndDay - ����� ��������� �������� ���
	 * @param nDayDurationMinutes - ����������������� �������� ���
	 * @param nBreakTimeStart - ����� ������ ���������� �������� (� �����)
	 * @param nBreakTimeEnd - ����� ��������� ���������� �������� (� �����)
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
	 * �������������� ���� � ������� "dd.MM.yyyy" � ������ Calendar
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
	 * @Description �������������� ������� Date � ������ Calendar
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
	 * ������������� ������ ���-����������
	 * @param a_bInitializeExcludeList<Boolean> - ���� true, �� ������������� ������ ���������� �� names.nsf
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
				// ��������� ������ ���-����������
				dbNames = session.getDatabase(curDB.getServer(), "names.nsf");

				adrBookView = dbNames.getView("Holidays");
				if (adrBookView == null) throw new Exception("adrBookView is NULL!!");

				dc =  adrBookView.getAllDocumentsByKey("Ukraine");			

				//��� ��������� ������ ������� ���������� ������ ���-����������			
				if (arrExcludeList != null)		ClearExcludeList();
				arrExcludeList = new Vector<Calendar>();

				if (dc != null){
					if (dc.getCount()>0){
						doc = dc.getFirstDocument();
						while(doc != null){
							// ��������� ������ ���-���������� �������, ����������� �� ����������� names.nsf
							//1.�������� ������ �� ���������, �������������� �� � ������
							//2. ��������������� ������ � ������ ���� Calendar
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
	 * L��������� � ������ ���-���������� ������ ��������  
	 * @param DT<Date>
	 * @throws ParseException
	 */
	void AddDateToExcludeList(Date DT) throws ParseException{
		if (arrExcludeList == null) arrExcludeList = new Vector<Calendar>();
		arrExcludeList.add(DateToCalendar(DT));
	}
	
	/**
	 * 	���������� � ������ ���-���������� ����� ���� - ������� �������� ������ ���� Calendar
	 * @param DT
	 */
	void AddDateToExcludeList(Calendar DT){
		if (arrExcludeList == null) arrExcludeList = new Vector<Calendar>();
		arrExcludeList.add(DT);
	}
	
	/**
	 * ���������� � ������ ���-���������� ����� ����
	 * @param DT<String>
	 * @throws ParseException
	 */
	void AddDateToExcludeList(String sDT) throws ParseException{
		Calendar cal = DateToCalendar(sDT);
		if (arrExcludeList == null) arrExcludeList = new Vector<Calendar>();
		arrExcludeList.add(cal);
	}
	
	/**
	 * ������� ������ ���-����������
	 */
	void ClearExcludeList(){
		arrExcludeList.removeAllElements();
	}
	
	/**
	 * �������� ��������� �������� ���� � ������ ����������
	 * @param lDate<Calendar>
	 * @return
	 * @throws Exception
	 */
	boolean CheckDateInExcludeList(Calendar lDate) throws Exception{
		// ���� ������ ���������� ����, �� �������������� ��� � ����������� �� ��������� bInitializeExcludeList
		if (arrExcludeList == null) {InitializeExcludeList(bInitializeExcludeList);}
		String sDate = "";

		try {
			String sValue = "";
			sValue = lDate.get(Calendar.DATE)  + "." + lDate.get(Calendar.MONTH);
			for(Calendar calendarItem: arrExcludeList){
				//+1 ������������ � ������ ������, ��� ����� ��� ���������� �� 1 ������� ������, ��� �����...�.�. ������, �� ���-���� ��������.
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
	 * @param lDTStart<String> ��������� ����
	 * @param lDTEnd<String> �������� ����
	 * @param a_bInitializeExcludeList<Boolean> ���� true, �� ���������������� ������ ���-���������� �� names.nsf
	 * @return ���������� ���������� ������� ����� ����� ����� ������
	 * @throws Exception
	 */
	public int GetWorkTimeBetweenTwoDates(String lDTStart, String lDTEnd, Boolean a_bInitializeExcludeList) throws Exception{		
		return GetWorkTimeBetweenTwoDates(DateToCalendar(lDTStart),DateToCalendar(lDTEnd), a_bInitializeExcludeList);
	}
	/**
	 * @param lDTStart<Date> ��������� ����
	 * @param lDTEnd<Date> �������� ����
	 * @param a_bInitializeExcludeList<Boolean> ���� true, �� ���������������� ������ ���-���������� �� names.nsf
	 * @return ���������� ���������� ������� ����� ����� ����� ������
	 * @throws Exception
	 */
	public int GetWorkTimeBetweenTwoDates(Date lDTStart, Date lDTEnd, Boolean a_bInitializeExcludeList) throws Exception{		
		return GetWorkTimeBetweenTwoDates(DateToCalendar(lDTStart),DateToCalendar(lDTEnd), a_bInitializeExcludeList);
	}
	
	/**
	 * @param lDTStart<Calendar> ��������� ����
	 * @param lDTEnd<Calendar> �������� ����
	 * @param a_bInitializeExcludeList<Boolean> ���� true, �� ���������������� ������ ���-���������� �� names.nsf
	 * @return ���������� ���������� ������� ����� ����� ����� ������
	 * ����������� ����������
	 * ����� ������� ����� ����� ����� ������ ����������� ���:
	 * 1. ����������� ����� ���������� ������� ���� ����� ����� ������ (��������), ���� � ����� ���������/�������� ��� ��� ���� �� �����������.
	 * ������: 
	 * StartDate = 22/09/2014
	 * EndDate = 25/09/2014
	 * ���������� ����� ������� ���� - 2*8*60 (23,24)
	 * 2. ���������� ������� ����� � ��������� ����: ��������� ���������� ������� ����� � �������� ������� �� ��������� �������� ��� 
	 * 3. ���������� ������� ����� � �������� ����: ��������� ���������� ������� ����� � �������� ������� �� ��������� �������� ���
	 * @throws Exception
	 */
	public int GetWorkTimeBetweenTwoDates(Calendar lDTStart, Calendar lDTEnd, Boolean a_bInitializeExcludeList) throws Exception{
		bInitializeExcludeList = a_bInitializeExcludeList;

		int totalWorkTime = 0;
		
		int n1 = GetWorkingDaysBetweenTwoDates(lDTStart, lDTEnd)*8*60;//1. ����������� ����� ���������� ������� ���� ����� ����� ������ (� �������), ���� � ����� ���������/�������� ��� ��� ���� �� �����������.
		int n2 = GetDayWorkTime(lDTStart); //2. ���������� ������� ����� � ��������� ����: ��������� ���������� ������� ����� � �������� ������� �� ��������� �������� ���
		//=======================================================================
		//3. ���������� ������� ����� � �������� ����: ��������� ���������� ������� ����� � �������� ������� �� ��������� �������� ���
		int n3 = 0;//���������� ����� � ������ � �������� ����
		//���� �������� � ��������� ���� �� ���������, �� ��������� ����� � ������ � �������� ����
		if (lDTStart.get(Calendar.DAY_OF_MONTH) != lDTEnd.get(Calendar.DAY_OF_MONTH) || lDTStart.get(Calendar.MONTH) != lDTEnd.get(Calendar.MONTH) || lDTStart.get(Calendar.YEAR) != lDTEnd.get(Calendar.YEAR)){
			// ���� ������ �������� � ������� ����, ��������� ������� ������ � ������ ��� �� ������� lDTEnd
			if(GetDayWorkTime(lDTEnd)>0){
				n3 = nDayDurationMinutes - GetDayWorkTime(lDTEnd);	
			} else if( !isDTWeekend(lDTEnd) && lDTEnd.get(Calendar.HOUR_OF_DAY) >= nEndDay){//����� ���� �������� ����� ������� �� ������� ����� - ������� ��� ��� ����� ���� �������� �������  
				n3 = nDayDurationMinutes;
			}
			
			totalWorkTime =  n1 + n2 + n3;
		} else	{		//���� ���������
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
	 * ���������� ������� � �������, ������� ������ �� �������, ���������� � ������� DT, �� ��������� �������� ���. 
	 * @param DT<Calendar> 
	 * @return ����� � �������, ������� ������ �� �������, ���������� � ������� DT, �� ��������� �������� ���.
	 * @throws Exception
	 */
	public int GetDayWorkTime(Calendar DT) throws Exception{
		int nStartDayWorkTime = 0;

		// ���� ���� �� ������ � ������ ����������, �� ��������� ����� ������
		if ( DT.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && DT.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && !CheckDateInExcludeList(DT) ) {
			//- ��������� ����� ���������� ��������� � ������ � ���������� ����
			//���� �������� �������� � ������ �� ������ ���, ������� ����� ������ ������ ��� ����������
			if ( DT.get(Calendar.HOUR_OF_DAY) < nStartDay){
				DT.set(DT.get(Calendar.YEAR), DT.get(Calendar.MONTH), DT.get(Calendar.DATE), nStartDay, 0);
				
			}else if(DT.get(Calendar.HOUR_OF_DAY) > nEndDay){//���� �������� �������� � ������ ����� ��������� �������� ���, �� ������������� ����� ������ ����� � 18-00
				DT.set(DT.get(Calendar.YEAR), DT.get(Calendar.MONTH), DT.get(Calendar.DATE), nEndDay, 0);
			}
			
			// ���� ����� ������/��������� ������ �������� � ��������� �������, �� ������������� ����� � ������ ���������� ��������
			if ( DT.get(Calendar.HOUR_OF_DAY) == nBreakTimeStart ){
				DT.set(DT.get(Calendar.YEAR), DT.get(Calendar.MONTH), DT.get(Calendar.DATE), nBreakTimeEnd, 0);
			}
			
			// ���� ����� ����������� � ������ �� ������� �� ����� �������� ���, ��������� ����� ���������� � ������
			if ( DT.get(Calendar.HOUR_OF_DAY) >= nStartDay && DT.get(Calendar.HOUR_OF_DAY) < nEndDay){
				nStartDayWorkTime = nEndDay*60 - DT.get(Calendar.HOUR_OF_DAY)*60 - DT.get(Calendar.MINUTE);//����� � �������
				// ���� ����� ������ ������ 4 �����, �� �������� ����� ��������: 
				// ���� ������ 5 �����, �������� 1 ��� �������� �� ������ ������� ������ 
				if (nStartDayWorkTime>300){
					nStartDayWorkTime -= 60;
				}
			}
		}
		return nStartDayWorkTime;
	}
	
	/**
	 * ������� ���������� ����� ���������� ������� ���� (� �������), ��������� ����� ����� ������.
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws Exception
	 */
	public int GetWorkingDaysBetweenTwoDates(Calendar startDate, Calendar endDate) throws Exception {
		Calendar lStartCalendar = (Calendar) startDate.clone();
		Calendar lEndCalendar = (Calendar) endDate.clone();
		int workDays = 0;
		// ���� ��������� � �������� ���� ���������, �� 
		if (lStartCalendar.get(Calendar.YEAR) == lEndCalendar.get(Calendar.YEAR) &&  lStartCalendar.get(Calendar.MONTH) == lEndCalendar.get(Calendar.MONTH) && lStartCalendar.get(Calendar.DATE) == lEndCalendar.get(Calendar.DATE)) {
			return 0;
		}
		
		// �� ��������� ��������� ����
		lStartCalendar.set(lStartCalendar.get(Calendar.YEAR), lStartCalendar.get(Calendar.MONTH), lStartCalendar.get(Calendar.DATE) + 1, nStartDay, 0);
		lStartCalendar.setTime(lStartCalendar.getTime());        

		// �� ��������� �������� ����
		lEndCalendar.set(lEndCalendar.get(Calendar.YEAR), lEndCalendar.get(Calendar.MONTH), lEndCalendar.get(Calendar.DATE) - 1, nEndDay, 0);
		lEndCalendar.setTime(lEndCalendar.getTime());

		if (lStartCalendar.getTimeInMillis() > lEndCalendar.getTimeInMillis()) {
			lStartCalendar.setTime(lEndCalendar.getTime());
			lEndCalendar.setTime(lStartCalendar.getTime());
		}
		//=======================================================================================
		//���������� ���������� ������� ���� ����� ����� ������
		while (lStartCalendar.getTimeInMillis() < lEndCalendar.getTimeInMillis()){
			if(!CheckDateInExcludeList(lStartCalendar)){
				if (lStartCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && lStartCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
					workDays++;
				}
			}
			lStartCalendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		// ���������� ����� ���������� ����, ������� �������� ��������� � ������ 
		return workDays;
	}
	
	// ================== ����������� ������ ============================
	// ������� ����� ����� ������, �������� ������� � �������: "dd.MM.yyyy HH:mm", ��� ����� ����������, �������� � names.nsf
	public static int GetWorkTimeFromString(String sStartDate, String sEndDate) throws Exception{		
		DateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		
		Calendar dtStart = Calendar.getInstance();
		dtStart.setTime(formatter.parse(sStartDate));
		
		Calendar dtEnd = Calendar.getInstance();
		dtEnd.setTime(formatter.parse(sEndDate));		
		
		return new WorkTimeBetweenTwoDates().GetWorkTimeBetweenTwoDates(dtStart, dtEnd, false);
	}
	
}
package com.fuib.lotus.agents;

import com.fuib.lotus.utils.Tools;

public class TimerInfo {
	private long startMS = 0;		// time start in milliseconds
	private long endMS = 0;			// time end in milliseconds
	private String _msg = "";

	public TimerInfo(String msg) {
		_msg = msg;
		startMS = System.currentTimeMillis();
	}
	
	public void Stop() {
		endMS = System.currentTimeMillis();
	}
	
	public long getDiff() {
		Stop();
		return endMS - startMS;
	}
	
	/**
	 * ������� ������� � ������������� ������� � ������� ���������� ������:
	 * 	� �����, ���� ����� - ��� � �����;
	 * 	� �������, ���� ����� ����, �� ������ �������;
	 * 	����� � ��������
	 */
	public String toString() {
		String msg = _msg;
		double diff = ((double) getDiff()) / 1000;
		if (diff < 60) {
			if (diff < 1000)
				return msg + ": [" + diff + " s.]";						// �����������, ���� ����� �������
			else
				return msg + ": [" + Tools.round(diff, 3) + " s.]";		// �������
		}
		else {
			diff = diff / 60;
			if (diff < 60)
				return msg + ": [" + Tools.round(diff, 2) + " m.]";		// ������
			else
				return msg + ": [" + Tools.round(diff/60, 2) + " h.]";	// ����
		}
			
	}
}
package com.fuib.lotus.agents;

public class TimerInfo {
	private long startMS = 0;		// time start in milliseconds
	private long endMS = 0;		// time end in milliseconds
	private String _msg = "";

	public TimerInfo(String msg) {
		_msg = msg;
		startMS = System.currentTimeMillis();
	}
	
	public void Stop() {
		if (endMS == 0)	endMS = System.currentTimeMillis();
	}
	
	public long getDiff() {
		Stop();
		return endMS - startMS;
	}
	
	public String toString() {
		String msg = _msg;
		return msg + " ["+(getDiff())+" ms]";
	}
}
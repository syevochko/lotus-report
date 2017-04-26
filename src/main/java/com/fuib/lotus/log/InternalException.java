package com.fuib.lotus.log;

/**
 * ����� ������������ ��� ��������� ��������� ���������������, �.�. ��������� ��� ������ - ���� ��� ������� ���� �� ���������� � �� ��������� � �������
 * ���� ������ id � text �� ������ �� ������, �.�. ��� �� ������ ����� ����� ������� ������ - ��������� ���������� � NotesException,
 * 	����� ����, ���� � ����� ������� � ����������� ����� ����������� �� ���� ������ ��� ���������,
 * 	�� ������ � �������, ���� ���� �������� ����� ������ � ���������� �������
 */
public class InternalException extends Exception {
	/**
	 * Default serial ID
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * ��� ������<br /><br />
	 * ������ ��� ���������� � NotesException;<br />
	 * �� ��������� - �� QA
	 */
	public int id = LogEx.ERRc1222;
	/**
	 * ����� ������<br /><br />
	 * ������ ��� ���������� � NotesException
	 */
	public String text = null;
	
	
	public InternalException(int code, String message) {
		super(message);
		id = code;
		text = message;
	}
	
	public String getMessage() {
		return (super.getMessage() != null) ? super.getMessage() : this.toString();
	}
	
}

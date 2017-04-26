package com.fuib.lotus.log;

/**
 * Класс предназначен для выделения обработки предопределённых, т.е. известных нам ошибок - стэк для данного типа не логируется и не выводится в консоль
 * Поля класса id и text не менять на методы, т.к. при их вызове нужно будет ставить скобки - нарушится унификация с NotesException,
 * 	кроме того, хоть с одной стороны и небезопасно иметь возможность на ходу менять эти параметры,
 * 	но бывает и полезно, если надо уточнить текст ошибки в вызывающих методах
 */
public class InternalException extends Exception {
	/**
	 * Default serial ID
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Код ошибки<br /><br />
	 * Служит для унификации с NotesException;<br />
	 * по умолчанию - на QA
	 */
	public int id = LogEx.ERRc1222;
	/**
	 * Текст ошибки<br /><br />
	 * Служит для унификации с NotesException
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

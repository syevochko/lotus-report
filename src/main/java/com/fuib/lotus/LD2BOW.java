package com.fuib.lotus;

import lotus.domino.Document;
import lotus.domino.NotesException;

/**
 * Класс LD2BOW - Lotus Document to Business Object Wrapper
 * 		Базовый класс оберток документов Lotus для бизнесс-объектов
 * 		Конструктор не должен генерировать ошибки
 * 		Для проверки удачной инициализации объекта использовать метод IsInitialize
 * @author pashun
 *
 */
public class LD2BOW {
	protected LNEnvironment m_env = null;
	// Абстрактный документ
	protected Document m_doc = null;
	// Флаг удачной инициализации объекта
	protected boolean m_bIsInitialize;
	
	/**
	 * Инициализация по объекту документа
	 * @param pcore Core Объект ядра
	 * @param pdoc Document Объект документа
	 * @throws NotesException 
	 */
	public LD2BOW(LNEnvironment penv, Document pdoc) throws NotesException {
		m_env = penv;
		if (pdoc != null) {
			m_doc = pdoc;
			m_bIsInitialize = isValidDocument();
		}
	}
	
	public void Recycle() throws NotesException {}
	
	// ========================================================
	// Свойства
	/**
	 * Флаг удачной инициализации
	 */
	public boolean IsInitialize() {
		return m_bIsInitialize;
	}
	
	public Document Doc() {
		return m_doc;
	}
	
	public String UNID() throws NotesException {
		if (m_bIsInitialize) return m_doc.getUniversalID();
		else return "";
	}
	// Свойства
	// ========================================================

	/**
	 * Применение изменений в бизнесс-объекте (сохранение объекта документа)
	 */
	public void Update() throws NotesException {
		m_doc.save(true);
	}
	
	/**
	 * Проверка: Является ли полученый документ валидным документом
	 */
	protected boolean isValidDocument() throws NotesException {
		if (m_doc != null)
			return true;
		return false;
	}
}
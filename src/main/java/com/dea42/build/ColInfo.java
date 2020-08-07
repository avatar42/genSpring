package com.dea42.build;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Types;

/**
 * Title: ColInfo <br>
 * Description: Holds info about a column from a table or view. <br>
 * Copyright: Copyright (c) 2001-2004 <br>
 * Company: RMRR <br>
 * <br>
 * 
 * @author David Abigt <br>
 * @version 1.0
 */

public class ColInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	// name of column in DB
	private String colName;
	// variable name
	private String vName;
	// Java type
	private String type;
	// the java.sql.Types value
	private int stype;
	// getter/setter name (the bit after get/set
	private String gsName;
	private int length;
	// needed import(s) for field type and annotation
	private String importStr;
	/**
	 * This column is the primary key
	 */
	private boolean pk = false;
	private String defaultVal;
	private String constraint;

	private boolean required = false;
	// show in list pages
	private boolean list = true;
	// filter from REST interface
	private boolean jsonIgnore = false;
	// add unique flag
	private boolean unique = false;
	// Framework set field not to be exposed in GUI or REST
	private boolean hidden = false;

	private String foreignTable;
	private String foreignCol;

	private int colScale;
	private int colPrecision;

	/**
	 * If true can not be null
	 * 
	 * @return
	 */
	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	/**
	 * return true if a String type
	 * 
	 * @return
	 */
	public boolean isString() {
		return stype == Types.VARCHAR || stype == Types.NVARCHAR || stype == Types.LONGNVARCHAR
				|| stype == Types.LONGVARCHAR || stype == Types.BLOB || stype == Types.CHAR || stype == Types.SQLXML;
	}

	public boolean isTimestamp() {
		return stype == Types.TIMESTAMP;
	}

	public boolean isDate() {
		return stype == Types.DATE;
	}

	/**
	 * get field number (DB order)
	 * 
	 * @return
	 */
	public int getfNum() {
		return fNum;
	}

	/**
	 * set field number (DB order)
	 * 
	 * @param fNum
	 */
	public void setfNum(int fNum) {
		this.fNum = fNum;
	}

	private int fNum;

	/**
	 * Get the base getter/setter name
	 * 
	 * @return
	 */
	public String getGsName() {
		return gsName;
	}

	/**
	 * Get the base getter/setter name
	 * 
	 * @param gsName
	 */
	public void setGsName(String gsName) {
		this.gsName = gsName;
	}

	public ColInfo() {
	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
	}

	public void setColName(String colName) {
		this.colName = colName;
	}

	/**
	 * gets the name of the column in the table
	 * 
	 * @return String
	 */
	public String getColName() {
		return colName;
	}

	/**
	 * gets the name of the constant name for the column
	 * 
	 * @return String
	 */
	public String getConstName() {
		return colName.toUpperCase();

	}

	public void setVName(String vName) {
		this.vName = vName;
	}

	/**
	 * gets what we want to call the member
	 * 
	 * @return String
	 */
	public String getVName() {
		return vName;
	}

	/**
	 * @return the vName
	 */
	public String getvName() {
		return vName;
	}

	/**
	 * @param vName the vName to set
	 */
	public void setvName(String vName) {
		this.vName = vName;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * gets java type of data colmun will hold
	 * 
	 * @return String
	 */
	public String getType() {
		return type;
	}

	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * max length of data column will hold if type String
	 * 
	 * @return String
	 */
	public int getLength() {
		return length;
	}

	public void setPk(boolean pk) {
		this.pk = pk;
	}

	/**
	 * true is this column is an IDENITY column
	 * 
	 * @return String
	 */
	public boolean isPk() {
		return pk;
	}

	public void setDefaultVal(String defaultVal) {
		this.defaultVal = defaultVal;
	}

	/**
	 * value used as default
	 * 
	 * @return String
	 */
	public String getDefaultVal() {
		return defaultVal;
	}

	/**
	 * name of constraint assoc with this column if there is one
	 * 
	 * @param constraint
	 */
	public void setConstraint(String constraint) {
		this.constraint = constraint;
	}

	/**
	 * name of constraint assoc with this column if there is one
	 * 
	 * @return String
	 */
	public String getConstraint() {
		return constraint;
	}

	public String getForeignTable() {
		return foreignTable;
	}

	public void setForeignTable(String foreignTable) {
		this.foreignTable = foreignTable;
	}

	public String getForeignCol() {
		return foreignCol;
	}

	public void setForeignCol(String foreignCol) {
		this.foreignCol = foreignCol;
	}

	public int getColScale() {
		return colScale;
	}

	public void setColScale(int colScale) {
		this.colScale = colScale;
	}

	public int getColPrecision() {
		return colPrecision;
	}

	public void setColPrecision(int colPrecision) {
		this.colPrecision = colPrecision;
	}

	/**
	 * get java.sql.Types type
	 * 
	 * @return
	 */
	public int getStype() {
		return stype;
	}

	/**
	 * Set java.sql.Types type
	 * 
	 * @param stype
	 */
	public void setStype(int stype) {
		this.stype = stype;
	}

	/**
	 * @return the list
	 */
	public boolean isList() {
		return list;
	}

	/**
	 * @param list the list to set
	 */
	public void setList(boolean list) {
		this.list = list;
	}

	/**
	 * @return the hidden
	 */
	public boolean isHidden() {
		return hidden;
	}

	/**
	 * @param hidden the hidden to set
	 */
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	/**
	 * @return the jsonIgnore
	 */
	public boolean isJsonIgnore() {
		return jsonIgnore;
	}

	/**
	 * @param jsonIgnore the jsonIgnore to set
	 */
	public void setJsonIgnore(boolean jsonIgnore) {
		this.jsonIgnore = jsonIgnore;
	}

	/**
	 * @return the unique
	 */
	public boolean isUnique() {
		return unique;
	}

	/**
	 * @param unique the unique to set
	 */
	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	/**
	 * @return the importStr
	 */
	public String getImportStr() {
		return importStr;
	}

	/**
	 * @param importStr the importStr to set
	 */
	public void setImportStr(String importStr) {
		this.importStr = importStr;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ColInfo [colName=").append(colName).append(", vName=").append(vName).append(", type=")
				.append(type).append(", stype=").append(stype).append(", gsName=").append(gsName).append(", length=")
				.append(length).append(", importStr=").append(importStr).append(", pk=").append(pk)
				.append(", defaultVal=").append(defaultVal).append(", constraint=").append(constraint)
				.append(", required=").append(required).append(", list=").append(list).append(", jsonIgnore=")
				.append(jsonIgnore).append(", unique=").append(unique).append(", hidden=").append(hidden)
				.append(", foreignTable=").append(foreignTable).append(", foreignCol=").append(foreignCol)
				.append(", colScale=").append(colScale).append(", colPrecision=").append(colPrecision).append(", fNum=")
				.append(fNum).append("]");
		return builder.toString();
	}

}
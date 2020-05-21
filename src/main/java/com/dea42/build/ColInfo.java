package com.dea42.build;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

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

	private String colName;
	private String constName;
	private String vName;
	private String type;
	// the java.sql.Types value
	private int stype;
	// TODO: remove
	private String jType;
	private String gsName;
	private int length;
	/**
	 * This column is the primary key
	 */
	private boolean pk = false;
	/**
	 * 2nd key of combo prime key (for Views)
	 */
	private boolean sk = false;
	private String defaultVal;
	private String constraint;
	private boolean numeric = false;
	private boolean date = false;

	// todo: add support for this
	private boolean required = false;

	private String foreignTable;
	private String foreignCol;

	private int colScale;
	private int colPrecision;

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isDate() {
		return date;
	}

	/**
	 * @deprecated
	 * @return
	 */
	public boolean isNumeric() {
		return numeric;
	}

	/**
	 * @deprecated
	 * @param numeric
	 */
	public void setNumeric(boolean numeric) {
		this.numeric = numeric;
	}

	public String getvName() {
		return vName;
	}

	public void setvName(String vName) {
		this.vName = vName;
	}

	public boolean isSk() {
		return sk;
	}

	public void setSk(boolean sk) {
		this.sk = sk;
	}

	public int getfNum() {
		return fNum;
	}

	public void setfNum(int fNum) {
		this.fNum = fNum;
	}

	private int fNum;

	public String getGsName() {
		return gsName;
	}

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
		constName = colName.toUpperCase();
	}

	/**
	 * GetName SQL used to create
	 * 
	 * @return String
	 * @deprecated
	 */
	public String getjType() {
		return jType;
	}

	/**
	 * @param jType
	 * @deprecated
	 */
	public void setjType(String jType) {
		this.jType = jType;
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
		return constName;
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

	public void setType(String type) {
		this.type = type;
		if ("Float".equals(type)) {
			setNumeric(true);
		} else if ("Double".equals(type)) {
			setNumeric(true);
		} else if (type.startsWith("Integer")) {// can be INT or INT IDENTITY
			setNumeric(true);
		} else if ("Long".equals(type)) {
			setNumeric(true);
		} else if ("Timestamp".equals(type)) {
			date = true;
		}
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

	public void setConstName(String constName) {
		this.constName = constName;
	}

	public int getStype() {
		return stype;
	}

	public void setStype(int stype) {
		this.stype = stype;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ColInfo [colName=").append(colName).append(", constName=").append(constName).append(", vName=")
				.append(vName).append(", type=").append(type).append(", stype=").append(stype).append(", jType=")
				.append(jType).append(", gsName=").append(gsName).append(", length=").append(length).append(", pk=")
				.append(pk).append(", sk=").append(sk).append(", defaultVal=").append(defaultVal)
				.append(", constraint=").append(constraint).append(", numeric=").append(numeric).append(", date=")
				.append(date).append(", required=").append(required).append(", foreignTable=").append(foreignTable)
				.append(", foreignCol=").append(foreignCol).append(", colScale=").append(colScale)
				.append(", colPrecision=").append(colPrecision).append(", fNum=").append(fNum).append("]");
		return builder.toString();
	}

}
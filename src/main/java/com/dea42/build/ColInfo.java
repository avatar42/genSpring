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
	// message key
	private String msgKey;
	// variable name
	private String vName;
	// Java type used in app
	private String type;
	// Java type used by driver (may be parent of type)
	private String jtype;
	// the java.sql.Types value
	private int stype;
	// getter/setter name (the bit after get/set
	private String gsName;
	// length where needed
	private int length;
	// needed import(s) for field type and annotation
	private String importStr;
	// This column is the primary key
	private boolean pk = false;
	// default value
	private String defaultVal;
	// constraint if there is one
	private String constraint;
	// is this a non nullable field
	private boolean required = false;
	// show in list pages
	private boolean list = false;
	// filter from REST interface
	private boolean jsonIgnore = false;
	// add unique flag
	private boolean unique = false;
	// Framework set field not to be exposed in GUI lists or REST returns
	private boolean hidden = false;
	// treat as password field
	private boolean password = false;
	// treat as email field
	private boolean email = false;
	// treat as created field
	private boolean created = false;
	// treat as lastMod field
	private boolean lastMod = false;

	private boolean adminOnly = false;

	private String foreignTable;
	private String foreignCol;

	private int colScale;
	private int colPrecision;

	private String comment;

	/**
	 * If true can not be null
	 * 
	 * @return
	 */
	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean trueFalse) {
		this.required = trueFalse;
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
	 * Gets java type of data column will hold
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
	 * For setting test numbers where l needs added longs
	 * 
	 * @return
	 */
	public String getMod() {
		if ("Long".equals(getType()))
			return "l";

		return "";
	}

	/**
	 * max length of data column will hold if type String
	 * 
	 * @return String
	 */
	public int getLength() {
		return length;
	}

	public void setPk(boolean trueFalse) {
		this.pk = trueFalse;
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
		if (defaultVal != null) {
			return defaultVal;
		} else {
			if (created || lastMod) {
				return "new " + type + "(System.currentTimeMillis())";
			} else {
				return null;
			}
		}
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
	 * @param trueFalse the list to set
	 */
	public void setList(boolean trueFalse) {
		this.list = trueFalse;
	}

	/**
	 * @return the hidden
	 */
	public boolean isHidden() {
		return hidden;
	}

	/**
	 * @param trueFalse the hidden to set
	 */
	public void setHidden(boolean trueFalse) {
		this.hidden = trueFalse;
	}

	/**
	 * @return the jsonIgnore
	 */
	public boolean isJsonIgnore() {
		return jsonIgnore;
	}

	/**
	 * @param trueFalse the jsonIgnore to set
	 */
	public void setJsonIgnore(boolean trueFalse) {
		this.jsonIgnore = trueFalse;
	}

	/**
	 * @return the unique
	 */
	public boolean isUnique() {
		return unique;
	}

	/**
	 * @param trueFalse the unique to set
	 */
	public void setUnique(boolean trueFalse) {
		this.unique = trueFalse;
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

	/**
	 * @return the password
	 */
	public boolean isPassword() {
		return password;
	}

	/**
	 * @param trueFalse the password to set. Note if true set hidden as well
	 */
	public void setPassword(boolean trueFalse) {
		this.password = trueFalse;
		this.hidden = trueFalse;
	}

	/**
	 * @return the msgKey
	 */
	public String getMsgKey() {
		return msgKey;
	}

	/**
	 * @param msgKey the msgKey to set
	 */
	public void setMsgKey(String msgKey) {
		this.msgKey = msgKey;
	}

	/**
	 * @return the email
	 */
	public boolean isEmail() {
		return email;
	}

	/**
	 * @param trueFalse the email to set
	 */
	public void setEmail(boolean trueFalse) {
		this.email = trueFalse;
	}

	/**
	 * @return the created
	 */
	public boolean isCreated() {
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(boolean created) {
		this.created = created;
	}

	/**
	 * @return the lastMod
	 */
	public boolean isLastMod() {
		return lastMod;
	}

	/**
	 * @param lastMod the lastMod to set
	 */
	public void setLastMod(boolean lastMod) {
		this.lastMod = lastMod;
	}

	/**
	 * @return the jtype
	 */
	public String getJtype() {
		return jtype;
	}

	/**
	 * @param jtype the jtype to set
	 */
	public void setJtype(String jtype) {
		this.jtype = jtype;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the adminOnly
	 */
	public boolean isAdminOnly() {
		return adminOnly;
	}

	/**
	 * @param adminOnly the adminOnly to set
	 */
	public void setAdminOnly(boolean adminOnly) {
		this.adminOnly = adminOnly;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (adminOnly ? 1231 : 1237);
		result = prime * result + ((colName == null) ? 0 : colName.hashCode());
		result = prime * result + colPrecision;
		result = prime * result + colScale;
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result + ((constraint == null) ? 0 : constraint.hashCode());
		result = prime * result + (created ? 1231 : 1237);
		result = prime * result + ((defaultVal == null) ? 0 : defaultVal.hashCode());
		result = prime * result + (email ? 1231 : 1237);
		result = prime * result + fNum;
		result = prime * result + ((foreignCol == null) ? 0 : foreignCol.hashCode());
		result = prime * result + ((foreignTable == null) ? 0 : foreignTable.hashCode());
		result = prime * result + ((gsName == null) ? 0 : gsName.hashCode());
		result = prime * result + (hidden ? 1231 : 1237);
		result = prime * result + ((importStr == null) ? 0 : importStr.hashCode());
		result = prime * result + (jsonIgnore ? 1231 : 1237);
		result = prime * result + ((jtype == null) ? 0 : jtype.hashCode());
		result = prime * result + (lastMod ? 1231 : 1237);
		result = prime * result + length;
		result = prime * result + (list ? 1231 : 1237);
		result = prime * result + ((msgKey == null) ? 0 : msgKey.hashCode());
		result = prime * result + (password ? 1231 : 1237);
		result = prime * result + (pk ? 1231 : 1237);
		result = prime * result + (required ? 1231 : 1237);
		result = prime * result + stype;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + (unique ? 1231 : 1237);
		result = prime * result + ((vName == null) ? 0 : vName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColInfo other = (ColInfo) obj;
		if (adminOnly != other.adminOnly)
			return false;
		if (colName == null) {
			if (other.colName != null)
				return false;
		} else if (!colName.equals(other.colName))
			return false;
		if (colPrecision != other.colPrecision)
			return false;
		if (colScale != other.colScale)
			return false;
		if (comment == null) {
			if (other.comment != null)
				return false;
		} else if (!comment.equals(other.comment))
			return false;
		if (constraint == null) {
			if (other.constraint != null)
				return false;
		} else if (!constraint.equals(other.constraint))
			return false;
		if (created != other.created)
			return false;
		if (defaultVal == null) {
			if (other.defaultVal != null)
				return false;
		} else if (!defaultVal.equals(other.defaultVal))
			return false;
		if (email != other.email)
			return false;
		if (fNum != other.fNum)
			return false;
		if (foreignCol == null) {
			if (other.foreignCol != null)
				return false;
		} else if (!foreignCol.equals(other.foreignCol))
			return false;
		if (foreignTable == null) {
			if (other.foreignTable != null)
				return false;
		} else if (!foreignTable.equals(other.foreignTable))
			return false;
		if (gsName == null) {
			if (other.gsName != null)
				return false;
		} else if (!gsName.equals(other.gsName))
			return false;
		if (hidden != other.hidden)
			return false;
		if (importStr == null) {
			if (other.importStr != null)
				return false;
		} else if (!importStr.equals(other.importStr))
			return false;
		if (jsonIgnore != other.jsonIgnore)
			return false;
		if (jtype == null) {
			if (other.jtype != null)
				return false;
		} else if (!jtype.equals(other.jtype))
			return false;
		if (lastMod != other.lastMod)
			return false;
		if (length != other.length)
			return false;
		if (list != other.list)
			return false;
		if (msgKey == null) {
			if (other.msgKey != null)
				return false;
		} else if (!msgKey.equals(other.msgKey))
			return false;
		if (password != other.password)
			return false;
		if (pk != other.pk)
			return false;
		if (required != other.required)
			return false;
		if (stype != other.stype)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (unique != other.unique)
			return false;
		if (vName == null) {
			if (other.vName != null)
				return false;
		} else if (!vName.equals(other.vName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ColInfo [colName=").append(colName).append(", msgKey=").append(msgKey).append(", vName=")
				.append(vName).append(", type=").append(type).append(", jtype=").append(jtype).append(", stype=")
				.append(stype).append(", gsName=").append(gsName).append(", length=").append(length)
				.append(", importStr=").append(importStr).append(", pk=").append(pk).append(", defaultVal=")
				.append(defaultVal).append(", constraint=").append(constraint).append(", required=").append(required)
				.append(", list=").append(list).append(", jsonIgnore=").append(jsonIgnore).append(", unique=")
				.append(unique).append(", hidden=").append(hidden).append(", password=").append(password)
				.append(", email=").append(email).append(", created=").append(created).append(", lastMod=")
				.append(lastMod).append(", adminOnly=").append(adminOnly).append(", foreignTable=").append(foreignTable)
				.append(", foreignCol=").append(foreignCol).append(", colScale=").append(colScale)
				.append(", colPrecision=").append(colPrecision).append(", comment=").append(comment).append(", fNum=")
				.append(fNum).append("]");
		return builder.toString();
	}

}
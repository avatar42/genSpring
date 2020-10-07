package com.dea42.build;

import java.io.Serializable;
import java.sql.Types;

import lombok.Data;

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
@Data
public class ColInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	// Order in table
	private int fNum;
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
	 * @param trueFalse the password to set. Note if true set hidden as well
	 */
	public void setPassword(boolean trueFalse) {
		this.password = trueFalse;
		this.hidden = trueFalse;
	}

}
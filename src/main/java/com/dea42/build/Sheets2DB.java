package com.dea42.build;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dea42.common.Db;
import com.dea42.common.Utils;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;

/**
 * App for exporting a Google sheet to a DB. For each tab it drops the matching
 * table<BR>
 * Does a first pass to read the data into a Map based on the header row (1st or
 * bottom frozen)<br>
 * Does a second pass checking for links. If found adds headers with "link"
 * added to end with the associated data to Map.<br>
 * Creates a table based on header fields. <br>
 * Imports the data into the table. Note currently uses the row number as the pk
 * for each table to ensure there is one.
 * 
 * @see /genSpring/src/main/resources/sheet.properties for customizable
 *      properties
 * 
 * @author avata
 *
 */
public class Sheets2DB {
	private static final Logger LOGGER = LoggerFactory.getLogger(Sheets2DB.class.getName());
	public static long ONE_DAY_MILS = 86400000l;

	private static final String APPLICATION_NAME = "Google Sheets 2 DB";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	/*
	 * Datetime Date and Time formats supported
	 */
	public static final SimpleDateFormat WK_DATE_TIME = new SimpleDateFormat("EEE MM/dd/yy hh:mm a");
	public static final SimpleDateFormat WK_DATE_TIME24 = new SimpleDateFormat("EEE MM/dd/yy HH:mm");
	public static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("MM/dd/yy hh:mm a");
	public static final SimpleDateFormat DATE_TIME24 = new SimpleDateFormat("MM/dd/yy HH:mm");
	public static final SimpleDateFormat WK_DATE_TIMED = new SimpleDateFormat("EEE MM-dd-yy hh:mm a");
	public static final SimpleDateFormat WK_DATE_TIME24D = new SimpleDateFormat("EEE MM-dd-yy HH:mm");
	public static final SimpleDateFormat DATE_TIMED = new SimpleDateFormat("MM-dd-yy hh:mm a");
	public static final SimpleDateFormat DATE_TIME24D = new SimpleDateFormat("MM-dd-yy HH:mm");
	public static final SimpleDateFormat DATE_ONLY = new SimpleDateFormat("MM/dd/yy");
	public static final SimpleDateFormat DATE_ONLYD = new SimpleDateFormat("MM-dd-yy");
	public static final SimpleDateFormat TIVO_SORTABLE = new SimpleDateFormat("yyyyMMddHHmm");
	public static final SimpleDateFormat TIME_ONLY = new SimpleDateFormat("hh:mm a");
	public static final SimpleDateFormat TIME_ONLY24 = new SimpleDateFormat("HH:mm");
	public static final SimpleDateFormat TIME_ONLYS = new SimpleDateFormat("hh:mm:ss a");
	public static final SimpleDateFormat TIME_ONLY24S = new SimpleDateFormat("HH:mm:ss");

	public static final SimpleDateFormat DB_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	/**
	 * Global instance of the scopes required. If modifying these scopes, delete
	 * your previously saved tokens/ folder. See
	 * https://developers.google.com/sheets/api/quickstart/java
	 */
	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

	private ResourceBundle renames = ResourceBundle.getBundle("rename");
	private ResourceBundle bundle = null;
	public String propKey = "sheet";
	private int passed = 0;
	private int failed = 0;
	private int skipped = 0;

	private Db db = new Db(propKey + ".runSQL()", propKey);

	public Sheets2DB() {
	}

	/**
	 * For testing
	 * 
	 * @param propKey
	 */
	protected Sheets2DB(String propKey) {
		this.propKey = propKey;
	}

	protected long parseDateStr(String source) {
		LOGGER.debug(source);

		if (source == null || source.length() < 5 || source.length() > 30)
			return 0;

		source = source.trim();

		int slashCnt = 0;
		int colonCnt = 0;
		int dashCnt = 0;
		boolean isNum = true;
		for (int i = 0; i < source.length(); i++) {
			if (source.charAt(i) == '/')
				slashCnt++;
			else if (source.charAt(i) == ':')
				colonCnt++;
			else if (source.charAt(i) == '-')
				dashCnt++;
			else if (!Character.isDigit(source.charAt(i)))
				isNum = false;
		}
		boolean hasSlashs = slashCnt == 2;
		boolean hasColon = colonCnt == 1 || colonCnt == 2;
		boolean hasSeconds = colonCnt == 2;
		boolean hasDashs = dashCnt == 2;
		boolean firstDigit = Character.isDigit(source.charAt(0));
		boolean lastDigit = Character.isDigit(source.charAt(source.length() - 1));
		Date d = null;
		if (!firstDigit && hasSlashs && hasColon && !hasDashs) {
			if (!lastDigit) {
				try {
					d = WK_DATE_TIME.parse(source);
					return d.getTime();
				} catch (ParseException e2) {
					LOGGER.info(WK_DATE_TIME.toPattern(), e2);
					LOGGER.info(WK_DATE_TIME.format(new Date()));
				}
			} else {
				try {
					d = WK_DATE_TIME24.parse(source);
					return d.getTime();
				} catch (ParseException e3) {
					LOGGER.info(WK_DATE_TIME24.toPattern(), e3);
					LOGGER.info(WK_DATE_TIME24.format(new Date()));
				}
			}
		}

		if (firstDigit && hasSlashs && hasColon && !hasDashs) {
			if (!lastDigit) {
				try {
					d = DATE_TIME.parse(source);
					return d.getTime();
				} catch (ParseException e2) {
					LOGGER.info(DATE_TIME.toPattern(), e2);
					LOGGER.info(DATE_TIME.format(new Date()));
				}
			} else {
				try {
					d = DATE_TIME24.parse(source);
					return d.getTime();
				} catch (ParseException e3) {
					LOGGER.info(DATE_TIME24.toPattern(), e3);
					LOGGER.info(DATE_TIME24.format(new Date()));
				}
			}
		}

		if (!firstDigit && !hasSlashs && hasColon && hasDashs) {
			if (!lastDigit) {
				try {
					d = WK_DATE_TIMED.parse(source);
					return d.getTime();
				} catch (ParseException e2) {
					LOGGER.info(WK_DATE_TIMED.toPattern(), e2);
					LOGGER.info(WK_DATE_TIMED.format(new Date()));
				}
			} else {
				if (source.indexOf('.') < 0) {
					try {
						d = WK_DATE_TIME24D.parse(source);
						return d.getTime();
					} catch (ParseException e3) {
						LOGGER.info(WK_DATE_TIME24D.toPattern(), e3);
						LOGGER.info(WK_DATE_TIME24D.format(new Date()));
					}
				} else {
					try {
						d = DB_DATETIME.parse(source);
						return d.getTime();
					} catch (ParseException e4) {
						LOGGER.info(DB_DATETIME.toPattern(), e4);
						LOGGER.info(DB_DATETIME.format(new Date()));
					}
				}
			}
		}

		if (firstDigit && !hasSlashs && hasColon && hasDashs) {
			if (!lastDigit) {
				try {
					d = DATE_TIMED.parse(source);
					return d.getTime();
				} catch (ParseException e2) {
					LOGGER.info(DATE_TIMED.toPattern(), e2);
					LOGGER.info(DATE_TIMED.format(new Date()));
				}
			} else {
				try {
					d = DATE_TIME24D.parse(source);
					return d.getTime();
				} catch (ParseException e3) {
					LOGGER.info(DATE_TIME24D.toPattern(), e3);
					LOGGER.info(DATE_TIME24D.format(new Date()));
				}
			}
		}

		if (isNum && hasSlashs && !hasColon && !hasDashs) {
			try {
				d = DATE_ONLY.parse(source);
				return d.getTime();
			} catch (ParseException e2) {
				LOGGER.info(DATE_ONLY.toPattern(), e2);
				LOGGER.info(DATE_ONLY.format(new Date()));
			}
		}

		if (isNum && !hasSlashs && !hasColon && hasDashs) {
			try {
				d = DATE_ONLYD.parse(source);
				return d.getTime();
			} catch (ParseException e2) {
				LOGGER.info(DATE_ONLYD.toPattern(), e2);
				LOGGER.info(DATE_ONLYD.format(new Date()));
			}
		}

		if (firstDigit && hasSlashs && hasColon && !hasDashs) {
			if (!lastDigit) {
				try {
					d = DATE_TIME.parse(source);
					return d.getTime();
				} catch (ParseException e2) {
					LOGGER.info(DATE_TIME.toPattern(), e2);
					LOGGER.info(DATE_TIME.format(new Date()));
				}
			} else {
				try {
					d = DATE_TIME24.parse(source);
					return d.getTime();
				} catch (ParseException e3) {
					LOGGER.info(DATE_TIME24.toPattern(), e3);
					LOGGER.info(DATE_TIME24.format(new Date()));
				}
			}
		}

		if (firstDigit && !hasSlashs && hasColon && !hasDashs) {
			if (!lastDigit) {
				if (hasSeconds) {
					try {
						d = TIME_ONLYS.parse(source);
						return d.getTime();
					} catch (ParseException e2) {
						LOGGER.info(TIME_ONLYS.toPattern(), e2);
						LOGGER.info(TIME_ONLYS.format(new Date()));
					}
				} else {
					try {
						d = TIME_ONLY.parse(source);
						return d.getTime();
					} catch (ParseException e2) {
						LOGGER.info(TIME_ONLY.toPattern(), e2);
						LOGGER.info(TIME_ONLY.format(new Date()));
					}
				}
			} else {
				if (hasSeconds) {
					try {
						d = TIME_ONLY24.parse(source);
						return d.getTime();
					} catch (ParseException e3) {
						LOGGER.info(TIME_ONLY24.toPattern(), e3);
						LOGGER.info(TIME_ONLY24.format(new Date()));
					}
				} else {
					try {
						d = TIME_ONLY24.parse(source);
						return d.getTime();
					} catch (ParseException e3) {
						LOGGER.info(TIME_ONLY24.toPattern(), e3);
						LOGGER.info(TIME_ONLY24.format(new Date()));
					}
				}
			}
		}

		// sortable date type returned by TiVo
		if (source.length() == 12 && isNum && !hasSlashs && !hasColon && !hasDashs) {
			try {
//				int year = Integer.parseInt(source.substring(0, 4));
//				int month = Integer.parseInt(source.substring(4, 6)) - 1;
//				int dayOfMonth = Integer.parseInt(source.substring(6, 8));
//				int hourOfDay = Integer.parseInt(source.substring(8, 10));
//				int minute = Integer.parseInt(source.substring(10, 12));
//				int second = 0;
//				GregorianCalendar gc = new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute, second);
//				return gc.getTimeInMillis();
				d = TIVO_SORTABLE.parse(source);
				return d.getTime();
			} catch (Exception e) {
				LOGGER.info(TIVO_SORTABLE.toPattern(), e);
				LOGGER.info(TIVO_SORTABLE.format(new Date()));
			}
		}

		return 0;
	}

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		InputStream in = Sheets2DB.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	/**
	 * returns column name matching 1 based inputColumnNumber
	 * 
	 * @param inputColumnNumber
	 * @return
	 */
	public String columnNumberToLetter(int inputColumnNumber) {
		String outputColumnName = "";
		int Base = 26;
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		int TempNumber = inputColumnNumber;
		while (TempNumber > 0) {
			int position = TempNumber % Base;
			outputColumnName = (position == 0 ? 'Z' : chars.charAt(position > 0 ? position - 1 : 0)) + outputColumnName;
			TempNumber = (TempNumber - 1) / Base;
		}
		LOGGER.debug("ColumnNumberToLetter :" + inputColumnNumber + " = " + outputColumnName);

		return outputColumnName;
	}

	/**
	 * Returns 1 based column number matching inputColumnName
	 * 
	 * @param inputColumnName
	 * @return
	 */
	public int columnLetterToNumber(String inputColumnName) {
		int outputColumnNumber = 0;

		if (inputColumnName == null || inputColumnName.length() == 0) {
			throw new IllegalArgumentException("Input (" + inputColumnName + ")is not valid!");
		}

		int i = inputColumnName.length() - 1;
		int t = 0;
		while (i >= 0) {
			char curr = inputColumnName.charAt(i);
			outputColumnNumber = outputColumnNumber + (int) Math.pow(26, t) * (curr - 'A' + 1);
			t++;
			i--;
		}
		LOGGER.debug("columnLetterToNumber : " + inputColumnName + " = " + outputColumnNumber);

		return outputColumnNumber;
	}

	/**
	 * returns list of zero based column numbers from the column names and ranges in
	 * colStr
	 * 
	 * @param colStr
	 * @return
	 */
	public List<Integer> strToCols(String colStr) {
		List<Integer> wantedCols = null;
		if (!StringUtils.isBlank(colStr)) {
			wantedCols = new ArrayList<Integer>();
			StringTokenizer st = new StringTokenizer(colStr, ",");
			while (st.hasMoreTokens()) {
				String def = st.nextToken();
				int del = def.indexOf("-");
				if (del > -1) {
					int startCol = columnLetterToNumber(def.substring(0, del)) - 1;
					int endCol = columnLetterToNumber(def.substring(del + 1)) - 1;
					for (int i = startCol; i <= endCol; i++)
						wantedCols.add(i);
				} else {
					wantedCols.add(columnLetterToNumber(def) - 1);
				}
			}
		}
		LOGGER.debug(wantedCols.toString());
		return wantedCols;
	}

	public void exportTab(Sheets service, String spreadsheetId, Sheet sheet, String colStr) {

		SheetProperties p = sheet.getProperties();
		String tabName = p.getTitle();
		GridProperties gp = p.getGridProperties();
		int columnCount = gp.getColumnCount();
		int rowCount = Utils.getProp(bundle, tabName + ".lastRow", gp.getRowCount());
		// headers / field names assumed to be in last frozen row or first row
		int frozenRowCount = gp.getFrozenRowCount();
		if (frozenRowCount < 1)
			frozenRowCount = 1;

		ResourceBundle bundle = ResourceBundle.getBundle(propKey);
		List<Integer> wantedColNums = strToCols(Utils.getProp(bundle, tabName + ".columns"));
		List<Integer> requiredColNums = strToCols(Utils.getProp(bundle, tabName + ".required"));
		List<Object> requiredFields = new ArrayList<Object>();

		// Get headers and basic cell data
		// fine max length to String fields
		Map<Object, Integer> maxFieldLenghts = new HashMap<Object, Integer>();
		// discovered field types
		Map<Object, Class<?>> fieldTypes = new HashMap<Object, Class<?>>();
		// preserve order of fields
		Object[] ha = new Object[columnCount];
		// holds the actual data
		Map<Integer, Map<Object, Object>> rowsData = new HashMap<Integer, Map<Object, Object>>();

		Sheets.Spreadsheets.Values.Get request;
		List<List<Object>> values;
		try {
			request = service.spreadsheets().values().get(spreadsheetId, tabName);
			// https://developers.google.com/sheets/api/reference/rest/v4/ValueRenderOption
			request.setValueRenderOption("UNFORMATTED_VALUE");// UNFORMATTED_VALUE//FORMULA
			// https://developers.google.com/sheets/api/reference/rest/v4/DateTimeRenderOption
			request.setDateTimeRenderOption("FORMATTED_STRING");

			ValueRange response = request.execute();

			values = response.getValues();
			if (values == null || values.isEmpty()) {
				LOGGER.error("No data found for tab " + tabName);
			} else {
				int rowId = 1;
				for (List<Object> row : values) {
					if (rowId == frozenRowCount) {
						// init field lengths to 0
						int colNum = 0;
						for (Object header : row) {
							if (wantedColNums == null || wantedColNums.contains(colNum)) {
								if (StringUtils.isBlank(Utils.tabToStr(renames, (String) header))) {
									header = "Col" + columnNumberToLetter(colNum + 1);
								}
								maxFieldLenghts.put(header, 0);
								fieldTypes.put(header, null);
								// note just pulling the keySet scrambles the order
								ha[colNum] = header;
								if (requiredColNums.contains(colNum)) {
									requiredFields.add(header);
								}
							}
							colNum++;
						}
						LOGGER.debug(row.toString());
					} else if (rowId > frozenRowCount && rowId <= rowCount) {
						Map<Object, Object> rowMap = new HashMap<Object, Object>();
						rowsData.put(rowId, rowMap);
						Object[] cells = row.toArray();
						for (int i = 0; i < cells.length; i++) {
							if (wantedColNums == null || wantedColNums.contains(i)) {
								rowMap.put(ha[i], cells[i]);

								Class<?> fieldCls = fieldTypes.get(ha[i]);
								if (cells[i] != null) {
									Class<?> cellCls = cells[i].getClass();
									if (cellCls.isAssignableFrom(BigDecimal.class)) {
										BigDecimal bd = (BigDecimal) cells[i];
										if (bd.stripTrailingZeros().scale() <= 0)
											cellCls = Integer.class;
									}
									// if String value with text in it, not "" or " "
									if (cells[i] instanceof String) {
										String s = (String) cells[i];
										if (!StringUtils.isBlank(s)) {
											long d = parseDateStr(s);
											if (d == 0) {
												int len = maxFieldLenghts.get(ha[i]);
												int vlen = s.length();
												if (len < vlen)
													maxFieldLenghts.put(ha[i], vlen);

												// if null or something other than String then set to String now
												if (fieldCls == null || !fieldCls.isAssignableFrom(String.class)) {
													fieldTypes.put(ha[i], String.class);
												}
											} else {
												// if date / time object then use that type instead of String
												if (d > ONE_DAY_MILS) {
													fieldTypes.put(ha[i], Date.class);
													cells[i] = new Date(d);
												} else {
													fieldTypes.put(ha[i], Time.class);
													cells[i] = new Time(d);
												}
												rowMap.put(ha[i], cells[i]);
											}
										}
									} else if (cells[i] instanceof Boolean) {
										fieldTypes.put(ha[i], Boolean.class);
									} else if (fieldCls == null) {
										fieldTypes.put(ha[i], cellCls);
										// if some reason the type changes by row set it to String
									} else if (!fieldCls.isAssignableFrom(cellCls)) {
										if (cellCls.isAssignableFrom(BigDecimal.class)) {
											if (fieldCls.isAssignableFrom(Integer.class)) {
												fieldTypes.put(ha[i], BigDecimal.class);
											} else {
												fieldTypes.put(ha[i], String.class);
												int vlen = cells[i].toString().length();
												maxFieldLenghts.put(ha[i], vlen);
											}
										} else if (cellCls.isAssignableFrom(Integer.class)) {
											if (!fieldCls.isAssignableFrom(BigDecimal.class)) {
												fieldTypes.put(ha[i], String.class);
												int vlen = cells[i].toString().length();
												maxFieldLenghts.put(ha[i], vlen);
											}
										} else {
											fieldTypes.put(ha[i], String.class);
											int vlen = cells[i].toString().length();
											maxFieldLenghts.put(ha[i], vlen);
										}
									}
								}
							}
						}
						LOGGER.debug(rowMap.toString());
					}
					rowId++;
				}
			}
		} catch (

		IOException e) {
			LOGGER.error("Failed to get field data for " + tabName, e);
		}

		// rescan for cells with links
		try {
			request = service.spreadsheets().values().get(spreadsheetId, tabName);
			request.setValueRenderOption("FORMULA");
			request.setDateTimeRenderOption("FORMATTED_STRING");
			ValueRange response = request.execute();

			values = response.getValues();
			if (values == null || values.isEmpty()) {
				LOGGER.error("No data found for tab " + tabName);
			} else {
				int rowId = 1;
				for (List<Object> row : values) {
					if (rowId > frozenRowCount && rowId <= rowCount) {
						Map<Object, Object> rowMap = rowsData.get(rowId);
						Object[] cells = row.toArray();
						for (int i = 0; i < cells.length; i++) {
							if (wantedColNums == null || wantedColNums.contains(i)) {

								if (cells[i] instanceof String) {
									String s = (String) cells[i];
									if (!StringUtils.isBlank(s)) {
										int beginIndex = s.indexOf("HYPERLINK(");
										if (beginIndex > -1) {
											beginIndex += 11;
											String header = ha[i] + " link";
											int endIndex = s.indexOf('"', beginIndex);
											s = s.substring(beginIndex, endIndex);
											Integer len = maxFieldLenghts.get(header);
											if (len == null)
												len = 0;
											int vlen = s.length();
											if (len < vlen)
												maxFieldLenghts.put(header, vlen);

											fieldTypes.put(header, String.class);
											rowMap.put(header, s);
										}
									}
								}
							}
						}
					}
					rowId++;
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to get link fields for " + tabName, e);
		}
		LOGGER.debug(maxFieldLenghts.toString());
		LOGGER.debug(fieldTypes.toString());

		String tableName = Utils.tabToStr(renames, tabName);
		// Drop old table if there is one
		runSQL("Drop table IF EXISTS " + tableName + ";");

		// create new table
		StringBuilder sb = new StringBuilder(
				"CREATE TABLE " + tableName + "(id integer NOT NULL primary key autoincrement");
		for (Object name : maxFieldLenghts.keySet()) {
			Class<?> cls = fieldTypes.get(name);
			if (cls == null) {
				cls = String.class;
			}
			String fieldName = Utils.tabToStr(renames, (String) name);
			sb.append(",\n").append(fieldName).append("\t");
			if (cls.isAssignableFrom(String.class)) {
				int len = maxFieldLenghts.get(name);
				if (len == 0)
					len = 20;
				sb.append("VARCHAR(" + len + ")");
			} else if (cls.isAssignableFrom(Boolean.class)) {
				// NOTE SQLite does not have boolean type
				if (db.isSQLite())
					sb.append("INTEGER");
				else
					sb.append("BOOLEAN");
			} else if (cls.isAssignableFrom(Integer.class)) {
				sb.append("INTEGER");
			} else if (cls.isAssignableFrom(Date.class)) {
				if (db.isSQLite())
					sb.append("INTEGER");
				else
					sb.append("DATETIME");
			} else if (cls.isAssignableFrom(Time.class)) {
				if (db.isSQLite())
					sb.append("INTEGER");
				else
					sb.append("TIME");
			} else if (cls.isAssignableFrom(BigDecimal.class)) {
				sb.append("REAL");
			} else {
				LOGGER.warn("Unknown field type:" + cls.getCanonicalName() + " for " + fieldName);
			}
			if (requiredFields.contains(name)) {
				sb.append(" NOT NULL");
			}
		}
		sb.append(");");
		runSQL(sb.toString());

		genInsert:
		// import data gathered into DB
		for (Integer rowId : rowsData.keySet()) {
			Map<Object, Object> row = rowsData.get(rowId);
			sb = new StringBuilder("INSERT INTO " + tableName + " (");
			boolean addcom = false;
			for (Object name : maxFieldLenghts.keySet()) {
				if (addcom) {
					sb.append(", ");
				} else {
					addcom = true;
				}
				String fieldName = Utils.tabToStr(renames, (String) name);
				sb.append(fieldName);
			}
			sb.append(") VALUES (");
			addcom = false;
			for (Object name : maxFieldLenghts.keySet()) {
				if (addcom) {
					sb.append(", ");
				} else {
					addcom = true;
				}
				Object val = row.get(name);
				if (rowId > 484) {
					LOGGER.debug("rowId:" + rowId);
				}
				// skip row
				if (val == null && requiredColNums.contains(name)) {
					LOGGER.warn("Skipping due to missing required data:" + row);
					skipped++;
					continue genInsert;
				}
				if (val instanceof String) {
					if (StringUtils.isBlank((String) val) && requiredFields.contains(name)) {
						LOGGER.warn("Skipping due to missing required data:" + row);
						skipped++;
						continue genInsert;
					}
					sb.append("'").append(((String) val).replace("'", "'\'")).append("'");
				} else if (val instanceof Boolean) {
					if (db.isSQLite()) {
						if (((Boolean) val)) {
							sb.append("1");
						} else {
							sb.append("0");
						}
					} else {
						if (((Boolean) val)) {
							sb.append("TRUE");
						} else {
							sb.append("FALSE");
						}
					}
				} else if (val instanceof Date) {
					sb.append("'").append(DB_DATETIME.format(((Date) val).getTime())).append("'");
				} else if (val instanceof Time) {
					if (db.isSQLite()) {
						sb.append(((Time) val).getTime());
					} else {
						sb.append("'").append(TIME_ONLY24S.format(((Time) val).getTime())).append("'");
					}
				} else {
					sb.append(val);
				}
			}
			sb.append(");");
			if (runSQL(sb.toString())) {
				passed++;
			} else {
				failed++;
			}
		}
	}

	/**
	 * Since we are recreating the table each time, do each call without
	 * transactions so we can see any trouble rows that might exist in one go.
	 * 
	 * @param sql
	 * @return pass / fail
	 */
	private boolean runSQL(String sql) {
		LOGGER.debug("Running:" + sql);
		boolean rtn = false;

		try {
			Connection conn = db.getConnection(propKey + ".runSQL()");
			Statement stmt = conn.createStatement();
			rtn = stmt.execute(sql);
			if (sql.startsWith("INSERT")) {
				int cnt = stmt.getUpdateCount();
				rtn = (cnt == 1);
//				LOGGER.error("SQL return was" + cnt);
			}
		} catch (SQLException e) {
			LOGGER.error("Failed to run:" + sql, e);
		} finally {
			db.close(propKey + ".runSQL()");
		}
		return rtn;
	}

	public void getSheet() {
		try {
			bundle = ResourceBundle.getBundle(propKey);

			// Build a new authorized API client service.
			final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			// https://docs.google.com/spreadsheets/d/1-xYv1AVkUC5J3Tqpy2_3alZ5ZpBPnnO2vUGUCUeLVVE/edit?usp=sharing
			final String spreadsheetId = Utils.getProp(bundle, "sheet.id",
					"1-xYv1AVkUC5J3Tqpy2_3alZ5ZpBPnnO2vUGUCUeLVVE");
			final String tabStr = Utils.getProp(bundle, "sheet.tabs", "");
			List<String> tabs = Arrays.asList(tabStr.split("\\s*,\\s*"));

			Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME).build();
			Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
			List<Sheet> sheets = spreadsheet.getSheets();
			// {"basicFilter":{"range":{"endColumnIndex":19,"endRowIndex":38,"sheetId":1049211208,"startColumnIndex":0,"startRowIndex":0},"sortSpecs":[{"dimensionIndex":0,"sortOrder":"ASCENDING"}]},
			// "properties":{
			// "gridProperties":{"columnCount":19,"frozenColumnCount":1,"frozenRowCount":1,"rowCount":38},
			// "index":2,"sheetId":1049211208,"sheetType":"GRID","title":"Networks"}}
			for (Sheet sheet : sheets) {
				SheetProperties p = sheet.getProperties();
				String tabName = p.getTitle();
				if (tabs.contains(tabName)) {
					final String colStr = Utils.getProp(bundle, tabName + ".columns", "");
					exportTab(service, spreadsheetId, sheet, colStr);
				}
			}
		} catch (GeneralSecurityException | IOException e) {
			LOGGER.error("Failed to get sheet ", e);
		}

		System.out.println("Inserted " + passed + " records.");
		System.out.println("Failed to insert " + failed + " records.");
		System.out.println("Skipped inserting " + skipped + " records.");
	}

	/**
	 * Prints the names and majors of students in a sample spreadsheet:
	 * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
	 */
	public static void main(String... args) throws IOException, GeneralSecurityException {
		Sheets2DB s = new Sheets2DB();
		s.getSheet();
	}
}

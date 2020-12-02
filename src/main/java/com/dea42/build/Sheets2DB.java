package com.dea42.build;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;

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

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class Sheets2DB extends CommonMethods {

	private static final String APPLICATION_NAME = "Google Sheets 2 DB";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";
	public static final String SCRIPTS_FOLDER = "Scripts";

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

	private int passed = 0;
	private int failed = 0;
	private int skipped = 0;

	private boolean failOnAnyError = false;

	/**
	 * default constructor using the sheet bundle name
	 * 
	 * @throws IOException
	 */
	public Sheets2DB() throws IOException {
		this(PROPKEY, false, false);
	}

	/**
	 * For used for testing
	 * 
	 * @param bundelName
	 * @param cleanFirst
	 * @param failOnAnyError
	 * @throws IOException
	 */
	public Sheets2DB(String bundelName, boolean cleanFirst, boolean failOnAnyError) throws IOException {
		this.failOnAnyError = failOnAnyError;
		super.initVars(bundelName);
	}

	protected long parseDateStr(String source) {
		log.debug("source:" + source);

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
					log.info(WK_DATE_TIME.toPattern(), e2);
					log.info(WK_DATE_TIME.format(new Date()));
				}
			} else {
				try {
					d = WK_DATE_TIME24.parse(source);
					return d.getTime();
				} catch (ParseException e3) {
					log.info(WK_DATE_TIME24.toPattern(), e3);
					log.info(WK_DATE_TIME24.format(new Date()));
				}
			}
		}

		if (firstDigit && hasSlashs && hasColon && !hasDashs) {
			if (!lastDigit) {
				try {
					d = DATE_TIME.parse(source);
					return d.getTime();
				} catch (ParseException e2) {
					log.info(DATE_TIME.toPattern(), e2);
					log.info(DATE_TIME.format(new Date()));
				}
			} else {
				try {
					d = DATE_TIME24.parse(source);
					return d.getTime();
				} catch (ParseException e3) {
					log.info(DATE_TIME24.toPattern(), e3);
					log.info(DATE_TIME24.format(new Date()));
				}
			}
		}

		if (!firstDigit && !hasSlashs && hasColon && hasDashs) {
			if (!lastDigit) {
				try {
					d = WK_DATE_TIMED.parse(source);
					return d.getTime();
				} catch (ParseException e2) {
					log.info(WK_DATE_TIMED.toPattern(), e2);
					log.info(WK_DATE_TIMED.format(new Date()));
				}
			} else {
				if (source.indexOf('.') < 0) {
					try {
						d = WK_DATE_TIME24D.parse(source);
						return d.getTime();
					} catch (ParseException e3) {
						log.info(WK_DATE_TIME24D.toPattern(), e3);
						log.info(WK_DATE_TIME24D.format(new Date()));
					}
				} else {
					try {
						d = DB_DATETIME.parse(source);
						return d.getTime();
					} catch (ParseException e4) {
						log.info(DB_DATETIME.toPattern(), e4);
						log.info(DB_DATETIME.format(new Date()));
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
					log.info(DATE_TIMED.toPattern(), e2);
					log.info(DATE_TIMED.format(new Date()));
				}
			} else {
				try {
					d = DATE_TIME24D.parse(source);
					return d.getTime();
				} catch (ParseException e3) {
					log.info(DATE_TIME24D.toPattern(), e3);
					log.info(DATE_TIME24D.format(new Date()));
				}
			}
		}

		if (isNum && hasSlashs && !hasColon && !hasDashs) {
			try {
				d = DATE_ONLY.parse(source);
				return d.getTime();
			} catch (ParseException e2) {
				log.info(DATE_ONLY.toPattern(), e2);
				log.info(DATE_ONLY.format(new Date()));
			}
		}

		if (isNum && !hasSlashs && !hasColon && hasDashs) {
			try {
				d = DATE_ONLYD.parse(source);
				return d.getTime();
			} catch (ParseException e2) {
				log.info(DATE_ONLYD.toPattern(), e2);
				log.info(DATE_ONLYD.format(new Date()));
			}
		}

		if (firstDigit && hasSlashs && hasColon && !hasDashs) {
			if (!lastDigit) {
				try {
					d = DATE_TIME.parse(source);
					return d.getTime();
				} catch (ParseException e2) {
					log.info(DATE_TIME.toPattern(), e2);
					log.info(DATE_TIME.format(new Date()));
				}
			} else {
				try {
					d = DATE_TIME24.parse(source);
					return d.getTime();
				} catch (ParseException e3) {
					log.info(DATE_TIME24.toPattern(), e3);
					log.info(DATE_TIME24.format(new Date()));
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
						log.info(TIME_ONLYS.toPattern(), e2);
						log.info(TIME_ONLYS.format(new Date()));
					}
				} else {
					try {
						d = TIME_ONLY.parse(source);
						return d.getTime();
					} catch (ParseException e2) {
						log.info(TIME_ONLY.toPattern(), e2);
						log.info(TIME_ONLY.format(new Date()));
					}
				}
			} else {
				if (hasSeconds) {
					try {
						d = TIME_ONLY24.parse(source);
						return d.getTime();
					} catch (ParseException e3) {
						log.info(TIME_ONLY24.toPattern(), e3);
						log.info(TIME_ONLY24.format(new Date()));
					}
				} else {
					try {
						d = TIME_ONLY24.parse(source);
						return d.getTime();
					} catch (ParseException e3) {
						log.info(TIME_ONLY24.toPattern(), e3);
						log.info(TIME_ONLY24.format(new Date()));
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
				log.info(TIVO_SORTABLE.toPattern(), e);
				log.info(TIVO_SORTABLE.format(new Date()));
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
		log.debug("ColumnNumberToLetter :" + inputColumnNumber + " = " + outputColumnName);

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
		log.debug("columnLetterToNumber : " + inputColumnName + " = " + outputColumnNumber);

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
		List<Integer> wantedCols = new ArrayList<Integer>();
		if (!StringUtils.isBlank(colStr)) {
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
		log.debug(colStr + "=>" + wantedCols.toString());
		return wantedCols;
	}

	public Object getTypedVal(Object val, Class<?> fieldCls) {
		if (val == null)
			return null;
		Class<?> cellCls = val.getClass();
		if (fieldCls == null) {
			if (val instanceof String) {
				String s = (String) val;
				if (!StringUtils.isBlank(s)) {
					s = s.trim();
					long d = parseDateStr(s);
					if (d == 0) {
						if (StringUtils.isNumeric(s)) {
							try {
								val = Integer.parseInt(s);
							} catch (NumberFormatException e) {
								try {
									val = Long.parseLong(s);
								} catch (NumberFormatException e2) {
									// should not happen but if does just return val
									log.warn("Value:(" + s + ") looked like a number but did not parse to one.");
									val = s;
								}
							}
						} else if (s.contains(".")) {
							try {
								val = new BigDecimal(s);
							} catch (NumberFormatException e) {
								val = s;
							}
						}
					} else {
						// if date / time object then use that type instead of String
						if (d > ONE_DAY_MILS) {
							val = new Date(d);
						} else {
							val = new Time(d);
						}
					}
				}
			} else if (cellCls.isAssignableFrom(BigDecimal.class)) {
				BigDecimal bd = (BigDecimal) val;
				if (bd.stripTrailingZeros().scale() <= 0) {
					try {
						try {
							val = bd.intValueExact();
						} catch (ArithmeticException e) {
							val = bd.longValueExact();
						}
					} catch (ArithmeticException e) {
						log.warn("Could not convert BigDecimal to " + fieldCls + " leaving as BigDecimal");
					}
				}
			}
		} else if (fieldCls.isAssignableFrom(String.class)) {
			val = val.toString();
		} else if (!fieldCls.isAssignableFrom(cellCls)) {
			if (cellCls.isAssignableFrom(BigDecimal.class)) {
				BigDecimal bd = (BigDecimal) val;
				if (bd.stripTrailingZeros().scale() <= 0) {
					try {
						if (fieldCls.isAssignableFrom(Integer.class)) {
							try {
								val = bd.intValueExact();
							} catch (ArithmeticException e) {
								val = bd.longValueExact();
							}
						} else if (fieldCls.isAssignableFrom(Long.class)) {
							val = bd.longValueExact();
						}
					} catch (ArithmeticException e) {
						log.warn("Could not convert BigDecimal to " + fieldCls + " leaving as BigDecimal");
					}
				}
			} else if (cellCls.isAssignableFrom(Integer.class) || cellCls.isAssignableFrom(Long.class)) {
				if (fieldCls.isAssignableFrom(BigDecimal.class)) {
					val = new BigDecimal("" + val);
				}
			} else if (fieldCls.isAssignableFrom(Date.class) || fieldCls.isAssignableFrom(Time.class)) {
				long d = parseDateStr((String) val);
				if (d > 0) {
					// if date / time object then use that type instead of String
					if (d > ONE_DAY_MILS) {
						val = new Date(d);
					} else {
						val = new Time(d);
					}
				}
			} else {
				val = val.toString();
			}
		}

		return val;
	}

	public void exportTab(Sheets service, String spreadsheetId, Sheet sheet) throws Exception {

		SheetProperties p = sheet.getProperties();
		String tabName = p.getTitle();
		String tableName = Utils.tabToStr(renames, tabName);
		// {"basicFilter":{"range":{"endColumnIndex":19,"endRowIndex":38,"sheetId":1049211208,"startColumnIndex":0,"startRowIndex":0},"sortSpecs":[{"dimensionIndex":0,"sortOrder":"ASCENDING"}]},
		// "properties":{
		// "gridProperties":{"columnCount":19,"frozenColumnCount":1,"frozenRowCount":1,"rowCount":38},
		// "index":2,"sheetId":1049211208,"sheetType":"GRID","title":"Networks"}}
		GridProperties gp = p.getGridProperties();
		int columnCount = gp.getColumnCount();
		int rowCount = Utils.getProp(bundle, tableName + ".lastRow", gp.getRowCount());
		// headers / field names assumed to be in last frozen row or first row
		int frozenRowCount = 0;
		try {
			frozenRowCount = gp.getFrozenRowCount();
		} catch (Exception e1) {
			// if no frozen rows gets a null pointer!
		}
		if (frozenRowCount < 1)
			frozenRowCount = 1;

		//
		List<Integer> wantedColNums = strToCols(Utils.getProp(bundle, tableName + ".columns"));
		List<Integer> userColNums = strToCols(Utils.getProp(bundle, tableName + ".user"));
		List<Integer> requiredColNums = strToCols(Utils.getProp(bundle, tableName + ".required"));

		Map<String, String> foreignColNums = Utils.getPropMap(bundle, tableName + ".foreign");

		// Get headers and basic cell data
		// fine max length to String fields
		Map<String, Integer> maxFieldLenghts = new HashMap<String, Integer>();
		Map<String, Integer> maxUserFieldLenghts = new HashMap<String, Integer>();
		// discovered field types
		Map<String, Class<?>> fieldTypes = new HashMap<String, Class<?>>();
		Map<String, Class<?>> userFieldTypes = new HashMap<String, Class<?>>();
		// required field names
		List<String> requiredFields = new ArrayList<String>();
		List<String> requiredUserFields = new ArrayList<String>();
		// holds the actual data
		Map<Integer, Map<String, Object>> rowsData = new HashMap<Integer, Map<String, Object>>();
		Map<Integer, Map<String, Object>> rowsUserData = new HashMap<Integer, Map<String, Object>>();
		// preserve order of fields
		String[] ha = new String[columnCount];

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
				log.error("No data found for tab " + tabName);
			} else {
				int rowId = 1;
				for (List<Object> row : values) {
					if (rowId == 306) {
						log.error("rowId == " + rowId);
					}
					if (rowId == frozenRowCount) {
						// init field lengths to 0
						int colNum = 0;
						for (Object h : row) {
							String header = (String) h;
							if (userColNums.contains(colNum)) {
								if (StringUtils.isBlank(Utils.tabToStr(renames, header))) {
									header = "Col" + columnNumberToLetter(colNum + 1);
								}
								maxUserFieldLenghts.put(header, 0);
								userFieldTypes.put(header, Utils.getPropCls(bundle,
										tabName + "." + columnNumberToLetter(colNum + 1) + ".type", null));
								// note just pulling the keySet scrambles the order
								ha[colNum] = header;
								if (requiredColNums.contains(colNum)) {
									requiredUserFields.add(header);
								}
							} else if (wantedColNums.isEmpty() || wantedColNums.contains(colNum)) {
								if (StringUtils.isBlank(Utils.tabToStr(renames, (String) header))) {
									header = "Col" + columnNumberToLetter(colNum + 1);
								}
								maxFieldLenghts.put(header, 0);
								fieldTypes.put(header, Utils.getPropCls(bundle,
										tabName + "." + columnNumberToLetter(colNum + 1) + ".type", null));
								// note just pulling the keySet scrambles the order
								ha[colNum] = header;
								if (requiredColNums.contains(colNum)) {
									requiredFields.add(header);
								}
							}
							colNum++;
						}
						log.debug("row:" + row.toString());
					} else if (rowId > frozenRowCount && rowId <= rowCount) {
						Map<String, Object> rowMap = new HashMap<String, Object>();
						rowsData.put(rowId, rowMap);
						Object[] cells = row.toArray();
						for (int i = 0; i < cells.length; i++) {
							Map<String, Integer> mfl = maxFieldLenghts;
							Map<String, Class<?>> ft = fieldTypes;
							if (userColNums.contains(i)) {
								mfl = maxUserFieldLenghts;
								ft = userFieldTypes;
								rowsUserData.put(rowId, rowMap);
							}
							if (wantedColNums.isEmpty() || wantedColNums.contains(i) || userColNums.contains(i)) {
								// deal with blank cells
								if (cells[i] != null && StringUtils.isBlank(cells[i].toString()))
									cells[i] = null;

								rowMap.put(ha[i], cells[i]);

								Class<?> fieldCls = ft.get(ha[i]);
								// set blank and cells with errors to null
								if (cells[i] != null) {
									if (cells[i] instanceof String) {
										String s = (String) cells[i];
										if (StringUtils.isBlank(s))
											cells[i] = null;
										else if (s.startsWith("#VALUE!"))
											cells[i] = null;
										else if (s.startsWith("#NUM!"))
											cells[i] = null;
										else if (s.startsWith("#REF!"))
											cells[i] = null;
										else if (s.startsWith("#N/A"))
											cells[i] = null;
										else if (s.equals("#N/A"))
											cells[i] = null;
									}
								}

								if (i == 0 && cells[i] != null) {
									log.debug("Before:" + i + ":" + cells[i] + ":" + cells[i].getClass() + ":"
											+ fieldCls);
								}
								Object val = getTypedVal(cells[i], fieldCls);
								if (i == 0 && cells[i] != null) {
									log.debug("After:" + i + ":" + val + ":" + cells[i].getClass() + ":" + fieldCls);
								}
								if (val != null) {
									ft.put(ha[i], val.getClass());
									if (val instanceof String) {
										int len = mfl.get(ha[i]);
										int vlen = ((String) val).length();
										if (len < vlen)
											mfl.put((String) ha[i], vlen);
									}
								}
								rowMap.put(ha[i], val);
							}
						}
						log.debug("rowMap:" + rowMap.toString());
					}
					rowId++;
				}
			}
		} catch (IOException e) {
			log.error("Failed to get field data for " + tabName, e);
			if (failOnAnyError) {
				throw e;
			}
		}

		// rescan for cells with links
		try {
			request = service.spreadsheets().values().get(spreadsheetId, tabName);
			request.setValueRenderOption("FORMULA");
			request.setDateTimeRenderOption("FORMATTED_STRING");
			ValueRange response = request.execute();

			values = response.getValues();
			if (values == null || values.isEmpty()) {
				log.error("No data found for tab " + tabName);
			} else {
				int rowId = 1;
				for (List<Object> row : values) {
					if (rowId > frozenRowCount && rowId <= rowCount) {
						Object[] cells = row.toArray();
						for (int i = 0; i < cells.length; i++) {
							Map<String, Integer> mfl = maxFieldLenghts;
							Map<String, Class<?>> ft = fieldTypes;
							Map<Integer, Map<String, Object>> rd = rowsData;

							if (userColNums.contains(i)) {
								mfl = maxUserFieldLenghts;
								ft = userFieldTypes;
								rd = rowsUserData;
							}
							Map<String, Object> rowMap = rd.get(rowId);
							if (wantedColNums.isEmpty() || wantedColNums.contains(i) || userColNums.contains(i)) {
								if (cells[i] instanceof String) {
									String s = (String) cells[i];
									if (!StringUtils.isBlank(s)) {
										int beginIndex = s.indexOf("HYPERLINK(");
										if (beginIndex > -1) {
											beginIndex += 11;
											String header = ha[i] + " link";
											int endIndex = s.indexOf('"', beginIndex);
											s = s.substring(beginIndex, endIndex);
											Integer len = mfl.get(header);
											if (len == null)
												len = 0;
											int vlen = s.length();
											if (len < vlen)
												mfl.put(header, vlen);

											ft.put(header, String.class);
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
			log.error("Failed to get link fields for " + tabName, e);
			if (failOnAnyError) {
				throw e;
			}
		}

		List<String> userTabs = Utils.getPropList(bundle, PROPKEY + ".userTabs");
		// normalize userTabs list
		List<String> userTables = new ArrayList<String>();
		for (String tab : userTabs) {
			userTables.add(Utils.tabToStr(renames, tab));
		}
		log.debug("Exporting tab:" + tabName + " to table:" + tableName);
		Map<String, String> foreignKeys = new HashMap<String, String>();
		for (String fnam : fieldTypes.keySet()) {
			if (foreignColNums.containsKey(fnam)) {
				foreignKeys.put(fnam, foreignColNums.get(fnam));
			}
		}
		if (userTables.contains(tableName)) {
			foreignKeys.put(USERID_COLUMN, ACCOUNT_TABLE + ".id");
			fieldTypes.put(USERID_COLUMN, db.getIdTypeCls());
		}

		genTable(tableName, null, maxFieldLenghts, fieldTypes, requiredFields, rowsData, foreignKeys);
		// If has user columns to be placed in separate table, create that user table.
		if (!userColNums.isEmpty()) {
			Map<String, String> userForeignKeys = new HashMap<String, String>();
			for (String fnam : userFieldTypes.keySet()) {
				if (foreignColNums.containsKey(fnam)) {
					userForeignKeys.put(fnam, foreignColNums.get(fnam));
				}
			}
			userForeignKeys.put(USERID_COLUMN, ACCOUNT_TABLE + ".id");
			userForeignKeys.put(tableName + "_Id", tableName + ".id");

			userFieldTypes.put(USERID_COLUMN, db.getIdTypeCls());
			userFieldTypes.put(tableName + "_Id", db.getIdTypeCls());
			log.debug("Exporting tab:" + tabName + " to table:" + tableName);
			genTable(tableName + "User", tableName, maxUserFieldLenghts, userFieldTypes, requiredUserFields,
					rowsUserData, userForeignKeys);
		}
	}

	/**
	 * Generate the table from the sheet data with foreign keys to the account and
	 * mainTable if needed
	 * 
	 * @param tableName
	 * @param mainTable
	 * @param maxFieldLenghts
	 * @param fieldTypes
	 * @param requiredFields
	 * @param rowsData
	 * @param foreignKeys     keys to add
	 * @throws SQLException
	 */
	private void genTable(String tableName, String mainTable, Map<String, Integer> maxFieldLenghts,
			Map<String, Class<?>> fieldTypes, List<String> requiredFields, Map<Integer, Map<String, Object>> rowsData,
			Map<String, String> foreignKeys) throws SQLException {
		log.debug("maxFieldLenghts:" + maxFieldLenghts.toString());
		log.debug("fieldTypes:" + fieldTypes.toString());
		log.debug("requiredFields:" + requiredFields.toString());
		log.debug("rowsData:" + rowsData.toString());
		String mainTableId = "_";
		if (mainTable != null)
			mainTableId = mainTable + "_Id";

		String colCreated = Utils.tabToStr(renames, (String) Utils.getProp(bundle, "col.created", null));
		String colLastMod = Utils.tabToStr(renames, (String) Utils.getProp(bundle, "col.lastMod", null));
		String className = Utils.tabToStr(renames, tableName);
		List<String> uniqueCols = Utils.getPropList(bundle, className + ".unique");

		String schema = db.getPrefix();

		// Drop of old table if there is one though should have been in getSheet()
		// or addAccountTable()

		String idType = db.getIdType();
		String autoincrement = " NOT NULL primary key auto_increment";
		if (isSQLite()) {
			autoincrement = " NOT NULL primary key autoincrement";
		}
		if (db.isSqlserver()) {
			autoincrement = " IDENTITY(1, 1) PRIMARY KEY";
		}

		// create new table
		StringBuilder sb = new StringBuilder("CREATE TABLE " + schema + tableName + "(id " + idType + autoincrement);
//		if (addUserFK)
//			sb.append(", userId " + idType);
//
//		if (mainTable != null)
//			sb.append(", " + mainTable + "_Id " + idType);

		if (colCreated != null) {
			if (db.isMySQL()) {
				sb.append(", " + colCreated + " TIMESTAMP NOT NULL");
			} else {
				sb.append(", " + colCreated + " DATETIME NOT NULL");
			}
		}
		if (colLastMod != null) {
			if (db.isMySQL()) {
				sb.append(", " + colLastMod + " TIMESTAMP NOT NULL");
			} else {
				sb.append(", " + colLastMod + " DATETIME NOT NULL");
			}
		}

		for (Object name : fieldTypes.keySet()) {
			if (name.equals(colCreated) || name.equals(colLastMod))
				continue;

			Class<?> cls = fieldTypes.get(name);
			if (cls == null) {
				cls = String.class;
			}
			String fieldName = Utils.tabToStr(renames, (String) name);
			if (db.isMySQL())
				sb.append(",\n`").append(fieldName).append("`\t");
			else
				sb.append(",\n").append(fieldName).append("\t");
			if (cls.isAssignableFrom(String.class)) {
				int len = maxFieldLenghts.get(name);
				if (len == 0)
					len = 20;
				sb.append("VARCHAR(" + len + ")");
				if (db.isMySQL()) {
					// work around bug in MySQL not returning correct len
					sb.append(" COMMENT 'len=" + len + "'");
				}
			} else if (cls.isAssignableFrom(Boolean.class)) {
				// NOTE SQLite does not have boolean type
				if (isSQLite())
					sb.append("INTEGER");
				else
					sb.append("BOOLEAN");
			} else if (cls.isAssignableFrom(Long.class)) {
				sb.append("BIGINT");
			} else if (cls.isAssignableFrom(Integer.class)) {
				sb.append("INTEGER");
			} else if (cls.isAssignableFrom(Date.class)) {
//				// Note SQLite does not have Date type
//				// https://stackoverflow.com/questions/17227110/how-do-datetime-values-work-in-sqlite
//				if (isSQLite())
//					sb.append("TEXT");
////				sb.append("INTEGER");
////				sb.append("REAL");
//				else
				// technically not supported by SQLite but works as Integer with hint.
				sb.append("DATETIME");
			} else if (cls.isAssignableFrom(Time.class)) {
				if (isSQLite())
					sb.append("INTEGER");
				else
					sb.append("TIME");
			} else if (cls.isAssignableFrom(BigDecimal.class) || cls.isAssignableFrom(Float.class)
					|| cls.isAssignableFrom(Double.class)) {
				sb.append("REAL");
			} else {
				log.warn("Unknown field type:" + cls.getCanonicalName() + " for " + fieldName);
			}
			if (requiredFields.contains(name)) {
				sb.append(" NOT NULL");
			}
		}

		if (foreignKeys != null) {
			for (String field : foreignKeys.keySet()) {
				sb.append(",");
				String fieldName = Utils.tabToStr(renames, field);
				sb.append("    CONSTRAINT FK_" + tableName + "_" + fieldName + " FOREIGN KEY (" + fieldName + ")");
				String fkey = foreignKeys.get(field);
				String[] split = fkey.split("\\s*\\.\\s*");
				if (split.length == 2)
					sb.append("    REFERENCES " + schema + split[0] + "(" + split[1] + ")");
				else
					throw new PatternSyntaxException(fkey + " is invalid syntax", "\\s*\\.\\s*", 0);
			}
		}
		for (String fieldName : uniqueCols) {
			sb.append(", CONSTRAINT UC_" + fieldName + " UNIQUE (" + fieldName + ")");
		}
		sb.append(");");

		runSQL(sb.toString(), tableName + ".sql");

		try {
			Utils.deletePath(Utils.getPath(baseDir, SCRIPTS_FOLDER, tableName + ".data.sql"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		genInsert:
		// import data gathered into DB
		for (Integer rowId : rowsData.keySet()) {
			Map<String, Object> row = rowsData.get(rowId);
			sb = new StringBuilder("INSERT INTO " + schema + tableName + " (");
			boolean addcom = false;
			if (colCreated != null) {
				sb.append(colCreated + ",");
			}
			if (colLastMod != null) {
				sb.append(colLastMod + ",");
			}
			for (Object name : fieldTypes.keySet()) {
				if (name.equals(colCreated) || name.equals(colLastMod))
					continue;

				if (addcom) {
					sb.append(", ");
				} else {
					addcom = true;
				}
				String fieldName = Utils.tabToStr(renames, (String) name);
				if (db.isMySQL())
					sb.append('`').append(fieldName).append('`');
				else
					sb.append(fieldName);

			}
			sb.append(") VALUES (");
			addcom = false;
			if (colCreated != null) {
				if (isSQLite()) {
					sb.append(System.currentTimeMillis()).append(",");
				} else if (db.isMySQL()) {
					sb.append("NOW(),");
				} else if (db.isSqlserver()) {
					sb.append("SYSDATETIME(),");
				} else {
					sb.append("NOW(),");
				}
			}
			if (colLastMod != null) {
				if (isSQLite()) {
					sb.append(System.currentTimeMillis()).append(",");
				} else if (db.isMySQL()) {
					sb.append("NOW(),");
				} else if (db.isSqlserver()) {
					sb.append("SYSDATETIME(),");
				} else {
					sb.append("NOW(),");
				}
			}
			for (Object name : fieldTypes.keySet()) {
				if (name.equals(colCreated) || name.equals(colLastMod))
					continue;

				if (addcom) {
					sb.append(", ");
				} else {
					addcom = true;
				}
				if (name.equals(USERID_COLUMN)) {
					sb.append("1");
					continue;
				}
				if (name.equals(mainTableId)) {
					sb.append((rowId - 1));
					continue;
				}
				Object val = row.get(name);
				// Validate val is of expected type
				Class<?> fieldCls = fieldTypes.get(name);
				if (val != null && !fieldCls.isInstance(val)) {
					// deal with numbers to be stored as Strings
					if (fieldCls.isAssignableFrom(String.class)) {
						val = val.toString();
					} else if (val instanceof BigDecimal) {
						if (fieldCls.isAssignableFrom(Integer.class)) {
							((BigDecimal) val).intValue();
							log.warn("rowId:" + rowId + " converting (" + val + ") for " + name + ": to Integer");
						} else if (fieldCls.isAssignableFrom(Long.class)) {
							((BigDecimal) val).longValue();
							log.warn("rowId:" + rowId + " converting (" + val + ") for " + name + ": to Long");
						} else {
							log.warn("rowId:" + rowId + " has bad value (" + val + ") is the wrong class:"
									+ val.getClass().getCanonicalName() + " for " + name + ":"
									+ fieldCls.getCanonicalName() + " setting to null");
							val = null;
						}
					} else if (!fieldCls.isAssignableFrom(val.getClass())) {
						// Deal with other random things like - denoting an empty date field that can go
						// into the DB but then case and Exception when Hibernate reads it.
						log.warn("rowId:" + rowId + " has bad value (" + val + ") is the wrong class:"
								+ val.getClass().getCanonicalName() + " for " + name + ":" + fieldCls.getCanonicalName()
								+ " setting to null");
						val = null;
					}
				}
				// skip row
				if (val == null && requiredFields.contains(name)) {
					log.warn("Skipping due to missing required data:" + row);
					skipped++;
					continue genInsert;
				}
				if (val instanceof String) {
					if (StringUtils.isBlank((String) val) && requiredFields.contains(name)) {
						log.warn("Skipping due to missing required data:" + row);
						skipped++;
						continue genInsert;
					}
					sb.append("'").append(((String) val).replace("'", "'\'")).append("'");
				} else if (val instanceof Boolean) {
					if (isSQLite()) {
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
					if (isSQLite()) {
						sb.append(((Date) val).getTime());
					} else {
						sb.append("'").append(DB_DATETIME.format(((Date) val).getTime())).append("'");
					}
				} else if (val instanceof Time) {
					if (isSQLite()) {
						sb.append(((Time) val).getTime());
					} else {
						sb.append("'").append(TIME_ONLY24S.format(((Time) val).getTime())).append("'");
					}
				} else {
					sb.append(val);
				}
			}
			sb.append(");");
			try {
				runSQL(sb.toString(), tableName + ".data.sql");
				passed++;
			} catch (SQLException e) {
				if (failOnAnyError) {
					throw e;
				}
				failed++;
			}
		}

	}

	/**
	 * Since we are recreating the table each time, do each call without
	 * transactions so we can see any trouble rows that might exist in one go. TODO:
	 * add check if table exists or should be replaced instead of just doing it
	 * every time.
	 * 
	 * @param sql
	 * @return pass / fail
	 * @throws IOException
	 * @throws SQLException
	 */
	private void addAccountTable() throws IOException, SQLException {
		Map<String, Integer> maxFieldLenghts = new HashMap<String, Integer>();
		// maxFieldLenghts:{Decimal=0, Text=7, Int=0, Date=0}
		maxFieldLenghts.put("email", 254);
		maxFieldLenghts.put("password", 254);
		maxFieldLenghts.put("role", 25);
		// discovered field types
		Map<String, Class<?>> fieldTypes = new HashMap<String, Class<?>>();
		// fieldTypes:{Decimal=class java.math.BigDecimal, Text=class java.lang.String,
		// Int=class java.lang.Integer, Date=class java.util.Date}
		fieldTypes.put("email", String.class);
		fieldTypes.put("password", String.class);
		fieldTypes.put("role", String.class);
		// required field names
		List<String> requiredFields = new ArrayList<String>();
		// requiredFields:[Int]
		requiredFields.add("email");
		requiredFields.add("password");
		requiredFields.add("role");
		// holds the actual data
		Map<Integer, Map<String, Object>> rowsData = new HashMap<Integer, Map<String, Object>>();
		Map<String, Object> rowMap = new HashMap<String, Object>();

		rowMap.put("email", TEST_USER);
		rowMap.put("password", TEST_PASS);
		rowMap.put("role", TEST_ROLE);
		rowsData.put(1, rowMap);

		rowMap = new HashMap<String, Object>();
		rowMap.put("email", ADMIN_USER);
		rowMap.put("password", ADMIN_PASS);
		rowMap.put("role", ADMIN_ROLE);
		rowsData.put(2, rowMap);

		log.debug("Creating account table");

		runSQL("DROP TABLE IF EXISTS " + db.getPrefix() + ACCOUNT_TABLE + ";", ACCOUNT_TABLE + ".drop.sql");
		genTable(ACCOUNT_TABLE, "", maxFieldLenghts, fieldTypes, requiredFields, rowsData, null);

	}

	/**
	 * Since we are recreating the table each time, do each call without
	 * transactions so we can see any trouble rows that might exist in one go.
	 * 
	 * @param sql
	 * @param saveFile TODO
	 * @return pass / fail
	 */
	private boolean runSQL(String sql, String saveFile) throws SQLException {
		log.debug("Running:" + sql);
		boolean rtn = false;

		try {
			if (!StringUtils.isBlank(saveFile)) {
				Path p = Utils.getPath(baseDir, SCRIPTS_FOLDER, saveFile);
				if (!p.toFile().exists()) {
					try {
						Files.createDirectories(p.getParent());
					} catch (IOException e) {
						log.warn(e.getMessage());
					}
				}
				try (PrintStream ps = new PrintStream(new FileOutputStream(p.toFile(), true))) {
					ps.println(sql);
					log.warn("Wrote:" + p.toString());
				} catch (Exception e) {
					log.error("failed to create " + p, e);
					p.toFile().delete();
				}

			}
			Connection conn = db.getConnection(getClass().getSimpleName() + ".runSQL()");
			Statement stmt = conn.createStatement();
			rtn = stmt.execute(sql);
			if (sql.startsWith("INSERT")) {
				int cnt = stmt.getUpdateCount();
				rtn = (cnt == 1);
				log.trace("SQL return was" + cnt);
			}
		} catch (SQLException e) {
			log.warn(e.getMessage());
			throw e;
		} finally {
			db.close(getClass().getSimpleName() + ".runSQL()");
		}
		return rtn;
	}

	public void getSheet() throws Exception {
		String outdir = Utils.getProp(bundle, GenSpring.PROPKEY + ".outdir", ".");
		Utils.deletePath(Utils.getPath(outdir, SCRIPTS_FOLDER));

		List<String> tabs = null;
		try {
			String schema = db.getPrefix();

			// Build a new authorized API client service.
			final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			// https://docs.google.com/spreadsheets/d/1-xYv1AVkUC5J3Tqpy2_3alZ5ZpBPnnO2vUGUCUeLVVE/edit?usp=sharing
			final String spreadsheetId = Utils.getProp(bundle, PROPKEY + ".id");
			tabs = Utils.getPropList(bundle, PROPKEY + ".tabs");

			Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME).build();
			Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
			List<Sheet> sheets = spreadsheet.getSheets();

			// clear DB in reverse order to deal with constraints
			for (Sheet sheet : sheets) {
				SheetProperties p = sheet.getProperties();
				String tableName = Utils.tabToStr(renames, p.getTitle());
				if (tabs.contains(tableName) || tabs.contains(p.getTitle())) {
					List<Integer> userColNums = strToCols(Utils.getProp(bundle, tableName + ".user"));
					if (!userColNums.isEmpty()) {
						runSQL("DROP TABLE IF EXISTS " + schema + tableName + "User;", tableName + "User.drop.sql");
					}
					runSQL("DROP TABLE IF EXISTS " + schema + tableName + ";", tableName + ".drop.sql");
				}
			}

			addAccountTable();

			for (Sheet sheet : sheets) {
				SheetProperties p = sheet.getProperties();
				String tableName = Utils.tabToStr(renames, p.getTitle());
				if (tabs.contains(tableName) || tabs.contains(p.getTitle())) {
					exportTab(service, spreadsheetId, sheet);
				}
			}
		} catch (Exception e) {
			log.error("Failed to get to export sheet ", e);
			if (failOnAnyError) {
				throw e;
			}
		}

		System.out.println("Inserted " + passed + " records into " + tabs);
		System.out.println("Failed to insert " + failed + " records.");
		System.out.println("Skipped inserting " + skipped + " records.");
	}

	/**
	 * Prints the names and majors of students in a sample spreadsheet:
	 * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
	 */
	public static void main(String... args) {
		try {
			Sheets2DB s = new Sheets2DB();
			s.getSheet();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

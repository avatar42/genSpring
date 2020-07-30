package com.dea42.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Title: DB <br>
 * Description: Class of DB util methods <br>
 * Copyright: Copyright (c) 2001-2020 <br>
 * Company: RMRR <br>
 * <br>
 * 
 * @author David Abigt <br>
 * @version 1.0
 */
public class Db {
	private static final Logger LOGGER = LoggerFactory.getLogger(Db.class.getName());

	/**
	 * Get connection to display / log utils
	 */
	private static int s_openCount = 0;
	private static int s_totalCount = 0;

	/**
	 * SimpleDateFormat for writing date to DB
	 */
	private static final SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/**
	 * hold name of DB connected to
	 */
	private String s_dbName = null;
	/**
	 * hold url of DB connected to
	 */
	private String s_dbUrl = null;
	/**
	 * hold driver name of DB connected to
	 */
	private String s_dbDriver = null;

	/**
	 * hold driver name of version 3 DB connected to
	 */
	private String s_dbDriver2 = null;
	/**
	 * hold user of DB connected to
	 */
	private String s_dbUser = null;
	/**
	 * hold password of DB connected to
	 */
	private String s_dbPass = null;
	/**
	 * hold pool name of DB connected to
	 */
	private String s_dbPool = null;
	/**
	 * hold DataSource of pool connected to DB
	 */
	private DataSource ds = null;

	private boolean s_loaded = false;
	/**
	 * holds Connection in use
	 */
	protected Connection connection;

	private String config = "db";

	public Db(String calledBy, String config) {
		this(calledBy, config, null);
	}

	public Db(String calledBy, String config, String folder) {
		this.config = config;
		init(config, folder);
		if (calledBy != null) {
			getConnection(calledBy);
		} else {
			getConnection("");
		}
	}

	public void reset() {
		s_loaded = false;
	}

	public boolean isSQLite() {
		return s_dbDriver.contains("sqlite");
	}

	public boolean isMySQL() {
		return s_dbDriver.contains("mysql");
	}

	public boolean isSqlserver() {
		return s_dbDriver.contains("sqlserver");
	}

	public boolean isDb2() {
		return s_dbDriver.contains("db2");
	}

	public String getUrl(ResourceBundle bundle, String folder) {
		s_dbUrl = Utils.getProp(bundle, "db.url", null);
		s_dbDriver = Utils.getProp(bundle, "db.driver", "org.gjt.mm.mysql.Driver");
		if (StringUtils.isAllBlank(s_dbUrl) && s_dbDriver.contains("sqlite")) {
			// db.url=jdbc:sqlite:L:/sites/git/Watchlist/watchlistDB.sqlite
			Path outPath = Utils.getPath(folder);
			if (!outPath.toFile().isDirectory())
				outPath.toFile().mkdirs();

			s_dbUrl = "jdbc:sqlite:" + outPath.normalize().toString().replace('\\', '/') + "/" + config + "DB.sqlite";
		}

		return s_dbUrl;
	}

	/**
	 * Load the properties file with the passed name and use the parms in it to init
	 * the DB driver
	 * 
	 * @param config
	 * @param folder TODO
	 */
	public void init(String config, String folder) {
		if (!s_loaded) {
			ResourceBundle bundle = ResourceBundle.getBundle(config);
			s_dbDriver2 = Utils.getProp(bundle, "db.driver2", "com.mysql.jdbc.Driver");
			s_dbPool = Utils.getProp(bundle, "db.pool", null);
			LOGGER.info("db.pool =" + s_dbPool);

			if (s_dbPool != null) {
				try {
					Context env = (Context) new InitialContext().lookup("java:comp/env");
					ds = (DataSource) env.lookup(s_dbPool);
				} catch (Exception e) {
					ds = null;
				}
			}

			if (ds == null) {
				StringBuffer sb = new StringBuffer(16);
				sb.append(s_dbPool).append(" is an unknown DataSource ");
				sb.append("db.pool= ").append(s_dbPool).append('\n');
				LOGGER.warn(sb.toString());

				for (String key : bundle.keySet()) {
					LOGGER.info(key + "=" + bundle.getString(key));
				}
				s_dbUser = Utils.getProp(bundle, "db.user", null);
				s_dbPass = Utils.getProp(bundle, "db.password", null);
				s_dbName = Utils.getProp(bundle, "db.name", null);
				s_dbUrl = getUrl(bundle, folder);
				s_loaded = true;

			} else {
				s_loaded = true;
			}
		}

	}

	public static int getOpenCount() {
		return s_openCount;
	}

	/**
	 * returns the connection and creates a new one if needed.
	 * 
	 * @param calledBy
	 * @return Connection
	 */
	public Connection getConnection(String calledBy) {
		try {
			if (connection == null || connection.isClosed()) {
				if (connection != null) {
					s_openCount--;
					LOGGER.error("Connection timed out=" + s_openCount);
				}
				if (ds == null) {
					connection = doConnect();
					LOGGER.info(config + ".getConnection(" + calledBy + ')' + s_dbUrl + "=" + s_openCount);
				} else {
					try {
						connection = ds.getConnection();
					} catch (SQLException e) {
						// if connection is stale might get error so grab again.
						connection = ds.getConnection();
					}
					LOGGER.info(config + ".getConnection(" + calledBy + ")" + s_dbPool + "=" + s_openCount);
				}
				s_openCount++;
				s_totalCount++;
			}
		} catch (ClassNotFoundException e) {
			LOGGER.error("Exception caught", e);

		} catch (SQLException e) {
			LOGGER.error("getConnection() error: ", e);
			LOGGER.error("Open connections=" + s_openCount);
			LOGGER.error("Total connections=" + s_totalCount);

		}
		return connection;
	}

	public String getDbUrl() {
		return s_dbUrl;
	}

	public String getDbName() {
		return s_dbName;
	}

	/**
	 * Get a direct connection to the DB.
	 * 
	 */
	private Connection doConnect() throws SQLException, ClassNotFoundException {
		Properties props = new Properties();

		// load the driver
		try {
			Class.forName(s_dbDriver);
			props.put("db.driver", s_dbDriver);
			LOGGER.info("Loaded driver =" + s_dbDriver);
		} catch (ClassNotFoundException e) {
			LOGGER.error("Could not load prefered driver " + s_dbDriver, e);
			Class.forName(s_dbDriver2);
			props.put("db.driver", s_dbDriver2);
			LOGGER.info("Loaded driver =" + s_dbDriver2);
		}

		if (s_dbUser != null)
			props.put("user", s_dbUser);
		if (s_dbPass != null)
			props.put("password", s_dbPass);
		props.put("db.url", s_dbUrl);
		props.put("zeroDateTimeBehavior", "convertToNull");

		LOGGER.info(config + ".Connecting to:" + s_dbUrl);
		LOGGER.trace("props=" + props);

		try {
			return DriverManager.getConnection(s_dbUrl, props);
		} catch (SQLException e) {

			LOGGER.error("Connecting to:" + s_dbUrl);
			throw e;
		}
	}

	/**
	 * Creates sub directories as needed to complete path.
	 * 
	 * @param s String representing the directory path that needs to be created.
	 * @return success / failure
	 * @throws IOException if unable to create directory
	 */
	public boolean mkdirs(String s) throws IOException {
		boolean rtn = true;

		LOGGER.trace("Making " + s);
		// mkdirs does not like / as a separator
		String fulldir = s.replace('/', '\\');
		LOGGER.trace("Making " + fulldir);
		File fileObj = new File(fulldir);
		if (fileObj.exists()) {
			if (!fileObj.isDirectory()) {
				rtn = fileObj.delete();
			}
		} else {
			rtn = fileObj.mkdirs();
		}
		if (!fileObj.exists()) {
			// if the directory does not exist and we could not make it we
			// have a problem - stop everything
			throw new IOException("Unable to make directory " + s);
		}
		return rtn;
	}

	/**
	 * Format String parm for SQL statement
	 * 
	 * @param parm
	 * @return String
	 */
	public static String sqlStr(String parm) {
		if (parm != null) {
			return '\'' + parm + '\'';
		} else {
			return "NULL";
		}
	}

	/**
	 * Format Timestamp parm for SQL statement
	 * 
	 * @param parm
	 * @return String
	 */
	public static String sqlStr(Timestamp parm) {
		if (parm != null) {
			return '\'' + formater.format(parm) + '\'';
		} else {
			return "NULL";
		}
	}

	public static String sqlStr(Date parm) {
		if (parm != null) {
			return '\'' + formater.format(parm) + '\'';
		} else {
			return "NULL";
		}
	}

	/**
	 * Gets next primary key for insert. For a true production env this should call
	 * a stored procedure
	 * 
	 * @param tableName
	 * @return long
	 */
	public long nextPk(String tableName) {
		LOGGER.debug("nextPk(String tableName)");
		LOGGER.debug("tableName=" + tableName);

		ResultSet results;
		long rtn = 0;
		Connection con;
		try {
			con = doConnect();
			Statement sStatement = con.createStatement();
			String sql = "SELECT key_name,key_val from pkeys WHERE key_name=\'" + tableName + "\';";

			LOGGER.info("sql=" + sql);
			results = sStatement.executeQuery(sql);
			if (results.next()) {
				rtn = results.getLong("key_val");
				results.updateLong("key_val", rtn + 1);
				results.updateRow();
			} else {
				sStatement.close();
				sql = "INSERT into pkeys VALUES(\'" + tableName + "\',2);";
				sStatement = con.createStatement();
				sStatement.execute(sql);
			}
			con.close();
		} catch (ClassNotFoundException e) {
			LOGGER.error("load(Connection cConnection) error: ", e);
		} catch (SQLException e) {
			LOGGER.error("load(Connection cConnection) error: ", e);
		} finally {
			close(getClass().getName());
		}

		LOGGER.debug("Exiting load(Connection cConnection)" + toString());
		return rtn;
	}

	public void close(String calledBy) {
		try {
			if (connection != null) {
				connection.close();
				s_openCount--;
				if (!connection.isClosed()) {
					LOGGER.error("Connection did not close.");
				}
			}
			connection = null;
		} catch (SQLException ex) {
			LOGGER.error("Unable to close connection to DB");
		}
		LOGGER.info("close(" + calledBy + ')' + s_openCount);
	}

	/**
	 * just in case I miss a close()
	 * 
	 * @throws Throwable
	 */
	protected void finalize() throws Throwable {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close(); // close open connection
			}
		} finally {
			super.finalize();
		}
	}

}

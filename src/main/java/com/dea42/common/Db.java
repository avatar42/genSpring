package com.dea42.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
	private static int openCount = 0;
	private static int totalCount = 0;

	/**
	 * SimpleDateFormat for writing date to DB
	 */
	private static final SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/**
	 * hold name of DB connected to
	 */
	private String dbName = null;
	/**
	 * holds schema if one
	 */
	private String schema = null;

	/**
	 * hold url of DB connected to
	 */
	private String dbUrl = null;
	/**
	 * hold driver name of DB connected to
	 */
	private String dbDriver = null;

	/**
	 * hold driver name of version 3 DB connected to
	 */
	private String dbDriver2 = null;
	/**
	 * hold user of DB connected to
	 */
	private String dbUser = null;
	/**
	 * hold password of DB connected to
	 */
	private String dbPass = null;
	/**
	 * hold pool name of DB connected to
	 */
	private String dbPool = null;
	/**
	 * hold DataSource of pool connected to DB
	 */
	private DataSource ds = null;

	private boolean loaded = false;
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
		loaded = false;
	}

	public boolean isSQLite() {
		return dbDriver.contains("sqlite");
	}

	public String getIdType() {
		if (isSQLite()) {
			return "INTEGER";
		} else {
			return "BIGINT";
		}
	}

	public Class<?> getIdTypeCls() {
		if (isSQLite()) {
			return Integer.class;
		} else {
			return Long.class;
		}
	}

	public boolean isMySQL() {
		return dbDriver.contains("mysql");
	}

	public boolean isSqlserver() {
		return dbDriver.contains("sqlserver");
	}

	public boolean isDb2() {
		return dbDriver.contains("db2");
	}

	public boolean isSupported() {
		return isSQLite() || isMySQL() || isSqlserver();// || isDb2();
	}

	public String getUrl(ResourceBundle bundle, String folder) {
		dbUrl = Utils.getProp(bundle, "db.url", null);
		dbDriver = Utils.getProp(bundle, "db.driver", "org.gjt.mm.mysql.Driver");
		if (StringUtils.isAllBlank(dbUrl) && isSQLite()) {
			// db.url=jdbc:sqlite:L:/sites/git/Watchlist/watchlistDB.sqlite
			Path outPath = Utils.getPath(folder);
			if (!outPath.toFile().isDirectory())
				outPath.toFile().mkdirs();

			dbUrl = "jdbc:sqlite:" + outPath.normalize().toString().replace('\\', '/') + "/" + config + "DB.sqlite";
		}

		return dbUrl;
	}

	/**
	 * Load the properties file with the passed name and use the parms in it to init
	 * the DB driver
	 * 
	 * @param config
	 * @param folder TODO
	 */
	public void init(String config, String folder) {
		if (!loaded) {
			ResourceBundle bundle = ResourceBundle.getBundle(config);
			dbDriver2 = Utils.getProp(bundle, "db.driver2", "com.mysql.jdbc.Driver");
			dbPool = Utils.getProp(bundle, "db.pool", null);
			LOGGER.info("db.pool =" + dbPool);

			if (dbPool != null) {
				try {
					Context env = (Context) new InitialContext().lookup("java:comp/env");
					ds = (DataSource) env.lookup(dbPool);
				} catch (Exception e) {
					ds = null;
				}
			}

			if (ds == null) {
				StringBuffer sb = new StringBuffer(16);
				sb.append(dbPool).append(" is an unknown DataSource ");
				sb.append("db.pool= ").append(dbPool).append('\n');
				LOGGER.warn(sb.toString());

				for (String key : bundle.keySet()) {
					LOGGER.info(key + "=" + bundle.getString(key));
				}
				dbUser = Utils.getProp(bundle, "db.user", null);
				dbPass = Utils.getProp(bundle, "db.password", null);
				dbName = Utils.getProp(bundle, "db.name", null);
				schema = Utils.getProp(bundle, "db.schema", null);
				dbUrl = getUrl(bundle, folder);
				loaded = true;

			} else {
				loaded = true;
			}
		}

	}

	public static int getOpenCount() {
		return openCount;
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
					openCount--;
					LOGGER.error("Connection timed out=" + openCount);
				}
				if (ds == null) {
					connection = doConnect();
					LOGGER.info(config + ".getConnection(" + calledBy + ')' + dbUrl + "=" + openCount);
				} else {
					try {
						connection = ds.getConnection();
					} catch (SQLException e) {
						// if connection is stale might get error so grab again.
						connection = ds.getConnection();
					}
					LOGGER.info(config + ".getConnection(" + calledBy + ")" + dbPool + "=" + openCount);
				}
				openCount++;
				totalCount++;
			}
		} catch (ClassNotFoundException e) {
			LOGGER.error("Exception caught", e);

		} catch (SQLException e) {
			LOGGER.error("getConnection() error: ", e);
			LOGGER.error("Open connections=" + openCount);
			LOGGER.error("Total connections=" + totalCount);

		}
		return connection;
	}

	public String getDbUrl() {
		return dbUrl;
	}

	public String getDbName() {
		return dbName;
	}

	/**
	 * Return schema prefix
	 * 
	 * @return if dbName not blank returns dbName + "." otherwise ""
	 */
	public String getPrefix() {
		if (StringUtils.isBlank(dbName))
			return "";
		else {
			if (StringUtils.isBlank(schema))
				return dbName + ".";
			else {
				return dbName + "." + schema + ".";
			}
		}
	}

	/**
	 * Get a direct connection to the DB.
	 * 
	 */
	private Connection doConnect() throws SQLException, ClassNotFoundException {
		Properties props = new Properties();

		// load the driver
		try {
			Class.forName(dbDriver);
			props.put("db.driver", dbDriver);
			LOGGER.info("Loaded driver =" + dbDriver);
		} catch (ClassNotFoundException e) {
			LOGGER.error("Could not load prefered driver " + dbDriver, e);
			Class.forName(dbDriver2);
			props.put("db.driver", dbDriver2);
			LOGGER.info("Loaded driver =" + dbDriver2);
		}

		if (dbUser != null)
			props.put("user", dbUser);
		if (dbPass != null)
			props.put("password", dbPass);
		props.put("db.url", dbUrl);
		props.put("zeroDateTimeBehavior", "convertToNull");

		LOGGER.info(config + ".Connecting to:" + dbUrl);
		LOGGER.trace("props=" + props);

		try {
			return DriverManager.getConnection(dbUrl, props);
		} catch (SQLException e) {

			LOGGER.error("Connecting to:" + dbUrl);
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
				openCount--;
				if (!connection.isClosed()) {
					LOGGER.error("Connection did not close.");
				}
			}
			connection = null;
		} catch (SQLException ex) {
			LOGGER.error("Unable to close connection to DB");
		}
		LOGGER.info("close(" + calledBy + ')' + openCount);
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

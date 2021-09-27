package stockfetcher.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.mysql.cj.jdbc.MysqlDataSource;

public class StockDatabase {

	// Database connection info
	private static final String DB_USERNAME = "stockfetcher";
	private static final String DB_PASSWORD = "stockfetcher";
	private static final String DB_SERVER = "localhost";
	private static final int DB_PORT = 3306;
	private static final String DB_NAME = "stock_data";
	
	private static Connection conn = null;
	
	public static void initialize() throws SQLException {
		// Establish the database connection.
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setUser(DB_USERNAME);
		dataSource.setPassword(DB_PASSWORD);
		dataSource.setServerName(DB_SERVER);
		dataSource.setDatabaseName(DB_NAME);
		dataSource.setPort(DB_PORT);
		dataSource.setCreateDatabaseIfNotExist(true);
		conn = dataSource.getConnection();
		
		// Create database tables if they are not present
		Statement stmt = conn.createStatement();
		stmt.execute(
			"CREATE TABLE IF NOT EXISTS symbols("
			+ "		symbol_id INT NOT NULL AUTO_INCREMENT,"
			+ "		symbol VARCHAR(10) NOT NULL UNIQUE,"
			+ "		PRIMARY KEY (symbol_id)"
			+ ")"
		);
		
		stmt.execute(
			"CREATE TABLE IF NOT EXISTS prices("
			+ "		price_id INT NOT NULL AUTO_INCREMENT,"
			+ "		symbol_id INT NOT NULL,"
			+ "		date DATE NOT NULL,"
			+ "		open DECIMAL(13, 4) NOT NULL,"
			+ "		high DECIMAL(13, 4) NOT NULL,"
			+ "		low DECIMAL(13, 4) NOT NULL,"
			+ "		close DECIMAL(13, 4) NOT NULL,"
			+ "		adjusted_close DECIMAL(13, 4) NOT NULL,"
			+ "		volume INT NOT NULL,"
			+ "		dividend_amount DECIMAL(8, 4),"
			+ "		split_coefficient DECIMAL(8, 4),"
			+ "		PRIMARY KEY (price_id),"
			+ "		FOREIGN KEY (symbol_id) REFERENCES symbols(symbol_id),"
			+ "		CONSTRAINT unique_price UNIQUE (symbol_id, date)"
			+ ")"
		);
	}
	
	public static Connection getConnection() {
		return conn;
	}
	
}

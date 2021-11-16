package stockfetcher.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.cj.jdbc.MysqlDataSource;

import javafx.util.Pair;
import stockfetcher.api.CompanyData;
import stockfetcher.api.EtfData;
import stockfetcher.api.PriceData;

public final class StockDatabase {

	private static final Logger logger = LoggerFactory.getLogger(StockDatabase.class);
	
	// Database connection info
	private static final String DB_USERNAME = "stockfetcher";
	private static final String DB_PASSWORD = "stockfetcher";
	private static final String DB_SERVER = "localhost";
	private static final int DB_PORT = 3306;
	private static final String DB_NAME = "stock_data";
	
	private static Connection conn = null;
	
	private static final HashMap<String, Integer> symbolIdMap = new HashMap<>();
	private static final HashMap<String, Integer> companyIdMap = new HashMap<>();
	private static final HashMap<String, Integer> etfIdMap = new HashMap<>();
	
	private StockDatabase() {}
	
	public static void initialize() throws SQLException {
		logger.info("Connecting to stock data database...");
		
		// Establish the database connection.
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setUser(DB_USERNAME);
		dataSource.setPassword(DB_PASSWORD);
		dataSource.setServerName(DB_SERVER);
		dataSource.setDatabaseName(DB_NAME);
		dataSource.setPort(DB_PORT);
		dataSource.setCreateDatabaseIfNotExist(true);
		conn = dataSource.getConnection();
		
		logger.info("Connection successful!");
		
		// Create database tables if they are not present		
		Statement stmt = conn.createStatement();
		
		stmt.execute(
			"CREATE TABLE IF NOT EXISTS symbols("
			+ "		symbol_id INT NOT NULL AUTO_INCREMENT,"
			+ "		symbol VARCHAR(50) NOT NULL UNIQUE,"
			+ "		PRIMARY KEY (symbol_id)"
			+ ")"
		);
	
		stmt.execute(
			"CREATE TABLE IF NOT EXISTS companies("
			+ "		company_id INT NOT NULL AUTO_INCREMENT,"
			+ "		symbol_id INT NOT NULL UNIQUE,"
			+ "		name VARCHAR(100) NOT NULL,"
			+ "		description TEXT,"
			+ "		pe_ratio FLOAT,"
			+ "		shares_outstanding BIGINT,"
			+ "		shares_float BIGINT,"
			+ "		shares_short BIGINT,"
			+ "		PRIMARY KEY (company_id),"
			+ "		FOREIGN KEY (symbol_id) REFERENCES symbols(symbol_id)"
			+ ")"
		);
		
		stmt.execute(
			"CREATE TABLE IF NOT EXISTS etfs("
			+ "		etf_id INT NOT NULL AUTO_INCREMENT,"
			+ "		symbol_id INT NOT NULL UNIQUE,"
			+ "		etf_name VARCHAR(100),"
			+ "		PRIMARY KEY (etf_id),"
			+ "		FOREIGN KEY (symbol_id) REFERENCES symbols(symbol_id)"
			+ ")"
		);
		
		stmt.execute(
			"CREATE TABLE IF NOT EXISTS etf_holdings("
			+ "		etf_id INT NOT NULL,"
			+ "		symbol_id INT NOT NULL,"
			+ "		percent DECIMAL(5, 2),"
			+ "		CONSTRAINT pk_holding PRIMARY KEY (etf_id, symbol_id),"
			+ "		FOREIGN KEY (etf_id) REFERENCES etfs(etf_id),"
			+ "		FOREIGN KEY (symbol_id) REFERENCES symbols(symbol_id)"
			+ ")"
		);
		
		stmt.execute(
			"CREATE TABLE IF NOT EXISTS prices("
			+ "		symbol_id INT NOT NULL,"
			+ "		date DATE NOT NULL,"
			+ "		open DECIMAL(13, 4) NOT NULL,"
			+ "		high DECIMAL(13, 4) NOT NULL,"
			+ "		low DECIMAL(13, 4) NOT NULL,"
			+ "		close DECIMAL(13, 4) NOT NULL,"
			+ "		adjusted_close DECIMAL(13, 4) NOT NULL,"
			+ "		volume INT NOT NULL,"
			+ "		CONSTRAINT pk_price PRIMARY KEY (symbol_id, date),"
			+ "		FOREIGN KEY (symbol_id) REFERENCES symbols(symbol_id)"
			+ ")"
		);
		
	}
	
	public static Connection getConnection() {
		return conn;
	}
	
	/**
	 * Helper method to quickly find the symbol id for a given symbol. This
	 * method caches the mapping to avoid repeated sql executions.
	 * @param symbol
	 * @return symbol id; -1 if error
	 */
	public static int getSymbolId(String symbol) {
		if(symbolIdMap.containsKey(symbol)) {
			return symbolIdMap.get(symbol);
		}
		
		String sql = String.format("SELECT symbol_id FROM symbols WHERE symbol = '%s'", symbol);
		int symbolId = -1;
		
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if(rs.isBeforeFirst()) {
				rs.next();
				symbolId = rs.getInt("symbol_id");
				symbolIdMap.put(symbol, symbolId);
			}
		} catch (SQLException e) {
			logger.error("Unable to get symbol id for symbol {}: {}.", symbol, e.getMessage());
		}
		
		return symbolId;
	}
	
	/**
	 * Checks if a given symbol is present in the database
	 * @param symbol
	 * @return
	 */
	public static boolean isSymbolPresent(String symbol) {
		String sql = String.format("SELECT * FROM symbols WHERE symbol = '%s'", symbol);
		Statement stmt;
		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			return rs.isBeforeFirst();
		} catch (SQLException e) {
			logger.error("Error checking symbol present in db: {}", e.getMessage());
			return false;
		}
	}
	
	/**
	 * Helper method to quickly find the company id for a given symbol. This
	 * method caches the mapping to avoid repeated sql executions.
	 * @param symbol
	 * @return company id; -1 if error
	 */
	public static int getCompanyId(String symbol) {
		if(companyIdMap.containsKey(symbol)) {
			return companyIdMap.get(symbol);
		}
		
		int symbolId = getSymbolId(symbol);
		if(symbolId == -1) {
			return -1;
		}
		
		String sql = String.format("SELECT company_id FROM companies WHERE symbol_id = %d", symbolId);
		int companyId = -1;
		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
		) {
			if(rs.isBeforeFirst()) {
				rs.next();
				companyId = rs.getInt("company_id");
				companyIdMap.put(symbol, companyId);
			}
			else {
				logger.error("Unable to get company id for symbol {}: no company found for that symbol.", symbol);
			}
		} catch (SQLException e) {
			logger.error("Unable to get company id for symbol {}: {}.", symbol, e.getMessage());
		}
		
		return companyId;
	}
	
	/**
	 * Checks if a company for the given symbol is present in the datbase
	 * @param symbol
	 * @return
	 */
	public static boolean isCompanyPresent(String symbol) {
		String sql = String.format("SELECT * FROM companies WHERE symbol_id = %d", getSymbolId(symbol));
		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
		) {
			return rs.isBeforeFirst();
		} catch (SQLException e) {
			logger.error("Error checking company present in db: {}", e.getMessage());
			return false;
		}
	}
	
	public static void addCompanyData(CompanyData company) {
		logger.info("Inserting company data for {} ({})", company.name, company.symbol);
		String sql = "INSERT INTO"
				+ "	companies (symbol_id, name, description, pe_ratio, shares_outstanding, shares_float, shares_short)"
				+ "VALUES"
				+ "	(?, ?, ?, ?, ?, ?, ?)"
				+ "ON DUPLICATE KEY UPDATE"
				+ "	symbol_id = VALUES(symbol_id),"
				+ "	name = VALUES(name),"
				+ "	description = VALUES(description),"
				+ "	pe_ratio = VALUES(pe_ratio),"
				+ "	shares_outstanding = VALUES(shares_outstanding),"
				+ "	shares_float = VALUES(shares_float),"
				+ "	shares_short = VALUES(shares_short)";
		
		try (
			PreparedStatement prep = conn.prepareStatement(sql);
		) {
			prep.setInt(1, getSymbolId(company.symbol));
			prep.setString(2, company.name);
			prep.setString(3, company.desc);
			prep.setDouble(4, company.peRatio);
			prep.setLong(5, company.sharesOutstanding);
			prep.setLong(6, company.sharesFloat);
			prep.setLong(7, company.sharesShort);
			prep.execute();
		} catch (SQLException e) {
			logger.error("Error while attempting to insert price data: {}", e.getMessage());
		}
	}
	
	public static int getEtfId(String symbol) {
		if(etfIdMap.containsKey(symbol)) {
			return etfIdMap.get(symbol);
		}
		
		int symbolId = getSymbolId(symbol);
		if(symbolId == -1) {
			return -1;
		}
		
		String sql = String.format("SELECT etf_id FROM etfs WHERE symbol_id = %d", symbolId);
		int etfId = -1;
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if(rs.isBeforeFirst()) {
				rs.next();
				etfId = rs.getInt("etf_id");
				etfIdMap.put(symbol, etfId);
			}
			else {
				logger.error("Unable to get etf id for symbol {}: no etf found for that symbol.", symbol);
			}
		} catch (SQLException e) {
			logger.error("Unable to get etf id for symbol {}: {}", symbol, e.getMessage());
		}
		
		return etfId;
	}
	
	public static boolean isEtfPresent(String symbol) {
		String sql = String.format("SELECT * FROM etfs WHERE symbol_id = %d", getSymbolId(symbol));
		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
		) {
			return rs.isBeforeFirst();
		} catch (SQLException e) {
			logger.error("Error checking etf present in db: {}", e.getMessage());
			return false;
		}
	}
	
	public static void addEtfData(EtfData etf) {
		logger.info("Adding etf data for {} ({})", etf.name, etf.symbol);
		// Add to etfs table first
		String sql = "INSERT INTO"
				+ "	etfs (symbol_id, etf_name)"
				+ "VALUES"
				+ "	(?, ?)"
				+ "ON DUPLICATE KEY UPDATE"
				+ "	symbol_id = VALUES(symbol_id),"
				+ "	etf_name = VALUES(etf_name)";
		
		try (
			PreparedStatement prep = conn.prepareStatement(sql);
		) {
			prep.setInt(1, getSymbolId(etf.symbol));
			prep.setString(2, etf.name);
			prep.execute();
		} catch (SQLException e) {
			logger.error("Error while attempting to insert etf data: {}", e.getMessage());
		}
		
		// Now, add holding info
		int etfId = getEtfId(etf.symbol);
		sql = "INSERT INTO"
				+ "	etf_holdings (etf_id, symbol_id, percent)"
				+ "VALUES"
				+ "	(?, ?, ?)";
	
		String addSymbol = "INSERT INTO symbols(symbol) VALUES (?)";
	
		try (
			PreparedStatement prep = conn.prepareStatement(sql);
			PreparedStatement addSymbolPrep = conn.prepareStatement(addSymbol);
		) {
			
			for(String holdingSymbol : etf.topHoldings.keySet()) {
				int symbolId = getSymbolId(holdingSymbol);
				
				if(symbolId == -1) {
					addSymbolPrep.setString(1, holdingSymbol);
					addSymbolPrep.execute();
					symbolId = getSymbolId(holdingSymbol);
				}
				
				prep.setInt(1, etfId);
				prep.setInt(2, symbolId);
				prep.setDouble(3, etf.topHoldings.get(holdingSymbol));
				prep.addBatch();
			}
			
			prep.executeBatch();
			
		} catch (SQLException e) {
			logger.error("Error while updating etf holdings for {}: {}", etf.name, e.getMessage());
		}
	}
	
	public static void addPriceData(PriceData[] data) {
		logger.info("Inserting new price data ({} entries)", data.length);
		// Setup the prepared statement for insertion
		String sql = "INSERT INTO\n"
					+ "	prices (symbol_id, date, open, high, low, close, adjusted_close, volume)\n"
					+ "VALUES\n"
					+ "	(?, ?, ?, ?, ?, ?, ?, ?)\n"
					+ "ON DUPLICATE KEY UPDATE\n"
					+ "	symbol_id = VALUES(symbol_id),"
					+ "	date = VALUES(date),"
					+ "	open = VALUES(open),"
					+ "	high = VALUES(high),"
					+ "	low = VALUES(low),"
					+ "	close = VALUES(close),"
					+ "	adjusted_close = VALUES(adjusted_close),"
					+ "	volume = VALUES(volume)";
		
		try (
			PreparedStatement prep = conn.prepareStatement(sql);
			Statement stmt = conn.createStatement();
		) {		
			// Look at each daily entry for this stock and add the price
			for (PriceData singleDay : data) {
				
				// Get symbol id
				int symbolId = getSymbolId(singleDay.symbol);
				
				// Check that symbol is present in db
				if(symbolId == -1) {
					sql = String.format("INSERT INTO symbols(symbol) VALUES ('%s')", singleDay.symbol);
					stmt.execute(sql);
					symbolId = getSymbolId(singleDay.symbol);
				}
				
				// Add price data
				prep.setInt(1, getSymbolId(singleDay.symbol));
				prep.setDate(2, Date.valueOf(singleDay.date));
				prep.setDouble(3, singleDay.open);
				prep.setDouble(4, singleDay.high);
				prep.setDouble(5, singleDay.low);
				prep.setDouble(6, singleDay.close);
				prep.setDouble(7, singleDay.adjClose);
				prep.setInt(8, singleDay.volume);
				
				prep.addBatch();
				
			}
			
			prep.executeBatch();
			prep.close();
			stmt.close();
		} catch (SQLException e) {
			logger.error("Error while attempting to insert price data: {}", e.getMessage());
		}
	}
	
	public static ArrayList<String> getAllStockSymbols() {
		logger.info("Loading all stock symbols from database.");
		ArrayList<String> symbols = new ArrayList<>();
		
		String sql = "SELECT symbol FROM companies JOIN symbols ON symbols.symbol_id = companies.symbol_id";
		try(
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
		) {
			while(rs.next()) {
				symbols.add(rs.getString("symbol"));
			}
		} catch (SQLException e) {
			logger.error("Error retreiving company stock symbols from database: {}", e.getMessage());
		}
		
		return symbols;
	}
	
	public static ArrayList<String> getAllETFSymbols() {
		logger.info("Loading all ETF symbols from database.");
		ArrayList<String> symbols = new ArrayList<>();
		
		String sql = "SELECT symbol FROM etfs JOIN symbols ON symbols.symbol_id = etfs.symbol_id";
		try(
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
		) {
			while(rs.next()) {
				symbols.add(rs.getString("symbol"));
			}
		} catch (SQLException e) {
			logger.error("Error retreiving ETF symbols from database: {}", e.getMessage());
		}
		
		return symbols;
	}
	
	public static ArrayList<PriceData> getSymbolPriceData(String symbol) {
		ArrayList<PriceData> data = new ArrayList<>();	
		logger.info("Loading price data for {} from database.", symbol);
		
		int symbolId = getSymbolId(symbol);
		String sql = String.format("SELECT * FROM prices WHERE symbol_id=%d", symbolId);
		
		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
		) {
			while(rs.next()) {
				data.add(new PriceData(
						symbol, 
						LocalDate.parse(rs.getString("date")), 
						rs.getDouble("open"), 
						rs.getDouble("high"), 
						rs.getDouble("low"), 
						rs.getDouble("close"), 
						rs.getDouble("adjusted_close"), 
						rs.getInt("volume")
				));
			}
		} catch (SQLException e) {
			logger.error("Error loading price data for {} from database: {}", symbol, e.getMessage());
		}
		
		return data;
	}
	
	public static ArrayList<Pair<String, Double>> getEtfHoldings(String symbol) {
		ArrayList<Pair<String, Double>> holdings = new ArrayList<>();
		logger.info("Loading holding data for ETF {}.", symbol);
		
		String sql = "SELECT "
				+ "	   held_symbol.symbol AS held_symbol,"
				+ "    etf_symbol.symbol AS etf_symbol,  "
				+ "    percent "
				+ "FROM etf_holdings "
				+ "JOIN symbols AS held_symbol ON held_symbol.symbol_id = etf_holdings.symbol_id "
				+ "JOIN etfs ON etfs.etf_id = etf_holdings.etf_id "
				+ "JOIN symbols AS etf_symbol ON etfs.symbol_id = etf_symbol.symbol_id "
				+ "WHERE etf_symbol.symbol = ? "
				+ "ORDER BY percent DESC";
		try (
			PreparedStatement prep = conn.prepareStatement(sql);
		) {
			prep.setString(1, symbol);
			ResultSet rs = prep.executeQuery();
			while(rs.next()) {
				String heldSymbol = rs.getString("held_symbol");
				double percent = rs.getDouble("percent");
				holdings.add(new Pair<>(heldSymbol, percent));
			}
		} catch (SQLException e) {
			logger.error("Error loading ETF holding data for {} from database: {}", symbol, e.getMessage());
		} 
		
		return holdings;
	}
	
}


















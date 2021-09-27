package stockfetcher;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import stockfetcher.api.StockApi;
import stockfetcher.db.StockDatabase;

public class Start {

	public static void main(String[] args) {

		// Initialize the Stock Database
		try {
			StockDatabase.initialize();
		} catch (SQLException e) {
			System.err.println("Error intializing Stock Database.");
			System.err.println("Do you have the stockfetcher user created?");
			e.printStackTrace();
			System.exit(-1);
		}

		// Fetch the data for a stock
		JsonObject data = null;
		try {
			data = StockApi.dailyAdjusted("IBM", true);
		} catch (IOException | InterruptedException e) {
			System.err.println();
			e.printStackTrace();
			System.exit(-1);
		}

		// Insert data into the database
		Connection conn = StockDatabase.getConnection();
		try (
				Statement stmt = conn.createStatement();
		) {
			String sql;

			// Add the symbol to the symbols table if it isn't already present
			String symbol = data.get("Meta Data").getAsJsonObject().get("2. Symbol").getAsString();
			sql = String.format("INSERT IGNORE INTO symbols (symbol) VALUES ('%s')", symbol);
			stmt.execute(sql);
			
			sql = String.format("SELECT symbol_id FROM symbols WHERE symbol = '%s'", symbol);
			ResultSet result = stmt.executeQuery(sql);
			result.first();
			int symbolId = result.getInt("symbol_id");
			result.close();
			stmt.close();
			
			// Setup the prepared statement for insertion
			sql = "INSERT INTO\n"
					+ "	prices (symbol_id, date, open, high, low, close, adjusted_close, volume, dividend_amount, split_coefficient)\n"
					+ "VALUES\n"
					+ "	(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)\n"
					+ "ON DUPLICATE KEY UPDATE\n"
					+ "	symbol_id = VALUES(symbol_id),"
					+ "	date = VALUES(date),"
					+ "	open = VALUES(open),"
					+ "	high = VALUES(high),"
					+ "	low = VALUES(low),"
					+ "	close = VALUES(close),"
					+ "	adjusted_close = VALUES(adjusted_close),"
					+ "	volume = VALUES(volume),"
					+ "	dividend_amount = VALUES(dividend_amount),"
					+ "	split_coefficient = VALUES(split_coefficient)";
			PreparedStatement prep = conn.prepareStatement(sql);
			
			// Look at each daily entry for this stock and add the price
			JsonObject dailyData = data.get("Time Series (Daily)").getAsJsonObject();
			for (Entry<String, JsonElement> entry : dailyData.entrySet()) {
				JsonObject day = entry.getValue().getAsJsonObject();
				
				String date = entry.getKey();
				double open = day.get("1. open").getAsDouble();
				double high = day.get("2. high").getAsDouble();
				double low = day.get("3. low").getAsDouble();
				double close = day.get("4. close").getAsDouble();
				double adjClose = day.get("5. adjusted close").getAsDouble();
				int volume = day.get("6. volume").getAsInt();
				double divAmt = day.get("7. dividend amount").getAsDouble();
				double splitCoef = day.get("8. split coefficient").getAsDouble();
				
				prep.setInt(1, symbolId);
				prep.setDate(2, Date.valueOf(date));
				prep.setDouble(3, open);
				prep.setDouble(4, high);
				prep.setDouble(5, low);
				prep.setDouble(6, close);
				prep.setDouble(7, adjClose);
				prep.setInt(8, volume);
				prep.setDouble(9, divAmt);
				prep.setDouble(10, splitCoef);
				
				prep.addBatch();
				
			}
			
			prep.executeBatch();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

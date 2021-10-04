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

import stockfetcher.api.CompanyData;
import stockfetcher.api.EtfApi;
import stockfetcher.api.PriceData;
import stockfetcher.api.StockApi;
import stockfetcher.db.StockDatabase;

public class Start {

	public static void main(String[] args) {
		
		String symbol = "^TNX";
		
		// Initialize the Stock Database
		try {
			StockDatabase.initialize();
		} catch (SQLException e) {
			System.err.println("Error intializing Stock Database.");
			System.err.println("Do you have the stockfetcher user created?");
			e.printStackTrace();
			System.exit(-1);
		}
		
		fetchCompanyStockData("CRSR");
		
		PriceData[] etf = EtfApi.dailyAdjusted(symbol, true);
		if(etf != null) {
			StockDatabase.addPriceData(etf);			
		}
		
	}
	
	public static boolean fetchCompanyStockData(String symbol) {
		// Fetch the data for a stock
		PriceData[] data = StockApi.dailyAdjusted(symbol, true);
		
		// Make sure that the symbol was valid
		if(data == null) {
			return false;
		}
		
		// Add the prices
		StockDatabase.addPriceData(data);
		
		// If company data isn't present, fetch that too
		if(!StockDatabase.isCompanyPresent(symbol)) {
			CompanyData companyData;
			try {
				companyData = StockApi.companyOverview(symbol);
				StockDatabase.addCompanyData(companyData);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

}

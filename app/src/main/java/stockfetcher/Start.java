package stockfetcher;

import java.sql.SQLException;

import stockfetcher.api.CompanyData;
import stockfetcher.api.EtfData;
import stockfetcher.api.PriceData;
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
		
		fetchCompanyStockData("CRSR");
		fetchEtfData("VOO");
		
	}
	
	public static boolean fetchEtfData(String symbol) {
		// Fetch etf price data
		PriceData[] data = StockApi.dailyAdjustedEtf(symbol, true);
		
		// Check that symbol was valid
		if(data == null) {
			return false;
		}
		
		// Add the prices
		StockDatabase.addPriceData(data);
		
		// If etf data isn't present, fetch that too
		if(!StockDatabase.isEtfPresent(symbol)) {
			EtfData etfData = StockApi.getEtfOverview(symbol);
			if(etfData != null) {
				StockDatabase.addEtfData(etfData);
			}
		}
		
		return true;
	}
	
	public static boolean fetchCompanyStockData(String symbol) {
		// Fetch the data for a stock
		PriceData[] data = StockApi.dailyAdjustedCompany(symbol, true);
		
		// Make sure that the symbol was valid
		if(data == null) {
			return false;
		}
		
		// Add the prices
		StockDatabase.addPriceData(data);
		
		// If company data isn't present, fetch that too
		if(!StockDatabase.isCompanyPresent(symbol)) {
			CompanyData companyData = StockApi.getCompanyOverview(symbol);
			if(companyData != null) {
				StockDatabase.addCompanyData(companyData);
			}
		}

		return true;
	}

}

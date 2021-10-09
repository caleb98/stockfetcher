package stockfetcher;

import java.sql.SQLException;

import stockfetcher.api.CompanyData;
import stockfetcher.api.EtfData;
import stockfetcher.api.PriceData;
import stockfetcher.api.StockApi;
import stockfetcher.db.StockDatabase;

public class Start {

	public static void main(String[] args) {
		
		EtfData data = StockApi.etfData("VOO");
		System.out.println("Sectors");
		for(String sector : data.sectorWeightings.keySet()) {
			System.out.printf(
				"\t%s: %.2f%%\n",
				sector,
				data.sectorWeightings.get(sector)
			);
		}
		System.out.println("Holdings");
		for(String holding : data.topHoldings.keySet()) {
			System.out.printf(
				"\t%s: %.2f%%\n",
				holding,
				data.topHoldings.get(holding)
			);
		}
		System.exit(0);
		
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
		
		PriceData[] etf = StockApi.dailyAdjustedEtf(symbol, true);
		if(etf != null) {
			StockDatabase.addPriceData(etf);			
		}
		
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
			CompanyData companyData = StockApi.companyOverview(symbol);
			if(companyData != null) {
				StockDatabase.addCompanyData(companyData);
			}
		}

		return true;
	}

}

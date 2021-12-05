package stockfetcher.ui;

import java.io.IOException;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import stockfetcher.api.CompanyData;
import stockfetcher.api.EtfData;
import stockfetcher.api.PriceData;
import stockfetcher.api.StockApi;
import stockfetcher.db.StockDatabase;

public class Utils {

	public static final void downloadStockData(String... symbols) {
		final ProgressDialog progress = new ProgressDialog("Adding symbols...");
		progress.show();
		
		// Loop through all the symbols to add
		var task = new Task<Void>() {
			public Void call() {
				int processed = 0;
				for(String symbol : symbols) {
					
					// Fetch price data
					Platform.runLater(()->{
						progress.setInfo("Downloading price data for " + symbol + "...");
					});
					PriceData[] data = StockApi.getStockPriceData(symbol, true);
					updateProgress(processed + 1.0 / 3.0, symbols.length);
					
					// Check symbol was valid
					if(data == null) {
						Platform.runLater(()->{
							Alert alert = new Alert(
									AlertType.ERROR, 
									"There was an error downloading price data for " + symbol + ".\nPlease try again later.");
							alert.show();
						});
						updateProgress(++processed, symbols.length);
						continue;
					}
					
					// Add the price data
					Platform.runLater(()->{
						progress.setInfo(String.format(
							"Adding price data for %s to database (%d entries)...", 
							symbol,
							data.length
						));
					});
					StockDatabase.addPriceData(data);
					updateProgress(processed + 2.0 / 3.0, symbols.length);
					
					// Check whether it was an etf or company, and add
					// data as appropriate
					try {
						Platform.runLater(()->{
							progress.setInfo("Checking if " + symbol + " is a stock or ETF...");
						});
						if(StockApi.isSymbolCompany(symbol)) {
							Platform.runLater(()->{
								progress.setInfo(String.format(
									"Downloading company data for %s...",
									symbol
								));
							});
							CompanyData cData = StockApi.getCompanyOverview(symbol);
							
							// Check that company data was retrieved successfully
							if(cData == null) {
								Platform.runLater(() -> {
									Alert alert = new Alert(
										AlertType.WARNING,
										"Unable to retrieve company info for " + symbol + ".\n"
												+ "The API used for this data is rate limited (5 requests/min), so you may\n"
												+ "need to try again later if you have requested many symbols recently."
									);
									alert.show();
								});
								updateProgress(++processed, symbols.length);
								continue;
							}
							
							// Insert the data in the database
							Platform.runLater(()->{
								progress.setInfo(String.format(
									"Adding company data for %s (%s) to database...",
									cData.name,
									symbol
								));
							});
							StockDatabase.addCompanyData(cData);
						}
						else if(StockApi.isSymbolETF(symbol)) {
							Platform.runLater(()->{
								progress.setInfo(String.format(
									"Downloading ETF data for %s...",
									symbol
								));
							});
							EtfData eData = StockApi.getEtfOverview(symbol);
							
							// Check that etf data was retrieved successfully
							if(eData == null) {
								//TODO: warn user 
								continue;
							}
							
							// Add data to the database
							Platform.runLater(()->{
								progress.setInfo(String.format(
									"Adding ETF data for %s (%s) to database...",
									eData.name,
									symbol
								));
							});
							StockDatabase.addEtfData(eData);
						}
					} catch (IOException | InterruptedException e1) {
						// TODO: log this error/handle appropriately
						e1.printStackTrace();
					}
					updateProgress(++processed, symbols.length);
				}
				
				updateProgress(1, 1);
				Platform.runLater(()->{
					progress.setInfo("Complete!");
				});
				return null;
			}
		};
		progress.progressProperty().bind(task.progressProperty());
		new Thread(task).start();
	}
	
}

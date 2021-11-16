package stockfetcher.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import stockfetcher.api.CompanyData;
import stockfetcher.api.EtfData;
import stockfetcher.api.PriceData;
import stockfetcher.api.StockApi;
import stockfetcher.db.StockDatabase;

public class UIController {
	
	@FXML private ListView<String> stockList;
	@FXML private ListView<String> etfList;
	private String selectedSymbol = null;
	private boolean isEtfSelected = false;

	@FXML private TabPane chartTabs;
	@FXML private Tab newTabButton;
	
	@FXML private ListView<String> holdingsList;

	public void initialize() {
		// Setup relevant lists
		updateStocksList();
		updateEtfList();
		updateHoldingsList();
		
		// Add a chart tab
		createNewTab();
		
		// Setup the new tab button
		chartTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab)->{
			if(newTab == newTabButton) {
				createNewTab();
			}
		});
	}
	
	private void updateStocksList() {
		stockList.getItems().setAll(StockDatabase.getAllStockSymbols());
	}
	
	private void updateEtfList() {
		etfList.getItems().setAll(StockDatabase.getAllETFSymbols());
	}
	
	private void updateHoldingsList() {
		if(isEtfSelected) {
			ArrayList<Pair<String, Double>> holdings = StockDatabase.getEtfHoldings(selectedSymbol);
			holdingsList.getItems().clear();
			for(var holding : holdings) {
				holdingsList.getItems().add(String.format(
					"%s - %.2f%%", 
					holding.getKey(),
					holding.getValue()
				));
			}
			
			if(holdings.size() == 0) {
				holdingsList.getItems().add("No holding data available for this ETF.");
			}
		}
		else {
			holdingsList.getItems().setAll("Select an ETF to see holdings.");
		}
	}
	
	private void createNewTab() {
		try {			
			FXMLLoader loader = new FXMLLoader(ClassLoader.getSystemResource("chart_layout.fxml"));
			VBox tabContents = loader.<VBox>load();
			ChartController chart = loader.getController();
			
			Tab newTab = new Tab("New Tab", tabContents);
			newTab.textProperty().bind(chart.chartNameProperty());
			chartTabs.getTabs().add(chartTabs.getTabs().size() - 1, newTab);
			chartTabs.getSelectionModel().select(chartTabs.getTabs().size() - 2);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	private void stockSelected(Event e) {
		selectedSymbol = stockList.getSelectionModel().getSelectedItem();
		isEtfSelected = false;
		updateHoldingsList();
	}
	
	@FXML
	private void etfSelected(Event e) {
		selectedSymbol = etfList.getSelectionModel().getSelectedItem();
		isEtfSelected = true;
		updateHoldingsList();
	}
	
	@FXML
	private void requestExit(Event e) {
		Platform.exit();
	}
	
	@FXML
	private void addNewStock(Event e) {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("New Tracked Symbol");
		dialog.setHeaderText("Enter the new symbol you would like to track.");
		dialog.setContentText("Symbol");
		
		Optional<String> result = dialog.showAndWait();
		result.ifPresent(symbol -> {
			// TODO: warn stock present?
			
			// Fetch price data
			PriceData[] data = StockApi.getStockPriceData(symbol, true);
			
			// Check symbol was valid
			if(data == null) {
				// TODO: actual error handling
				return;
			}
			
			// Add the price data
			StockDatabase.addPriceData(data);
			
			// Check whether it was an etf or company, and add
			// data as appropriate
			try {
				if(StockApi.isSymbolCompany(symbol)) {
					CompanyData cData = StockApi.getCompanyOverview(symbol);
					
					// Check that company data was retrieved successfully
					if(cData == null) {
						//TODO: warn user
						return;
					}
					
					// Insert the data in the database
					StockDatabase.addCompanyData(cData);
					
					// Update stock list
					updateStocksList();
				}
				else if(StockApi.isSymbolETF(symbol)) {
					EtfData eData = StockApi.getEtfOverview(symbol);
					
					// Check that etf data was retrieved successfully
					if(eData == null) {
						//TODO: warn user
						return;
					}
					
					// Add data to the database
					StockDatabase.addEtfData(eData);
					
					// Update etf list
					updateEtfList();
				}
			} catch (IOException | InterruptedException e1) {
				// TODO: log this error/handle appropriately
				e1.printStackTrace();
			}
		});
	}
	
}

package stockfetcher.ui;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import stockfetcher.api.CompanyData;
import stockfetcher.api.EtfData;
import stockfetcher.api.PriceData;
import stockfetcher.api.StockApi;
import stockfetcher.db.StockDatabase;

public class UIController {
	
	private static final NumberFormat SHARES_FORMAT = NumberFormat.getNumberInstance();
	
	@FXML private BorderPane root;
	
	@FXML private ListView<String> stockList;
	@FXML private ListView<String> etfList;
	private String selectedSymbol = null;
	private BooleanProperty isEtfSelected = new SimpleBooleanProperty(false);

	@FXML private TabPane chartTabs;
	@FXML private Tab newTabButton;
	
	@FXML private VBox companyBox;
	@FXML private VBox companyDataBox;
	@FXML private Label noDataMessage;
	@FXML private Label companyName;
	@FXML private Label peRatio;
	@FXML private Label sharesOutstanding;
	@FXML private Label companyDescription;
	private ObjectProperty<CompanyData> companyData = new SimpleObjectProperty<>(null);
	
	@FXML private VBox holdingsBox;
	@FXML private ListView<String> holdingsList;

	public void initialize() {
		// Setup relevant lists
		updateStocksList();
		updateEtfList();
		updateHoldingInfo();
		
		// Add a chart tab
		createNewTab();
		
		// Setup the new tab button
		chartTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab)->{
			if(newTab == newTabButton) {
				createNewTab();
			}
		});
		
		// Setup visibility for the right hand boxes
		companyBox.managedProperty().bind(companyBox.visibleProperty());
		companyBox.visibleProperty().bind(isEtfSelected.not());
		companyDataBox.managedProperty().bind(companyDataBox.visibleProperty());
		noDataMessage.managedProperty().bind(noDataMessage.visibleProperty());
		
		holdingsBox.managedProperty().bind(holdingsBox.visibleProperty());
		holdingsBox.visibleProperty().bind(isEtfSelected);
		
		// Setup company data bindings
		companyData.addListener((obs, oldValue, newValue)->{
			if(newValue == null) {
				companyDataBox.setVisible(false);
				noDataMessage.setVisible(true);
				companyName.setText("Company Info");
			}
			else {
				peRatio.setText(String.valueOf(companyData.get().peRatio));
				sharesOutstanding.setText(SHARES_FORMAT.format(companyData.get().sharesOutstanding));
				companyDescription.setText(companyData.get().desc);
				companyName.setText(companyData.get().name);
				
				companyDataBox.setVisible(true);
				noDataMessage.setVisible(false);
			}
		});
	}
	
	private void updateStocksList() {
		ArrayList<String> stocks = StockDatabase.getAllStockSymbols();
		Collections.sort(stocks);
		stockList.getItems().setAll(stocks);
	}
	
	private void updateEtfList() {
		ArrayList<String> etfs = StockDatabase.getAllETFSymbols();
		Collections.sort(etfs);
		etfList.getItems().setAll(etfs);
	}
	
	private void updateHoldingInfo() {
		if(isEtfSelected.get()) {
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
	
	private void updateCompanyInfo() {
		companyData.set(StockDatabase.getCompanyData(selectedSymbol));
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
		isEtfSelected.set(false);
		updateCompanyInfo();
	}
	
	@FXML
	private void etfSelected(Event e) {
		selectedSymbol = etfList.getSelectionModel().getSelectedItem();
		isEtfSelected.set(true);
		updateHoldingInfo();
	}
	
	@FXML
	private void requestExit(Event e) {
		Platform.exit();
	}
	
	@FXML
	private void addNewStock(Event e) {
		AddTrackedStockDialog dialog = new AddTrackedStockDialog();
		
		Optional<ArrayList<String>> result = dialog.showAndWait();
		result.ifPresent(symbolsList -> {
			final ProgressDialog progress = new ProgressDialog("Adding symbols...");
			progress.show();
		
			// Check for symbols already in the database
			Iterator<String> symbolsIter = symbolsList.iterator();
			while(symbolsIter.hasNext()) {
				String symbol = symbolsIter.next();
				
				// Check if the symbol is already present and warn the
				// user if so.
				if(StockDatabase.hasPriceData(symbol)) {
					ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
					ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
					
					Alert alert = new Alert(AlertType.WARNING, null, yes, no);
					alert.setHeaderText("The symbol " + symbol + " is already present. Continue loading data anyway?");
					
					// If the user responds "no", then remove it from the list
					Optional<ButtonType> loadAnyway = alert.showAndWait();
					if(loadAnyway.isPresent() && loadAnyway.get() == no) {
						symbolsIter.remove();
					}
				}
			}
			
			// Loop through all the symbols to add
			var task = new Task<Void>() {
				public Void call() {
					int processed = 0;
					for(String symbol : symbolsList) {
						
						// Fetch price data
						Platform.runLater(()->{
							progress.setInfo("Downloading price data for " + symbol + "...");
						});
						PriceData[] data = StockApi.getStockPriceData(symbol, true);
						updateProgress(processed + 1.0 / 3.0, symbolsList.size());
						
						// Check symbol was valid
						if(data == null) {
							// TODO: actual error handling
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
						updateProgress(processed + 2.0 / 3.0, symbolsList.size());
						
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
									//TODO: warn user
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
								
								// Update stock list
								Platform.runLater(()->{updateStocksList();});
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
								
								// Update etf list
								Platform.runLater(()->{updateEtfList();});
							}
						} catch (IOException | InterruptedException e1) {
							// TODO: log this error/handle appropriately
							e1.printStackTrace();
						}
						updateProgress(++processed, symbolsList.size());
					}

					Platform.runLater(()->{
						progress.setInfo("Complete!");
					});
					return null;
				}
			};
			progress.progressProperty().bind(task.progressProperty());
			new Thread(task).start();
		});
	}
	
	@FXML
	private void updateStockData(Event e) {
		//TODO: this method!
	}
	
	@FXML
	private void toggleDarkMode(Event e) {
		if(root.getStylesheets().contains("app_style_dark.css")) {
			root.getStylesheets().remove("app_style_dark.css");
		}
		else {
			root.getStylesheets().add("app_style_dark.css");
		}
	}
	
}




























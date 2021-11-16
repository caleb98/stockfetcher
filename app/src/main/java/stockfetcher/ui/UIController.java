package stockfetcher.ui;

import java.io.IOException;
import java.util.ArrayList;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
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
		stockList.getItems().addAll(StockDatabase.getAllStockSymbols());
		etfList.getItems().addAll(StockDatabase.getAllETFSymbols());
		
		createNewTab();
		updateHoldingsList();
		
		// Setup the new tab button
		chartTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab)->{
			if(newTab == newTabButton) {
				createNewTab();
			}
		});
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
	
}

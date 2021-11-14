package stockfetcher.ui;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import stockfetcher.db.StockDatabase;

public class UIController {
	
	@FXML private ListView<String> stockList;
	@FXML private ListView<String> etfList;

	@FXML private TabPane chartTabs;
	@FXML private Tab newTabButton;
	
	@FXML private ListView<String> holdingsList;

	public void initialize() {
		stockList.getItems().addAll(StockDatabase.getAllStockSymbols());
		etfList.getItems().addAll(StockDatabase.getAllETFSymbols());
		
		holdingsList.getItems().add("FB - 100%");
		
		createNewTab();
		
		// Setup the new tab button
		chartTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab)->{
			if(newTab == newTabButton) {
				createNewTab();
			}
		});
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
	
}

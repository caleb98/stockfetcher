package stockfetcher.ui;

import java.util.Random;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ListView;

public class UIController {

	@FXML private ListView<String> stockList;
	@FXML private ListView<String> etfList;

	@FXML private LineChart<Double, Double> dataChart;
	
	@FXML private ListView<String> holdingsList;
	
	public void test() {
		stockList.getItems().add("GME");
		stockList.getItems().add("CRSR");
		stockList.getItems().add("AMD");
		stockList.getItems().add("GOOGL");
		stockList.getItems().add("APPL");
		
		etfList.getItems().add("SPGP");
		etfList.getItems().add("IWY");
		etfList.getItems().add("MGK");
		etfList.getItems().add("IWF");
		etfList.getItems().add("SPYG");
		
		XYChart.Series<Double, Double> crsr = new XYChart.Series<>();
		crsr.setName("CRSR");
		XYChart.Series<Double, Double> gme = new XYChart.Series<>();
		gme.setName("GME");
		Random rand = new Random();
		for(int i = 0; i < 50; i++) {
			crsr.getData().add(new XYChart.Data<Double, Double>(1 + i * 1.0, 20 + i * 0.6 + rand.nextDouble() * 5));
			gme.getData().add(new XYChart.Data<Double, Double>(1 + i * 1.0, 40 + i * -0.6 + rand.nextDouble() * 10));
		}
		dataChart.getData().add(crsr);
		dataChart.getData().add(gme);
		
		holdingsList.getItems().add("FB - 100%");
	}
	
}

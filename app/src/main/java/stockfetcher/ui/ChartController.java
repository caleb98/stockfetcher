package stockfetcher.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class ChartController {

	@FXML LineChart<String, Double> dataChart;
	@FXML CategoryAxis xAxis;
	@FXML NumberAxis yAxis;
	
	public void initialize() { 
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd LLL YY");
		
		XYChart.Series<String, Double> crsr = new XYChart.Series<>();
		crsr.setName("CRSR");
		XYChart.Series<String, Double> gme = new XYChart.Series<>();
		gme.setName("GME");
		Random rand = new Random();
		for(int i = 0; i < 50; i++) {
			LocalDate d = LocalDate.now().plusDays(i);
			String date = d.format(formatter);
			crsr.getData().add(new XYChart.Data<String, Double>(date, 20 + i * 0.6 + rand.nextDouble() * 5));
			gme.getData().add(new XYChart.Data<String, Double>(date, 40 + i * -0.6 + rand.nextDouble() * 10));
		}
		
		dataChart.getData().add(crsr);
		dataChart.getData().add(gme);
	}
	
}

package stockfetcher.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

public class ChartController {
	
	@FXML private LineChart<String, Double> dataChart;
	@FXML private CategoryAxis xAxis;
	@FXML private NumberAxis yAxis;
	
	private ArrayList<String> symbolsTracked = new ArrayList<>();
	
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
	
	@FXML
	private void changeSymbolsTracked(Event e) {
		String[] symbols = symbolsTracked.toArray(new String[0]);
		String symbolsString = String.join(",", symbols);
		
		Dialog<Pair<String, String>> dialog = new Dialog<>();
		dialog.setTitle("Update Chart");
		dialog.setHeaderText("Enter new chart name and symbols tracked.");
		
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		TextField nameField = new TextField(dataChart.titleProperty().get());
		TextField symbolsField = new TextField(symbolsString);
		
		GridPane grid = new GridPane();
		grid.setHgap(5);
		grid.setVgap(5);
		
		grid.add(new Label("Chart Name:"), 0, 0);
		grid.add(nameField, 1, 0);
		grid.add(new Label("Symbols:"), 0, 1);
		grid.add(symbolsField, 1, 1);
		
		dialog.getDialogPane().setContent(grid);
		
		dialog.setResultConverter((buttonType)->{
			if(buttonType == ButtonType.OK) {	
				return new Pair<String, String>(nameField.getText(), symbolsField.getText());
			}
			else {
				return null;
			}
		});
		
		Optional<Pair<String, String>> result = dialog.showAndWait();
		result.ifPresent(resultValue -> {
			// Check that the OK button was pressed
			if(resultValue == null) {
				return;
			}
			
			// Get the values from the UI elements
			String newChartName = resultValue.getKey();
			String newSymbols = resultValue.getValue();
			
			// Update the chart name
			dataChart.titleProperty().set(newChartName);
			
			// Update the tracked symbols
			String[] newSymbolsArray = newSymbols.split(",");
			for(int i = 0; i < newSymbolsArray.length; i++) {
				newSymbolsArray[i] = newSymbolsArray[i].trim().toUpperCase();
			}
			//TODO: check if new symbols are not in db
			symbolsTracked.clear();
			for(String s : newSymbolsArray) {
				symbolsTracked.add(s);
			}
			//TODO: update chart
		});
	}
	
	public String getChartName() {
		return dataChart.titleProperty().get();
	}
	
	public void setChartName(String newName) {
		dataChart.titleProperty().set(newName);
	}
	
	public StringProperty chartNameProperty() {
		return dataChart.titleProperty();
	}
	
}

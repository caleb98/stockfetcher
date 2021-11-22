package stockfetcher.ui;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;

import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tooltip;
import javafx.util.Pair;
import javafx.util.StringConverter;
import stockfetcher.api.PriceData;
import stockfetcher.db.StockDatabase;

public class ChartController {
	
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd LLL YY");
	
	@FXML private LineChart<Number, Double> dataChart;
	@FXML private NumberAxis xAxis;
	@FXML private NumberAxis yAxis;
	
	@FXML private DatePicker startDatePicker;
	@FXML private DatePicker endDatePicker;
	
	private ArrayList<String> symbolsTracked = new ArrayList<>();
	
	public void initialize() {		
		// Formatter for converting x axis date to numbers/strings and vice versa
		xAxis.setTickLabelFormatter(new StringConverter<Number>() {
			@Override
			public String toString(Number epochDay) {
				Long day = Math.round((Double) epochDay);
				LocalDate date = LocalDate.ofEpochDay(day);
				return date.format(DATE_FORMAT);
			}
			
			@Override
			public Number fromString(String stringDate) {
				LocalDate date = LocalDate.parse(stringDate, DATE_FORMAT);
				return date.toEpochDay();
			}
		});
	}
	
	private void refreshTrackedSymbols() {
		// Remove untracked symbols from the data chart
		var iter = dataChart.getData().iterator();
		while(iter.hasNext()) {
			if(!symbolsTracked.contains(iter.next().getName())) {
				iter.remove();
				continue;
			}
		}
		
		// Loop through all tracked symbols and add them if they
		// aren't already present
		for(String symbol : symbolsTracked) {
			
			// Check if symbol already charted
			boolean isPresent = false;
			for(var series : dataChart.getData()) {
				if(series.getName().equals(symbol)) {
					isPresent = true;
					break;
				}
			}
			
			if(isPresent) {
				continue;
			}
			
			// Pull the data
			ArrayList<PriceData> priceData = StockDatabase.getSymbolPriceData(symbol);
			
			if(priceData.size() == 0) {
				// TODO: warn no data
				continue;
			}
			
			// Add data to the chart
			XYChart.Series<Number, Double> pricePoints = new XYChart.Series<>();
			pricePoints.setName(symbol);
			for(PriceData point : priceData) {
				long date = point.date.toEpochDay();
				var dataPoint = new XYChart.Data<Number, Double>(date, point.adjClose);
				pricePoints.getData().add(dataPoint);
			}
			
			dataChart.getData().add(pricePoints);
			
		}
		
		// Re-add tooltips to all the points
		iter = dataChart.getData().iterator();
		while(iter.hasNext()) {
			
			Series<Number, Double> series = iter.next();
			System.out.println("\tUpdating " + series.getName());
			
			for(var point : series.getData()) {
				
				// Update style class (for visibility)
				point.getNode().getStyleClass().add("line-node");
				
				// Add tooltip
				LocalDate date = LocalDate.ofEpochDay((Long) point.getXValue());
				Tooltip t = new Tooltip(String.format("%s: $%.2f", date.format(DATE_FORMAT), point.getYValue()));
				t.setShowDelay(javafx.util.Duration.millis(200));
				point.getNode().getProperties().put("pricedata-tooltip", t);
				Tooltip.install(point.getNode(), t);
				
			}
		}
	}
	
	@FXML
	private void resetDateRange(Event e) {
		xAxis.setAutoRanging(true);
		startDatePicker.setValue(null);
		endDatePicker.setValue(null);
	}
	
	@FXML
	private void updateDateRange(Event e) {
		LocalDate startDate = startDatePicker.getValue();
		LocalDate endDate = endDatePicker.getValue();
		
		if(startDate == null || endDate == null) {
			return;
		}
		
		LocalDateTime start = LocalDateTime.of(startDatePicker.getValue(), LocalTime.NOON);
		LocalDateTime end = LocalDateTime.of(endDatePicker.getValue(), LocalTime.NOON);
		long daysBetween = Duration.between(start, end).toDays();
		
		if(daysBetween < 1) {
			// TODO: no range less than 30 days
			return;
		}
		
		Number startEpoch = start.toLocalDate().toEpochDay();
		Number endEpoch = end.toLocalDate().toEpochDay();
		xAxis.setAutoRanging(false);
		xAxis.setLowerBound(startEpoch.doubleValue());
		xAxis.setUpperBound(endEpoch.doubleValue());
		xAxis.setTickUnit(daysBetween / 15);
	}
	
	@FXML
	private void changeSymbolsTracked(Event e) {
		// Pull the currently tracked symbols
		String[] symbols = symbolsTracked.toArray(new String[0]);
		String symbolsString = String.join(",", symbols);
		
		// Create the dialog box
		Dialog<Pair<String, String>> dialog = new ChartEditDialog(dataChart.titleProperty().get(), symbolsString);
		
		// Get dialog result
		dialog.resultProperty().addListener((obs, oldValue, newValue) -> {
			if(newValue == null) {
				return;
			}
			
			// Get the values from the UI elements
			String newChartName = newValue.getKey();
			String newSymbols = newValue.getValue();
			
			// Update the chart name
			dataChart.setTitle(newChartName);
			
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
			
			refreshTrackedSymbols();
		});
		dialog.show();
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

package stockfetcher.ui;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
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
	
	@FXML private Button resetDateButton;
	@FXML private Button editChartButton;
	
	private boolean isLocked = false;
	
	private HashSet<String> symbolsTracked = new HashSet<>();
	
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
		
		yAxis.setTickLabelFormatter(new StringConverter<Number>() {
			@Override
			public String toString(Number object) {
				return String.format("$%.0f", object.doubleValue());
			}

			@Override
			public Number fromString(String string) {
				return Double.valueOf(string);
			}
		});
	}
	
	public void predict(String symbol, LocalDate begin, LocalDate end) {
		dataChart.getData().removeIf(series -> !series.getName().equals(symbol));	
		
		XYChart.Series<Number, Double> symbolData = null;
		for(var series : dataChart.getData()) {
			if(series.getName().equals(symbol)) {
				symbolData = series;
				break;
			}
		}
		
		if(symbolData == null) {
			// Add the data, then call this method again when we're done
			symbolsTracked.add(symbol);
			refreshTrackedSymbols().setOnSucceeded((e)->{
				predict(symbol, begin, end);
			});
			return;
		}
		
		
		
		setChartName(symbol + " Prediction");
		ArrayList<Data<Number, Double>> data = new ArrayList<>(symbolData.getData());
		data.removeIf(point -> {
			LocalDate date = LocalDate.ofEpochDay(point.getXValue().longValue());
			return date.compareTo(begin) < 0 || date.compareTo(end) > 0;
		});
		
		// Linear least squares
		double[] xpts = new double[data.size()];
		double[] ypts = new double[data.size()];
		long earliest = Long.MAX_VALUE;
		for(int i = 0; i < data.size(); i++) {
			xpts[i] = data.get(i).getXValue().doubleValue();
			ypts[i] = data.get(i).getYValue().doubleValue();
			
			if(xpts[i] < earliest) {
				earliest = (long) xpts[i];
			}
		}
		
		double m = data.size();
		double sumXCubed = 0;
		double sumXY = 0;
		double sumY = 0;
		double sumX = 0;
		for(int i = 0; i < data.size(); i++) {
			sumXCubed += xpts[i] * xpts[i];
			sumXY += xpts[i] * ypts[i];
			sumY += ypts[i];
			sumX += xpts[i];
		}

		double mean = sumY / m;
		double std = 0;
		for(int i = 0; i < data.size(); i++) {
			std += Math.pow(ypts[i] - mean, 2);
		}
		std = Math.sqrt(std / m);
		
		double a0 = ((sumXCubed * sumY) - (sumXY * sumX)) / ((m * sumXCubed) - (sumX * sumX));
		double a1 = ((m * sumXY) - (sumX * sumY)) / ((m * sumXCubed) - (sumX * sumX));
		
		// Build prediction data
		XYChart.Series<Number, Double> prediction = new XYChart.Series<>();
		XYChart.Series<Number, Double> predPlusStd = new XYChart.Series<>();
		XYChart.Series<Number, Double> predLessStd = new XYChart.Series<>();
		XYChart.Series<Number, Double> predPlus2Std = new XYChart.Series<>();
		XYChart.Series<Number, Double> predLess2Std = new XYChart.Series<>();
		
		prediction.setName("Prediction");
		predPlusStd.setName("Prediction + σ");
		predLessStd.setName("Prediction - σ");
		predPlus2Std.setName("Prediction + 2σ");
		predLess2Std.setName("Prediction - 2σ");
		
		double minPrice = Double.POSITIVE_INFINITY;
		double maxPrice = Double.NEGATIVE_INFINITY;
		
		for(int i = 0; i < m; i++) {
			var dataPoint = new XYChart.Data<Number, Double>(xpts[i], a0 + a1 * (xpts[i]));
			var dataPlusStd = new XYChart.Data<Number, Double>(xpts[i], a0 + a1 * (xpts[i]) + std);
			var dataLessStd = new XYChart.Data<Number, Double>(xpts[i], a0 + a1 * (xpts[i]) - std);
			var dataPlus2Std = new XYChart.Data<Number, Double>(xpts[i], a0 + a1 * (xpts[i]) + 2 * std);
			var dataLess2Std = new XYChart.Data<Number, Double>(xpts[i], a0 + a1 * (xpts[i]) - 2 * std);
			prediction.getData().add(dataPoint);
			predPlusStd.getData().add(dataPlusStd);
			predLessStd.getData().add(dataLessStd);
			predPlus2Std.getData().add(dataPlus2Std);
			predLess2Std.getData().add(dataLess2Std);
			
			if(dataPoint.getYValue().doubleValue() < minPrice) {
				minPrice = dataPoint.getYValue().doubleValue();
			}
			if(dataPoint.getYValue().doubleValue() > maxPrice) {
				maxPrice = dataPoint.getYValue().doubleValue();
			}
			
			if(dataPlusStd.getYValue().doubleValue() < minPrice) {
				minPrice = dataPlusStd.getYValue().doubleValue();
			}
			if(dataPlusStd.getYValue().doubleValue() > maxPrice) {
				maxPrice = dataPlusStd.getYValue().doubleValue();
			}
			
			if(dataLessStd.getYValue().doubleValue() < minPrice) {
				minPrice = dataLessStd.getYValue().doubleValue();
			}
			if(dataLessStd.getYValue().doubleValue() > maxPrice) {
				maxPrice = dataLessStd.getYValue().doubleValue();
			}
			
			if(dataPlus2Std.getYValue().doubleValue() < minPrice) {
				minPrice = dataPlus2Std.getYValue().doubleValue();
			}
			if(dataPlus2Std.getYValue().doubleValue() > maxPrice) {
				maxPrice = dataPlus2Std.getYValue().doubleValue();
			}
			
			if(dataLess2Std.getYValue().doubleValue() < minPrice) {
				minPrice = dataLess2Std.getYValue().doubleValue();
			}
			if(dataLess2Std.getYValue().doubleValue() > maxPrice) {
				maxPrice = dataLess2Std.getYValue().doubleValue();
			}
		}
		
		long endPrediction = LocalDate.now().plusDays(100).toEpochDay();
		for(int i = (int) xpts[(int) m - 1] + 1; i < endPrediction; i++) {
			var dataPoint = new XYChart.Data<Number, Double>(i, a0 + a1 * i);
			var dataPlusStd = new XYChart.Data<Number, Double>(i, a0 + a1 * i + std);
			var dataLessStd = new XYChart.Data<Number, Double>(i, a0 + a1 * i - std);
			var dataPlus2Std = new XYChart.Data<Number, Double>(i, a0 + a1 * i + 2 * std);
			var dataLess2Std = new XYChart.Data<Number, Double>(i, a0 + a1 * i - 2 * std);
			prediction.getData().add(dataPoint);
			predPlusStd.getData().add(dataPlusStd);
			predLessStd.getData().add(dataLessStd);
			predPlus2Std.getData().add(dataPlus2Std);
			predLess2Std.getData().add(dataLess2Std);
			
			if(dataPoint.getYValue().doubleValue() < minPrice) {
				minPrice = dataPoint.getYValue().doubleValue();
			}
			if(dataPoint.getYValue().doubleValue() > maxPrice) {
				maxPrice = dataPoint.getYValue().doubleValue();
			}
			
			if(dataPlusStd.getYValue().doubleValue() < minPrice) {
				minPrice = dataPlusStd.getYValue().doubleValue();
			}
			if(dataPlusStd.getYValue().doubleValue() > maxPrice) {
				maxPrice = dataPlusStd.getYValue().doubleValue();
			}
			
			if(dataLessStd.getYValue().doubleValue() < minPrice) {
				minPrice = dataLessStd.getYValue().doubleValue();
			}
			if(dataLessStd.getYValue().doubleValue() > maxPrice) {
				maxPrice = dataLessStd.getYValue().doubleValue();
			}
			
			if(dataPlus2Std.getYValue().doubleValue() < minPrice) {
				minPrice = dataPlus2Std.getYValue().doubleValue();
			}
			if(dataPlus2Std.getYValue().doubleValue() > maxPrice) {
				maxPrice = dataPlus2Std.getYValue().doubleValue();
			}
			
			if(dataLess2Std.getYValue().doubleValue() < minPrice) {
				minPrice = dataLess2Std.getYValue().doubleValue();
			}
			if(dataLess2Std.getYValue().doubleValue() > maxPrice) {
				maxPrice = dataLess2Std.getYValue().doubleValue();
			}
		}
		
		dataChart.getData().add(prediction);
		dataChart.getData().add(predPlusStd);
		dataChart.getData().add(predLessStd);
		dataChart.getData().add(predPlus2Std);
		dataChart.getData().add(predLess2Std);

		predPlusStd.getNode().getStyleClass().add("std-line-inner");
		predLessStd.getNode().getStyleClass().add("std-line-inner");
		
		predPlus2Std.getNode().getStyleClass().add("std-line-outer");
		predLess2Std.getNode().getStyleClass().add("std-line-outer");
		
		cleanDataLines();
		
		startDatePicker.setValue(LocalDate.ofEpochDay(earliest));
		endDatePicker.setValue(LocalDate.now().plusDays(100));
		updateDateRange(null);
		
		double priceRange = maxPrice - minPrice;
		double padding = priceRange * 0.1;
		yAxis.setAutoRanging(false);
		yAxis.setLowerBound(Math.round(minPrice - padding));
		yAxis.setUpperBound(Math.round(maxPrice + padding));
		yAxis.setTickUnit(priceRange / 10);
		
		startDatePicker.setDisable(true);
		endDatePicker.setDisable(true);
		resetDateButton.setDisable(true);
		editChartButton.setDisable(true);
	
		isLocked = true;
	}
	
	public boolean isLocked() {
		return isLocked;
	}
	
	private Task<Void> refreshTrackedSymbols() {
		// Remove untracked symbols from the data chart
		var iter = dataChart.getData().iterator();
		while(iter.hasNext()) {
			if(!symbolsTracked.contains(iter.next().getName())) {
				iter.remove();
				continue;
			}
		}
		
		// Task for pulling data from database
		var task = new Task<Void>() {
			@Override
			protected Void call() {
				Platform.runLater(()->{
					dataChart.getScene().setCursor(Cursor.WAIT);
				});
				
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
					Platform.runLater(()->{
						XYChart.Series<Number, Double> pricePoints = new XYChart.Series<>();
						pricePoints.setName(symbol);
						for(PriceData point : priceData) {
							long date = point.date.toEpochDay();
							var dataPoint = new XYChart.Data<Number, Double>(date, point.adjClose);
							pricePoints.getData().add(dataPoint);
						}
						
						dataChart.getData().add(pricePoints);
					});					
				}
				
				// Re-add tooltips to all the points
				Platform.runLater(()->{
					cleanDataLines();					
					dataChart.getScene().setCursor(Cursor.DEFAULT);
				});
				return null;
			}
		};
		new Thread(task).start();
		return task;
	}
	
	private void cleanDataLines() {
		var iter = dataChart.getData().iterator();
		while(iter.hasNext()) {
			Series<Number, Double> series = iter.next();
			
			for(var point : series.getData()) {
				
				// Update style class (for visibility)
				point.getNode().getStyleClass().add("line-node");
				
				// Add tooltip
				LocalDate date = LocalDate.ofEpochDay(point.getXValue().longValue());
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
	
	public void addChartSymbol(String symbol) {
		symbolsTracked.add(symbol);
		refreshTrackedSymbols();
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

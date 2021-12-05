package stockfetcher.ui;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.SetChangeListener.Change;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.Pair;
import stockfetcher.api.CompanyData;
import stockfetcher.api.EtfData;
import stockfetcher.api.StockApi;
import stockfetcher.db.StockDatabase;

public class UIController {
	
	private static final NumberFormat SHARES_FORMAT = NumberFormat.getNumberInstance();
	
	@FXML private BorderPane root;
	
	@FXML private ListView<String> symbolList;
	private String selectedSymbol = null;
	private BooleanProperty isEtfSelected = new SimpleBooleanProperty(false);

	@FXML private TextField searchBar;
	@FXML private VBox actionList;
	private ArrayList<AppAction> availableActions = new ArrayList<>();
	private int selectedAction = -1;
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
		// Setup event handler
		root.addEventFilter(KeyEvent.KEY_PRESSED, new SearchFocusHandler());
		searchBar.addEventHandler(KeyEvent.KEY_PRESSED, new ActionSelectHanlder());
		
		// Prevent action list from eating mouse events for the chart
		actionList.setMaxHeight(Region.USE_PREF_SIZE);
		
		// Setup relevant lists
		updateSymbolList();
		updateHoldingInfo();
		
		// Add a chart tab
		createNewTab();
		
		// Setup the new tab button
		chartTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab)->{
			if(newTab == newTabButton) {
				createNewTab();
			}
		});
		
		// Setup the symbols context menus
		symbolList.setCellFactory(lv -> {
			ListCell<String> cell = new ListCell<>();
			
			MenuItem addToChart = new MenuItem("Add to Current Chart");
			addToChart.setOnAction(e -> {
				ChartController controller = (ChartController) chartTabs.getSelectionModel().getSelectedItem().getProperties().get("chartController");
				controller.addChartSymbol(cell.getItem());
			});
			
			MenuItem addNewChart = new MenuItem("Add to New Chart");
			addNewChart.setOnAction(e -> {
				createNewTab();
				ChartController controller = (ChartController) chartTabs.getSelectionModel().getSelectedItem().getProperties().get("chartController");
				controller.addChartSymbol(cell.getItem());
				controller.setChartName(cell.getItem());
			});
			
			MenuItem predict = new MenuItem("Predict");
			predict.setOnAction(e -> {
				PredictionDialog dialog = new PredictionDialog();
				Optional<Pair<LocalDate, LocalDate>> result = dialog.showAndWait();
				if(result.isPresent() && result.get() != null) {
					var dates = result.get();
					var begin = LocalDateTime.of(dates.getKey(), LocalTime.NOON);
					var end = LocalDateTime.of(dates.getValue(), LocalTime.NOON);
					
					if(java.time.Duration.between(begin, end).toDays() < 30) {
						Alert alert = new Alert(AlertType.ERROR, "Please select an interval of at least 30 days.", ButtonType.OK);
						alert.show();
					}
					else {
						createNewTab();
						ChartController controller = (ChartController) chartTabs.getSelectionModel().getSelectedItem().getProperties().get("chartController");
						controller.predict(cell.getItem(), dates.getKey(), dates.getValue());
					}
				}
			});
			
			ContextMenu menu = new ContextMenu(addToChart, addNewChart, predict);
			
			cell.textProperty().bind(cell.itemProperty());
			cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
				if(isNowEmpty) {
					cell.setContextMenu(null);
				}
				else {
					cell.setContextMenu(menu);
				}
			});
			
			return cell;
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
				peRatio.setText(companyData.get().peRatio == -1 ? "N/A" : String.valueOf(companyData.get().peRatio));
				sharesOutstanding.setText(companyData.get().sharesOutstanding == -1 ? "N/A" : SHARES_FORMAT.format(companyData.get().sharesOutstanding));
				companyDescription.setText(companyData.get().desc);
				companyName.setText(companyData.get().name);
				
				companyDataBox.setVisible(true);
				noDataMessage.setVisible(false);
			}
		});
		
		// Setup search bar
		searchBar.textProperty().addListener((obs, oldText, newText) -> {
			if(!oldText.trim().equals(newText.trim())) {
				searchInputChanged(newText, oldText.trim().equals(""));
			}
		});
		
		// Stock List & ETF List selection
		symbolList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			symbolSelected();
		});
		symbolList.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
			symbolSelected();
		});
		
		// Whenever price data is changed, update our etfs/stocks
		StockDatabase.trackedSymbolsProperty().addListener((Change<? extends String> c) -> {
			if(c.wasAdded()) {
				String added = c.getElementAdded();
				if(!symbolList.getItems().contains(added)) {
					symbolList.getItems().add(added);
					Collections.sort(symbolList.getItems());
				}
			}
			else if(c.wasRemoved()) {
				//TODO: removed
			}
		});
		
		// Add available actions
		availableActions.add(new SearchAction());
		availableActions.add(new AddNewSymbolAction());
	}
	
	private void updateSymbolList() {
		ArrayList<String> stocks = new ArrayList<>(StockDatabase.trackedSymbolsProperty());
		Collections.sort(stocks);
		symbolList.getItems().setAll(stocks);
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
		if(selectedSymbol != null) {
			companyData.set(StockDatabase.getCompanyData(selectedSymbol));
		}
	}
	
	private void createNewTab() {
		try {			
			FXMLLoader loader = new FXMLLoader(ClassLoader.getSystemResource("chart_layout.fxml"));
			VBox tabContents = loader.<VBox>load();
			ChartController chart = loader.getController();
			
			// Bindings for editing the tab name
			final Label tabLabel = new Label();
			tabLabel.setStyle("-fx-padding: 2 10 2 10");
			final TextField tabTitleEdit = new TextField();
			tabTitleEdit.getStyleClass().add("tab-title-edit");
			tabLabel.textProperty().bindBidirectional(tabTitleEdit.textProperty());
			tabLabel.textProperty().bindBidirectional(chart.chartNameProperty());
			
			// Setup tab
			Tab newTab = new Tab();
			newTab.setContent(tabContents);
			newTab.setGraphic(tabLabel);
			tabLabel.setOnMouseClicked(e -> {
				System.out.flush();
				if(e.getClickCount() == 2) {
					newTab.setGraphic(tabTitleEdit);
					tabTitleEdit.selectAll();
					tabTitleEdit.requestFocus();
				}
				else if(e.getButton() == MouseButton.MIDDLE) {
					chartTabs.getTabs().remove(newTab);
				}
			});
			
			tabTitleEdit.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
				if(!isFocused) {
					newTab.setGraphic(tabLabel);
				}
			});
			
			tabTitleEdit.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
				if(e.getCode() == KeyCode.ENTER) {
					root.requestFocus();
				}
			});
			
			// Add tab and select
			newTab.getProperties().put("chartController", chart);
			chartTabs.getTabs().add(chartTabs.getTabs().size() - 1, newTab);
			chartTabs.getSelectionModel().select(chartTabs.getTabs().size() - 2);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void symbolSelected() {
		String old = selectedSymbol;
		selectedSymbol = symbolList.getSelectionModel().getSelectedItem();
		if(old == null || !old.equals(selectedSymbol)) {
			if(StockDatabase.isEtfPresent(selectedSymbol)) {
				isEtfSelected.set(true);
				updateHoldingInfo();
			}
			else if(StockDatabase.isCompanyPresent(selectedSymbol)) {
				isEtfSelected.set(false);
				updateCompanyInfo();
			}
			else {
				isEtfSelected.set(false);
				updateCompanyInfo();
			}
		}
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
			
			Utils.downloadStockData(symbolsList.toArray(new String[0]));
		});
	}

	private void searchInputChanged(String newInput, boolean useTransition) {
		// If input empty, clear
		if(newInput.equals("")) {
			actionList.getChildren().clear();
			selectedAction = -1;
			return;
		}
		
		// Filter available actions
		var filteredActions = availableActions.stream()
				.filter(a -> a.isApplicable(newInput))
				.collect(Collectors.toList());

		// Create dropdown menu
		var dropdown = new ArrayList<TextField>();
		int pos = 0;
		for(AppAction a : filteredActions) {
			var tf = new TextField(a.getActionName() + ": " + newInput);
			tf.getStyleClass().add("available-action");
			if(pos == filteredActions.size() - 1) {
				tf.getStyleClass().add("available-action-bottom");
			}
			tf.getProperties().put("action", a);
			tf.setEditable(false);
			dropdown.add(tf);
			pos++;
		}
		
		// Check that there are applicable actions
		if(dropdown.size() > 0) {
			// Select first action if none selected previously
			// or if filter made previous selection invalid
			if(selectedAction == -1 || selectedAction >= dropdown.size()) {
				selectedAction = 0;
			}

			// Set style for selected element
			dropdown.get(selectedAction).getStyleClass().add("selected-action");
			
			// Set vbox contents
			actionList.getChildren().setAll(dropdown);
			if(useTransition) {
				FadeTransition transition = new FadeTransition(Duration.millis(200), actionList);
				transition.setFromValue(0);
				transition.setToValue(1);
				transition.setInterpolator(Interpolator.EASE_BOTH);
				transition.play();
			}
		}
		else {
			selectedAction = -1;
		}
		
	}
	
	@FXML
	private void refreshCompanyData(Event e) {
		CompanyData data = StockApi.getCompanyOverview(selectedSymbol);
		if(data == null) {
			// Just in case, check that the symbol isn't an etf
			EtfData etfData = StockApi.getEtfOverview(selectedSymbol);
			if(etfData != null) {
				// Actually was an etf
				StockDatabase.addEtfData(etfData);
				selectedSymbol = "";
				symbolSelected();
			}
			else {
				// Not an etf and company data couldn't be retrieved, so just warn the user
				Alert alert = new Alert(
					AlertType.ERROR,
					"There was a problem downloading the company data.\nThe API rate limit may have been exceeded. Please try again later."
				);
				alert.show();
			}
			return;
		}
		
		StockDatabase.addCompanyData(data);
		updateCompanyInfo();
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
	
	private class SearchFocusHandler implements EventHandler<KeyEvent> {

		@Override
		public void handle(KeyEvent event) {
			if(event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.P) {
				searchBar.requestFocus();
			}
			else if(event.getCode() == KeyCode.ESCAPE) {
				searchBar.clear();
				root.requestFocus();
			}
		}
		
	}
	
	private class ActionSelectHanlder implements EventHandler<KeyEvent> {
		
		@Override
		public void handle(KeyEvent event) {
			if(event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.TAB) {
				// Remove selected from previous action
				actionList.getChildren().get(selectedAction).getStyleClass().remove("selected-action");
				
				// Update selected action value
				selectedAction++;
				if(actionList.getChildren().size() == 0) {
					selectedAction = -1;
				}
				else if(selectedAction >= actionList.getChildren().size()) {
					selectedAction = 0;
				}
				
				// Add style class to new selection
				actionList.getChildren().get(selectedAction).getStyleClass().add("selected-action");
				
				event.consume();
			}
			else if(event.getCode() == KeyCode.UP || (event.isShiftDown() && event.getCode() == KeyCode.TAB)) {
				// Remove selected from previous action
				actionList.getChildren().get(selectedAction).getStyleClass().remove("selected-action");
				
				// Update selected action value
				selectedAction--;
				if(actionList.getChildren().size() == 0) {
					selectedAction = -1;
				}
				else if(selectedAction < 0) {
					selectedAction = actionList.getChildren().size() - 1;
				}
				
				// Add style class to new selection
				actionList.getChildren().get(selectedAction).getStyleClass().add("selected-action");
				
				event.consume();
			}
			else if(event.getCode() == KeyCode.ENTER) {
				// Get the action and run
				AppAction action = (AppAction) actionList.getChildren().get(selectedAction).getProperties().get("action");
				action.execute(searchBar.getText());
				
				// Clear search
				searchBar.clear();
				root.requestFocus();
			}
		}
		
	}
	
	private class SearchAction implements AppAction {

		@Override
		public String getActionName() {
			return "Search";
		}
		
		@Override
		public boolean isApplicable(String currentInput) {
			return !currentInput.trim().contains(" ");
		}

		@Override
		public void execute(String input) {
			input = input.trim().toUpperCase();
			if(symbolList.getItems().contains(input)) {
				symbolList.getSelectionModel().select(input);
				// TODO: show graph?
			}
			
			// symbol not found, see if the user wants to pull the data
			else {
				var alert = new LoadNonpresentSymbolDialog(input);
				alert.showAndDownload();
			}
		}
		
	}
	
	private class AddNewSymbolAction implements AppAction {

		@Override
		public String getActionName() {
			return "Track New Symbol";
		}

		@Override
		public boolean isApplicable(String currentInput) {
			return !currentInput.trim().contains(" ");
		}

		@Override
		public void execute(String input) {
			input = input.trim().toUpperCase();
			Utils.downloadStockData(input);
		}
		
	}
	
}




























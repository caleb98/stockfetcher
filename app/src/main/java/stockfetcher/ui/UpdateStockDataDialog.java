package stockfetcher.ui;

import java.util.ArrayList;
import java.util.Collections;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import stockfetcher.db.StockDatabase;

public class UpdateStockDataDialog extends Dialog<ArrayList<Pair<String, Boolean>>> {

	public UpdateStockDataDialog() {
		// Setup dialog
		setTitle("Update Data");
		setHeaderText("Select the symbols you would like to update data for.");
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		// Image displayed
		Label img = new Label();
		img.getStyleClass().addAll("choice-dialog", "dialog-pane");
		setGraphic(img);
		
		// Content
		VBox content = new VBox(5);
		GridPane updateRows = new GridPane();
		updateRows.setPadding(new Insets(5, 20, 5, 20));
		ScrollPane scroll = new ScrollPane(updateRows);
		scroll.setPrefViewportHeight(200);
		scroll.setFitToWidth(true);
		scroll.vbarPolicyProperty().setValue(ScrollBarPolicy.ALWAYS);
		
		Label help = new Label("The full option will refresh stock data for the entire stock history."
				+ " Recent will pull only the data from the previous 100 days.");
		help.setMaxWidth(400);
		help.setWrapText(true);
		
		Button allFull = new Button("Full Update All");
		Button allRecent = new Button("Recent Update All");
		HBox buttons = new HBox(5, allFull, allRecent);
		allFull.prefWidthProperty().bind(buttons.widthProperty().divide(2));
		allRecent.prefWidthProperty().bind(buttons.widthProperty().divide(2));
		
		content.getChildren().addAll(help, buttons, scroll);
		getDialogPane().setContent(content);
		
		ColumnConstraints symbolCol = new ColumnConstraints();
		symbolCol.setFillWidth(true);
		symbolCol.setHgrow(Priority.ALWAYS);
		ColumnConstraints radioCol = new ColumnConstraints();
		radioCol.setMinWidth(100);
		
		updateRows.getColumnConstraints().addAll(symbolCol, radioCol, radioCol);
		
		ArrayList<String> symbols = new ArrayList<>(StockDatabase.trackedSymbolsProperty());
		Collections.sort(symbols);
		
		final ArrayList<Pair<String, ToggleGroup>> selections = new ArrayList<>();
		final ArrayList<RadioButton> fullButtons = new ArrayList<>();
		final ArrayList<RadioButton> recentButtons = new ArrayList<>();
		
		for(int i = 0; i < symbols.size(); i++) {
			String symbol = symbols.get(i);
			Label label = new Label(symbol);
			
			ToggleGroup updateType = new ToggleGroup();
			RadioButton fullUpdate = new RadioButton("Full");
			RadioButton recentUpdate = new RadioButton("Recent");
			fullUpdate.setToggleGroup(updateType);
			recentUpdate.setToggleGroup(updateType);
			
			updateRows.add(label, 0, 2 * i);
			updateRows.add(fullUpdate, 1, 2 * i);
			updateRows.add(recentUpdate, 2, 2 * i);
			
			if(i != symbols.size() - 1) {
				Separator sep = new Separator();
				sep.setPadding(new Insets(5, 0, 3, 0));
				updateRows.add(sep, 0, 2 * i + 1, 3, 1);
			}
			
			selections.add(new Pair<>(symbol, updateType));
			fullButtons.add(fullUpdate);
			recentButtons.add(recentUpdate);
		}
		
		allFull.setOnAction(e -> {
			for(var b : fullButtons) {
				b.setSelected(true);
			}
		});
		
		allRecent.setOnAction(e -> {
			for(var b : recentButtons) {
				b.setSelected(true);
			}
		});
		
		setResultConverter((buttonType) -> {
			if(buttonType == ButtonType.OK) {
				ArrayList<Pair<String, Boolean>> updates = new ArrayList<>();
				for(var selection : selections) {
					RadioButton rb = (RadioButton) selection.getValue().getSelectedToggle();
					if(rb != null) {
						updates.add(new Pair<>(selection.getKey(), rb.getText().equals("Full")));
					}
				}
				return updates;
			}
			else {
				return null;
			}
		});
	}
	
}

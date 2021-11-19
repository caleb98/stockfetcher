package stockfetcher.ui;

import java.util.ArrayList;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class AddTrackedStockDialog extends Dialog<ArrayList<String>> {

	public AddTrackedStockDialog() {
		// Setup dialog
		setTitle("New Tracked Symol");
		setHeaderText("Enter the new symbol(s) you would like to track.\n"
				+ "Multiple symbols should be separated with a space");
		
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		Label img = new Label();
		img.getStyleClass().addAll("choice-dialog", "dialog-pane");
		setGraphic(img);
		
		// Setup content
		GridPane grid = new GridPane();
		grid.setHgap(5);
		grid.setVgap(5);
		
		TextField symbolsField = new TextField();
		grid.add(new Label("Symbols:"), 0, 0);
		grid.add(symbolsField, 1, 0);
		
		ColumnConstraints left = new ColumnConstraints();
		ColumnConstraints right = new ColumnConstraints();
		
		right.setFillWidth(true);
		right.setHgrow(Priority.ALWAYS);
		
		grid.getColumnConstraints().addAll(left, right);
		
		getDialogPane().setContent(grid);
		
		setResultConverter((buttonType) -> {
			if(buttonType == ButtonType.OK) {
				ArrayList<String> newSymbols = new ArrayList<>();
				
				String[] symbols = symbolsField.getText().split(",");
				for(String s : symbols) {
					newSymbols.add(s.trim().toUpperCase());
				}
				
				return newSymbols;
			}
			else {
				return null;
			}
		});
		
		Platform.runLater(symbolsField::requestFocus);
	}
	
	
	
}

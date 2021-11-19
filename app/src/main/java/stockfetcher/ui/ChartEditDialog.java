package stockfetcher.ui;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Pair;

public class ChartEditDialog extends Dialog<Pair<String, String>> {

	public ChartEditDialog(String chartName, String symbols) {
		setTitle("Update Chart");
		setHeaderText("Enter new chart name and symbols tracked.");
		
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		TextField nameField = new TextField(chartName);
		TextField symbolsField = new TextField(symbols);
		
		GridPane grid = new GridPane();
		grid.setHgap(5);
		grid.setVgap(5);
		
		grid.add(new Label("Chart Name:"), 0, 0);
		grid.add(nameField, 1, 0);
		grid.add(new Label("Symbols:"), 0, 1);
		grid.add(symbolsField, 1, 1);
		
		ColumnConstraints left = new ColumnConstraints();
		ColumnConstraints right = new ColumnConstraints();
		
		right.setFillWidth(true);
		right.setHgrow(Priority.ALWAYS);
		
		grid.getColumnConstraints().addAll(left, right);
		
		getDialogPane().setContent(grid);
		
		setResultConverter((buttonType) -> {
			if(buttonType == ButtonType.OK) {	
				return new Pair<String, String>(nameField.getText(), symbolsField.getText());
			}
			else {
				return null;
			}
		});
		

		Platform.runLater(nameField::requestFocus);
	}
	
}

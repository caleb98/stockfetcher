package stockfetcher.ui;

import java.time.LocalDate;

import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Pair;

public class PredictionDialog extends Dialog<Pair<LocalDate, LocalDate>> {

	public PredictionDialog() {
		// Setup dialog
		setTitle("Predcict");
		setHeaderText("Choose Prediction Date");
		
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		Label img = new Label();
		img.getStyleClass().addAll("choice-dialog", "dialog-pane");
		setGraphic(img);
		
		// Setup content
		GridPane grid = new GridPane();
		grid.setHgap(5);
		grid.setVgap(5);
		
		Label helpLabel = new Label("Select the date interval you would like to use for the prediction. "
				+ "Longer intervals may produce less useful data predictions, but "
				+ "too short of an interval will produce less accurate predictions. "
				+ "Interval should be at least 30 days.");
		helpLabel.setWrapText(true);
		helpLabel.setMaxWidth(300);
		
		grid.add(helpLabel, 0, 0, 2, 1);
	
		final DatePicker begin = new DatePicker(LocalDate.of(2000, 1, 1));
		final DatePicker end = new DatePicker(LocalDate.now());
		
		grid.add(new Label("From:"), 0, 1);
		grid.add(begin, 1, 1);
		
		grid.add(new Label("Fromn"), 0, 2);
		grid.add(end, 1, 2);
		
		ColumnConstraints left = new ColumnConstraints();
		ColumnConstraints right = new ColumnConstraints();
		
		right.setFillWidth(true);
		right.setHgrow(Priority.ALWAYS);
		
		grid.getColumnConstraints().addAll(left, right);
		
		getDialogPane().setContent(grid);
		
		setResultConverter((buttonType) -> {
			if(buttonType == ButtonType.OK) {
				return new Pair<>(begin.getValue(), end.getValue());
			}
			else {
				return null;
			}
		});
	}
	
}

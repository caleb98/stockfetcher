package stockfetcher.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class ProgressDialog extends Dialog<Void> {

	private ProgressBar progressBar = new ProgressBar();
	private Label progressInfo = new Label();
	
	public ProgressDialog(String initialInfo) {
		setTitle("Progress");
		
		// Setup the content
		VBox content = new VBox();
		progressBar = new ProgressBar();
		progressBar.setPrefWidth(400);
		
		progressInfo = new Label(initialInfo);
		
		content.getChildren().addAll(progressBar, progressInfo);
		getDialogPane().setContent(content);
	}
	
	public void setProgress(double progress) {
		progressBar.setProgress(progress);
	}
	
	public double getProgress() {
		return progressBar.getProgress();
	}
	
	public DoubleProperty progressProperty() {
		return progressBar.progressProperty();
	}
	
	public void setInfo(String info) {
		progressInfo.setText(info);
	}
	
	public String getInfo() {
		return progressInfo.getText();
	}
	
	public StringProperty infoProperty() {
		return progressInfo.textProperty();
	}
	
}

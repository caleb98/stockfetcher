package stockfetcher;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import stockfetcher.ui.UIController;

public class StockfetcherApp extends Application {

	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		FXMLLoader loader = new FXMLLoader(ClassLoader.getSystemResource("layout.fxml"));
		
		BorderPane pane = loader.<BorderPane>load();
		Scene scene = new Scene(pane);
		
		UIController control = loader.getController();
		control.test();
		
		stage.setTitle("Stockfetcher");
		stage.setScene(scene);
		stage.show();
	}

}

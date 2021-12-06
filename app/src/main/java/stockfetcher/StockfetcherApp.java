package stockfetcher;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import stockfetcher.db.StockDatabase;

public class StockfetcherApp extends Application {

	private static final Logger logger = LoggerFactory.getLogger(StockfetcherApp.class);
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		// Initialize the database
		try {
			StockDatabase.initialize();
		} catch(SQLException e) {
			logger.error("Unable to initialize stock database: {}", e.getMessage());
			System.exit(-1);
		}
		
		// Load the UI 
		FXMLLoader loader = new FXMLLoader(ClassLoader.getSystemResource("app_layout.fxml"));
		
		BorderPane pane = loader.<BorderPane>load();
		Scene scene = new Scene(pane);
		
		// Setup and show window
		stage.setTitle("Stockfetcher");
		stage.setScene(scene);
		stage.setMinWidth(850);
		stage.setMinHeight(600);
		stage.show();
	}

}

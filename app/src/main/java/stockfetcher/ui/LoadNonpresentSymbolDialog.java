package stockfetcher.ui;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

public class LoadNonpresentSymbolDialog extends Alert {

	private static final ButtonType YES = new ButtonType("Yes", ButtonData.OK_DONE);
	private static final ButtonType NO  = new ButtonType("No",  ButtonData.CANCEL_CLOSE);
	
	private String symbol;
	
	public LoadNonpresentSymbolDialog(String symbol) {
		super(
			AlertType.INFORMATION,
			"Data for the symbol " + symbol + " could not be found in the database.\nWould you like to download it now?",
			YES, NO
		);
		
		this.symbol = symbol;
		setTitle("Warning");
		setHeaderText("Symbol Not Found");
	}
	
	public boolean showAndDownload() {
		Optional<ButtonType> result = showAndWait();
		if(result.isPresent() && result.get() == YES) {
			Utils.downloadStockData(symbol);
			return true;
		}
		else {
			return false;
		}
	}
	
}

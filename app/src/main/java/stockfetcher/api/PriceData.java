package stockfetcher.api;

import java.time.LocalDate;

public class PriceData {

	public String symbol;
	public LocalDate date;
	public double open;
	public double high;
	public double low;
	public double close;
	public double adjClose;
	public int volume;
	
	public PriceData(String symbol, LocalDate date, double open, double high, double low, double close, double adjClose, int volume) {
		this.symbol = symbol;
		this.date = date;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.adjClose = adjClose;
		this.volume = volume;
	}
	
}

package stockfetcher.api;

import java.util.HashMap;

public class EtfData {

	public final String symbol;
	public final String name;
	public final HashMap<String, Double> sectorWeightings = new HashMap<>();
	public final HashMap<String, Double> topHoldings = new HashMap<>();
	
	public EtfData(String symbol, String name) {
		this.symbol = symbol;
		this.name = name;
	}
	
}

package stockfetcher.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * API for retrieving company stock data. 
 * Implemented using the AlphaVantage api, 
 * Yahoo Finance, and marketwatch.com
 * 
 * @author Caleb Cassady
 */
public final class StockApi {
	
	private static final Logger logger = LoggerFactory.getLogger(StockApi.class);
	
	// Alphavantage API data
	private static final String API_KEY = "K6657SE3OT4SBIQU"; // alternative: PXZ12RS30X92UU2U
	private static final String API_ROOT = "https://www.alphavantage.co/query?";
	
	private static final Gson gson = new GsonBuilder()
			.create();
	
	private static final HttpClient client = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_2)
			.followRedirects(HttpClient.Redirect.NEVER)
			.connectTimeout(Duration.ofSeconds(20))
			.build();
	
	private StockApi() {}
	
	/**
	 * Returns the daily adjusted price data for a given stock.
	 * @param symbol stock symbol
	 * @param full true to fetch all existing data; false for last 100 data points
	 * @return 
	 */
	public static PriceData[] getStockPriceData(String symbol, boolean full) {
		logger.info("Requesting {} data for {}.", full ? "full" : "compact", symbol);
		
		long period1;
		long period2;
		
		if(full) {
			period1 = 0;
			period2 = System.currentTimeMillis() / 1000;
		}
		else {
			ZonedDateTime now = ZonedDateTime.now();
			ZonedDateTime past = now.minusDays(100);
			period1 = past.toEpochSecond();
			period2 = now.toEpochSecond();
		}
		
		// Build csv fetch url
		String etf = symbol;		
		String url = String.format(
				"https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%d&period2=%d&interval=1d&events=history&includeAdjustedClose=true", 
				etf,
				period1,
				period2
		);
		
		// Download csv data
		try {
			ArrayList<PriceData> priceData = new ArrayList<>();
			URL website = new URL(url);
			BufferedReader reader = new BufferedReader(new InputStreamReader(website.openStream()));
			reader.readLine(); // Skip header info
			String line;
			while((line = reader.readLine()) != null) {
				String[] data = line.split(",");
				String date = data[0];
				
				try {
					double open = Double.valueOf(data[1]);
					double high = Double.valueOf(data[2]);
					double low = Double.valueOf(data[3]);
					double close = Double.valueOf(data[4]);
					double adjClose = Double.valueOf(data[5]);
					int volume = Integer.valueOf(data[6]);
				
					priceData.add(new PriceData(symbol, LocalDate.parse(date), open, high, low, close, adjClose, volume));
				} catch (NumberFormatException e) {
					logger.warn("Unable to add {} data for date {}. Data invalid: {}", symbol, date, line);
				}
			}
			
			logger.info("Stock data for {} successfully downloaded.", symbol);
			return priceData.toArray(new PriceData[priceData.size()]);
		} catch (IOException e) {
			logger.error("Error retrieving data for {}: {}", symbol, e.getMessage());
			return null;
		}
	}
	
	/**
	 * Returns an overview for the ETF of the given symbol
	 * @param symbol
	 * @return etf data; null if error or if symbol isn't an etf
	 */
	public static EtfData getEtfOverview(String symbol) {
		logger.info("Requesting ETF overview for {}", symbol);
		// Make a request to marketwatch holdings page
		String url = String.format("https://www.marketwatch.com/investing/fund/%s/holdings", symbol.toLowerCase());
		Document doc;
		try {
			Connection conn = Jsoup.connect(url);
			doc = conn.get();
		} catch (IOException e) {
			logger.error("Error fetching {} ETF data: {}", symbol, e.getMessage());
			return null;
		}
		
		// Check that the document url is equal to the url we requested
		// If they aren't equal, we got redirected because the symbol
		// wasn't an etf
		if(!url.equals(doc.baseUri())) {
			logger.error("The symbol {} does not correspond to an ETF.", symbol);
			return null;
		}
		
		// Grab ETF name
		String etfName = doc.select(".company__name").get(0).text();
		
		// Create data object
		EtfData data = new EtfData(symbol, etfName);
		
		// Pull sector data
		try {
			Elements sectors = doc.getElementsByAttributeValue("aria-label", "sector allocation data table").get(0).select(".table__row");
			for(Element sector : sectors) {
				String sectorName = sector.child(0).text();
				double percent = Double.valueOf(sector.child(1).text().replace("%", ""));
				data.sectorWeightings.put(sectorName, percent);
			}
		} catch (IndexOutOfBoundsException e) {
			logger.warn("Unable to get sector data for {}", etfName);
		}
		
		// Pull top holding data
		try {
			Elements holdings = doc.select(".element.element--table.holdings").get(0).child(1).child(1).select(".table__row");
			for(Element holding : holdings) {
				String hSymbol = holding.child(1).text();
				double percent = Double.valueOf(holding.child(2).text().replace("%", ""));
				data.topHoldings.put(hSymbol, percent);
			}
		} catch (IndexOutOfBoundsException e) {
			logger.warn("Unable to get holding data for {}", etfName);
		}
		
		logger.info("ETF overview for {} ({}) retrieved successfully.", etfName, symbol);
		
		return data;
	}
	
	/**
	 * Request company information for a given stock symbol
	 * @param symbol
	 * @return company data; null if symbol does not represent a company
	 */
	public static CompanyData getCompanyOverview(String symbol) {
		logger.info("Requesting {} company info.", symbol);
		
		// Setup request parameters
		HashMap<String, String> params = new HashMap<>();
		params.put("function", "OVERVIEW");
		params.put("symbol", symbol);
		params.put("apikey", API_KEY);

		// Make the request
		HttpRequest request = getRequest(params);
		HttpResponse<String> response;
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			logger.error("Retrieving company data for {} failed: {}", symbol, e.getMessage());
			return null;
		}
		
		// Parse to json object
		JsonObject data = gson.fromJson(response.body(), JsonObject.class);
		
		// Check for error (this call returns empty object on error)
		if(data.entrySet().size() == 0) {
			logger.error("Retrieving company data for {} failed. Requested symbol likely does not exist.", symbol);
			return null;
		}
		
		// If it doesn't have a name/desc field, API calls exceeded
		else if(!data.has("Name") || !data.has("Description")) {
			logger.error("Retrieving company data for {} failed: {}", symbol, data.has("Note") ? data.get("Note") : "Returned data is incomplete.");
			return null;
		}
		
		// Data should be okay
		logger.info("Company info for {} retrieved successfully.", symbol);
		
		// Pull company data from Json
		String name = data.get("Name").getAsString();
		String desc = data.get("Description").getAsString();
		
		double peRatio = -1;
		if(data.has("PERatio") && !data.get("PERatio").getAsString().equals("None")) 
			peRatio = data.get("PERatio").getAsDouble();
		
		long sharesOutstanding = -1;
		if(data.has("SharesOutstanding") && !data.get("PERatio").getAsString().equals("None")) 
			sharesOutstanding = data.get("SharesOutstanding").getAsLong();
		
		long sharesFloat = -1;
		if(data.has("SharesFloat") && !data.get("SharesFloat").getAsString().equals("None"))
			sharesFloat = data.get("SharesFloat").getAsLong();
		
		long sharesShort = -1;
		if(data.has("ShareShort") && !data.get("ShareShort").getAsString().equals("None"))
			sharesShort = data.get("SharesShort").getAsLong();
		
		return new CompanyData(symbol, name, desc, peRatio, sharesOutstanding, sharesFloat, sharesShort);
	}
	
	/**
	 * Checks if a given symbol represents a company. 
	 * @param symbol
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static boolean isSymbolCompany(String symbol) throws IOException, InterruptedException {
		// Symbols starting with ^ are indexes, so skip these
		if(symbol.startsWith("^"))
			return false;
		
		// Setup request parameters
		HashMap<String, String> params = new HashMap<>();
		params.put("function", "OVERVIEW");
		params.put("symbol", symbol);
		params.put("apikey", API_KEY);
		
		// Make request
		HttpRequest request = getRequest(params);
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		
		// Parse json
		JsonObject data = gson.fromJson(response.body(), JsonObject.class);
		
		// If the object is not empty, then it is a company
		return data.entrySet().size() != 0;
	}
	
	/**
	 * Checks if a given symbol represents an ETF.
	 * @param symbol
	 * @return
	 * @throws IOException 
	 */
	public static boolean isSymbolETF(String symbol) throws IOException {
		// Symbols starting with ^ are indexes, so skip these
		if(symbol.startsWith("^"))
			return false;
		
		// Make a request to marketwatch holdings page
		String url = String.format("https://www.marketwatch.com/investing/fund/%s/holdings", symbol.toLowerCase());
		Document doc = Jsoup.connect(url).get();
		
		// Check that the document url is equal to the url we requested
		// If they aren't equal, we got redirected because the symbol
		// wasn't an etf (this is a jank way to do this but it works.)
		return url.equals(doc.baseUri());
	}
	
	/**
	 * Helper method for converting a GET parameters map into
	 * a URI that can be used to make an api call
	 * @param params parameters map
	 * @return request with the given parameters
	 */
	private static HttpRequest getRequest(Map<String, String> params) {
		StringBuilder uriPath = new StringBuilder(API_ROOT);
		String[] paramNames = params.keySet().toArray(new String[0]);
		
		// Look through parameters and add them to the string
		for(int i = 0; i < paramNames.length; i++) {
			uriPath.append(String.format("%s=%s", 
					paramNames[i],
					params.get(paramNames[i])
			));
			
			if(i != paramNames.length - 1) {
				uriPath.append('&');
			}
		}
		
		URI uri = URI.create(uriPath.toString());
		
		// Create the actual request
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(uri)
				.build();
		
		return request;
	}
	
}

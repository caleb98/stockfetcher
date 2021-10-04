package stockfetcher.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.DateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * API for retrieving company stock data. 
 * Implemented using the AlphaVantage api:
 * https://www.alphavantage.co/
 * 
 * @author Caleb Cassady
 */
public final class StockApi {
	
	private static final Logger logger = LoggerFactory.getLogger(StockApi.class);
	
	// API Data
	private static final String API_KEY = "PXZ12RS30X92UU2U";
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
	 * Returns the daily adjusted stock values for a given stock.
	 * @param symbol the stock ticker
	 * @param full whether or not to retrieve full history or only last 100 data points
	 * @return JsonObject representing the stock data; null if api error occurred
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static PriceData[] dailyAdjusted(String symbol, boolean full) {
		logger.info("Requesting {} stock data for {}.", full ? "full" : "compact", symbol);
		
		// Setup request parameters
		HashMap<String, String> params = new HashMap<>();
		params.put("function", "TIME_SERIES_DAILY_ADJUSTED");
		params.put("symbol", symbol);
		params.put("apikey", API_KEY);
		
		if(full) {
			params.put("outputsize", "full");
		}
		
		// Make the request
		HttpRequest request = getRequest(params);
		HttpResponse<String> response;
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			logger.error("Retrieving data for {} failed: {}", symbol, e.getMessage());
			return null;
		}
		
		// Parse to json object
		JsonObject data = gson.fromJson(response.body(), JsonObject.class);

		// Check for error
		if(data.has("Error Message")) {
			logger.error("Retrieving data for {} failed: {}", symbol, data.get("Error Message").getAsString());
			return null;
		}
		else {
			logger.info("Stock data for {} retrieved successfully.", symbol);
		}
		
		// Pull price data from Json
		data = data.get("Time Series (Daily)").getAsJsonObject();
		PriceData[] priceData = new PriceData[data.entrySet().size()];
		int index = 0;
		for(Entry<String, JsonElement> entry : data.entrySet()) {
			LocalDate date = LocalDate.parse(entry.getKey());
			
			JsonObject dataObj = entry.getValue().getAsJsonObject();
			double open = dataObj.get("1. open").getAsDouble();
			double high = dataObj.get("2. high").getAsDouble();
			double low = dataObj.get("3. low").getAsDouble();
			double close = dataObj.get("4. close").getAsDouble();
			double adjClose = dataObj.get("5. adjusted close").getAsDouble();
			int volume = dataObj.get("6. volume").getAsInt();
			
			priceData[index] = new PriceData(symbol, date, open, high, low, close, adjClose, volume);
			
			index++;
		}
		
		return priceData;
	}
	
	public static CompanyData companyOverview(String symbol) throws IOException, InterruptedException {
		logger.info("Requesting {} company info.", symbol);
		
		// Setup request parameters
		HashMap<String, String> params = new HashMap<>();
		params.put("function", "OVERVIEW");
		params.put("symbol", symbol);
		params.put("apikey", API_KEY);
		
		// Make the request
		HttpRequest request = getRequest(params);
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		
		// Parse to json object
		JsonObject data = gson.fromJson(response.body(), JsonObject.class);
		
		// Check for error (this call returns empty object on error)
		if(data.entrySet().size() == 0) {
			logger.error("Retrieving company data for {} failed. Requested symbol likely does not exist.", symbol);
		}
		else {
			logger.info("Company info for {} retrieved successfully.", symbol);
		}
		
		// Pull company data from Json
		String name = data.get("Name").getAsString();
		String desc = data.get("Description").getAsString();
		double peRatio = data.get("PERatio").getAsDouble();
		long sharesOutstanding = data.get("SharesOutstanding").getAsLong();
		long sharesFloat = data.get("SharesFloat").getAsLong();
		long sharesShort = data.get("SharesShort").getAsLong();
		
		return new CompanyData(symbol, name, desc, peRatio, sharesOutstanding, sharesFloat, sharesShort);
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

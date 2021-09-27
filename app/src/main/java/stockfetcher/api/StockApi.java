package stockfetcher.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class StockApi {
	
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
	
	/**
	 * Returns the daily adjusted stock values for a given stock.
	 * @param symbol the stock ticker
	 * @param full whether or not to retrieve full history or only last 100 data points
	 * @return JsonObject representing the stock data; null if api error occurred
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static JsonObject dailyAdjusted(String symbol, boolean full) throws IOException, InterruptedException {
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
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		
		// Parse to json object
		JsonObject data = gson.fromJson(response.body(), JsonObject.class);

		// Check for error
		if(data.has("Error Message")) {
			logger.error("Retrieving data for {} failed: {}", symbol, data.get("Error Message").getAsString());
			return null;
		}
		else {
			logger.info("Data for {} retrieved successfully.", symbol);
		}
		
		return data;
	}
	
	public static JsonObject companyOverview(String symbol) throws IOException, InterruptedException {
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
		
		return data;
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

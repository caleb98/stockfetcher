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
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * API for retrieving company stock data. 
 * Implemented using the AlphaVantage api 
 * and Yahoo Finance.
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
	 * Returns the daily adjusted stock values for a given company stock.
	 * @param symbol the stock ticker
	 * @param full whether or not to retrieve full history or only last 100 data points
	 * @return JsonObject representing the stock data; null if api error occurred
	 */
	public static PriceData[] dailyAdjustedCompany(String symbol, boolean full) {
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
	
	/**
	 * Returns the daily adjusted price data for a given etf.
	 * @param symbol etf symbol
	 * @param full true to fetch all existing data; false for last 100 data points
	 * @return 
	 */
	public static PriceData[] dailyAdjustedEtf(String symbol, boolean full) {
		logger.info("Requesting {} ETF data for {}.", full ? "full" : "compact", symbol);
		
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
					logger.warn("Unable to add {} ETF data for date {}: {}", symbol, date, e.getMessage());
				}
			}
			
			logger.info("ETF data for {} successfully downloaded.", symbol);
			return priceData.toArray(new PriceData[priceData.size()]);
		} catch (IOException e) {
			logger.error("Error retrieving ETF data for {}: {}", symbol, e.getMessage());
		}
	
		return null;
	}
	
	/**
	 * Request company information for a given stock symbol
	 * @param symbol
	 * @return company data; null if symbol does not represent a company
	 */
	public static CompanyData companyOverview(String symbol) {
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
	 * Checks if a given symbol represents a company. 
	 * @param symbol
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static boolean isSymbolCompany(String symbol) throws IOException, InterruptedException {
		logger.info("Checking if {} is a company stock symbol.", symbol);
		
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

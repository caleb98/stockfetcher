package stockfetcher.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API for retrieving ETF data.
 * Implemented using Yahoo Finance by downloading
 * their historical data CSVs.
 * 
 * @author Caleb Cassady
 */
public class EtfApi {
	
	private static final Logger logger = LoggerFactory.getLogger(EtfApi.class);

	public static PriceData[] dailyAdjusted(String symbol, boolean full) {
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
	
}

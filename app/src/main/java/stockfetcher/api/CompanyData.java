package stockfetcher.api;

public class CompanyData {

	public final String symbol;
	public final String name;
	public final String desc;
	public final double peRatio;
	public final long sharesOutstanding;
	public final long sharesFloat;
	public final long sharesShort;
	
	public CompanyData(String symbol, String name, String desc, double peRatio, long sharesOutstanding, long sharesFloat, long sharesShort) {
		this.symbol = symbol;
		this.name = name;
		this.desc = desc;
		this.peRatio = peRatio;
		this.sharesOutstanding = sharesOutstanding;
		this.sharesFloat = sharesFloat;
		this.sharesShort = sharesShort;
	}
	
}

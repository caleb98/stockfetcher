package stockfetcher.ui;

public interface AppAction {

	/**
	 * @return name of this action as should be displayed to the end-user
	 */
	public String getActionName();
	
	/**
	 * Returns true or false depending on whether the given current input
	 * is applicable for calling this action.
	 * @param currentInput current input in the search bar
	 * @return
	 */
	public boolean isApplicable(String currentInput);
	
	/**
	 * Executes this action with the given input.
	 * @param input
	 */
	public void execute(String input);
	
}

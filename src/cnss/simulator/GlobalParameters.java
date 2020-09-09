package cnss.simulator;

import java.util.HashMap;
import java.util.Map;

public class GlobalParameters {

	private Map<String, String> vars;

	/**
	 * <code>GlobalVars</code> constructor, maintains a map of global variables
	 */
	public GlobalParameters() {
		vars = new HashMap<>();
	}

	/**
	 * Inserts a new variable in the map
	 * 
	 * @param name  of the variable
	 * @param value its value
	 */
	public void put(String name, String value) {
		if (vars.get(name) != null) {
			System.err.println("Global vars map warning: double specification of var " + name);
		}
		vars.put(name, value);
	}

	/**
	 * Gets the value of a variable from the map
	 * 
	 * @param name of the variable
	 * @return value its value
	 */
	public String get(String name) {
		if (vars.get(name) == null) {
			System.err.println("Global vars map warning: undefined var " + name);
		}
		return vars.get(name);
	}

	/**
	 * Generic toString method returning the contents of the mapping.
	 * 
	 * @return String
	 */
	public String toString() {
		return vars.toString();
	}

}

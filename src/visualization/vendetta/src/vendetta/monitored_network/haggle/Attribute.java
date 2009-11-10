/* Haggle testbed
 * Uppsala University
 *
 * Haggle internal release
 *
 * Copyright Haggle
 */

package vendetta.monitored_network.haggle;

import vendetta.util.log.Log;

/**
 */
public class Attribute {
	private static Log LOG = Log.getInstance("Attribute");
	
	private String name;
	private String value;
	
	Attribute(String name, String value)
	{
		this.name = name;
		this.value = value;
	}
	
	String getName()
	{
		return name;
	}
	
	String getValue()
	{
		return value;
	}
}

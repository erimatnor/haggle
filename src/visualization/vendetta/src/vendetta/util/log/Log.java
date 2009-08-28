/* Copyright (c) 2008 Uppsala Universitet.
 * All rights reserved.
 * 
 * This file is part of Vendetta.
 *
 * Vendetta is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Vendetta is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vendetta.  If not, see <http://www.gnu.org/licenses/>.
 */

package vendetta.util.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A logging facility to enable uniform logging within the Vendetta monitor.
 * 
 * This logging facility is meant for reporting run-time information, such as exceptions,
 * abnormal conditions or debug output. It is *NOT* meant for logging data from the
 * observed network.
 * 
 * All Log instances share the same set of `sinks'. A sink is an output for a 
 * log and is responsible for delivering the information to e.g. the terminal, or
 * a file, ...  
 * 
 * @version $Id: Log.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
public class Log {
	/**
	 * A log message has exactly one of these levels.
	 */
	public enum Level {
		DEBUG, INFO, WARN, ERROR
	}

	/**
	 * The singleton map of instances.
	 */
	private static final Map<String, Log> INSTANCES = new HashMap<String, Log>();
	
	/**
	 * A list of sinks the log messages are printed to.
	 */
	private static List<LogSink> outStreams = new ArrayList<LogSink>();
	
	/**
	 * The name of the log.
	 */
	private String name;
	
	static {
		outStreams.add(new StdoutSink());
	}
	
	/**
	 * Private constructor. Use getInstance() instead.
	 * 
	 * @param name
	 */
	private Log(String name) {
		this.name = name;
	}
	
	/**
	 * Singleton constructor for a log named "Vendetta".
	 * 
	 * @return A singleton log object.
	 */
	public static Log getInstance() {
		return getInstance("Vendetta");
	}
	
	/**
	 * Singleton constructor for a custom-named log.
	 * 
	 * @param name The name of the log.
	 * @return A singleton log object.
	 */
	public static Log getInstance(String name) {
		Log ret = INSTANCES.get(name);
		
		if (ret == null) {
			ret = new Log(name);
			INSTANCES.put(name, ret);
		}
		
		return ret;
	}
	
	/**
	 * Output a log message.
	 * 
	 * @param level The level of the log message.
	 * @param message The message itself.
	 */
	public synchronized void log(Level level, String message) {
		for (LogSink out : outStreams) {
			out.println(name, level, message);
		}
	}

	/**
	 * Add a sink to the log.
	 * 
	 * All log messages will subsequently be printed to this sink. 
	 * 
	 * @param out The LogSink to add.
	 */
	public static synchronized void addSink(LogSink out) {
		outStreams.add(out);
	}

	/**
	 * Remove a sink from the log.
	 * 
	 * @param out The sink Stream to remove.
	 */
	public static synchronized void removeSink(LogSink out) {
		outStreams.remove(out);
	}

	public void debug(String msg) {
		log(Level.DEBUG, msg);
	}
	
	public void info(String msg) {
		log(Level.INFO, msg);
	}
	
	public void warn(String msg) {
		log(Level.WARN, msg);
	}
	
	public void error(String msg) {
		log(Level.ERROR, msg);
	}
	
	public void warn(Exception e) {
		log(Level.WARN, e.getMessage());
	}
	
	public void error(String msg, Exception e) {
		log(Level.ERROR, msg + " " + e.getMessage() + " (" + e.getClass().getName() + ")");
	}
	
	public void error(Exception e) {
		log(Level.ERROR, e.getMessage() + " (" + e.getClass().getName() + ")");
	}	
}

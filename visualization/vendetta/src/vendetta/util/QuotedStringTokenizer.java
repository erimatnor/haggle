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

package vendetta.util;

import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * A helper class for tokenizing a string with respect to spaces and quotes.
 *
 * @version $Id$
 */
public class QuotedStringTokenizer {
	/**
	 * Delimiters for splitting tokens.
	 */
	private static final String DELIMITERS = " '";
	
	/**
	 * The list of tokens in str.
	 */
	private LinkedList<String> tokens = new LinkedList<String>();
	
	/**
	 * Java native tokenizer as helper.
	 */
	private StringTokenizer tokenizer;
	
	/**
	 * The string that will be tokenized.
	 */
	private final String str;
	
	/**
	 * Creates a tokenizer for the given string.
	 * 
	 * @param str The string that will split into tokens.
	 */
	public QuotedStringTokenizer(String str) {
		this.str = str;
		tokenize();
	}
	
	/**
	 * Split the strink into tokens.
	 */
	private void tokenize() {
		String nextDelimiter = DELIMITERS;
		boolean open = false;

		tokens.clear();
		tokenizer = new StringTokenizer(str, DELIMITERS, true);
		
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken(nextDelimiter);
			if (DELIMITERS.indexOf(token) != -1) {
				/* This is a delimiter.
				 */
				if ("'".equals(token)) {
					if (!open) {
						nextDelimiter = "'";
						open = true;
					} else {
						open = false;
					}
				}
			} else {
				/* This is a token.
				 */
				tokens.addLast(token);
				nextDelimiter = DELIMITERS;
			}
		}
		
		if (open) {
			System.err.println("Unclosed quote.");
		}
	}
	
	public boolean hasMoreTokens() {
		return !tokens.isEmpty();
	}
	
	public String nextToken() {
		return tokens.removeFirst();
	}
	
	public String nextToken(String delim) {
		throw new UnsupportedOperationException();
	}
	
	public int countTokens() {
		return tokens.size();
	}
	
	public String[] toArray() {
		String[] ret;
		
		tokenize();
		ret = new String[tokens.size()];
		
		for (int i=0;i<tokens.size();i++)
			ret[i] = tokens.get(i);
		
		return ret;
	}
}

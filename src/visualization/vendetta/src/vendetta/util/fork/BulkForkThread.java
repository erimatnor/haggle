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

package vendetta.util.fork;

import vendetta.util.log.Log;

/**
 * A helper class to fork a lot of processes.
 * 
 * An object of this class will always run at most MAX_PROCESSES at
 * the same time.
 * 
 * @todo Comment
 * @version $Id$
 */
public class BulkForkThread implements Runnable {
	private static final Log LOG = Log.getInstance("Bulk");
	private static final int MAX_PROCESSES = 10;
	
	private int TIMEOUT = 150000;
	private BulkForkWindow win;
	private String[] commands;
	private String[] hostnames;
	private Thread thread = null;

	/**
	 * Create a new BulkForkThread.
	 * 
	 * This implicitly starts execution of the commands.
	 * 
	 * @param win A window for reporting back progress.
	 * @param commands The commands to execute.
	 * @param hosts The names of the hosts on which the processes run.
	 * @param timeout A time-out after which to terminate a running process.
	 */
	public BulkForkThread(BulkForkWindow win, String[] commands, String[] hosts,
			int timeout) {
		this.win = win;
		this.commands = commands;
		this.hostnames = hosts;
		if (timeout > 0)
			TIMEOUT = timeout;
		
		thread = new Thread(this, "ForkThread");
		thread.start();
	}

	/**
	 * Fork the processes and wait for termination.
	 */
	public void run() {
		int started = 0, done = 0, to_do = commands.length;

		int max_num_p = (to_do > MAX_PROCESSES ? MAX_PROCESSES : to_do);
		int failed = 0;
		Process[] P = new Process[max_num_p];
		String[] current = new String[max_num_p];
		long[] timeouts = new long[max_num_p];

		/* Loop untill all commands are executed
		 */
		while (done < to_do) {
			int free_p = -1;
			/* Can and want we to start a new process?
			 */
			if (started < to_do) {
				for (int p = 0; p < P.length; p++) {
					if (P[p] == null) {
						free_p = p;
						break;
					}
				}
				if (free_p != -1) {
					String[] command = commands[started].split(" ");
					LOG.debug("Started " + commands[started]);
					current[free_p] = hostnames[started];
					started++;
					try {
						timeouts[free_p] = System.currentTimeMillis() + TIMEOUT;
						P[free_p] = Runtime.getRuntime().exec(command);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			/* Give the processes some time befor we again check for termination.
			 */
			try {
				Thread.sleep(50);
			} catch (InterruptedException ie) {	}

			/* Check if we have any finished processes
			 */
			for (int p = 0; p < P.length; p++) {
				int exit = 0;
				if (P[p] != null) {
					/* Try and see if it's finished
					 */
					try {
						/* Timeout?
						 */
						if (timeouts[p] < System.currentTimeMillis()) {
							LOG.warn("TIMEOUT: " + current[p]);
							P[p].destroy();
						}
						exit = P[p].exitValue();
						P[p] = null;
						done++;
						win.set_bar(done, current[p]);
						if (exit != 0) {
							failed++;
							LOG.error("error-exit: " + exit);
							win.add_failed();
						}
					} catch (Exception e) {
						/* No it was not
						 */
					}
				}
			}
		}

		win.set_bar(-1, "Done");
		if (failed > 0)
			LOG.info("Finished. " + failed + " failed.");
		else
			LOG.info("Finished.");

	}
}

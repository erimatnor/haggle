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

package vendetta.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;

import vendetta.Vendetta;
import vendetta.util.log.Log;

/**
 * A class for a downstream connection from the proxy.
 * 
 * An object of this class will connect to the proxy at the given address
 * and read any vclient log events from the proxy. If you do not use
 * a proxy, but connect directly to vclient instances, TCPDownDirect
 * will be used instead.
 * 
 * @see TCPDownDirect
 * @version $Id: TCPDownStatic.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
public class TCPDownStatic implements TCPDown {
	private static Log LOG = Log.getInstance("TCPDown");
	
	private String incomplete = null;
	private boolean hadIncompleteLine = false;
	public SocketChannel channel = null;

	/**
	 * Create a new downstream connection to a proxy.
	 * 
	 * This will connect to the proxy.
	 * 
	 * @param host The proxy's host name.
	 * @param port The proxy's port.
	 * @throws IOException
	 */
	public TCPDownStatic(String host, int port) throws IOException {
		InetSocketAddress addr = new InetSocketAddress(host, port);
		channel = SocketChannel.open(addr);
		LOG.debug("Connected to middleware ...");
	}

	/**
	 * The thread's main loop reads from the proxy connection and
	 * passes log events on to Vendetta.
	 */
	public void run() {
		ByteBuffer inBuf = ByteBuffer.allocate(512);

		/* As long as we're connected, read from the channel.
		 * If the connection is closed by the close() method,
		 * the loop and thus also the thread will terminate.
		 */
		while (channel.isConnected()) {
			try {
				inBuf.clear();
				channel.read(inBuf);
				handleTCPMsg(inBuf);
			} catch (AsynchronousCloseException ae) {
				break;
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}

		if (channel.isOpen()) {
			try {
				channel.close();
			} catch (IOException ioe) {
				LOG.error("Failed to channel: ", ioe);
			}
		}
	}

	/**
	 * Close the channel and terminate the thread.
	 */
	public void close() {
		try {
			channel.close();
		} catch (IOException e) {
			LOG.error("Failed to close channel: ", e);
		}
	}

	/**
	 * Handle a message that we have received from the proxy.
	 * 
	 * @param inBuf
	 * @throws IOException
	 */
	private synchronized void handleTCPMsg(ByteBuffer inBuf) throws IOException {
		inBuf.flip();
		
		int nrBytes = inBuf.limit();
		byte[] pkg = inBuf.array();
		String[] msg = null;
		String payload;

		if (hadIncompleteLine) {
			payload = incomplete + (new String(pkg, 0, nrBytes, Vendetta.CHARSET));
			hadIncompleteLine = false;
		} else {
			payload = new String(pkg, 0, nrBytes, Vendetta.CHARSET);
		}

		payload = checkIfLost(payload);

		if ("".equals(payload)) {
			return;
		}

		msg = payload.split("\n");

		/* Give the log event to Vendetta
		 */
		for (int i = 0; i < msg.length; i++) {
			Vendetta.netReceiveLogEvent(msg[i] + "\n", Vendetta.NetworkType.TCP);
		}
	}

	/**
	 * Check if the parameter contains a complete log event(s).
	 * 
	 * @param payload
	 * @return
	 */
	private String checkIfLost(String payload) {
		int lastNewlinePos = payload.lastIndexOf('\n');
		String ret;

		if (lastNewlinePos != payload.length() - 1) {
			// Incomplete.
			this.incomplete = payload.substring(lastNewlinePos + 1);
			this.hadIncompleteLine = true;

			ret = payload.substring(0, lastNewlinePos + 1);
		} else {
			// Complete.
			ret = payload;
		}

		return ret;
	}
}

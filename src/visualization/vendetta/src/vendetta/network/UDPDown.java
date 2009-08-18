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
import java.io.UnsupportedEncodingException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;

import vendetta.Vendetta;
import vendetta.util.log.Log;

/**
 * A class for receiving data from vclients over UDP.
 * 
 * Received data will be passed to Vendetta for handling.
 * 
 * @version $Id: UDPDown.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
public class UDPDown extends Thread {
	private static final Log LOG = Log.getInstance("UDPDown");
	private static final int INPUT_BUFFER_SIZE = 512;

	/**
	 * A flag indicating if we're still running.
	 */
	private boolean cont = true;

	/**
	 * The datagram channel on which we listen for message from vclients.
	 */
	private DatagramChannel dChannel;

	/**
	 * Create a new object that listens for UDP messages on the given port.
	 * 
	 * @param port The port to listen on.
	 * @throws BindException If the socket could not be bound.
	 */
	public UDPDown(int port) throws BindException {
		super("UDPDown");
		try {
			dChannel = DatagramChannel.open();
			dChannel.socket().bind(new InetSocketAddress(port));
		} catch (BindException be) {
			throw be;
		} catch (IOException ioe) {
			LOG.error("Cannot initialize UDP down: ", ioe);
		}
	}

	/**
	 * Listen on for incoming messages and handle them.
	 */
	public void run() {
		ByteBuffer inBuf = ByteBuffer.allocate(INPUT_BUFFER_SIZE);

		while (cont) {
			try {
				dChannel.receive(inBuf);
				handleUDPMsg(inBuf);
				inBuf.clear();
			} catch (ClosedChannelException e) {
				/* Ignore.
				 */
			} catch (IOException e) {
				LOG.error(e);
			} 
		}
		LOG.info("Closed.");
	}

	/**
	 * Stop the thread's loop and terminate the thread.
	 */
	public void close() {
		cont = false;
		try {
			dChannel.close();
		} catch (IOException e) { }
	}

	/**
	 * Handle a received UDP message.
	 * 
	 * @param inBuf
	 */
	private void handleUDPMsg(ByteBuffer inBuf) {
		inBuf.flip();
		
		int nrBytes = inBuf.limit();
		byte[] pkg = inBuf.array();
		String payload;
		
		try {
			payload = new String(pkg, 0, nrBytes, Vendetta.CHARSET);
		} catch (UnsupportedEncodingException uee) {
			LOG.warn("Unsupported encoding in UDP message: " + uee.getMessage());
			return;
		}
		
		/* Each udp message ends in \0.
		 */
		String msgs[] = payload.split("\0");
		
		/* Loop through each message and let vendetta handle it.
		 */
		for (String msg : msgs) {
			Vendetta.netReceiveLogEvent(msg, Vendetta.NetworkType.UDP);
		}
	}
}

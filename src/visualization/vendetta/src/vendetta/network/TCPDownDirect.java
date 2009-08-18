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
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import vendetta.Vendetta;
import vendetta.util.log.Log;

/**
 * A class for downstream connections to vclient instances.
 * 
 * An instance of this class will create a server socket on the
 * given port. On this socket, it will listen for connections from
 * vclient instances. In contrast to older version of Vendetta,
 * connections will be kept open until the program is terminated
 * or the remote party closes the connection.
 * 
 * For connections to the middleware, see TCPDownStatic.
 * 
 * Connection handling is implemented with non-blocking IO.
 * 
 * @see TCPDownStatic
 * @version $Id$
 */
public class TCPDownDirect implements TCPDown {
	private static final Log LOG = Log.getInstance("TCPDown"); 
	private static final int INPUT_BUFFER_SIZE = 512;
	
	/**
	 * A flag that indicates whether we are still running.
	 */
	private volatile boolean cont = true;

	/**
	 * Channel of the server socket for incoming connections.
	 */
	private ServerSocketChannel serverChannel;
	
	/**
	 * Map of currently open channels to their buffers.
	 */
	private Map<SocketChannel, ByteBuffer> channelBuffers;
	
	/**
	 * Used for multiplexing IO on open connections and the server socket.
	 */
	private Selector sel;

	/**
	 * A class for downstream connections to vclient instances.
	 * 
	 * Start the returned thread by running new TCPDownDirect(XZY).start();
	 * See the class description for more information. 
	 * 
	 * @param port The port for the server socket.
	 * @throws BindException If the server socket could not be opened.
	 */
	public TCPDownDirect(int port) throws BindException {
		channelBuffers = new HashMap<SocketChannel, ByteBuffer>();
		
		try {
			sel = Selector.open();
			openServerChannel(port);
			LOG.debug("Initialized.");
		} catch (BindException be) {
			/* Report this fatal error to the caller so he
			 * can handle it and shut down cleanly.
			 */
			throw be;
		} catch (IOException ioe) {
			LOG.error("Failed to open server channel: ", ioe);
		}
	}

	/**
	 * Open, bind, and register the server socket channel.
	 * 
	 * @param port The port for the server socket.
	 */
	private void openServerChannel(int port) throws IOException {
		serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		serverChannel.socket().bind(new InetSocketAddress(port));
		serverChannel.register(sel, SelectionKey.OP_ACCEPT);
	}

	/**
	 * This thread's loop. It will continuously wait for either
	 * new connections on the server socket or data to read on the
	 * connection sockets. Whenever it has data to read, it
	 * will pass the read data to the NetBuffer (see processBuffer).
	 *  
	 */
	public void run() {
		while (cont) {
			Set<SelectionKey> keys = null;
			try {
				/* Wait for something interesting to happen ...
				 */
				sel.select();
				keys = sel.selectedKeys();
			} catch (IOException ioe) {
				LOG.error("select() failed: ", ioe);
				break;
			} catch (ClosedSelectorException cse) {
				break;
			}
			
			for (SelectionKey key : keys) {
				if (!key.isValid()) {
					continue;
				}
				
				try {
					if (key.isAcceptable()) {
						/* Accept the connection on the server channel and
						 * add the new connection to the selector.
						 */
						ServerSocketChannel servCh = (ServerSocketChannel) key.channel();
						SocketChannel conCh = servCh.accept();
						if (conCh != null) {
							conCh.configureBlocking(false);
							conCh.register(sel, SelectionKey.OP_READ);
							channelBuffers.put(conCh, ByteBuffer.allocate(INPUT_BUFFER_SIZE));
						}
					} else if (key.isReadable()) {
						/* Data from a client to read.
						 */
						SocketChannel ch = (SocketChannel) key.channel();
						ByteBuffer in = channelBuffers.get(ch);
						int n;
						
						n = ch.read(in);
						
						if (-1 == n) {
							/* Something went wrong.
							 */
							LOG.debug("Connection to " + ch.socket().getRemoteSocketAddress() + " closed by remote party.");
							ch.socket().close();
							key.cancel();
							channelBuffers.remove(ch);
						} else if (0 == n) {
							/* ??? This should not happen.
							 */
//							LOG.debug("Read 0 bytes from remote party.");
						} else {
							/* Everything ok: Process the read data.
							 */
							processBuffer(in);
						}
					}
				} catch (ClosedSelectorException cse) {
				} catch (IOException ioe) {
					LOG.warn(ioe);
					key.cancel();
				}
			}
		}
	}
	
	/**
	 * Closes all current connections and the server socket.
	 */
	public void close() {
		cont = false;
		
		try {
			sel.close();
			serverChannel.close();
		} catch (IOException ioe) { }
		
		for (SocketChannel ch : channelBuffers.keySet()) {
			try {
				ch.socket().close();
			} catch (IOException ioe) { }
		}
		
		LOG.info("Closed.");
	}
	
	/**
	 * Process a buffer containing data received from a certain vclient.
	 * 
	 * The buffer will be scanned for a newline character. If a complete
	 * line is found, it will be passed to Vendetta's netReceiveLogEvent()
	 * method. The buffer will be cleared.
	 * 
	 * @param in The buffer to be processed
	 */
	private void processBuffer(ByteBuffer in) {
		int newlinePos;
		
		in.limit(in.position());
		in.position(0);
		
		newlinePos = getNewlinePosition(in);
		while (newlinePos != -1) {
			int pos = in.position();
			
			try {
				String payload = new String(in.array(), pos, newlinePos - pos,
											Vendetta.CHARSET);
				Vendetta.netReceiveLogEvent(payload, Vendetta.NetworkType.TCP);
			} catch (UnsupportedEncodingException uee) {
				LOG.error(uee);
			}
			
			in.position(newlinePos+1);
			newlinePos = getNewlinePosition(in);
		}
		
		if (in.position() < in.limit()) {
			/* There still is some data left, but it does not have a newline character.
			 */
			System.arraycopy(in.array(), in.position(), in.array(), 0, in.limit()-in.position());
			in.position(in.limit()-in.position());
			in.limit(in.capacity());
		} else {
			/* In this "else", in.position() == in.limit() always holds.
			 */
			in.clear();
		}
	}
	
	/**
	 * Find the position of the newline character within a byte buffer.
	 * 
	 * @param buf The buffer that possibly contains the newline character.
	 * @return The 0-indexed position of newline if found, -1 otherwise. 
	 */
	private static int getNewlinePosition(ByteBuffer buf) {
		int i;
		
		for (i=buf.position();i<buf.limit();i++) {
			if (buf.array()[i] == '\n') {
				break;
			}
		}
		
		if (i == buf.limit()) {
			i = -1;
		}
		
		return i;
	}
}

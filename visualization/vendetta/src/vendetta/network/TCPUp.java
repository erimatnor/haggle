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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import vendetta.MonitorNode;
import vendetta.util.log.Log;

/**
 * A class for sending control messages to vclient instances.
 * 
 * Start the created object's thread. Then, to send a message,
 * call the add() method with the target vclient nodes and
 * the control message as payload. The message will not be sent
 * until you call the send() message, which will cause the thread
 * to process the hosts queue and send the payload out over the
 * network.
 * 
 * @version $Id: TCPUp.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
public class TCPUp implements Runnable {
	private static Log LOG = Log.getInstance("TCPUp");
	private static final int MAX_CONNECTIONS = 10;

	/**
	 * A helper class representing the destination of a control message. 
	 */
	private static class Host {
		String addr;
		int port;

		Host(String addr, int port) {
			this.addr = addr;
			this.port = port;
		}
	}

	/**
	 * A flag indicating whether we're still running.
	 */
	private volatile boolean cont;
	
	/**
	 * A flag indicating if there's nothing to do.
	 */
	private volatile boolean waiting;

	/**
	 * A selector for the multiplexing the non-blocking IO.
	 */
	private Selector selector;

	/**
	 * A buffer for each channel.
	 */
	private Map<SocketChannel, ByteBuffer> channelBuffers = new HashMap<SocketChannel, ByteBuffer>();

	/**
	 * The payload to send to the vclient instances.
	 */
	private byte[] payload = null;
	
	/**
	 * The hosts to send the payload to.
	 */
	private Vector<Host> hosts;

	/**
	 * Whether to send all commands to a proxy instead of sending them directly.
	 */
	private boolean useProxy;
	
	/**
	 * The address of the proxy, or the empty string.
	 */
	private String proxyAddress;
	
	/**
	 * The port of the proxy, or -1.
	 */
	private int proxyPort;
	
	/**
	 * Create a new TCPUp thread that can be used for sending control commands.
	 * 
	 * @param proxyAddress
	 * @param proxyPort
	 */
	public TCPUp(String proxyAddress, int proxyPort) {
		hosts = new Vector<Host>();
		cont = true;
		waiting = true;
		LOG.debug("OK");

		if (!"".equals(proxyAddress) && proxyPort != -1) {
			/* If the proxy address is non-empty and a port is given,
			 * we use a proxy.
			 */
			useProxy = true;
			this.proxyAddress = proxyAddress;
			this.proxyPort = proxyPort;
		} else {
			/* If either is not set, we don't use a proxy.
			 */
			useProxy = false;
		}
	}
	
	/**
	 * Create a new TCPUp object not using a proxy.
	 */
	public TCPUp() {
		this("", -1);
	}

	/**
	 * Add a control command to the queue.
	 * 
	 * @param nodes The vclient instances the command will be sent to.
	 * @param msg The command to send.
	 */
	public synchronized void add(MonitorNode[] nodes, byte[] msg) {
		if (hosts.size() != 0 || payload != null) {
			LOG.warn("Wait, already sending control messages.");
			return;
		}
		
		for (int i = 0; i < nodes.length; i++) {
			hosts.add(new Host(nodes[i].getIP(), nodes[i].getPort()));
		}
		
		payload = msg;
	}

	/**
	 * Sends the queue.
	 */
	public synchronized void send() {
		/* Are we already sending?
		 */
		if (waiting) {
			waiting = false;
			/* Notify that our state changed.
			 */
			notify();
		}
	}

	/**
	 * Stop this thread's loop and terminate the thread.
	 */
	public synchronized void stopServer() {
		cont = false;
		waiting = false;
		notify();
	}
	
	/**
	 * The threaded sending.
	 * 
	 * This is what this beast does: wait() until send() is called, which
	 * sends waiting = false. It will then send the message that is in
	 * payload to every vclient instance that is in hosts. It will not
	 * use more than MAX_CONNECTIONS concurrent connections at once to
	 * be easy on the network connection.
	 */
	public void run() {
		while (cont) {
			synchronized (this) {
				/* If there is nothing to do, just wait() ...
				 */
				while (waiting) {
					try {
						wait();
					} catch (InterruptedException ie) { }
				}
			}

			if (!cont) {
				break;
			}
			
			/* Ok, there is something to send ...
			 */
			int channels_open = 0;
			int max_channels_used = 0;
			int success = 0, failed = 0;
			int to_send = hosts.size(), num_processed = 0;
			channelBuffers.clear();

			try {
				selector = Selector.open();
			} catch (IOException ioe) {
				LOG.error("Unable to open selector: ", ioe);
			}

			/* Do we have messages to send? (true once if < MAX to send)
			 */
			while (num_processed < to_send) {
				/* Can we open a new channel?
				 */
				if (channels_open < MAX_CONNECTIONS && hosts.size() > 0) {
					Host dest = hosts.remove(0);
					SocketChannel channel = null;
					
					try {
						channel = createSocketChannel(dest);
						channel.register(selector, channel.validOps());
						channels_open++;
						if (channels_open > max_channels_used) {
							max_channels_used = channels_open;
						}
							
						ByteBuffer channelBuffer = prepareBuffer(dest, payload);
						channelBuffers.put(channel, channelBuffer);
					} catch (IOException ioe) {
						failed++;
						num_processed++;
						LOG.error("Cannot open socket channel to " +
							      dest.addr + ": ", ioe);
					}
				}

				/* Do we have an open channel?
				 */
				if (channels_open > 0) {
					try {
						/* Wait for something interesting to happen.
						 */
						selector.select();
					} catch (IOException ioe) {
						LOG.error("Error using select(): ", ioe);
						break;
					}

					/* Process each key at a time
					 */
					for (SelectionKey key : selector.selectedKeys()) {
						/* Try to handle the selection key
						 */
						int val = processSelectionKey(key);
						
						if (val == 1) {
							/* A command has been sent successfully ...
							 */
							channels_open--;
							num_processed++;
							success++;
						} else if (val == -1) {
							/* There was an error sending the command.
							 */
							channels_open--;
							num_processed++;
							failed++;
						}
					}
				}
			}
			
			/* We are done sending!			 
			 */
			LOG.debug("Sent. Processed " + num_processed
					+ " nodes. " + " Success: " + success + " Failed: "
					+ failed);
			
//			+ " Channels: " + max_channels_used + "/"
//					+ MAX_CONNECTIONS);
			
			assert hosts.size() == 0;
			
			/* Back to waiting mode ...
			 */
			waiting = true;
			try {
				selector.close();
			} catch (IOException e) {
				LOG.error("Unable to close Selector: ", e);
			}
			selector = null;
			payload = null;
		}
		
		LOG.debug("Closed");
	}

	
	/**
	 * Handle a SelectionKey that is ready for an operation.
	 * 
	 * This means: Either finish connecting, or send the command
	 * and close the connection.
	 * 
	 * @param selKey The key to be processed.
	 * @return 1 if a command was sent successfully, -1 if sending
	 * 		   a command failed, 0 otherwise.
	 */
	public int processSelectionKey(SelectionKey selKey) {
		/* Since the ready operations are cumulative,
		 * need to check readiness for each operation
		 */ 
		SocketChannel sChannel = null;
		
		if (!selKey.isValid()) {
			return 0;
		}
		
		if (selKey.isConnectable()) {
			/* Get channel with connection request
			 */
			sChannel = (SocketChannel) selKey.channel();
			boolean success = false;
			
			try {
				success = sChannel.finishConnect();
			} catch (IOException ioe) {	}
			
			if (!success) {
				/* An error occurred; print it an unregister
				 * the key with the selector.
				 */
				LOG.error("Unable to finish connect");
				selKey.cancel();
				return -1;
			}
			
			return 0;
		} else if (selKey.isWritable()) {
			/* Get channel that's ready for more bytes and its associated
			 * buffer.
			 */
			SocketChannel s2Channel = (SocketChannel) selKey.channel();
			ByteBuffer channelBuffer = channelBuffers.remove(s2Channel);
			channelBuffer.flip();
			
			try {
				/* XXX We could lose data on impartial write!
				 */
				s2Channel.write(channelBuffer);
				s2Channel.close();
				
				channelBuffers.remove(s2Channel);
			} catch (IOException ioe) {
				LOG.error("Failed to send command to remote party.", ioe);
				selKey.cancel();
				return -1;
			}

			/* Sent successfully.
			 */
			return 1;
		}
		
		return 0;
	}

	/**
	 * Open a non-blocking socket channel.
	 * 
	 * @param n
	 * @return A non-blocking socket channel.
	 * 
	 * @throws IOException
	 */
	private SocketChannel createSocketChannel(Host n) throws IOException {
		/* Create a non-blocking socket channel
		 */
		SocketChannel sChannel = SocketChannel.open();
		sChannel.configureBlocking(false);

		/* Send a connection request to the server; this method is
		 * non-blocking.
		 */
		if (useProxy) {
			sChannel.connect(new InetSocketAddress(proxyAddress, proxyPort));
		} else {
			sChannel.connect(new InetSocketAddress(n.addr, n.port));
		}
		
		return sChannel;
	}
	
	/**
	 * Prepare a buffer to be send to a vclient instance.
	 * 
	 * @param dest The destination host.
	 * @param payload The payload.
	 * 
	 * @return A byte buffer.
	 */
	private ByteBuffer prepareBuffer(Host dest, byte[] payload) {
		/* XXX Encoding?
		 * 538: 3 chars of ID, space, 15 chars of IP, space,
		 * 5 chars of port, space, 512 chars of payload.
		 */
		ByteBuffer buf = ByteBuffer.allocateDirect(538);
		buf.put((new Integer(0).toString() + " ").getBytes());
		buf.put((dest.addr + " ").getBytes());
		buf.put((new Integer(dest.port).toString() + " ").getBytes());
		buf.put(payload);
		
		return buf;
	}
}

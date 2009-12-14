/*Author Christofer Ferm 
 * (first version there is room for improvements)
 * The TCPDown class handles incoming TCP connections from vclient and controlmessages. 
 * Some of this code is borrowed from the original version of the vendetta monitor
 *
 * TCP Down takes one or two parameter the port to listen to 
 * each log event sent from vclient creates a new TCP connection this might be change in the future*/

package Forwarding;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class TCPDown extends Thread {
	private final String CHARSET = "US-ASCII";
	private int port;
	private int destPort; 
	private volatile boolean cont = true;
	boolean notstatic = false; 
    protected MonitorManager m; 
	private SocketChannel channel = null;
	private ServerSocketChannel serverChannel = null;
	
	public TCPDown(int lport)
	{
		super( "TCPDown-Thread" );
		port = lport; 
		notstatic = true; 
		try {
			openServerChannel();
			debug( 0, "OK" );
		} catch( Exception e ) {
			debug( 0, "Error: " + e.getMessage() );
			e.printStackTrace();
		}
		
		
	}
	
	
	public TCPDown(int lport, int dport) {
		super( "TCPDown-Thread" );
		port = lport; 
		destPort = dport;
		try {
			openServerChannel();
			debug( 0, "OK" );
		} catch( Exception e ) {
			debug( 0, "Error: " + e.getMessage() );
			e.printStackTrace();
		}
	}
	
	public TCPDown(int lport, int dport,MonitorManager m) {
		super( "TCPDown-Thread" );
		port = lport; 
		destPort = dport;
		this.m = m;
		try {
			openServerChannel();
			debug( 0, "OK" );
		} catch( Exception e ) {
			debug( 0, "Error: " + e.getMessage() );
			e.printStackTrace();
		}
	}

	/** Open and bind the server-socket channel
    */
    private void openServerChannel() throws IOException {
		serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind( new InetSocketAddress( port ) );
        while( !serverChannel.isOpen() ) {
        }
    }
	public synchronized void stopServer() {
		cont = false;
		try {
			serverChannel.close();
		} catch( Exception e ) {}
	}
	public void run() {
		
		ByteBuffer inBuf = ByteBuffer.allocate( 512 );
		while( cont ) {
			try {
				channel = serverChannel.accept();
        		channel.read( inBuf );
				handleTCPMsg( inBuf );
        		inBuf.clear();
				channel.close();
			} catch( AsynchronousCloseException ae ) {
				cont = false;
			} catch( Exception e ) {
				error( e.getMessage() );
				e.printStackTrace();
			}
		}
		try {
			if( channel != null ) 
				channel.close();
		} catch( Exception e ) {}
		System.out.println( "(TCPDown) Closed" );
	}

	/**  
	 * TYPE ARGS1 ARGS2\n\0
	 */
	//handle a incoming TCP Message 
	private void handleTCPMsg( ByteBuffer inBuf ) throws IOException {
		inBuf.flip();
		int nrBytes = inBuf.limit();
		byte[] pkg = inBuf.array();
		
		String payload = new String( pkg, 0, nrBytes, CHARSET );		

		debug(1, "Received:" + payload); 
		if(notstatic == false) 
		{
			// check for new line correctness 
			int l = payload.length(); 
			payload.replace('\0', '\n');
			payload = checkAndAddnewLine(payload); 
			//if a newline was missing we added one and need to send one extra byte
			if(payload.length()>l)
			{
				nrBytes++; 
			}
			//Create a new packet  ready to send 
			Packet p = new Packet(payload.getBytes("US-ASCII"),nrBytes);
			
							
			//Loop through the monitorList and add this packet to their out buffers. 
			if(!m.MonitorList.isEmpty())
				{
						Iterator<Monitor> i = m.MonitorList.iterator(); 
						Monitor mo = m.MonitorList.get(0); 
					    while(i.hasNext())
						{
							mo = i.next();
						    
					    		if(p != null)
								{
									//System.out.print("Try to send:" + new String(p.pkg,0,p.nrOfBytes));
									//send the packet 
									mo.m.sendpacket(p);
					
									
								}else
								{
								  System.err.println("handleMonitor: Unexpected nullpointer"); 		
							    }
																
					
					
						}
					    
					    m.performDereg(); 
				}
			}
				
		else //if it is not supposed to be forwarded to all monitors
		{
		 CheckDestination(payload.getBytes("US-ASCII"),nrBytes); //we need to check where to send it 
			
		}
		// Give the log event to vendetta
		 
		
	}
	
	
	//if the incoming packet is missing a newline in the end add newline 
	private String checkAndAddnewLine(String p)
	{
		int len =  p.length();
		char c = '\n'; 
		String temp; 
		
		if(p.charAt(len-1) == '\n')
		{
		
			return p; 
			
		}
		else
		{
			if(p.charAt(len-1) == '\n')
			{
				temp = new String(p.substring(0, len-2)+c);
				return temp;
			}
			else
			{
				temp = new String(p+c); 
				return temp;
			}
		}
		
	}
	/* extracts id and ip and port from the packet and forwards it */
	private void CheckDestination(byte[] pkg, int nrbytes) 
	{
		try {
			String tmp = new String( pkg, 0, nrbytes, CHARSET );
			String id = null; 
			String ip = null;
			Integer port = null; 
			int ipstart = (tmp.indexOf(" ")+1);
			int portstart = (tmp.indexOf(" ", (tmp.indexOf(" ")+1))+1);
			int portstop = tmp.indexOf(" ",(tmp.indexOf(" ", (tmp.indexOf(" ")+1))+1)+1);
			
			id = tmp.substring(0, ipstart-1); 
			ip = tmp.substring(ipstart,portstart-1);
			port = new Integer(tmp.substring(portstart,portstop));
			
			if(id != null && ip != null && port != null)
			{
				new Forward(ip,port.intValue(),pkg, nrbytes).start(); 
			}
			else 
			{
				error("could not extract destination address"); 
			
			}
		
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	

		
		
	}
	//Debug methods
	private void error( String m ) {
		System.out.println( "(TCPDown) "+m );
	}
	private void debug( int level, String m ) {
		if( level == 0 )
			System.out.println( "(TCPDown) "+m );
		else if( level == 1 )
			System.out.println( "(TCPDown) "+m );
	}
}

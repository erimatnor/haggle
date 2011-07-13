/*Author Christofer Ferm 
 * (first version there is room for improvements) 
 * UDPDown class listens for incoming UDP packets from vclient. Some of the code is borrowed from 
 * the Original version of vendetta.
 * 
 * UDPDown takes two parameters the first one is the port to listen to and the second one is 
 * a pointer to then running instance of MonitorManager.  
 * 
 * */

package Forwarding;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class UDPDown implements Runnable {
	private final String CHARSET = "US-ASCII";

    private volatile boolean cont = true;
	private volatile Thread thread;
    private MonitorManager m = null; 
    private DatagramChannel dChannel;
		
	public UDPDown(int port,MonitorManager m) {
		try {
			this.m = m; 
			// Open a datagram channel
			dChannel = DatagramChannel.open();
			// Open a socket on the datagram channel
	        dChannel.socket().bind( new InetSocketAddress( port) );
			debug( "OK" );
			thread = new Thread( this, "UDPDown-Thread" );
			thread.start();
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	public synchronized void stopServer() {
		try {
			dChannel.close();
		} catch( Exception e ) {}
		cont = false;
	}
	
    public void run() {
		ByteBuffer inBuf = ByteBuffer.allocate( 512 );
    	InetSocketAddress sockAddr;
		
		while( cont ) {
            try {
            	sockAddr = (InetSocketAddress)dChannel.receive( inBuf );
				handleUDPMsg( inBuf );
				inBuf.clear();
			} catch( ClosedChannelException e ) {
		
			} catch( Exception e ) {
				error( e.getMessage() );
				e.printStackTrace();
            }
        }
		System.out.println( "(UDPDown) Closed" );
	}
	
	private synchronized void handleUDPMsg( ByteBuffer inBuf ) throws Exception {
		inBuf.flip();
		int nrBytes = inBuf.limit();
		byte[] pkg = inBuf.array();
		String payload = new String( pkg, 0, nrBytes, CHARSET );
		//debug("Received Packet:"+ payload);
		
		if(!m.MonitorList.isEmpty())
		{	
	
			int l = payload.length();
			
			payload = checkAndAddnewLine(payload);
			
			if(payload.length()> l)
			{
				nrBytes++; 
			//debug("adding one byte");
			}
			
			
			Packet p = new Packet(payload.getBytes("US-ASCII"),nrBytes);
				Iterator<Monitor> i = m.MonitorList.iterator(); 
				Monitor mo = m.MonitorList.get(0); 
				while(i.hasNext())
				{			
					mo = i.next();
	
					if(p != null)
					{
					//	System.out.print("Try to send:" + new String(p.pkg,0,p.nrOfBytes));
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
	
	//if the incoming packet don't have a newline at the end we add one 
	private String checkAndAddnewLine(String p)
	{
		int len =  p.length();
		char c = '\n'; 
		String temp; 
		
		if(p.charAt(len-1) == '\n')
		{
			//debug("has newline true");
			return p; 
			
		}
		else
		{
			if(p.charAt(len-1) == '\0')
			{
				//debug("contains \\0");
				temp = p.substring(0, len-2) + c; 
				return temp;
			}
			else
			{
				//debug("has newline false adding new line ");
				
				temp = new String(p+c); 
				return temp;
			}
		}
		
	}
	
	
	//Debug methods 
	private void error( String m ) {
		System.out.println( "(UDPDown)"+ m );
	}
	private void debug( String m ) {
		System.out.println( "(UDPDown)"+ m );
	}
}

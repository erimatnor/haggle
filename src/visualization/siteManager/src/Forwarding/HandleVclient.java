package Forwarding; 


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.util.Iterator;


public class HandleVclient extends Thread
{
	SocketChannel sock= null; 
	VDown vd = null; 
	String incomplete = null; 
	String CHARSET = "US-ASCII";
	private boolean notCompleteLine = false; 
	public HandleVclient(VDown v, SocketChannel s)
	{
		vd = v; 
		sock = s; 
			
	}

	
	public void run()
	{
	System.out.println("got Connection from Vclient" + sock.socket().getRemoteSocketAddress());	
	ByteBuffer inBuf = ByteBuffer.allocate(512); 
	try {
		while(sock.read( inBuf ) != -1)
		{
					
				
					handleTCPMsg( inBuf );
					inBuf.clear();
				
		
		}
	} catch (IOException e1) {
		
		e1.printStackTrace();
	}
	try {
		if( sock != null ) 
			sock.close(); 
	} catch( Exception e ) {}
	System.out.println( "(VDown) Closed" );

		
		
		
	}
	
	
	
	//handle a incoming TCP Message 
	private void handleTCPMsg( ByteBuffer inBuf ) throws IOException {
		inBuf.flip();
		int nrBytes = inBuf.limit();
		byte[] pkg = inBuf.array();
		
		String payload = null;	

		//debug(1, "Received:" + payload); 
		
		
		
		// check for new line correctness 
		if(notCompleteLine)
		{
			payload = incomplete +(new String(pkg, 0, nrBytes, CHARSET));	
			notCompleteLine = false; 
		}
		else 
		{
			payload = new String(pkg, 0, nrBytes, CHARSET);
			
		}
		
		payload = checkIfLost(payload); 
	
		
		if("".equals(payload))
		{
			return; 
			
		}
		/*int l = payload.length(); 
			payload.replace('\0', '\n');
			payload = checkAndAddnewLine(payload); 
			//if a newline was missing we added one and need to send one extra byte
			if(payload.length()>l)
			{
				nrBytes++; 
			}*/
		
			//Create a new packet  ready to send 
			nrBytes = payload.getBytes("US-ASCII").length;
			Packet p = new Packet(payload.getBytes("US-ASCII"),nrBytes);
			
							
			//Loop through the monitorList and add this packet to their out buffers. 
			if(!vd.m.MonitorList.isEmpty())
				{
						Iterator<Monitor> i = vd.m.MonitorList.iterator(); 
						Monitor mo = vd.m.MonitorList.get(0); 
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
						
						vd.m.performDereg(); 
				}
			}
				
	
	public String checkIfLost(String payload)
	{
		int lastNewlinePos = payload.lastIndexOf('\n');
		String ret;
		
		if (lastNewlinePos != payload.length() - 1) {
			// Incomplete.
			this.incomplete = payload.substring(lastNewlinePos+1);
			this.notCompleteLine = true;
			
			ret = payload.substring(0, lastNewlinePos+1);
		} else {
			// Complete.
			ret = payload;
		}

		return ret;
	}
	
		// Give the log event to vendetta
		 
		
	
	//Debug methods
	private void error( String m ) {
		System.out.println( "(VDown) "+m );
	}
	private void debug( int level, String m ) {
		if( level == 0 )
			System.out.println( "(VDown) "+m );
		else if( level == 1 )
			System.out.println( "(VDown) "+m );
	}
	


}

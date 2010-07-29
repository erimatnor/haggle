/*Author Christofer Ferm 
 * 
 * The hanldeMonitor class takes two parameters the first one is a "pointer" to the MonitorManager class 
 * and the second is the active socket of the monitor to handle.  
 * 
 * */ 
package Forwarding; 

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;


public class handleMonitor extends Thread 
{
	MonitorManager m = null;  
	protected Socket s = null; 
	Monitor me = null; //Will hold the information about the connected monitor 
	public DataOutputStream out;  
	public BufferedReader in; 
	//public List<Packet> Buffer = Collections.synchronizedList(new ArrayList<Packet>());  
	  
	
	public handleMonitor(MonitorManager m, Socket sock)
	{
		
		this.m  = m; 
		s = sock;  
		
		
		me = m.regMonitor(s.getInetAddress().getHostAddress(),this); // register the new monitor to the Monitorlist 
		try {
			       in = new BufferedReader(
				    new InputStreamReader(
				    s.getInputStream()));
			out = new DataOutputStream(s.getOutputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}
	
	public synchronized void sendpacket(Packet p)
	{
		
		try
		{
		
			
			out.write(p.pkg,0,p.nrOfBytes);
		    out.flush(); 
		}catch (SocketException e) 
		{
		
			 m.dereg.add(me); 
		} catch (IOException e) {
			
			
		}
		
	}
	
	
	


}





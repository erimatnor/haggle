/* Author Christofer Ferm 
 * Main class*/ 
package Forwarding; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main
{
	
	public static void main(String args[]) 
	{
		
		System.out.println("SiteManager middleware version 0.1b");
		MonitorManager m = new MonitorManager(5051); 
		//Listens for TCP Packets from vclient 
		//TCPDown t  = new TCPDown(4444,4444,m);
		VDown v = new VDown(4444,m); 
		//Listens for control messages from the monitor 
		TCPDown t2 = new TCPDown(5000);
		//Listens for UDP packages from vclient  
	    UDPDown d = new UDPDown(4445,m);
		v.start();
		t2.start();
		
		String  i  = new String();
	
		System.out.println("Type q to quit");
		while(!i.equals("q"))
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				i = br.readLine(); 
				if(i.equals("monitors"))
				{
					m.DisplayConnectedMonitors(); 
				}
			} catch (IOException e) {
				e.printStackTrace();
			} 
			
		}
	     v.listening =false; 
		t2.stopServer(); 
		d.stopServer();
		m.listening = false; 
		System.exit(0); 
		
		
			
		
	}
	

}
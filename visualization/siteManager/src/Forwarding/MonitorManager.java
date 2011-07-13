/*Author Christofer Ferm 
 * (first version there is room for improvements)
 * The MonitorManager class is the Main class for handling incoming connections from vendetta Monitors
 * it takes only one parameter the port to listen to, this class also keeps track of all the connected monitors*/
package Forwarding; 

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class MonitorManager extends Thread
{
	int listenport;
	boolean listening = true;  
	public List<Monitor> MonitorList =  Collections.synchronizedList(new ArrayList<Monitor>());
	public List<Monitor> dereg = Collections.synchronizedList(new ArrayList<Monitor>());
	public int numOfMonitors = 0; 
	public int nextID = 0; 
	int id = 0; 
	ServerSocket server = null; 

	public MonitorManager(int lport)
	{
		
		listenport = lport; //get port
		listen(); //start listening to that port 
		this.start();
	}

	//listen for new Monitors 
	private void listen()
	{
		try {
			server = new ServerSocket(listenport);
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	
	}

	
	public void run()
	{
		
		System.out.println("(MonitorManager) OK"); 
		while(listening)// while listening is true we listen for incoming connections from monitors  
		{
			try {
				
			
				//when a new monitor connects accept the connection and create a new thread for handling that monitor
				new handleMonitor(this,server.accept()).start();
			} catch (IOException e) {
				e.printStackTrace();
			} 
		
		}
	
		try {
			server.close();// close the socket  
			ResetMonitorList(); 
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		//on exit close all open connections to monitors and remove them from the Monitor list 
		
		while(MonitorList.get(0) != null)
		{
			Monitor m  = MonitorList.get(0); 
			
			if(m.m.s.isConnected()) 
			{
					try {
						m.m.s.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
		
			}
			
			MonitorList.remove(m.m);
			
		}

	}
	
	//Used to register a new Monitor 
	public Monitor regMonitor(String MonitorIp, handleMonitor h)
	{
		id++; 
		
		Monitor m = new Monitor(id,MonitorIp,h);
		
		MonitorList.add(m);
		System.out.println("New Monitor Registered: "+MonitorIp); 
		
		return m; 
	}
	
	//used to unregister a disconnected monitor
	public synchronized void deregMonitor(Monitor m)
	{
		
		System.out.println("Monitor at "+m.ip +" deregistered"); 
		MonitorList.remove(m); 
		
	}
	
	//clears the entire monitor list 
	private void ResetMonitorList()
	{
		System.out.println("Resetting Monitorlist"); 
		MonitorList.clear(); 
	
	}
	
	//show all connected Monitors 
	public void DisplayConnectedMonitors()
	{
		System.out.println("Connected Monitors");
		if(!MonitorList.isEmpty())
		{
				Iterator<Monitor> i = MonitorList.iterator(); 
				Monitor mo = MonitorList.get(0); 
				while(i.hasNext())
				{
					mo = i.next();
					System.out.println(mo.ip);
			
			
				}
		}
		
	}
	
	public synchronized void performDereg()
	{
		Iterator<Monitor> i = dereg.iterator(); 
		
		while(i.hasNext())
		{
		
			Monitor mon = i.next(); 
		
			deregMonitor(mon); 
		
		}
		dereg.clear();
		
	}


}
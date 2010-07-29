/*Author Christofer Ferm 
 * 
 * The Monitor class is just a basic data structure describing a monitor
 * it takes three parameters , MonitorID(not used right now),Monitor address, 
 * and a pointer to the instance handling this monitor*/

package Forwarding;  


public class Monitor
{
	protected int id; //monitor ID 
	protected String ip; // monitor IP 
	protected int port; //monitor port
	protected handleMonitor m;
	
	
	public Monitor(int MonitorID, String MonitorIP, handleMonitor m)
	{
		id = MonitorID; 
		ip = MonitorIP; 
		this.m =  m; 
		
	}
	


}
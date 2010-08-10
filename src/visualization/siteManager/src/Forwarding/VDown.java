package Forwarding; 

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;


public class VDown extends Thread  
{
	
	ServerSocketChannel sock = null; 
	MonitorManager m = null; 
	int port;
	boolean listening;	

public VDown(int port, MonitorManager m)
{

	this.port = port; 
	this.m = m; 
	
}


public void listen()
{
 try {
	 listening = true;
	sock = ServerSocketChannel.open();
	sock.socket().bind(new InetSocketAddress(port));
	} catch (IOException e) {
	e.printStackTrace();
} 
	

}

public void run()
{
	listen();
	System.out.println("(VDown) OK"); 
	while(listening)// while listening is true we listen for incoming connections from monitors  
	{
		try {
			//when a new vclient connects accept the connection and create a new thread for handling that monitor
			new HandleVclient(this,sock.accept()).start();
			System.out.println("(VDown) Connection accepted");
		} catch (IOException e) {
			e.printStackTrace();
		} 
	
	}

	try {
		sock.close();// close the socket  
	} catch (IOException e) {
		e.printStackTrace();
	} 	


}





}
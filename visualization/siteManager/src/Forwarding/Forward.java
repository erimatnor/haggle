/*Author Christofer Ferm
 * The Forward Class takes four parameters the destination address, destination port, 
 * the actual data to be sent as a byte array and the length of the data in bytes*/
package Forwarding;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;



public class Forward extends Thread 
{
	Socket Forwd; 
	DataOutputStream out;
	boolean err = false; 
	byte[] packet;
	int  port; 
	int bytesToSend; 
	String address; 
	//send packet to port at address 
	public Forward(String address, int port, byte[] packet, int bytesToSend)
	{
		
		this.packet = packet;
		this.port = port;
		this.bytesToSend = bytesToSend;
		this.address = address; 
	
	
	}
	
	
	
	public void run()
	{
		try {
			//open socket to destination 
			Forwd = new Socket(address,port);
		} catch (UnknownHostException e) {
		   e.printStackTrace();
		 return; 
		    
		} catch (IOException e) {
			e.printStackTrace();
			 return;
		}
		
		try {
			//open a DataOutputStream
			out = new DataOutputStream(Forwd.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace(); 
			return;
		}
		
		try {
			    //Write the packet to the output stream  
				out.write(packet, 0, bytesToSend); 
				out.flush();
				System.out.println("Packet Forwarded to:"+address+":"+port);
				//close the output stream  
				out.close();
				System.out.println("out closed"); 
				
				
				
		} catch (IOException e) {
			e.printStackTrace();
			
		} 
		
		
	}
	


}
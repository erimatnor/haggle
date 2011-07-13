/*Author Christofer Ferm 
 * 
 * The UDPForward class is used to forward UDPPackets received from the vclient. 
 * Vclient sends Ping messages over UDP that is to be forwarded over UDP by this class. 
 * But this class is no longer used due to that we now have static connections to the monitors so we
 * Now forwards the ping messages over the all ready open static TCP connection. 
 * This class will therefore not be further commented. 
 * */
package Forwarding; 

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class UDPForward
{
	public UDPForward(String Address, int port, byte[] pkg, int bytes)
	{
		System.out.println("Forwarding UDP Packet");
		
		
		
		DatagramSocket socket = null;
		DatagramPacket packet;
		InetAddress address = null;
		try {
			address = InetAddress.getByName(Address);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		try {
			socket = new DatagramSocket(); 
		} catch (SocketException e) {
		
			e.printStackTrace();
		}
		packet = new DatagramPacket(pkg, bytes, address, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
		
			e.printStackTrace();
		}
		
		socket.close();
		
		
		
		
	}



}
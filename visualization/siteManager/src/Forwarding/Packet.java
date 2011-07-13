/*Author Christofer Ferm 
 * 
 * the Packet class is just a small data structure describing a packet
 * it takes two parameters the first one is the data as a byte buffer 
 * and the second parameter is the number of bytes to be sent
 */
package Forwarding; 

public class Packet
{
	public byte[] pkg; // the actual packet to be sent 
	public int nrOfBytes; // the packet size in bytes 
	
	public Packet(byte[] buf, int nrbytes)
	{
		this.pkg = buf; 
		this.nrOfBytes = nrbytes; 

		
		
	}

}
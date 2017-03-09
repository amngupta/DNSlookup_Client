import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Random;

/**
 * 
 */

/**
 * @author Donald Acton
 * This example is adapted from Kurose & Ross
 * Feel free to modify and rearrange code as you see fit
 */
public class DNSlookup {

    static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
    static final int MAX_PERMITTED_ARGUMENT_COUNT = 3;
	private DatagramSocket serverSocket;
	private DNSResponse response;
	private boolean tracingOn = false;
	private boolean IPV6Query = false;
	/**
	 * Constructor
	 */
	DNSlookup(){
		try {
			this.serverSocket = new DatagramSocket(9876);
			this.response = new DNSResponse(); // Just to force compilation
		}catch(Exception e)
		{}
	}

	public void DNSLookup(String url, InetAddress rootNameServer){
		byte[] receiveData = new byte[1024];
		// Some problem here when calling in decodeResponse
		byte[] sendData = this.response.encodeQuery(url);
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, rootNameServer, 53);
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		try {
			this.serverSocket.send(sendPacket);
			this.serverSocket.receive(receivePacket);
		}catch(Exception e){
			e.printStackTrace();
		}
		byte[] receiveBytes = receivePacket.getData();
		this.response.decodeQuery(receiveBytes);
		this.serverSocket.close();
	}

	/**
     * @param args
     */
    public static void main(String[] args) throws Exception {
	String fqdn;
	DNSlookup looker = new DNSlookup();

	int argCount = args.length;
	InetAddress rootNameServer;

	if (argCount < MIN_PERMITTED_ARGUMENT_COUNT || argCount > MAX_PERMITTED_ARGUMENT_COUNT) {
	    usage();
	    return;
	}

	rootNameServer = InetAddress.getByName(args[0]);
	fqdn = args[1];

	if (argCount == 3) {  // option provided
		if (args[2].equals("-t"))
			looker.tracingOn = true;
		else if (args[2].equals("-6"))
			looker.IPV6Query = true;
		else if (args[2].equals("-t6")) {
			looker.tracingOn = true;
			looker.IPV6Query = true;
		} else { // option present but wasn't valid option
			usage();
			return;
		}
	}
		looker.DNSLookup(args[1], rootNameServer);
    }

    
    private static void usage() {
	System.out.println("Usage: java -jar DNSlookup.jar rootDNS name [-6|-t|t6]");
	System.out.println("   where");
	System.out.println("       rootDNS - the IP address (in dotted form) of the root");
	System.out.println("                 DNS server you are to start your search at");
	System.out.println("       name    - fully qualified domain name to lookup");
	System.out.println("       -6      - return an IPV6 address");
	System.out.println("       -t      - trace the queries made and responses received");
	System.out.println("       -t6     - trace the queries made, responses received and return an IPV6 address");
    }
}



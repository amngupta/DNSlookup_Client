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

    public static  byte[] encodeQuery(String query){
		byte[] encodedQuery = new byte[58];
		String [] split = query.split("\\.");
		int counter = 12;
		//FIX THIS SHIT
		int queryId = (int) (Math.random()*255);
		encodedQuery[1] = (byte) queryId;
		encodedQuery[2] = 0;
		encodedQuery[3] = 0;
		encodedQuery[5] = 1;
		for (int i = 0; i < split.length; i++){
			encodedQuery[counter] = (byte) split[i].length();
			counter++;
			for (int j = 0; j < split[i].length(); j++){
				String a = split[i];
				char c = a.charAt(j);
				String hexForAscii = Integer.toHexString((int) c);
				byte test = Byte.parseByte(hexForAscii,16);
				encodedQuery[counter] = test;
				counter++;
			}
		}
		encodedQuery[counter+2] = 1;
		encodedQuery[counter+4] = 1;
		return encodedQuery;
	}


    public static int convertBytesToInt (byte [] arr, int offset)      // unsigned
    {
        return (arr[offset] & 0xFF) << 8 | (arr[offset+1] & 0xFF);
    }



    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
	String fqdn;
	DNSResponse response; // Just to force compilation
	int argCount = args.length;
	boolean tracingOn = false;
	boolean IPV6Query = false;
	InetAddress rootNameServer;


	byte [] sendArr = (encodeQuery("www.google.com"));
	for (int i = 0; i < sendArr.length; i++){
		System.out.println(sendArr[i]);
	}

	byte[] test = {0xe, 0x0C};
    int test1 = convertBytesToInt(test, 0);
    System.out.println("BYTE TEST" + test1);

	if (argCount < MIN_PERMITTED_ARGUMENT_COUNT || argCount > MAX_PERMITTED_ARGUMENT_COUNT) {
	    usage();
	    return;
	}

	rootNameServer = InetAddress.getByName(args[0]);
	InetAddress finalIPAdd = InetAddress.getByName(args[1]);
	fqdn = args[1];

	//	System.out.println(finalIPAdd);
	if (argCount == 3) {  // option provided
		if (args[2].equals("-t"))
			tracingOn = true;
		else if (args[2].equals("-6"))
			IPV6Query = true;
		else if (args[2].equals("-t6")) {
			tracingOn = true;
			IPV6Query = true;
		} else { // option present but wasn't valid option
			usage();
			return;
		}
	}

	// Start adding code here to initiate the lookup
		System.out.println(rootNameServer);
		DatagramSocket serverSocket = new DatagramSocket(9876);
		byte[] receiveData = new byte[1024];
		//byte[] sendData = {0x2b,0x2b, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  0x03, 0x77, 0x77, 119, 0x05, 0x75, 0x67, 0x72, 0x61, 0x64, 0x02, 0x63, 0x73, 0x03, 0x75, 0x62, 0x63, 0x02, 0x63, 0x61,0x00,0x00,0x01,0x00,0x01};
		byte[] sendData = encodeQuery(args[1]);
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, rootNameServer, 53);
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

		serverSocket.send(sendPacket);
		serverSocket.receive(receivePacket);

		String modifiedSentence = new String(receivePacket.getData());
		serverSocket.close();
	
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



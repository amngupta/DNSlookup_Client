package dnsClient;

import java.net.*;

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
    protected boolean tracingOn = false;
    protected boolean IPV6Query = false;
    protected String queryString;
    protected String dnsString;

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

    /**
     * Main method to perform one iteration of querying
     * @param url the url (or query) to be found
     * @param rootNameServer the dns being queried
     * @return returns a DNSResponse.rData object which is the closest to the final answer
     * @throws Exception
     */
    public DNSResponse.rData DNSLookup(String url, InetAddress rootNameServer) throws Exception{
        byte[] receiveData = new byte[1024];
        // Some problem here when calling in decodeResponse
        byte[] sendData = this.response.encodeQuery(url, this);
        this.dnsString = rootNameServer.toString();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, rootNameServer, 53);
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            this.serverSocket.send(sendPacket);
            this.serverSocket.receive(receivePacket);
        }catch(Exception e){
            e.printStackTrace();
        }
        byte[] receiveBytes = receivePacket.getData();
        return this.response.decodeQuery(receiveBytes, this);
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

        looker.queryString = args[1];
        looker.dnsString = args[0];
        rootNameServer = InetAddress.getByName(looker.dnsString);
        if (argCount == 3) {  // option provided
            if (args[2].equals("-t"))
                looker.tracingOn = true;
            else if (args[2].equals("-6"))
                looker.IPV6Query = true;
            else if (args[2].equals("-t6")) {
                looker.tracingOn = true;
                looker.IPV6Query = true;
            }
            else { // option present but wasn't valid option
                usage();
                return;
            }
        }
        int queries = 0;
        boolean checker = true;
        DNSResponse.rData nextRes = looker.DNSLookup(looker.queryString,rootNameServer);
        while(checker) {
            if (looker.response.authoritative & looker.response.answerCount == 0) {
                System.out.println(looker.queryString+" -6 "+looker.response.getActualType(nextRes.type)+" 0.0.0.0");
                break;
            }
            if (looker.response.answerCount > 0) {
                //if we have an answer
                if(nextRes.type == 1 || nextRes.type == 28){
                    //if answer is of type ipV4 or ipV6
                    checker = false;
                }else if(nextRes.ns.length() > 0){
                    //if answer contains a CNAME
                    looker.queryString = nextRes.ns;
                    nextRes =  looker.DNSLookup(looker.queryString, InetAddress.getByName(args[0]));
                    queries++;
                }
            }
            else{
                if(nextRes != null) {
                    if(nextRes.ipAddress == null && nextRes.ns.length() > 0){
                        nextRes = looker.DNSLookup(nextRes.ns, InetAddress.getByName(args[0]));
                    }else
                        nextRes = looker.DNSLookup(looker.queryString, InetAddress.getByName(nextRes.ipAddress));
                    queries++;
                }
            }
            //If queries > 30; finish.
            if (queries > 30) {
                System.out.println(looker.queryString+" -3 "+looker.response.getActualType(nextRes.type)+" 0.0.0.0");
                checker = false;
            }
        }
        if(looker.response.answerCount > 0){
            //Print final response
            System.out.println(args[1]+" "+looker.response.getActualType(nextRes.type) +  " " + nextRes.ipAddress);
        }
        looker.serverSocket.close();

    }

    /**
     * Given method to print out usage in case of error.
     */
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



package dnsClient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class DNSlookup {

    static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
    static final int MAX_PERMITTED_ARGUMENT_COUNT = 3;
    private DatagramSocket serverSocket;
    private DNSResponse response;
    protected boolean tracingOn = false;
    protected boolean IPV6Query = false;
    protected String queryString;
    protected String dnsString;
    private static int finalQueryCount = 0;
    private static InetAddress fixedDNS;
    protected class Response{
        String ipAddress;
        boolean finalAnswer = false;
        int type;
    }


    /**
     * Constructor for the DNSlookup class
     * @param queryString the url being queried
     * @param dnsString the IP address of the rootNameServer being queried
     * @param isV6 if this query is v6
     * @param isTracing is tracing mode on
     * @param socket port to be assigned to the socket
     */
    DNSlookup(String queryString, String dnsString, boolean isV6, boolean isTracing, int socket){
        try {
            this.queryString = queryString;
            this.dnsString = dnsString;
            this.IPV6Query = isV6;
            this.tracingOn = isTracing;
            this.serverSocket = new DatagramSocket(socket);
            this.response = new DNSResponse(); // Just to force compilation
            this.serverSocket.setSoTimeout(5000);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * method to perform one iteration of querying
     * @param url the url (or query) to be found
     * @param rootNameServer the dns being queried
     * @return returns an ArrayList DNSResponse.rData object that have been decoded
     * @throws Exception
     */
    private ArrayList<DNSResponse.rData> DNSLookup(String url, InetAddress rootNameServer) throws Exception{
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
            System.out.println(this.queryString + " -2 " + "A" + " 0.0.0.0");
        }
        byte[] receiveBytes = receivePacket.getData();
        return this.response.decodeQuery(receiveBytes, this);
    }

    /**
     * This is the main method to call to perform the entire lookup. In case of only NS responses, the method
     * builds an Object of DNSlookup and finds the IP of that NS and then goes back to its original work
     * @param rootNameServer the rootNameServer to look with
     * @return returns an arrayList of Response object which states whether this is the final solution (Type A or AAAA) and the ipAddress
     * @throws Exception
     */
    public ArrayList<Response> startLookup(InetAddress rootNameServer) throws Exception{
        ArrayList<DNSResponse.rData> list = new ArrayList<DNSResponse.rData>();
        list = this.DNSLookup(this.queryString,rootNameServer);
        DNSResponse.rData nextRes = null;
        for(DNSResponse.rData c: list) {
            if(this.response.answerCount == 1){
                nextRes= list.get(0);
            }
            else {
                if (c.type == 1) {
                    nextRes=  c;
                }
                if(c.type == 6){
                    this.response.authoritative = true;
                    nextRes=  c;
                }
            }
        }
        boolean checker = true;
        while(checker) {
            if (this.response.authoritative & this.response.answerCount ==0){
                System.out.println(this.queryString+" A -6 0.0.0.0");
                break;
            }
            if (this.response.answerCount > 0) {
                //if we have an answer
                if(nextRes.type == 1 || nextRes.type == 28){
                    //if answer is of type ipV4 or ipV6
                    checker = false;
                }else if(nextRes.ns.length() > 0){
                    //if answer contains a CNAME
                    this.queryString = nextRes.ns;
                    list =  this.DNSLookup(this.queryString, fixedDNS);
                    finalQueryCount++;
                }
            }
            else{
                if(nextRes != null) {
                    if(nextRes.ipAddress == null && nextRes.ns.length() > 0){
                        int socket = (int) (Math.random() * (10000 - 8000)) + 8000;
                        DNSlookup temp = new DNSlookup(nextRes.ns, fixedDNS.toString(), this.IPV6Query, this.tracingOn,socket);
                        ArrayList<Response> here = temp.startLookup(rootNameServer);
                        list = this.DNSLookup(this.queryString, InetAddress.getByName(here.get(0).ipAddress));
                    }else {
//                        System.out.println("Querying with ip" + nextRes.ipAddress)
                        list = this.DNSLookup(this.queryString, InetAddress.getByName(nextRes.ipAddress));
                    }
                    finalQueryCount++;
                }
            }
            //If queries > 30; finish.
            if (finalQueryCount > 30) {
                System.out.println(this.queryString+" -3 0.0.0.0");
                checker = false;
            }
            boolean nextResChanged = false;
            for(DNSResponse.rData c: list) {
                if(this.response.answerCount >= 1){
                    nextRes= list.get(0);
                    nextResChanged = true;
                }
                else {
                    if (c.type == 1) {
                        nextRes=  c;
                        nextResChanged = true;
                    }
                    if(c.type == 6){
                        this.response.authoritative = true;
                        nextRes=  c;
                        nextResChanged = true;
                    }
                }
            }
            if(!nextResChanged){
                nextRes = list.get(0);
            }
        }
        ArrayList<Response> ansList = new ArrayList<>();
        if(this.response.answerCount >= 1) {
            for(int i = 0; i <this.response.answerCount; i++){
                Response ans = new Response();
                ans.ipAddress = list.get(i).ipAddress;
                ans.type = list.get(i).type;
                if (ans.type == 1 || ans.type == 28) {
                    ans.finalAnswer = true;
                }
                ansList.add(ans);
            }
        }
//        System.out.println("Length: " + ansList.size());

        this.serverSocket.close();
        return ansList;
    }

    /**
     * @param args must contain the first DNS, the URL to query and other optional inputs
     */
    public static void main(String[] args) throws Exception {
        String fqdn;

        int argCount = args.length;
        InetAddress rootNameServer;

        if (argCount < MIN_PERMITTED_ARGUMENT_COUNT || argCount > MAX_PERMITTED_ARGUMENT_COUNT) {
            usage();
            return;
        }
        boolean tracingOn =false;
        boolean isIPv6 =false;

        if (argCount == 3) {  // option provided
            if (args[2].equals("-t"))
                tracingOn = true;
            else if (args[2].equals("-6"))
                isIPv6 = true;
            else if (args[2].equals("-t6")) {
                tracingOn = true;
                isIPv6 = true;
            } else { // option present but wasn't valid option
                usage();
                return;
            }
        }
        DNSlookup looker = new DNSlookup(args[1],args[0],isIPv6,tracingOn,9876);
        rootNameServer = InetAddress.getByName(looker.dnsString);
        fixedDNS = rootNameServer;

        ArrayList<Response> test = looker.startLookup(rootNameServer);
//        System.out.println("Length: " + test.size());
        for (Response obj: test) {
            if (obj.finalAnswer) {
                System.out.println(args[1] + " " + looker.response.getActualType(obj.type) + " " + obj.ipAddress);
            }
        }
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



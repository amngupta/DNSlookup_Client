import java.awt.*;
import java.net.InetAddress;
import java.util.ArrayList;


// Lots of the action associated with handling a DNS query is processing
// the response. Although not required you might find the following skeleton of
// a DNSreponse helpful. The class below has bunch of instance data that typically needs to be 
// parsed from the response. If you decide to use this class keep in mind that it is just a 
// suggestion and feel free to add or delete methods to better suit your implementation as 
// well as instance variables.



public class DNSResponse {
    private int queryID;                  // this is for the response it must match the one in the request 
    private int answerCount = 0;          // number of answers  
    private boolean decoded = false;      // Was this response successfully decoded
    private int nsCount = 0;              // number of nscount response records
    private int additionalCount = 0;      // number of additional (alternate) response records
    private boolean authoritative = false;// Is this an authoritative record

    // Note you will almost certainly need some additional instance variables.
    class rData{
        InetAddress ipAddress;
        int name;
        int type;
        int classInt;
        long ttl;
        int rdLength;
        int totalOffset;
        rData(){
            return;
        }
    }
    // When in trace mode you probably want to dump out all the relevant information in a response
    DNSResponse(){
        return;
    }

    public static String getString(byte[] arr, int offset, int count){
        String result = "";
        for(int i = 0; i < count; i++){
            char c = (char) arr[offset+i];
            result+= c;
        }
        return result+".";
    }

    public int convertBytesToInt (byte [] arr, int offset)      // unsigned
    {
        return (arr[offset] & 0xFF) << 8 | (arr[offset+1] & 0xFF);
    }

    byte[] encodeQuery(String query) {
        byte[] encodedQuery = new byte[58];

        try {
            String[] split = query.split("\\.");
            int counter = 12;
            //FIX THIS SHIT
            int queryId = (int) (Math.random() * 255);
            encodedQuery[1] = (byte) queryId;
            encodedQuery[2] = 0;
            encodedQuery[3] = 0;
            encodedQuery[5] = 1;
            for (int i = 0; i < split.length; i++) {
                encodedQuery[counter] = (byte) split[i].length();
                counter++;
                for (int j = 0; j < split[i].length(); j++) {
                    String a = split[i];
                    char c = a.charAt(j);
                    String hexForAscii = Integer.toHexString((int) c);
                    byte test = Byte.parseByte(hexForAscii, 16);
                    encodedQuery[counter] = test;
                    counter++;
                }
            }
            encodedQuery[counter + 2] = 1;
            encodedQuery[counter + 4] = 1;
        }catch(Exception e){
            e.printStackTrace();
        }
        return encodedQuery;
    }

    public rData readRDATA(byte [] arr, int offset){
        rData rData = new rData();
        rData.name = this.convertBytesToInt(arr, offset);
        rData.type = this.convertBytesToInt(arr, offset+2);
        rData.classInt = this.convertBytesToInt(arr, offset+4);
        //rData.ttl //NEED TO CONVERT NEXT 4 bytes to ttl - 6-9
        rData.rdLength = this.convertBytesToInt(arr, offset+10);
        int innerOffset = offset+12;
        String ipAddress = "";
        if(rData.type == 1 && rData.classInt == 1 && rData.rdLength == 4){
            byte[] nextIp = new byte[4];
            nextIp[0] = arr[innerOffset];
            nextIp[1] = arr[innerOffset+1];
            nextIp[2] = arr[innerOffset+2];
            nextIp[3] = arr[innerOffset+3];
            try {
                rData.ipAddress = InetAddress.getByAddress(nextIp);
            }
            catch (Exception e) {
            }
        }
        else{
            for (int i = 0; i < rData.rdLength; i++)
            {
                int ipBits = arr[innerOffset+i];
                ipAddress += ipBits + ".";
            }
            try {
                rData.ipAddress = InetAddress.getByName(ipAddress.substring(0, ipAddress.length() - 1));
            }catch(Exception e){
//                e.printStackTrace();
            }
        }
        rData.totalOffset = 12+rData.rdLength;
        return rData;
    }

    /**
 * SAMPLE DNS RESPONSE:
 * 5F 9F 84 00 00 01 00 01 00 02 00 02 03 77 77 77 05 75 67 72 61 64 02 63 73 03 75 62 63 02 63 61 00 00 01 00 01 C0 0C 00 01 00 01 00 00 0E 10 00 04 8E 67 06 2B C0 10 00 02 00 01 00 00 0E 10 00 06 03 66 73 31 C0 10 C0 10 00 02 00 01 00 00 0E 10 00 06 03 6E 73 31 C0 16 C0 41 00 01 00 01 00 00 0E 10 00 04 C6 A2 23 01 C0 53 00 01 00 01 00 00 0E 10 00 04 8E 67 06 06
 * https://www.gasmi.net/hpd/
 * Decoder in JS:
 * https://github.com/mafintosh/dns-packet/blob/master/index.js
 * */
    String decodeQuery(byte[] response){
        InetAddress s;
        String qname = "";
        int counter = 0;
        this.queryID = this.convertBytesToInt(response, 0);
        int questionCount = this.convertBytesToInt(response, 4);
        this.answerCount = this.convertBytesToInt(response, 6);
        this.nsCount = this.convertBytesToInt(response,8);
        this.additionalCount = this.convertBytesToInt(response, 10);
        int offset = 12;
        while (response[offset] != 0){
            offset++;
        }
        System.out.println(offset + " " +response[offset+1]);
        int queryType = this.convertBytesToInt(response, offset+1);
        int queryClass = this.convertBytesToInt(response, offset+3);
        offset +=5;
        System.out.println(offset);
        //SHOULD BE TYPE BUT SOMEHOW NAME

        if(this.answerCount >= 1){
            //FOR ANSWER
            return "Answer";
        }
        int totalCount = this.nsCount + this.additionalCount;
        ArrayList<rData> rDataList = new ArrayList<rData>();
        for (int i = 0; i < totalCount; i++){
            rData r = readRDATA(response, offset);
            offset += r.totalOffset;
            System.out.println(r.type + "  "+r.classInt +" "+offset+"  "+r.ipAddress);
            rDataList.add(r);
        }
        // DNS Lookup for the returned NS
//        if(r.type  == 2){
//            try {
//                InetAddress rDataIP = InetAddress.getByName(r.ipAddress);
//                DNSlookup temp = new DNSlookup();
//                System.out.println(rDataIP);
//                temp.DNSLookup("www.ugrad.cs.ubc.ca", rDataIP);
//            }catch(Exception e){
//                e.printStackTrace();
//            }
//        }

        byte[] nextIp = new byte[4];
        for(byte c: response) {
            if (c == 1)
                if (response[counter+2] == 1)
                    if(response[counter+8] == 4){
                        nextIp[0] = response[counter+9];
                        nextIp[1] = response[counter+10];
                        nextIp[2] = response[counter+11];
                        nextIp[3] = response[counter+12];
                        break;
                    }
            counter++;
        }
        try {
            s = InetAddress.getByAddress(nextIp);
            System.out.print(s);
        }
        catch (Exception e) {
        }


        return "";
    }



	void dumpResponse() {


	}

    // The constructor: you may want to add additional parameters, but the two shown are 
    // probably the minimum that you need.

	public DNSResponse (byte[] data, int len) {
	    
	    // The following are probably some of the things 
	    // you will need to do.
	    // Extract the query ID

	    // Make sure the message is a query response and determine
	    // if it is an authoritative response or note

	    // determine answer count

	    // determine NS Count

	    // determine additional record count

	    // Extract list of answers, name server, and additional information response 
	    // records
	}


    // You will probably want a methods to extract a compressed FQDN, IP address
    // cname, authoritative DNS servers and other values like the query ID etc.


    // You will also want methods to extract the response records and record
    // the important values they are returning. Note that an IPV6 reponse record
    // is of type 28. It probably wouldn't hurt to have a response record class to hold
    // these records. 
}



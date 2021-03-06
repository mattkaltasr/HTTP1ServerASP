



import java.net.*;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import java.io.*;
import java.text.*;


public class HTTP1ServerASP{

    public static void main(String args[]) throws Exception {
        int portNum = 0;

        try {
            portNum = Integer.parseInt(args[0]);
        } catch (NumberFormatException conect) {
            System.out.print(" cannot format " + portNum);
            System.exit(-1);

        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("Please provide port.");
            System.exit(-1);
        }
        // end of catch

        // create Threadpool
        ThreadPoolServer serv = new ThreadPoolServer(portNum);
        new Thread(serv).start();

        // sleep for pool server threads to function properly
        try {
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    //end of main }

}

class ThreadPoolServer implements Runnable {

    protected boolean done = false; //used to end and check
    protected int portNumber;
    protected int counter = 0;  // used to count incoming threads
    protected Thread current = null;// current thread

    //create blocking queue for threads
    LinkedBlockingQueue<Runnable> servQueue = new LinkedBlockingQueue<Runnable>(5);

// create a thread pool with 5 threads hanging max 50 threads idle 5 min
    ThreadPoolExecutor myPool = new ThreadPoolExecutor(5, 50, 300000, TimeUnit.MILLISECONDS, servQueue);

    //object constructor instantiates the object
    ThreadPoolServer(int portNumber) {
        this.portNumber = portNumber;

    }// end of constructor

    public void run() {

        synchronized (this) {
            this.current = Thread.currentThread();
        }

        // declare sockets to get instantiated in try catch
        ServerSocket serverSocket = null;
        Socket ClientSocket = null;

        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Server started at port " + portNumber + " ... ");
        } catch (IOException | IllegalStateException e) {
            if (done) {
                System.out.println(" Error creating clientSocket or thread   :" + e.getMessage());
                return;
            }
        }// end of first try

        // to handle possible response to client
        DataOutputStream clientOut = null;
        while (true) {

            try {
                //check counter if 50 will deny connection
                if (counter == 50) {

                    clientOut = new DataOutputStream(ClientSocket.getOutputStream());
                    clientOut.writeBytes("HTTP/1.0 503 Service Unavailable");
                    clientOut.close();
                    ClientSocket.close();

                }// end of if
                else {
                    ClientSocket = serverSocket.accept();
                    counter++;
                    System.out.println("********** conection sucessful **********");
                    this.myPool.execute(new WorkThread(ClientSocket));
                }
            }/// end of try
            catch (IOException e) {

                if (done) {
                    System.out.println("server stopped running");
                    break;

                }// if done

            }// end of catch

        }// end of while

        // shut server off
        this.myPool.shutdown();
        try {
            this.done = true;
            serverSocket.close();
            return;
        } // end try
        catch (IOException e) {
            System.out.println(" closing socket error " + e.getLocalizedMessage());
        }

    }// end of run method
}// end of ThreadPool

//__________________________________________________________
//all the thread work below in worker thread
class WorkThread implements Runnable {

    // all variables needed for run method  and mime content
    public String sendHeader = null; /// string header to send as responce
    public byte[] messageBody = null;  // array of bytes to send in body of client message
    public static final String MIME_HTML = "text/html";
    public static final String MIME_PLAINTEXT = "text/plain";
    public static final String MIME_TEXT = "text/plain";
    public static final String MIME_GIF = "image/gif";
    public static final String MIME_JPG = "image/jpg";
    public static final String MIME_PNG = "image/png";
    public static final String MIME_PDF = "application/pdf";
    public static final String MIME_OCTET_STREAM = "application/octet-stream";
    public static final String MIME_XGZIP = "application/x_gzip";
    public static final String MIME_ZIP = "application/zip";
    public String dataQuery = null;  /// public global for post data
    public String getData = null;
    String fromUser;
    String UserAgent;
    String setCookie = null;
    String Cookies = null;
    String requestMethod = null;
    public int lengthOfContent;  // global for parsing content length
    public int contentLength;
    HashMap<String, String> env = new HashMap<String, String>();
    ArrayList<String> setCookies = new ArrayList<>();
    ArrayList<String> allCookies = new ArrayList<>();
    Socket wSocket;

    // constructor for class
    WorkThread(Socket wSocket) {
        this.wSocket = wSocket;

    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        //method variables
        Date ModifiedSince = null;
        String request = null;
        String requestHeader = null;
        DataOutputStream clientOut = null;
        BufferedReader clientIn = null;
        PrintWriter outHtml = null;
        String responce = null;
        String headerReponce = null;
        String[] postHeaderRequest = new String[5];
        postHeaderRequest[0] = null;
        postHeaderRequest[1] = null;

        int contLen = -1;

        try {
            wSocket.setSoTimeout(3000); // set time out to 3 sec
            clientOut = new DataOutputStream(wSocket.getOutputStream());
            outHtml = new PrintWriter(wSocket.getOutputStream(), true);
            clientIn = new BufferedReader(new InputStreamReader(wSocket.getInputStream()));
            request = clientIn.readLine();
            requestHeader = clientIn.readLine();
            System.out.println("req header first = " + requestHeader);
            System.out.println("request = " + request);
            //System.out.println("rheader = " + requestHeader); 
            if (requestHeader != null && requestHeader.startsWith("If-Modified-Since")) {
                ModifiedSince = parseRequestHead(requestHeader);
            }// end of header if
            else if (request.startsWith("POST") && requestHeader != null) {
                //loop for post or gets with content ect
                // System.out.println("In POST");
                requestMethod = "POST";
                while (requestHeader.length() > 0) {
                    if (requestHeader.startsWith("From:")) {
                        fromUser = requestHeader;
                        // System.out.println(fromUser);
                    }
                    if (requestHeader.startsWith("User-Agent:")) {
                        UserAgent = requestHeader;
                        //  System.out.println(UserAgent);
                    }
                    if (requestHeader.startsWith("Content-Type:")) {
                        postHeaderRequest[0] = requestHeader;
                    }
                    if (requestHeader.startsWith("Content-Length:")) {
                        postHeaderRequest[1] = requestHeader;
                    }
                    if (requestHeader.startsWith("Cookie:")) {
                        postHeaderRequest[3] = requestHeader;
                        Cookies = requestHeader;
                        allCookies.add(requestHeader.split("[:]")[1].trim() + ";");
                    }
                    if (requestHeader.startsWith("Set-Cookie:")) {
                        postHeaderRequest[2] = requestHeader;
                        setCookie = requestHeader;
                        setCookies.add(requestHeader.split("[:]")[1].trim());
                    }
                    requestHeader = clientIn.readLine();
                    System.out.println("reqested headers : " + requestHeader);
                }// end of while loop
                StringBuilder builder = new StringBuilder();
                while (clientIn.ready()) {
                    builder.append((char) clientIn.read());
                }
                System.out.println("Payload data = " + builder.toString());
                dataQuery = builder.toString();
                System.out.println("dq = " + dataQuery);
                String[] datavalues = dataQuery.split("[&]");
                int payload = datavalues.length;
                String name = null, namev = null, pass = null, passv = null;
                System.out.println("dq  = " + dataQuery);
                for (int i = 0; i < datavalues.length; ++i) {
                    System.out.println("now = " + datavalues[i]);
                    String cdata = datavalues[i].substring(datavalues[i].indexOf('=') + 1);
                    String cname = datavalues[i].substring(0, datavalues[i].indexOf('='));
                    cname = cname.trim();
                    System.out.println("cname = " + cname + " cdata = " + cdata);
                    if (cname != null) {
                        System.out.println("cname = " + cname + " cdata = " + cdata);
                        if ("products".equals(cname)) {
                            setCookie = "Set-Cookie: cart=" + cdata + "; expires=" + "Tue, 20 Jul 2029 14:13:49 GMT";
                            setCookies.add(setCookie);
                        } else if ("name".equals(cname)) {
                            setCookie = "Set-Cookie: " + cname + "=" + cdata + "; expires=" + genTimeDelay(3);
                            setCookies.add(setCookie);
                            setCookie = "Set-Cookie: cart=" + "" + "; expires=" + "Tue, 20 Jul 2029 14:13:49 GMT";
                            setCookies.add(setCookie);
                            System.out.println("name setcokies = " + setCookie);
                        } else if ("carted".equals(cname)) {
                            if (cdata.contains("total")) {
                                String resetCookies = "";
                                for (int j = 0; j < allCookies.size(); ++j) {
                                    String[] values = allCookies.get(j).split(";");
                                    for (int k = 0; k < values.length; ++k) {
                                        String[] parts = values[k].split("=");
                                        String fname = null, fval = null;
                                        fname = parts[0];
                                        if (fname != "name") {
                                            resetCookies += fname + "=" + "; ";
                                        }
                                    }
                                }
                                allCookies.clear();
                                allCookies.add(resetCookies);
                            } else {
                                String delOne = "";
                                ArrayList<String> narr = new ArrayList<>();
                                for (int j = 0; j < allCookies.size(); ++j) {
                                    String cook = allCookies.get(i);
                                    if (cook.contains(cdata)) {
                                        cook = cook.replace(cdata, "");
                                        narr.add(cook);
                                    } else {
                                        narr.add(cook);
                                    }
                                }
                                allCookies.clear();
                                allCookies = narr;
                            }
                        }
                        System.out.println("cookies setted = " + setCookie);
                    }

                }
            }//end if post
            else if (request.startsWith("GET") && requestHeader != null) {
                System.out.println("get request header");
                requestMethod = "GET";
                while (requestHeader.length() > 0) {
                    if (requestHeader.startsWith("From:")) {
                        fromUser = requestHeader;
                        // System.out.println(fromUser);
                    }
                    if (requestHeader.startsWith("User-Agent:")) {
                        UserAgent = requestHeader;
                        //  System.out.println(UserAgent);
                    }
                    if (requestHeader.startsWith("Content-Type:")) {
                        postHeaderRequest[0] = requestHeader;
                    }
                    if (requestHeader.startsWith("Content-Length:")) {
                        postHeaderRequest[1] = requestHeader;
                    }
                    if (requestHeader.startsWith("Cookie:")) {
                        postHeaderRequest[3] = requestHeader;
                        Cookies = requestHeader;
                        allCookies.add(requestHeader.split("[:]")[1].trim());
                    }
                    if (requestHeader.startsWith("Set-Cookie:")) {
                        postHeaderRequest[2] = requestHeader;
                        setCookie = requestHeader;
                        setCookies.add(requestHeader.split("[:]")[1].trim());
                    }
                    requestHeader = clientIn.readLine();
                    System.out.println("get headers = " + requestHeader);
                }// end of while loop
                //System.out.println("GET method");
                StringTokenizer tokenize = new StringTokenizer(request, " ");
                String token = null, xd;
                int idx = 0;
                while (tokenize.hasMoreTokens()) {
                    xd = tokenize.nextToken();
                    if (idx == 1) {
                        token = xd;
                    }
                    //System.out.println(xd + idx);
                    idx++;
                }
                //System.out.println("token selected = " + token);
                if (token != null) {
                    idx = token.indexOf('?');
                    dataQuery = token.substring(idx + 1);
                }
                //System.out.println("dataq = " + dataQuery);
            } //end if get

            if (dataQuery != null) {
                dataQuery = urlDecode(dataQuery);
            }

            // decodes pay-load prior to split
            // System.out.println("Finally");
            // System.out.println(dataQuery + " <------decoded ");
            // System.out.println(dataQuery.length()  + " <------decoded ");
            // System.out.println("calling responce ... ");
            responce = ParseRequest(request, ModifiedSince, postHeaderRequest);//.getBytes();
            //also for debuging
            //System.out.println("out of responce parse...>");
            System.out.println(sendHeader + " ----send header ");
            //System.out.println(messageBody + "---message body ");
            //System.out.println(responce + "------responce header ");
            //outHtml.println(sendHeader);
            //outHtml.println(responce);
            if (sendHeader != null && messageBody != null) {
                clientOut.writeBytes(sendHeader);
                clientOut.write(messageBody, 0, messageBody.length);
                //outHtml.println(responce);
            } else {
                //clientOut.write(responce,0,responce.length());
                if (sendHeader != null) {
                    clientOut.writeBytes(sendHeader);
                }
                clientOut.writeBytes(responce);
                //outHtml.println(responce);
            }

        }// end of try
        catch (SocketTimeoutException e) {
            System.out.println("Error. Request Time out ; " + e.getMessage());
            try {
                clientOut.writeBytes("HTTP/1.0 408 Request Timeout");
            } catch (IOException ex) {
                System.out.println("Error while sending 408 :" + ex.getMessage());
                // if unable send 500 message
                try {
                    clientOut.writeBytes("HTTP/1.0 500 Internal Error");
                } catch (IOException exc) {
                    System.out.println("Error while sending 500 Internal Error:" + exc.getMessage());
                }
            }
        } catch (IOException exc) {
            System.out.println("Error while running " + exc.getMessage());
            // if unable send 500 message
            try {
                clientOut.writeBytes("HTTP/1.0 500 Internal Server Error");
            } catch (IOException exd) {
                System.out.println("Error while sending 500 Internal Error:" + exd.getMessage());
            }
        }

        //closeConnection(Thread.currentThread(), wSocket, clientIn, outHtml);
        closeConnection(Thread.currentThread(), wSocket, clientIn, clientOut);

    }/// end of run method

    private void closeConnection(Thread currentThread, Socket wSocket2, BufferedReader clientIn,
            DataOutputStream clientOut) {
        Thread thread = currentThread;
        try {
            clientOut.flush();
            clientIn.close();
            thread.sleep(500);
            wSocket.close();
        } catch (IOException | InterruptedException except) {
            System.out.println("Error while closing connections: " + except.getMessage());
            return;
        }

    }

    public String genHTML() {
        String html = "";
        html += "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<body>\n"
                + "\n"
                + "<form method=\"POST\" action=\"/cgi-bin/store.cgi\">\n"
                + "Name:<br>\n"
                + "<input type=\"text\" name=\"name\">\n"
                + "<br>\n"
                + "Password:<br>\n"
                + "<input type=\"password\" name=\"password\">\n"
                + "<br><br>\n"
                + "<input type=\"submit\" value=\"Submit\">\n"
                + "<input type=\"reset\">\n"
                + "</form> \n"
                + "\n"
                + "</body>\n"
                + "</html>";
        return html;
    }

   
	public String genTimeDelay(int minute) {
        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date today = new Date();
        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(today); 
        // assigns calendar to given date
        System.out.println("was = " + dateFormat.format(today));
       calendar.set(minute, (calendar.get(minute)+3));
        //today.setMinutes(today.getMinutes() + 3);
        String tdate = dateFormat.format(today);
        System.out.println("now = " + tdate);
        return tdate;
    }

    public String genInfinite() {
        String Day[] = {"", "Sun", "Mon", "Tue", "Wed", "Thr", "Fri", "Sat"};
        String month[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String tdelay = "";
        tdelay += Day[Calendar.DAY_OF_WEEK] + ", ";
        tdelay += Calendar.DAY_OF_MONTH + "-";
        tdelay += month[Calendar.MONTH] + "-";
        tdelay += (Calendar.YEAR + 5) + " ";
        tdelay += Calendar.HOUR_OF_DAY + ":";
        tdelay += (Calendar.MINUTE) + ":";
        tdelay += Calendar.SECOND + "GMT;";
        return tdelay;
    }

    public String genHTML2(String name) {
        String html = "";
        html += "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<meta http-equiv=\"set-cookie\" content=\"name=" + name + "; expires=Sat, 25-Nov-2080 12:00:00 GMT; \">\n"
                + "<meta http-equiv=\"set-cookie\" content=\"cart=" + name + "; expires=" + genTimeDelay(3) + " \">\n"
                + "<body>\n"
                + "\n"
                + "<h1><strong>Hello " + name + "!</strong></h1>\n"
                + "<form method=\"POST\" action=\"/cgi-bin/store.cgi\">\n"
                + " <select name=\"products\">\n"
                + "    <option value=\"p1 10\">P1 10$</option>\n"
                + "    <option value=\"p2 15\">P2 15$</option>\n"
                + "    <option value=\"p3 20\">P3 20$</option>\n"
                + "    <option value=\"p4 12\">P4 12$</option>\n"
                + "    <option value=\"p5 18\">P5 18$</option>\n"
                + "  </select>\n"
                + "  <input type=\"submit\" value=\"Add to Cart\">"
                + "</form> \n"
                + "\n"
                + "</body>\n"
                + "</html>";
        return html;
    }

    public String processCgi(String filePath, String arguments[]) throws IOException {
        // System.out.println("filepath = " + filePath);
        String argss = "";
        if (arguments != null) {
            for (int i = 0; i < arguments.length; ++i) {
                argss += " ";
                argss += arguments[i];
            }
        }
        System.out.println("args = " + argss);
        Process process = null;
        try {
            Runtime runtime = Runtime.getRuntime();
            // System.out.println("Got runtime");
            process = runtime.exec("php ." + filePath + argss);
            System.out.println("Done Process");
        } catch (Exception e) {
            System.out.println("Error occured = " + e.getMessage());
        }
        BufferedReader after = new BufferedReader(new InputStreamReader(process.getInputStream()));
        //System.out.println("Buffered Reader");
        String output = after.readLine();
        //System.out.println("output = " + output);
        String worker = output;
        while (worker != null) {
            worker = after.readLine();
            //System.out.println(output);
            if (worker != null) {
                output += worker;
            }
        }
        //System.out.println("returned = " + output);
        return output;
    }

    public String urlDecode(String url) {
        String decode = "";

        try {
            decode = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block

        }
        return decode;
    }

    public void parsetokens(String resource) throws UnknownHostException {
        String delimcol = "[:]";
        String[] parcestuff;
        String trial = Integer.toString(dataQuery.length());
        env.put("CONTENT_LENGTH", trial);
        env.put("REQUEST_METHOD", requestMethod);
        //System.out.println(dataQuery.length()+"-----------in parse tokens");
        env.put("SCRIPT_NAME", resource);
        if (fromUser != null) {
            parcestuff = fromUser.split(delimcol);
            //	System.out.println(parcestuff[1]);
            String temp = parcestuff[1].trim();
            //	System.out.println(temp);
            env.put("HTTP_FROM", temp);
            parcestuff = null;
        }
        if (UserAgent != null) {
            parcestuff = UserAgent.split(delimcol);
            //	System.out.println(parcestuff[1]);
            String temp = parcestuff[1].trim();
            //	System.out.println(temp);
            env.put("HTTP_USER_AGENT", temp);
        }
        String server = InetAddress.getLocalHost().getHostName();
        int port = this.wSocket.getPort();
        String portNum = Integer.toString(port);
        env.put("SERVER_NAME", server);
        env.put("SERVER_PORT", portNum);
        if (Cookies != null || !allCookies.isEmpty()) {
            String temp = "";
            for (int i = 0; i < allCookies.size(); ++i) {
                //System.out.println("Cookie : " + temp);
                temp += allCookies.get(i);
                parcestuff = null;
            }
            temp = temp.trim();
            if (!temp.endsWith(";")) {
                temp += ";";
            }
            System.out.println("temp = " + temp);

            env.put("HTTP_COOKIE", temp);
        } else {
            System.out.println("No Cookies");
        }
        System.out.println("setcookie = " + setCookie);
        if (setCookie != null || !setCookies.isEmpty()) {
            for (int i = 0; i < setCookies.size(); ++i) {
                parcestuff = setCookies.get(i).split(delimcol);
                String temp = "", bst = setCookies.get(i).trim();
                if (!bst.endsWith(":")) {
                    for (int j = 1; j < parcestuff.length; ++j) {
                        if (j == 1) {
                            temp += parcestuff[j].trim();
                        } else {
                            temp += ":" + parcestuff[j].trim();
                        }
                    }
                }
                //System.out.println("Set-Cookie : " + temp);
                //env.put("HTTP_COOKIE", temp);
                parcestuff = null;
            }
        } else {
            System.out.println("Not set cookie");
        }
    }

    private String ParseRequest(String request, Date modifiedSince, String[] postHeaderRequest) throws IOException {
        String lineSeparator = "\r\n";  // unused put in method instead works dont want to delete stuff
        String deliminator = "[ ]";
        String[] tokens = request.split(deliminator);
        String command = "badcommand";
        String resource = "badresource";
        String version = "badversion";
        float vers;
        String delim = "[&]";
        String[] datavalues = null;
        String byteString = null;
        // puts commands and resourse and http type into seperate tokens
        if (tokens.length == 3) {
            command = tokens[0].trim();
            resource = tokens[1].trim();
            version = tokens[2].trim();
        }
        System.out.println("Command = " + command);
        System.out.println("resource = " + resource);
        System.out.println("version = " + version);
        if (resource.contains("?")) {
            getData = resource.substring(resource.indexOf('?') + 1);
            resource = resource.substring(0, resource.indexOf('?')).trim();
            if (getData.length() <= 0) {
                getData = null;
            }
        }
        //System.out.println("res = " + resource);
        //looks at number of token elements should have no more than or less than 3
        if (tokens.length != 3 || !resource.startsWith("/") || !command.equals(command.toUpperCase()) || command.toUpperCase().equals("KICK") || !version.substring(0, 5).equals("HTTP/")) {
            //	System.out.println("in parse method  --- bad request" );
            return "HTTP/1.0 400 Bad Request";
        }
        //Parse the HTTp version into a float so i can compare it
        // in a try catch in case format is wrong and throws error
        try {
            vers = Float.parseFloat(version.substring(version.length() - 3));
        } catch (NumberFormatException except) {
            System.out.println("Error in parsing version: " + except.getMessage());
            System.out.println("in parse method  --- internal");
            return "HTTP/1.0 500 Internal Server Error";
        } // end of catch

        //Check for HTTP version greater than 1.0
        if (vers > 1.0) {
            //System.out.println("in parse method  --- 505 returned" );
            //return "HTTP/1.0 505 HTTP Version Not Supported";
        }

        //Check if command is a valid supported methods of the server
        if (!command.equals("POST") && !command.equals("HEAD") && !command.equals("GET")) {
            //	System.out.println("in parse method  --- 501 returned" );
            return "HTTP/1.0 501 Not Implemented";
        }

        if (resource.startsWith("top_secret") || resource.contains("secret") || resource.contains("top_secret.txt")) {
            File mfile = new File("." + resource);
            if (!mfile.canRead()) {
                //	System.out.println("in parse method  --- 403 returned" );
                return "HTTP/1.0 403 Forbidden";
            }
        }
        if ((command.equals("POST"))) {
            //parsing of length
            if (postHeaderRequest[1] == null) {
                //	System.out.println("in parse method  --- 411 returned" );
                return "HTTP/1.0 411 Length Required";
            }
            try {
                int index = postHeaderRequest[1].indexOf(':') + 1;
                String len = postHeaderRequest[1].substring(index).trim();
                contentLength = Integer.parseInt(len);
                //System.out.println(contentLength  + "---contentLength");
            } catch (NumberFormatException except) {
                //    System.out.println("Error in parsing contentLength: " + except.getMessage());
                //   System.out.println("in parse method  --- 411 returned" );
                return "HTTP/1.0 411 Length Required";
            }

            //System.out.println("checking content length with 0");
            if (contentLength == 0) //no data query formated
            {
                return "HTTP/1.0 204 No Content";
            }

            if (postHeaderRequest[0] == null) {
                // System.out.println("in parse method postHeaderRequest[0]==null  --- 500 returned");
                return "HTTP/1.0 500 Internal Server Error";
            }

            // corrected if
            if (!resource.endsWith(".cgi")) {
                //	System.out.println("in parse method  --- 405 returned" );
                return "HTTP/1.0 405 Method Not Allowed";
            }
            if (resource.endsWith(".cgi")) {
                // System.out.println("In cgi");
                File mFile = new File("." + resource);
                if (!mFile.exists()) {
                    return "HTTP/1.0 404 Not Found";
                }
                if (!mFile.canExecute()) {
                    //System.out.println("in parse method cant execute --- 403 returned");
                    return "HTTP/1.0 403 Forbidden";
                }
                if (!mFile.exists()) {
                    return "HTTP/1.0 404 Not Found";
                }

            }//end if test excute
            // System.out.println("Con-len = " + contentLength);
            if (contentLength <= 0) //no data query formated
            {
                dataQuery = null;
            }
            if (dataQuery == null) {
                // System.out.println("in parse method from dataQuery==null  --- 500 returned" );
                return "HTTP/1.0 500 Internal Server Error";
            }
            //check if proper format
            if (!dataQuery.contains("=")) {
                //System.out.println("DataQ = " + dataQuery);
                return "HTTP/1.0 500 Internal Server Error";
            }

//            if (payload < 2) {
//                String header = addHeader(resource, command, modifiedSince, "z");
//                String response = addBody(header, byteString);
//                setHeader(header);
//                System.out.println("Not enough Parameters");
//                String genHtmltoPost = genHTML();
//                return genHtmltoPost;
//            }
            System.out.println("Exiting post..");
        }//end post if
        // need a if to parse query
        // if  no 3 sectiond seperating return 501
        // below is the reponces after all the checking is done
        /**
         * *************All checking is completed, below handles each supported
         * server requests GET,POST, and HEAD HTTP ******************
         */
        //  System.out.println("in parse method  --- checks done " );
        //COMMAND is a valid GET
        if (command.equals("GET")) {
            //Declare Buffered reader to be instantiated in try/catch block.
            //System.out.println("REQUEST = " + request);
            System.out.println("Inside Response GET");
            //System.out.println("res = " + resource);
            if (resource.endsWith(".cgi")) {
                String header = addHeader(resource, command, modifiedSince);
//                String response = addBody(header, byteString);
                setHeader(header);
                if (resource.endsWith("service.cgi")) {
                    return "<!DOCTYPE html>\n"
                            + "                <html>\n"
                            + "            <head><title>Services</title></head>\n"
                            + "            <body>\n"
                            + "            <form method=\"POST\" action=\"/cgi-bin/service.cgi\">\n"
                            + "            <input type=\"file\" name=\"file\">\n"
                            +"<br>"
                            + "              <input type=\"text\" name=\"text\">\n"
                            +"<br>"
                            + "            <input type=\"submit\" name=\"submit\" value=\"Submit\"></form> \n"
                            + "\n"
                            + "            </body>\n"
                            + "            </html></body> </html>";
                }
                return genHTML();
            }
            BufferedReader reader;
            try {
                String result = "";
                String currLine = "";
                //Open a file containing the specified resource. Store it in a byte[]
                //instantiate byte string with file encoding
                File file = new File("." + resource);
                FileInputStream finStream = new FileInputStream(file);
                byte fileBytes[] = new byte[(int) file.length()];
                finStream.read(fileBytes);
                byteString = new String(fileBytes);
                //Close file input stream
                if (finStream != null) {
                    finStream.close();
                }
                System.out.println("File Read Done!");
                if (getData != null) { //for experiment
                    datavalues = getData.split(delim);
                    for (int i = 0; i < datavalues.length; ++i) {
                        String nval = datavalues[i].trim();
                        //System.out.println("Before = " + nval);
                        datavalues[i] = nval.substring(nval.indexOf('=') + 1);
                        //System.out.println("after = " + datavalues[i]);
                    }
                }
                //String res = processCgi(resource, datavalues); //through stdin
                // return res;
                //System.out.println("response = " + res);
                //No exceptions were thrown, so get the header and the body and send it back in a response.

                String header = addHeader(resource, command, modifiedSince);
                String response = addBody(header, byteString);
                setHeader(header);
                setBody(fileBytes);
//                if (resource.endsWith("store.cgi")) {
//                    System.out.println("Near End GET");
//                    String genHtmltoPost = genHTML();
//                    return genHtmltoPost;
//                } else {
                //  return res;
                //}
//                //System.out.println(response);
                return response;

            } catch (FileNotFoundException except) {
                //  System.out.println("Cannot find file: " + except.getMessage());
                return "HTTP/1.0 404 Not Found";
            } catch (IOException ex) {
                System.out.println("Internal error " + ex.getMessage());
                return "HTTP/1.0 500 Internal Server Error";
            }

        }

        //Command is a valid POST
        if (command.equals("POST")) {
            //System.out.println("in post ------");
            String delimequal = "[=]";
            String[] comands = null;
            byte[] output = null;
            char[] outbuf = null;
            char inread;
            int limit = 64000;
            int nread;
            datavalues = dataQuery.split(delim);
            for (int i = 0; i < datavalues.length; ++i) {
                String nval = datavalues[i];
                //System.out.println("Before = " + nval);
                datavalues[i] = nval.substring(nval.indexOf('=') + 1);
                //System.out.println("after = " + datavalues[i]);
            }

            //String got = processCgi(resource, datavalues);
            //String header = addHeader(resource, command, modifiedSince);
            //String response = addBody(header, byteString);
            //setHeader(header);
////                setBody(fileBytes);
//            return got;
//            /// needs to be worked on
//            //all above past if here contentLength  hold INT  datavalues[] hold split querey
            File postFile = new File("." + resource); //used for path ect for process
            ProcessBuilder Post = new ProcessBuilder(postFile.getAbsolutePath());
            env.clear();

            Map<String, String> enviroment = Post.environment();
            enviroment.clear();
            //  System.out.println(postFile.getAbsolutePath());
            Post.redirectErrorStream(true);
//
            try {

                parsetokens(resource);// method to set hash map
                //	System.out.println("in post hash map values ---");
//
//                //	env.forEach((key, value) -> System.out.println(key + " " + value)); /// prints out for decoding
//                // ads all variables to map<string,string>
                enviroment.putAll(env);
//                // print for debug
                System.out.println("in post environment values  map values ---");
                enviroment.forEach((key, value) -> System.out.println(key + ":" + value));
                //System.out.println("in post try---");
//                //	BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                Process process = Post.start();
                OutputStream stdin = process.getOutputStream();
                InputStream stdout = process.getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(stdin));
//                //	System.out.println("in post -message--");
                outbuf = dataQuery.toCharArray();
                //System.out.println("outbuff = " + Arrays.toString(outbuf));
                //System.out.println("in post -message--"  + outbuf.length);
                writer.write(dataQuery);
                writer.flush();
                writer.flush();
                writer.flush();
                System.out.println("post written..");
                //stops here
                //reads all bytes
                StringBuilder sb = new StringBuilder();
                System.out.println("post first read ");
                File temp = new File("./gen/temp.html");
                if (temp.exists()) {
                    temp.delete();
                }
                temp = new File("./gen/temp.html");
                FileWriter fileWriter = new FileWriter(temp);
                //write string to file
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                while ((nread = reader.read()) != -1) {
                    inread = (char) nread;
                    bufferedWriter.append(inread);
                    sb.append(inread);
                }
                byteString = sb.toString();
                System.out.println(byteString + "-------BYTE String --------- " + byteString.length());
                // used to save new html code
                bufferedWriter.flush();
                bufferedWriter.close();
                fileWriter.close();
                // just so something goes not part needs work
            } catch (IOException e) {
                // TODO Auto-generated catch block
                System.out.println("Not worked " + e.getMessage());;
            }
            //all done call get to return results
            request = ("GET /gen/temp.html HTTP/1.1");
            Date ModifiedSince = null;
            String response = ParseRequest(request, ModifiedSince, postHeaderRequest);//.getBytes()
            //contentLength=byteString.length();
            System.out.println(response);
            //not valid needs work
            String header = addHeader(resource, command, modifiedSince);
            String body = addBody(header, byteString);
            //System.out.println("body = " + body);
            setHeader(header);
            return response;
        }

        // Command is a valid HEAD (return only header, no body)
        if (command.equals("HEAD")) {

            File file = new File(".", resource);
            Date lastModified = new Date(file.lastModified());

            //Only return the header if the command is a HEAD
            String result = addHeader(resource, command, modifiedSince);
            return result;
        }
        return null;
    }

    private void setHeader(String header) {
        sendHeader = header;
        return;

    }

    private void setBody(byte[] fileBytes) {
        messageBody = fileBytes;
        return;
    }

    private String addBody(String header, String byteString) {
        String lineSeparator = "\r\n";

        return header + byteString + lineSeparator + lineSeparator;
    }

    private String addHeader(String resource, String command, Date modifiedSince) {
        File file = new File(".", resource);
        //Set DateFormat to GMT. Use dateFormat to format the input date.
        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date lastModifiedDate = new Date(file.lastModified());
        //Declare a date format string and a date object to store modSince and lastModified in Gmt Format.
        String modSinceGmt = "";
        Date modSinceGmtDate = null;
        String lastModifiedGmt = "";
        Date lastModifiedDateGmt = null;
        lastModifiedGmt = dateFormat.format(lastModifiedDate);
        //Logic for formatting dates.
        if (modifiedSince != null) {
            modSinceGmt = dateFormat.format(modifiedSince);
            //	System.out.println("LAST MODIFIED GMT = " + lastModifiedGmt);
            try {
                modSinceGmtDate = dateFormat.parse(modSinceGmt);
                lastModifiedDateGmt = dateFormat.parse(lastModifiedGmt);
            } catch (Exception except) {
                System.out.println("Error: " + except.getMessage());
            }
        }
        String lineSeparator = "\r\n";
        if (modSinceGmtDate != null && lastModifiedDateGmt != null) {
            //Check to see if file has been modified since the If-modifice-since header field. If yes, return error code.
            if (lastModifiedDateGmt.before(modSinceGmtDate) && !command.equals("HEAD")) {
                return "HTTP/1.0 304 Not Modified" + lineSeparator + "Expires: Wed, 24 Jul 2020 14:13:49 GMT" + lineSeparator;
            }
        }
        //System.out.println("res = " + getContentType(resource));
        String[] cookies;
        String getCookies, cookieadd = "";
        if (setCookie != null || !setCookies.isEmpty()) {
            for (int i = 0; i < setCookies.size(); ++i) {
                cookies = setCookies.get(i).split("[:]");
                getCookies = "";
                for (int j = 1; j < cookies.length; ++j) {
                    if (j > 1) {
                        getCookies += ":" + cookies[j].trim();
                    } else {
                        getCookies += cookies[j].trim();
                    }
                }
                cookieadd += lineSeparator + "Set-Cookie: " + getCookies;
            }
        }

        //Header to be returned.
        // puts header together to return to client
        String input = ("HTTP/1.0 200 OK"
                + lineSeparator
                + "Content-Type: " + getContentType(resource)
                + lineSeparator
                + "Content-Length: " + file.length()
                + cookieadd
                + lineSeparator
                + "Last-Modified: " + lastModifiedGmt
                + lineSeparator
                + "Content-Encoding: identity"
                + lineSeparator
                + "Allow: GET, POST, HEAD"
                + lineSeparator
                + "Expires: Tue, 20 Jul 2019 14:13:49 GMT"
                + lineSeparator
                + lineSeparator);

        if (command.equals("POST")) {
            input = ("HTTP/1.0 200 OK"
                    + lineSeparator
                    + "Content-Type: " + getContentType(resource)
                    + lineSeparator
                    + "Content-Length: " + contentLength
                    + cookieadd
                    + lineSeparator
                    + "Last-Modified: " + lastModifiedGmt
                    + lineSeparator
                    + "Content-Encoding: identity"
                    + lineSeparator
                    + "Allow: GET, POST, HEAD"
                    + lineSeparator
                    + "Expires: Tue, 20 Jul 2019 14:13:49 GMT"
                    + lineSeparator
                    + lineSeparator);
        }
        return input;
    }

// gets mime type returns string for mime typing
    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm")
                || fileRequested.endsWith(".html")) {
            return MIME_HTML;
        } else if (fileRequested.endsWith(".txt")) {
            return MIME_TEXT;
        } else if (fileRequested.endsWith(".gif")) {
            return MIME_GIF;
        } else if (fileRequested.endsWith(".png")) {
            return MIME_PNG;
        } else if (fileRequested.endsWith(".jpg")
                || fileRequested.endsWith(".jpeg")) {
            return MIME_JPG;
        } else if (fileRequested.endsWith(".class")
                || fileRequested.endsWith(".jar")) {
            return MIME_OCTET_STREAM;
        } else if (fileRequested.endsWith(".pdf")) {
            return MIME_PDF;
        } else if (fileRequested.endsWith(".gz")
                || fileRequested.endsWith(".gzip")) {
            return MIME_XGZIP;
        } else if (fileRequested.endsWith(".zip")) {
            return MIME_ZIP;
        } else if (fileRequested.endsWith(".cgi")) {
            return MIME_HTML;
        } else {
            return MIME_OCTET_STREAM;
        }
    }

    //Method to get content length
    // not called thought might need
    private long getContentLength(File resource) {
        return resource.length();
    }

    // method to return format  of date
    private Date parseRequestHead(String requestHeader) {
        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = null;
        try {
            date = dateFormat.parse(requestHeader.substring(19));
        } catch (Exception except) {
            System.out.println(except.getMessage());
            return null;
        }
        return date;
    }

}/// end of worthread class


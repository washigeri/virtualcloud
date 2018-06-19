import com.madhukaraphatak.sizeof.SizeEstimator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("SpellCheckingInspection")
public class MiniServerSocketExample {
    private static final int PORT = 8080;
    static AtomicLong dataReceived = new AtomicLong(0L);
    private static long t0;
    private static boolean started = false;
    private static boolean endloop = false;

    public static void main(String[] args) {
        Thread runner = new Thread() {
            @Override
            public void run() {
                try {
                    ServerSocket server = new ServerSocket(PORT);
                    System.out.println("MiniServer active " + PORT);
                    while (!endloop && !Thread.currentThread().isInterrupted()) {
                        if (!started) {
                            started = true;
                            t0 = System.nanoTime();
                        }
                        new ThreadSocket(server.accept());
                    }
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }
        };
        runner.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            br.readLine();
            System.out.println("Stopping");
            endloop = true;
            //runner.join();
            runner.interrupt();
            long t1 = System.nanoTime();
            System.out.println("Ellipsed time : " + (t1 - t0));
            System.out.println("Received data : " + dataReceived.get());
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}

class ThreadSocket extends Thread {
    private Socket insocket;

    ThreadSocket(Socket insocket) {
        this.insocket = insocket;
        this.start();
    }

    @Override
    public void run() {
        try {
            InputStream is = insocket.getInputStream();
            PrintWriter out = new PrintWriter(insocket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            line = in.readLine();
            System.out.println("HTTP-HEADER: " + line);
            // looks for post data
            int postDataI = -1;
            while ((line = in.readLine()) != null && (line.length() != 0)) {
                // System.out.println("HTTP-HEADER: " + line);
                if (line.contains("Content-Length:")) {
                    postDataI = new Integer(
                            line.substring(
                                    line.indexOf("Content-Length:") + 16,
                                    line.length()));
                }
            }
            String postData = "";
            // read the post data
            if (postDataI > 0) {
                char[] charArray = new char[postDataI];
                //noinspection ResultOfMethodCallIgnored
                in.read(charArray, 0, postDataI);
                postData = new String(charArray);
            }
            System.out.println("Received data from " + insocket.getInetAddress() + " : " + postData);
            MiniServerSocketExample.dataReceived.addAndGet(SizeEstimator.estimate(postData));
            out.println("HTTP/1.0 200 OK");
            out.println("Content-Type: text/html; charset=utf-8");
            out.println("Server: MINISERVER");
            // this blank line signals the end of the headers
            out.println("");
            // Send the HTML page
            //out.println("<H1>Welcome to the Mini Server</H1>");
            //out.println("<H2>Request Method->" + request_method + "</H2>");
            out.println("<H2>Post-> DATA OK </H2>");
            //out.println("<form name=\"input\" action=\"form_submited\" method=\"post\">");
            //out.println("Username: <input type=\"text\" name=\"user\"><input type=\"submit\" value=\"Submit\"></form>");
            out.close();
            insocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package org.pipelineexample.apm;

import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import org.json.simple.JSONArray; 
import org.json.simple.JSONObject; 

public class LowBudgetKafka {

    private static final String LOCALHOST = "127.0.0.1";
    private static final String FIRST_MESSAGE = "First Message";
    private static final String END_MESSAGE = "Message Procesed at SINK Ending Pipeline";

    private final int inPort;
    private final int outPort;
    private final InfoConsole infoConsole;
    private boolean burnCpus;
    private JSONObject jsonMessage;


    public LowBudgetKafka(int inPort, int outPort, InfoConsole infoConsole, boolean burnCpus) {
        this.inPort = inPort;
        this.outPort = outPort;
        this.infoConsole = infoConsole;
        this.burnCpus = burnCpus;
    }


    public void sendMessage(String message) throws IOException, InterruptedException {
        if (outPort == -1) {
            infoConsole.info(END_MESSAGE);
        } else {
            infoConsole.info("Sending : \"" + message + "\" to port " + outPort);
            try(ServerSocket serverSocket = new ServerSocket(outPort);
                Socket socket = serverSocket.accept();
                OutputStream os = socket.getOutputStream();
                PrintWriter pw = new PrintWriter(os, true)) {
                pw.println(message);
            }
        }
        while (burnCpus) {
            infoConsole.info("~ ~~~~ ZZZZ - zzzz - zzzzz ~~~~~~~~~ ~~~ z ~~~~ ~ ~ z ~ ~");
            TimeUnit.SECONDS.sleep(15);
        }
        infoConsole.info("**** Bye ****\n-------------------------------------------------------------------------------------");
    }

    public String readMessage() throws IOException, InterruptedException {
        infoConsole.info("**** Started ****");
        if (inPort == -1) {
            JSONObject message = new JSONObject();
            message.put("payload_message", "FIRST_MESSAGE");
            message.put("other_stuff", "otherstuff message");
            return message.toString();
        }
        while (true) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (Socket client = new Socket(LOCALHOST, inPort);
                 InputStream inFromServer = client.getInputStream();
                 DataInputStream in = new DataInputStream(inFromServer)) {
                byte b = in.readByte();
                while (b != -1){
                    bos.write(b);
                    b = in.readByte();
                }
                bos.close();
            } catch (ConnectException e) {
                TimeUnit.SECONDS.sleep(1);
            } catch (EOFException e) {
                String message = new String(bos.toByteArray());
                bos.close();
                message = message.replace("\n", "");
                infoConsole.info("Received: \"" + message + "\" on port: " + inPort);
                return message;
            } finally {
                bos.close();
            }
        }
    }

}

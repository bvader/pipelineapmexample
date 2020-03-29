package org.pipelineexample.apm.processor;

import co.elastic.apm.api.*;
import jdk.nashorn.internal.runtime.ParserException;

import org.pipelineexample.apm.LowBudgetKafka;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PipelineProcessor {

    private static final String PARENT_TRANSACTION_NAME = "ToyPipelineTxn";

    private final String name;
    private final LowBudgetKafka communicationChannel;
    private final ProcessorType type;
    private String message;
    private JSONObject jsonMessageObectObect;
    private JSONParser jsonParser;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    public PipelineProcessor(String name, LowBudgetKafka communicationChannel, ProcessorType type) {
        this.name = name;
        this.communicationChannel = communicationChannel;
        this.type = type;

    }

    public void process() throws InterruptedException, IOException, ParseException {
        String inMessage = communicationChannel.readMessage();
        String outMessage = processMessage(inMessage);
        communicationChannel.sendMessage(outMessage);
    }

    /**
     * Wraps APM Transaction/Span around business logic.
     */
    private String processMessage(String message) throws InterruptedException, ParseException {

        // Convert message from String to JSONObject
        this.message = message;
        try {
            jsonParser = new JSONParser();
            jsonMessageObectObect = (JSONObject) jsonParser.parse(message);
        } catch (ParseException e) {
            e.printStackTrace();
            throw e;
        }

        Transaction transaction = createTransaction();
        Span span = transaction.startSpan();

        try {
            System.out.println("In Process Message... at " + dateFormat.format(new Date()));

            span.injectTraceHeaders(this::injectParentTransactionId);
            span.setName(name + "-span");
            thisIsActuallyABusinessLogic();
            String newMessage = "message processed by " + name;
            jsonMessageObectObect.put("payload_message", newMessage);
            return jsonMessageObectObect.toString();
        } catch (Exception e) {
            transaction.captureException(e);
            span.captureException(e);
            throw e;
        } finally {
            span.end();
            System.out.println("Transaction End : ####");
            transaction.end();
        }
    }

    private Transaction createTransaction() {
        Transaction transaction;
        if (type == ProcessorType.SOURCE) {
            System.out.println("Creating transaction new, processor type = " + type);
            transaction = ElasticApm.startTransaction();
        } else {
            System.out.println("Creating transaction with remote parent, processor type = " + type);
            transaction = ElasticApm.startTransactionWithRemoteParent(key -> extractKey(key));
        }
        transaction.setName(name + "-txn");
        return transaction;
    }

    /**
     * Some useful work.
     */
    private void thisIsActuallyABusinessLogic() throws InterruptedException {
        Random random = new Random();
        int processTime = random.nextInt(5) + 1;
        // int processTime = 3;
        System.out.println("processTime time in seconds = " + processTime);
        TimeUnit.SECONDS.sleep(processTime);
    }

    private void injectParentTransactionId(String key, String value) {
        System.out.println("header key : " + key);
        System.out.println("header value : " + value);
        jsonMessageObectObect.put(key, value);
    }

    private String extractKey(String key) {
        return (String) jsonMessageObectObect.get(key);
    }
}

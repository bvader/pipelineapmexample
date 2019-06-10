package org.pipelineexample.apm.processor;

import co.elastic.apm.api.*;
import org.pipelineexample.apm.LowBudgetKafka;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PipelineProcessor {

    private static final String PARENT_TRANSACTION_NAME = "ToyPipelineTxn";

    private final String name;
    private final LowBudgetKafka communicationChannel;
    private final ProcessorType type;
    private String message;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    public PipelineProcessor(String name, LowBudgetKafka communicationChannel, ProcessorType type) {
        this.name = name;
        this.communicationChannel = communicationChannel;
        this.type = type;

    }

    public void process() throws InterruptedException, IOException {
        String inMessage = communicationChannel.readMessage();
        String outMessage = processMessage(inMessage);
        communicationChannel.sendMessage(outMessage);
    }

    /**
     * Wraps APM Transaction/Span around business logic.
     */
    private String processMessage(String message) throws InterruptedException {
        this.message = message;
        System.out.println("In Process Message... at " + dateFormat.format(new Date()));
        Transaction transaction = createTransaction(message);
        Span span = transaction.startSpan();
        try {
           span.injectTraceHeaders(this::injectParentTransactionId);
           span.setName(name+"-span");
           thisIsActuallyABusinessLogic();
           return this.message + " processed by " + name;
        } catch (Exception e) {
            transaction.captureException(e);
            span.captureException(e);
            throw e;
        } finally {
            System.out.println("Before Span End");
            span.end();
            transaction.end();
            System.out.println("After Span End");
        }
    }


    private Transaction createTransaction(String message) {
        Transaction transaction;
        if (type == ProcessorType.SOURCE) {
            System.out.println("Creating transaction new, processor type = " + type);
            transaction = ElasticApm.startTransaction();
        } else {
            System.out.println("Creating transaction with remote parent, processor type = " + type);
            transaction = ElasticApm.startTransactionWithRemoteParent(key -> extractKey(key, message));
        }
        transaction.setName(name+"-txn");
        return transaction;
    }

    /**
     * Some useful work.
     */
    private void thisIsActuallyABusinessLogic() throws InterruptedException {
        Random random = new Random();
        //int processTime = random.nextInt(5) + 1;
        int processTime = 3;
        System.out.println("processTime time in seconds = " + processTime);
        TimeUnit.SECONDS.sleep(processTime);
    }

    private void injectParentTransactionId(String key, String value) {
        System.out.println("header key : " + key);
        System.out.println("header value : " + value);
        removeOldKey(key);
        message = "<" + key + ":" + value + "> " + message;

    }

    private void removeOldKey(String key) {
        Pattern pattern = getKeyPattern(key);
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            message = message.substring(0, matcher.start()) + message.substring(matcher.end());
        }
    }

    private String extractKey(String key, String message) {
        Matcher matcher = getKeyPattern(key).matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Pattern getKeyPattern(String key) {
        return Pattern.compile("<" + key + ":(.+)> ");
    }
}

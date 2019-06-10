# Pipeline APM example
Just a toy example of pipeline that consist of several java processes communicating by sockets.
## Expected result
This is a desirable result in APM Kibana, created with GIMP.

![It would be nice to have something like this](imgs/apm-example.png?raw=true "APM Kibana: Expected with parent")

Other possible option might be the same only without parent transaction.

![It would be nice to have something like this](imgs/apm-example2.png?raw=true "APM Kibana: Expected wihtout parent")


## Quick start
Run
```
./run-pipeline.sh
```

## More details
`run-pipeline.sh` builds the project and executes the result jar file several times, to create a pipeline that consist of several
java processes (one for each [processor](src/main/java/org/pipelineexample/apm/processor/PipelineProcessor.java)).
Number of processors and ports that they use for sending/receiving messages is defined in [run-pipeline.sh](run-pipeline.sh).
Each [processor](src/main/java/org/pipelineexample/apm/processor/PipelineProcessor.java) except `Source`(just a fancy name for the first [processor](src/main/java/org/pipelineexample/apm/processor/PipelineProcessor.java), last is `Sink`) waits for message on incoming port,
process it and write new message to outgoing port. `Source` does not read message, it just sends "First message" string.

```
     +---------+         +----------+         +----------+           +----------+
     |         | MESSAGE |          | MESSAGE |          | MESSAGE   |          |
     |Source   |+------->|Processor1|+------->|Processor2|+--------->|Sink      |
     +---------+         +----------+         +----------+           +----------+
```

Processor "Business logic" is executed in [`PipelineProcessor.thisIsActuallyABusinessLogic`](src/main/java/org/pipelineexample/apm/processor/PipelineProcessor.java)

```java
    private void thisIsActuallyABusinessLogic() throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
    }
```
[`PipelineProcessor.processMessage`](src/main/java/org/pipelineexample/apm/processor/PipelineProcessor.java) is used to wrap this logic with APM Transactions

```java
/**
 * Wraps APM Transaction/Span around business logic.
 */
private String processMessage(String message) throws InterruptedException {
    this.message = message;
    System.out.println("In Process Message... at " + dateFormat.format(new Date()));
    Transaction transaction = createTransaction(message);
    //transaction.injectTraceHeaders(this::injectParentTransactionId);
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
    // Random random = new Random();
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

```
## Actual result
 - All transactions and spans are created with correct parents but the timing of the distributed trace is not correct.
 - The duration of the entire flow is ~3 seconds, e.g. execution time of single processor but should be closer to 12 seconds total time.

![But we actually have this](imgs/actual_resultnew.png?raw=true "APM Kibana: Actual")

## `follows_from` relation
It could also be OK for us not to have parent transaction, but have span following one after another, like

![It would be nice to have something like this](imgs/apm-example2.png?raw=true "APM Kibana: Other OK result")


but [APM open trace bridge documentation](https://www.elastic.co/guide/en/apm/agent/java/current/opentracing-bridge.html) says
```
Currently, this bridge only supports child_of references. Other references,
like follows_from are not supported yet.
```
so not sure if it is possible.

## P.S.
Our real processors are listeners, so they do listen Kafka in a never ending loop. If this is the reason, this processor
also can run in a loop mode, by setting `burnCpus` property to `true` in [pipeline.properties](pipeline.properties).

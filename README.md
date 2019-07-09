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
The apm server / config properties can be found or configured in [run-processor.sh](run-processor.sh)

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
         span.end();
         transaction.end();
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
```
## Actual result
 - After Elastic 7.2 Release All transactions and spans are created with correct parents and now the timing looks pretty good! 
 - The duration of the entire flow is ~12 seconds in the waterfall, there still seems to be a minor issues with the Duration value. We will see if that gets fixed in 7.3 release.

![Now we actually have this](imgs/actual_result_new_7_2.png?raw=true "APM Kibana: Actual")


## P.S.
Our real processors are listeners, so they do listen Kafka in a never ending loop. If this is the reason, this processor
also can run in a loop mode, by setting `burnCpus` property to `true` in [pipeline.properties](pipeline.properties).

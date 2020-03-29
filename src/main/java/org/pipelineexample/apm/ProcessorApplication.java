package org.pipelineexample.apm;

import org.pipelineexample.apm.processor.PipelineProcessor;
import org.pipelineexample.apm.processor.PipelineProcessorFactory;

import java.io.IOException;
import org.json.simple.parser.ParseException;

/**
 * Builds and run processor which properties is defined by arguments and 'pipeline.properties'
 */
public class ProcessorApplication {

    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        if (args.length < 2) {
            throw new IllegalArgumentException("Expect 2 arguments: number of processor and comma separated list of ports");
        }
        boolean burnCpus = args.length > 2 && Boolean.valueOf(args[2]);
        PipelineProcessor pipelineProcessor = new PipelineProcessorFactory(args[1]).buildPipelineProcessor(args[0], burnCpus);
        pipelineProcessor.process();
    }

}

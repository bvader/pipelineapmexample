package org.pipelineexample.apm.processor;

import org.pipelineexample.apm.InfoConsole;
import org.pipelineexample.apm.LowBudgetKafka;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipelineProcessorFactory {

    private final List<Integer> ports;

    public PipelineProcessorFactory(String portsString) {
        this(portsString.split(","));
    }

    public PipelineProcessorFactory(String[] ports) {
        this(Stream.of(ports).map(Integer::valueOf).collect(Collectors.toList()));
    }

    public PipelineProcessorFactory(List<Integer> ports) {
        if (ports.size() <= 2) {
            throw new IllegalArgumentException("Number of ports must be > 2");
        }
        this.ports = ports;
    }

    public PipelineProcessor buildPipelineProcessor(String arg, boolean burnCpus) {
        Integer currentNumber = Integer.valueOf(arg);
        ProcessorType type = getType(currentNumber);
        String name = getName(currentNumber, type);
        int inPort = getPort(currentNumber, true);
        int outPort = getPort(currentNumber, false);
        InfoConsole infoConsole = new InfoConsole(name);
        LowBudgetKafka kafka = new LowBudgetKafka(inPort, outPort, infoConsole, burnCpus);
        return new PipelineProcessor(name, kafka, type);
    }

    private int getPort(Integer currentNumber, boolean isIn) {
        if ((currentNumber == 1 && isIn) || (currentNumber.equals(ports.size() + 1) && !isIn)) {
            return -1;
        }
        int index = isIn ? currentNumber - 2 : currentNumber - 1;
        return ports.get(index);
    }

    private String getName(Integer currentNumber, ProcessorType type) {
        switch (type) {
            case SINK:
                return "Sink";
            case SOURCE:
                return "Source";
            case PROCESSOR:
                return "Processor-" + (currentNumber - 1);
            default:
                throw new IllegalArgumentException("Unsupported type " + type);
        }
    }

    private ProcessorType getType(Integer currentNumber) {
        if (1 == currentNumber) {
            return ProcessorType.SOURCE;
        }
        if (currentNumber.equals(ports.size() + 1)) {
            return ProcessorType.SINK;
        }
        return ProcessorType.PROCESSOR;
    }
}

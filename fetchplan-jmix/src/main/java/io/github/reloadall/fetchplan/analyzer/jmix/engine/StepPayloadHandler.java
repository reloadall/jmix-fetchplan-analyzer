package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.util.List;

import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;

public interface StepPayloadHandler {

    boolean supports(StepPayload payload);

    List<Continuation> handle(RawTree rawTree,
                              AnalysisStep step,
                              EngineContext context);

}

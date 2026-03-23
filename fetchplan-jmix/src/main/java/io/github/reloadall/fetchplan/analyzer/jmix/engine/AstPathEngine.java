package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.visited.VisitedKey;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.visited.VisitedKeyFactory;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_AstPathEngine")
public class AstPathEngine {

    private final List<StepPayloadHandler> payloadHandlers;
    private final EngineContext context;
    private final VisitedKeyFactory visitedKeyFactory;
    private final AnalysisTrace analysisTrace;

    @Autowired
    public AstPathEngine(List<StepPayloadHandler> payloadHandlers,
                         EngineContext context,
                         VisitedKeyFactory visitedKeyFactory,
                         AnalysisTrace analysisTrace) {
        this.payloadHandlers = Objects.requireNonNull(payloadHandlers, "payloadHandlers is null");
        this.context = Objects.requireNonNull(context, "context is null");
        this.visitedKeyFactory = Objects.requireNonNull(visitedKeyFactory, "visitedKeyFactory is null");
        this.analysisTrace = Objects.requireNonNull(analysisTrace, "analysisTrace is null");
    }

    public RawTree analyze(MethodDeclaration method, String rootParamName) {
        Objects.requireNonNull(method, "method is null");
        Objects.requireNonNull(rootParamName, "rootParamName is null");

        RawTree rawTree = new RawTree();
        RawNode rootNode = rawTree.createRoot(rootParamName);

        Deque<AnalysisStep> queue = new ArrayDeque<>();
        queue.addLast(AnalysisStep.start(method, rootParamName, rootNode));

        Set<VisitedKey> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            AnalysisStep step = queue.removeFirst();
            processStep(rawTree, step, queue, visited);
        }

        return rawTree;
    }

    private void processStep(RawTree rawTree,
                             AnalysisStep step,
                             Deque<AnalysisStep> queue,
                             Set<VisitedKey> visited) {
        VisitedKey visitedKey = visitedKeyFactory.build(step);
        if (!visited.add(visitedKey)) {
            analysisTrace.logMethodEntry(step.getMethod());
            enqueueFinishContinuationIfAny(step, queue);
            return;
        }
        analysisTrace.logMethodEntry(step.getMethod());

        StepPayloadHandler handler = findHandler(step.getPayload());
        if (handler == null) {
            return;
        }

        List<Continuation> continuations = handler.handle(rawTree, step, context);
        for (Continuation continuation : continuations) {
            queue.addLast(continuation.toStep());
        }
    }

    private void enqueueFinishContinuationIfAny(AnalysisStep step, Deque<AnalysisStep> queue) {
        if (step.getPayload() instanceof StatementsPayload statementsPayload) {
            Continuation continuationOnFinish = statementsPayload.getContinuationOnFinish();
            if (continuationOnFinish != null) {
                queue.addLast(continuationOnFinish.toStep());
            }
        }
    }

    private StepPayloadHandler findHandler(StepPayload payload) {
        for (StepPayloadHandler handler : payloadHandlers) {
            if (handler.supports(payload)) {
                return handler;
            }
        }
        return null;
    }
}

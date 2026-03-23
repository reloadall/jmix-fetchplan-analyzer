package io.github.reloadall.fetchplan.analyzer.jmix.engine.visited;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StepPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import org.springframework.stereotype.Component;

@Component("fpa_VisitedKeyFactory")
public class VisitedKeyFactory {

    public VisitedKey build(AnalysisStep step) {
        return new VisitedKey(
                buildMethodKey(step.getMethod()),
                buildPayloadKey(step.getPayload()),
                step.getCurrentRawNode().getId(),
                buildBindingsKey(step.getBindings())
        );
    }

    private String buildMethodKey(MethodDeclaration method) {
        String owner = method.findAncestor(TypeDeclaration.class)
                .map(TypeDeclaration::getNameAsString)
                .orElse("<unknown-type>");

        String params = method.getParameters().stream()
                .map(parameter -> parameter.getType().asString())
                .collect(Collectors.joining(","));

        return owner + "#" + method.getNameAsString() + "(" + params + ")" + "@" + rangeKey(method);
    }

    private String buildPayloadKey(StepPayload payload) {
        if (payload instanceof StatementsPayload statementsPayload) {
            return buildStatementsPayloadKey(statementsPayload);
        }

        return payload.getClass().getName();
    }

    private String buildStatementsPayloadKey(StatementsPayload payload) {
        String bodyKey = buildStatementsContainerKey(payload);

        if (payload.hasCurrentStatement()) {
            return "STATEMENTS:CUR:" + nodeKey(payload.currentStatement())
                    + ":BODY=" + bodyKey
                    + ":FIN=" + buildContinuationKey(payload.getContinuationOnFinish());
        }

        return "STATEMENTS:END:" + bodyKey
                + ":FIN=" + buildContinuationKey(payload.getContinuationOnFinish());
    }

    private String buildStatementsContainerKey(StatementsPayload payload) {
        if (payload.getStatements().isEmpty()) {
            return "EMPTY";
        }

        Statement first = payload.getStatements().get(0);
        Statement last = payload.getStatements().get(payload.getStatements().size() - 1);

        return "SIZE=" + payload.getStatements().size()
                + ":FIRST=" + nodeKey(first)
                + ":LAST=" + nodeKey(last);
    }

    private String buildContinuationKey(Continuation continuation) {
        if (continuation == null) {
            return "null";
        }

        return continuation.getMethod().getNameAsString()
                + "@RAW=" + continuation.getCurrentRawNode().getId()
                + "@PAYLOAD=" + shallowPayloadKey(continuation.getPayload());
    }

    private String shallowPayloadKey(StepPayload payload) {
        if (payload instanceof StatementsPayload statementsPayload) {
            if (statementsPayload.hasCurrentStatement()) {
                return "STATEMENTS:" + nodeKey(statementsPayload.currentStatement());
            }
            return "STATEMENTS:END:" + buildStatementsContainerKey(statementsPayload);
        }

        return payload.getClass().getSimpleName();
    }

    private String buildBindingsKey(Map<String, ValueBinding> bindings) {
        return bindings.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey()
                        + "="
                        + entry.getValue().getNodes().stream()
                        .map(RawNode::getId)
                        .sorted()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","))
                        + "|u=" + entry.getValue().isUncertain())
                .collect(Collectors.joining("||"));
    }

    private String nodeKey(Node node) {
        return rangeKey(node) + "#" + node.getClass().getSimpleName();
    }

    private String rangeKey(Node node) {
        return node.getRange()
                .map(this::formatRange)
                .orElse("NO_RANGE@" + System.identityHashCode(node));
    }

    private String formatRange(Range range) {
        return range.begin.line + ":" + range.begin.column + "-" + range.end.line + ":" + range.end.column;
    }
}

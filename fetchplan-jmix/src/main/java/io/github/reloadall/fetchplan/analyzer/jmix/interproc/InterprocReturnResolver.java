package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.ast.stmt.ReturnStmt;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.stereotype.Component;

@Component("fpa_InterprocReturnResolver")
public class InterprocReturnResolver {

    public Optional<RawNode> resolveReturnNode(RawTree rawTree,
                                               com.github.javaparser.ast.body.MethodDeclaration targetMethod,
                                               Map<String, RawNode> targetBindings,
                                               RawNode entryAnchor,
                                               EngineContext context) {
        List<ReturnStmt> returnStatements = targetMethod.findAll(ReturnStmt.class).stream()
                .filter(returnStmt -> returnStmt.getExpression().isPresent())
                .toList();

        if (returnStatements.isEmpty()) {
            return Optional.empty();
        }

        AnalysisStep syntheticStep = new AnalysisStep(
                targetMethod,
                StatementsPayload.from(targetMethod),
                entryAnchor,
                new LinkedHashMap<>(targetBindings)
        );

        List<RawNode> resolvedReturnNodes = new ArrayList<>();

        for (ReturnStmt returnStmt : returnStatements) {
            RawNode resolved = context.getExpressionResolver().resolve(
                    rawTree,
                    syntheticStep,
                    returnStmt.getExpression().get(),
                    context
            );

            if (resolved == null) {
                return Optional.empty();
            }

            resolvedReturnNodes.add(resolved);
        }

        RawNode first = resolvedReturnNodes.get(0);
        boolean allSame = resolvedReturnNodes.stream()
                .allMatch(node -> node.getId().equals(first.getId()));

        return allSame ? Optional.of(first) : Optional.empty();
    }
}

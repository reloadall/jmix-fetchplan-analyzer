package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;

class LambdaElementBindingSupport {

    ScopeElements resolveScopeElements(RawTree rawTree,
                                       AnalysisStep step,
                                       MethodCallExpr methodCallExpr,
                                       EngineContext context) {
        ExpressionResolutionResult scopeResult = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                methodCallExpr.getScope().get(),
                context
        );
        if (scopeResult.isEmpty()) {
            return new ScopeElements(
                    scopeResult.isUncertain()
                            ? ExpressionResolutionResult.uncertainEmpty()
                            : ExpressionResolutionResult.empty(),
                    Set.of()
            );
        }

        Set<RawNode> elementNodes = new LinkedHashSet<>();
        for (RawNode scopeNode : scopeResult.getNodes()) {
            RawNode elementNode = rawTree.addChild(
                    scopeNode,
                    null,
                    FlowKind.COLLECTION_ELEMENT,
                    null,
                    UsageKind.INTERMEDIATE
            );
            elementNodes.add(elementNode);
        }

        return new ScopeElements(scopeResult, elementNodes);
    }

    AnalysisStep createLambdaStep(AnalysisStep originalStep,
                                  LambdaExpr lambdaExpr,
                                  Set<RawNode> elementNodes,
                                  boolean uncertain) {
        Map<String, ValueBinding> lambdaBindings = new LinkedHashMap<>(originalStep.copyBindings());
        lambdaBindings.put(
                lambdaExpr.getParameter(0).getNameAsString(),
                new ValueBinding(elementNodes, uncertain)
        );

        return new AnalysisStep(
                originalStep.getMethod(),
                originalStep.getPayload(),
                originalStep.getCurrentRawNode(),
                lambdaBindings
        );
    }

    ExpressionResolutionResult resolveLambdaBody(RawTree rawTree,
                                                AnalysisStep lambdaStep,
                                                LambdaExpr lambdaExpr,
                                                EngineContext context) {
        Statement body = lambdaExpr.getBody();
        if (body.isExpressionStmt()) {
            return context.getExpressionResolver().resolveAll(
                    rawTree,
                    lambdaStep,
                    body.asExpressionStmt().getExpression(),
                    context
            );
        }

        if (!body.isBlockStmt()) {
            return ExpressionResolutionResult.empty();
        }

        ExpressionResolutionResult merged = ExpressionResolutionResult.empty();
        for (Statement statement : body.asBlockStmt().getStatements()) {
            if (!statement.isExpressionStmt()) {
                continue;
            }

            ExpressionResolutionResult statementResult = context.getExpressionResolver().resolveAll(
                    rawTree,
                    lambdaStep,
                    statement.asExpressionStmt().getExpression(),
                    context
            );
            merged = merged.merge(statementResult);
        }

        return merged;
    }

    record ScopeElements(ExpressionResolutionResult scopeResult, Set<RawNode> elementNodes) {

    }
}
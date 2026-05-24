package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_ForEachLambdaExpressionHandler")
@Order(165)
public class ForEachLambdaExpressionHandler implements ExpressionHandler {

    @Override
    public boolean supports(Expression expression) {
        if (!expression.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        return "forEach".equals(methodCallExpr.getNameAsString())
                && methodCallExpr.getScope().isPresent()
                && methodCallExpr.getArguments().size() == 1
                && methodCallExpr.getArgument(0).isLambdaExpr()
                && methodCallExpr.getArgument(0).asLambdaExpr().getParameters().size() == 1;
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        LambdaExpr lambdaExpr = methodCallExpr.getArgument(0).asLambdaExpr();

        ExpressionResolutionResult scopeResult = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                methodCallExpr.getScope().get(),
                context
        );
        if (scopeResult.isEmpty()) {
            return scopeResult.isUncertain()
                    ? ExpressionResolutionResult.uncertainEmpty()
                    : ExpressionResolutionResult.empty();
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

        Map<String, ValueBinding> lambdaBindings = new LinkedHashMap<>(step.copyBindings());
        lambdaBindings.put(
                lambdaExpr.getParameter(0).getNameAsString(),
                new ValueBinding(elementNodes, scopeResult.isUncertain())
        );

        AnalysisStep lambdaStep = new AnalysisStep(
                step.getMethod(),
                step.getPayload(),
                step.getCurrentRawNode(),
                lambdaBindings
        );

        ExpressionResolutionResult bodyResult = resolveLambdaBody(rawTree, lambdaStep, lambdaExpr.getBody(), context);
        markTerminal(bodyResult);
        return new ExpressionResolutionResult(bodyResult.getNodes(), scopeResult.isUncertain() || bodyResult.isUncertain());
    }

    private ExpressionResolutionResult resolveLambdaBody(RawTree rawTree,
                                                         AnalysisStep lambdaStep,
                                                         Statement body,
                                                         EngineContext context) {
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

    private void markTerminal(ExpressionResolutionResult result) {
        for (RawNode node : result.getNodes()) {
            node.setUsageKind(UsageKind.TERMINAL);
        }
    }
}
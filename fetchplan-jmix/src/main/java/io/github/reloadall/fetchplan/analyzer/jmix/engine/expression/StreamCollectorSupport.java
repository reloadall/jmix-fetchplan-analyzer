package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.Optional;

import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;

final class StreamCollectorSupport {

    private StreamCollectorSupport() {
    }

    static boolean isSupportedCollectorsScope(MethodCallExpr collectorCall) {
        if (collectorCall.getScope().isEmpty()) {
            return true;
        }

        String scope = collectorCall.getScope().get().toString();
        return "Collectors".equals(scope) || "java.util.stream.Collectors".equals(scope);
    }

    static void markTerminal(ExpressionResolutionResult result) {
        for (RawNode node : result.getNodes()) {
            node.setUsageKind(UsageKind.TERMINAL);
        }
    }

    static boolean isSupportedReturnLikeLambdaBody(LambdaExpr lambdaExpr) {
        if (lambdaExpr.getBody().isExpressionStmt()) {
            return true;
        }

        return lambdaExpr.getBody().isBlockStmt()
                && lambdaExpr.getBody().asBlockStmt().getStatements().stream().anyMatch(statement -> statement.isReturnStmt());
    }

    static Optional<String> resolveBackedMethodReferenceField(AnalysisStep step,
                                                             MethodReferenceExpr methodReferenceExpr,
                                                             GetterPropertyAccessResolver resolver) {
        String ownerTypeName = methodReferenceExpr.getScope().toString();
        return resolver.resolveBackedPropertyName(
                step.getMethod(),
                ownerTypeName,
                methodReferenceExpr.getIdentifier()
        );
    }
}
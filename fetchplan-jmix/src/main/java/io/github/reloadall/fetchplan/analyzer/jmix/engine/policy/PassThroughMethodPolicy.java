package io.github.reloadall.fetchplan.analyzer.jmix.engine.policy;

import java.util.Set;

import com.github.javaparser.ast.expr.MethodCallExpr;
import org.springframework.stereotype.Component;

@Component("fpa_PassThroughMethodPolicy")
public class PassThroughMethodPolicy {

    private static final Set<String> PASS_THROUGH_METHODS = Set.of(
            "stream",
            "distinct",
            "forEach",
            "filter",
            "sorted",
            "peek",
            "limit",
            "skip"
    );

    public boolean isPassThrough(MethodCallExpr methodCallExpr) {
        return methodCallExpr.getScope().isPresent()
                && PASS_THROUGH_METHODS.contains(methodCallExpr.getNameAsString());
    }
}

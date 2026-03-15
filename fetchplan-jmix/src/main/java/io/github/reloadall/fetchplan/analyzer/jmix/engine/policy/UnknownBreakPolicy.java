package io.github.reloadall.fetchplan.analyzer.jmix.engine.policy;

import com.github.javaparser.ast.expr.Expression;
import org.springframework.stereotype.Component;

@Component("fpa_UnknownBreakPolicy")
public class UnknownBreakPolicy {

    public boolean shouldCreateForVariableInitializerFailure(String variableName, Expression initializer) {
        return variableName != null && !variableName.isBlank();
    }

    public boolean shouldCreateForAssignmentFailure(String variableName, Expression value) {
        return variableName != null && !variableName.isBlank();
    }

    public boolean shouldCreateForReturnFailure(Expression expression) {
        return true;
    }
}

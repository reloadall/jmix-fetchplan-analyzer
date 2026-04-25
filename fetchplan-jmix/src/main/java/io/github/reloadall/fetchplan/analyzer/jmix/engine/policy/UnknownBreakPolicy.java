package io.github.reloadall.fetchplan.analyzer.jmix.engine.policy;

import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import org.springframework.stereotype.Component;

@Component("fpa_UnknownBreakPolicy")
public class UnknownBreakPolicy {

    public boolean shouldCreateForVariableInitializerFailure(String variableName, Expression initializer) {
        if (isPureObjectCreation(initializer)) {
            return false;
        }
        return variableName != null && !variableName.isBlank();
    }

    public boolean shouldCreateForAssignmentFailure(String variableName, Expression value) {
        if (isPureObjectCreation(value)) {
            return false;
        }
        return variableName != null && !variableName.isBlank();
    }

    public boolean shouldCreateForReturnFailure(Expression expression) {
        if (isNullLiteral(expression)) {
            return false;
        }
        return true;
    }

    private boolean isPureObjectCreation(Expression expression) {
        if (expression == null) {
            return false;
        }

        if (expression.isObjectCreationExpr()) {
            return true;
        }

        if (expression.isEnclosedExpr()) {
            EnclosedExpr enclosedExpr = expression.asEnclosedExpr();
            return isPureObjectCreation(enclosedExpr.getInner());
        }

        if (expression.isCastExpr()) {
            CastExpr castExpr = expression.asCastExpr();
            return isPureObjectCreation(castExpr.getExpression());
        }

        return false;
    }

    private boolean isNullLiteral(Expression expression) {
        if (expression == null) {
            return false;
        }

        if (expression.isNullLiteralExpr()) {
            return true;
        }

        if (expression.isEnclosedExpr()) {
            return isNullLiteral(expression.asEnclosedExpr().getInner());
        }

        if (expression.isCastExpr()) {
            return isNullLiteral(expression.asCastExpr().getExpression());
        }

        return false;
    }
}

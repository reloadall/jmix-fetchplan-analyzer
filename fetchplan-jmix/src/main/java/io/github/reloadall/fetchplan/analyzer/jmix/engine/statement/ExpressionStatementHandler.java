package io.github.reloadall.fetchplan.analyzer.jmix.engine.statement;

import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementHandleResult;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.policy.UnknownBreakPolicy;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocCallPlan;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocCallPlanner;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_ExpressionStatementHandler")
@Order(100)
public class ExpressionStatementHandler implements StatementHandler {

    private final UnknownBreakPolicy unknownBreakPolicy;
    private final InterprocCallPlanner interprocCallPlanner;

    @Autowired
    public ExpressionStatementHandler(UnknownBreakPolicy unknownBreakPolicy,
                                      InterprocCallPlanner interprocCallPlanner) {
        this.unknownBreakPolicy = unknownBreakPolicy;
        this.interprocCallPlanner = interprocCallPlanner;
    }

    @Override
    public boolean supports(Statement statement) {
        return statement.isExpressionStmt();
    }

    @Override
    public StatementHandleResult handle(RawTree rawTree,
                                        AnalysisStep step,
                                        Statement statement,
                                        EngineContext context) {
        ExpressionStmt expressionStmt = statement.asExpressionStmt();
        Expression expression = expressionStmt.getExpression();

        if (expression.isVariableDeclarationExpr()) {
            return handleVariableDeclaration(rawTree, step, expression.asVariableDeclarationExpr(), context);
        }

        if (expression.isAssignExpr()) {
            return handleAssignment(rawTree, step, expression.asAssignExpr(), context);
        }

        if (expression.isMethodCallExpr()) {
            return handleTopLevelMethodCall(rawTree, step, expression.asMethodCallExpr(), context);
        }

        return StatementHandleResult.continueLinear();
    }

    private StatementHandleResult handleVariableDeclaration(RawTree rawTree,
                                                            AnalysisStep step,
                                                            VariableDeclarationExpr expr,
                                                            EngineContext context) {
        if (expr.getVariables().size() == 1) {
            VariableDeclarator variable = expr.getVariable(0);

            if (variable.getInitializer().isPresent()) {
                MethodCallExpr valueCall = extractValueCall(variable.getInitializer().get());
                if (valueCall != null) {
                    Optional<InterprocCallPlan> interprocPlan = interprocCallPlanner.planValueCall(
                            rawTree,
                            step,
                            valueCall,
                            variable.getNameAsString(),
                            context
                    );

                    if (interprocPlan.isPresent()) {
                        return StatementHandleResult.customContinuations(
                                List.of(interprocPlan.get().getTargetMethodContinuation())
                        );
                    }
                }
            }
        }

        for (VariableDeclarator variable : expr.getVariables()) {
            if (variable.getInitializer().isEmpty()) {
                continue;
            }

            String variableName = variable.getNameAsString();
            Expression initializer = variable.getInitializer().get();

            RawNode resolvedNode = context.getExpressionResolver().resolve(
                    rawTree,
                    step,
                    initializer,
                    context
            );

            if (resolvedNode != null) {
                bindResolved(rawTree, step, variableName, resolvedNode);
                continue;
            }

            if (unknownBreakPolicy.shouldCreateForVariableInitializerFailure(variableName, initializer)) {
                RawNode breakNode = rawTree.addUnknownBreak(
                        step.getCurrentRawNode(),
                        variableName,
                        UsageKind.INTERMEDIATE
                );
                breakNode.setUsageKind(UsageKind.INTERMEDIATE);
                step.bind(variableName, breakNode);
            }
        }

        return StatementHandleResult.continueLinear();
    }

    private StatementHandleResult handleAssignment(RawTree rawTree,
                                                   AnalysisStep step,
                                                   AssignExpr expr,
                                                   EngineContext context) {
        if (!expr.getTarget().isNameExpr()) {
            return StatementHandleResult.continueLinear();
        }

        String targetName = expr.getTarget().asNameExpr().getNameAsString();
        Expression value = expr.getValue();

        MethodCallExpr valueCall = extractValueCall(value);
        if (valueCall != null) {
            Optional<InterprocCallPlan> interprocPlan = interprocCallPlanner.planValueCall(
                    rawTree,
                    step,
                    valueCall,
                    targetName,
                    context
            );

            if (interprocPlan.isPresent()) {
                return StatementHandleResult.customContinuations(
                        List.of(interprocPlan.get().getTargetMethodContinuation())
                );
            }
        }

        RawNode resolvedNode = context.getExpressionResolver().resolve(
                rawTree,
                step,
                value,
                context
        );

        if (resolvedNode != null) {
            bindResolved(rawTree, step, targetName, resolvedNode);
            return StatementHandleResult.continueLinear();
        }

        if (unknownBreakPolicy.shouldCreateForAssignmentFailure(targetName, value)) {
            RawNode breakNode = rawTree.addUnknownBreak(
                    step.getCurrentRawNode(),
                    targetName,
                    UsageKind.INTERMEDIATE
            );
            breakNode.setUsageKind(UsageKind.INTERMEDIATE);
            step.bind(targetName, breakNode);
        }

        return StatementHandleResult.continueLinear();
    }

    private MethodCallExpr extractValueCall(Expression expression) {
        if (expression == null) {
            return null;
        }

        if (expression.isMethodCallExpr()) {
            return expression.asMethodCallExpr();
        }

        if (expression.isCastExpr()) {
            CastExpr castExpr = expression.asCastExpr();
            return extractValueCall(castExpr.getExpression());
        }

        if (expression.isEnclosedExpr()) {
            EnclosedExpr enclosedExpr = expression.asEnclosedExpr();
            return extractValueCall(enclosedExpr.getInner());
        }

        return null;
    }

    private void bindResolved(RawTree rawTree,
                              AnalysisStep step,
                              String variableName,
                              RawNode resolvedNode) {
        RawNode nodeToBind = shouldCreateAlias(variableName, resolvedNode)
                ? rawTree.addAlias(resolvedNode, variableName)
                : resolvedNode;

        nodeToBind.setUsageKind(UsageKind.INTERMEDIATE);
        step.bind(variableName, nodeToBind);
    }

    private boolean shouldCreateAlias(String variableName, RawNode resolvedNode) {
        if (variableName == null || variableName.isBlank()) {
            return false;
        }

        if (variableName.equals(resolvedNode.getVariableName())) {
            return false;
        }

        if (variableName.equals(resolvedNode.getEntityField())) {
            return false;
        }

        return true;
    }

    private StatementHandleResult handleTopLevelMethodCall(RawTree rawTree,
                                                           AnalysisStep step,
                                                           MethodCallExpr expr,
                                                           EngineContext context) {
        Optional<InterprocCallPlan> interprocPlan = interprocCallPlanner.plan(
                rawTree,
                step,
                expr,
                context
        );

        if (interprocPlan.isPresent()) {
            return StatementHandleResult.customContinuations(
                    List.of(interprocPlan.get().getTargetMethodContinuation())
            );
        }

        RawNode callResult = context.getExpressionResolver().resolve(rawTree, step, expr, context);
        markTerminal(callResult);

        for (Expression argument : expr.getArguments()) {
            RawNode argumentNode = context.getExpressionResolver().resolve(
                    rawTree,
                    step,
                    argument,
                    context
            );
            markTerminal(argumentNode);
        }

        return StatementHandleResult.continueLinear();
    }

    private void markTerminal(RawNode node) {
        if (node != null) {
            node.setUsageKind(UsageKind.TERMINAL);
        }
    }
}

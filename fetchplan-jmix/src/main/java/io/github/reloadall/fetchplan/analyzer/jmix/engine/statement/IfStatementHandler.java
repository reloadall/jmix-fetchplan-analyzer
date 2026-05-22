package io.github.reloadall.fetchplan.analyzer.jmix.engine.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementHandleResult;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolutionResult;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_IfStatementHandler")
@Order(150)
public class IfStatementHandler implements StatementHandler {

    @Override
    public boolean supports(Statement statement) {
        return statement.isIfStmt();
    }

    @Override
    public StatementHandleResult handle(RawTree rawTree,
                                        AnalysisStep step,
                                        Statement statement,
                                        EngineContext context) {
        IfStmt ifStmt = statement.asIfStmt();
        markConditionUsage(rawTree, step, ifStmt, context);

        StatementsPayload currentPayload = (StatementsPayload) step.getPayload();

        StatementsPayload afterIfPayload = currentPayload.next();
        Continuation afterIfContinuation = new Continuation(
                step.getMethod(),
                afterIfPayload,
                step.getCurrentRawNode(),
                step.getBindings()
        );

        List<Continuation> continuations = new ArrayList<>();

        StatementsPayload thenPayload = new StatementsPayload(
                asStatements(ifStmt.getThenStmt()),
                0,
                afterIfContinuation
        );
        continuations.add(step.continueWith(thenPayload));

        if (ifStmt.getElseStmt().isPresent()) {
            StatementsPayload elsePayload = new StatementsPayload(
                    asStatements(ifStmt.getElseStmt().get()),
                    0,
                    afterIfContinuation
            );
            continuations.add(step.continueWith(elsePayload));
        } else {
            if (afterIfPayload.hasCurrentStatement() || afterIfPayload.getContinuationOnFinish() != null) {
                continuations.add(afterIfContinuation);
            }
        }

        return StatementHandleResult.customContinuations(continuations);
    }

    private void markConditionUsage(RawTree rawTree,
                                    AnalysisStep step,
                                    IfStmt ifStmt,
                                    EngineContext context) {
        markExpressionUsage(rawTree, step, ifStmt.getCondition(), context);
    }

    private void markExpressionUsage(RawTree rawTree,
                                     AnalysisStep step,
                                     Expression expression,
                                     EngineContext context) {
        if (expression.isBinaryExpr()) {
            BinaryExpr binaryExpr = expression.asBinaryExpr();
            markBinaryOperandUsage(rawTree, step, binaryExpr.getLeft(), context);
            markBinaryOperandUsage(rawTree, step, binaryExpr.getRight(), context);
            return;
        }

        ExpressionResolutionResult conditionResult = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                expression,
                context
        );

        for (RawNode node : conditionResult.getNodes()) {
            node.setUsageKind(UsageKind.TERMINAL);
        }

        if (!conditionResult.isEmpty()) {
            return;
        }
    }

    private void markBinaryOperandUsage(RawTree rawTree,
                                        AnalysisStep step,
                                        Expression operand,
                                        EngineContext context) {
        if (operand.isNameExpr()) {
            if (isScalarLikeName(step.getMethod(), operand.asNameExpr().getNameAsString())) {
                markResolvedNodesTerminal(rawTree, step, operand.asNameExpr(), context);
            }
            return;
        }

        if (operand.isEnclosedExpr()) {
            EnclosedExpr enclosedExpr = operand.asEnclosedExpr();
            markBinaryOperandUsage(rawTree, step, enclosedExpr.getInner(), context);
            return;
        }

        if (operand.isBinaryExpr()) {
            BinaryExpr binaryExpr = operand.asBinaryExpr();
            markBinaryOperandUsage(rawTree, step, binaryExpr.getLeft(), context);
            markBinaryOperandUsage(rawTree, step, binaryExpr.getRight(), context);
            return;
        }

        context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                operand,
                context
        );
    }

    private void markResolvedNodesTerminal(RawTree rawTree,
                                           AnalysisStep step,
                                           NameExpr expression,
                                           EngineContext context) {
        ExpressionResolutionResult result = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                expression,
                context
        );

        for (RawNode node : result.getNodes()) {
            node.setUsageKind(UsageKind.TERMINAL);
        }
    }

    private boolean isScalarLikeName(MethodDeclaration method, String name) {
        return resolveNameType(method, name)
                .map(this::isScalarLikeType)
                .orElse(false);
    }

    private Optional<String> resolveNameType(MethodDeclaration method, String name) {
        for (Parameter parameter : method.getParameters()) {
            if (name.equals(parameter.getNameAsString())) {
                return Optional.of(parameter.getType().asString());
            }
        }

        return method.findAll(VariableDeclarator.class).stream()
                .filter(variable -> name.equals(variable.getNameAsString()))
                .map(variable -> variable.getType().asString())
                .findFirst();
    }

    private boolean isScalarLikeType(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return false;
        }

        String normalized = typeName.trim();
        int genericIndex = normalized.indexOf('<');
        if (genericIndex >= 0) {
            normalized = normalized.substring(0, genericIndex);
        }

        return switch (normalized) {
            case "boolean", "byte", "short", "int", "long", "float", "double", "char",
                 "Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double", "Character",
                 "String", "CharSequence" -> true;
            default -> false;
        };
    }

    private List<Statement> asStatements(Statement statement) {
        if (statement.isBlockStmt()) {
            return statement.asBlockStmt().getStatements();
        }
        return List.of(statement);
    }

}

package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.stereotype.Component;

@Component("fpa_InterprocReturnResolver")
public class InterprocReturnResolver {

    public Optional<RawNode> resolveReturnNode(RawTree rawTree,
                                               MethodDeclaration targetMethod,
                                               Map<String, RawNode> targetBindings,
                                               RawNode entryAnchor,
                                               EngineContext context) {
        if (targetMethod.getBody().isEmpty()) {
            return Optional.empty();
        }

        AnalysisStep syntheticStep = new AnalysisStep(
                targetMethod,
                StatementsPayload.from(targetMethod),
                entryAnchor,
                new LinkedHashMap<>(targetBindings)
        );

        List<RawNode> resolvedReturnNodes = new ArrayList<>();
        collectReturnNodes(
                rawTree,
                syntheticStep,
                targetMethod.getBody().get().getStatements(),
                context,
                resolvedReturnNodes
        );

        if (resolvedReturnNodes.isEmpty()) {
            return Optional.empty();
        }

        RawNode first = resolvedReturnNodes.get(0);
        boolean allSame = resolvedReturnNodes.stream()
                .allMatch(node -> node.getId().equals(first.getId()));

        return allSame ? Optional.of(first) : Optional.empty();
    }

    private void collectReturnNodes(RawTree rawTree,
                                    AnalysisStep step,
                                    NodeList<Statement> statements,
                                    EngineContext context,
                                    List<RawNode> resolvedReturnNodes) {
        for (Statement statement : statements) {
            if (statement.isExpressionStmt()) {
                handleExpressionStatement(rawTree, step, statement.asExpressionStmt().getExpression(), context);
                continue;
            }

            if (statement.isReturnStmt()) {
                handleReturnStatement(rawTree, step, statement.asReturnStmt(), context, resolvedReturnNodes);
                continue;
            }

            if (statement.isIfStmt()) {
                handleIfStatement(rawTree, step, statement.asIfStmt(), context, resolvedReturnNodes);
                continue;
            }

            if (statement.isBlockStmt()) {
                AnalysisStep nestedStep = copyStep(step);
                collectReturnNodes(
                        rawTree,
                        nestedStep,
                        statement.asBlockStmt().getStatements(),
                        context,
                        resolvedReturnNodes
                );
            }
        }
    }

    private void handleExpressionStatement(RawTree rawTree,
                                           AnalysisStep step,
                                           Expression expression,
                                           EngineContext context) {
        if (expression.isVariableDeclarationExpr()) {
            VariableDeclarationExpr variableDeclarationExpr = expression.asVariableDeclarationExpr();

            for (VariableDeclarator variable : variableDeclarationExpr.getVariables()) {
                if (variable.getInitializer().isEmpty()) {
                    continue;
                }

                RawNode resolvedNode = context.getExpressionResolver().resolve(
                        rawTree,
                        step,
                        variable.getInitializer().get(),
                        context
                );

                if (resolvedNode != null) {
                    bindResolved(rawTree, step, variable.getNameAsString(), resolvedNode);
                }
            }

            return;
        }

        if (expression.isAssignExpr()) {
            AssignExpr assignExpr = expression.asAssignExpr();

            if (!assignExpr.getTarget().isNameExpr()) {
                return;
            }

            RawNode resolvedNode = context.getExpressionResolver().resolve(
                    rawTree,
                    step,
                    assignExpr.getValue(),
                    context
            );

            if (resolvedNode != null) {
                bindResolved(rawTree, step, assignExpr.getTarget().asNameExpr().getNameAsString(), resolvedNode);
            }
        }
    }

    private void handleReturnStatement(RawTree rawTree,
                                       AnalysisStep step,
                                       ReturnStmt returnStmt,
                                       EngineContext context,
                                       List<RawNode> resolvedReturnNodes) {
        if (returnStmt.getExpression().isEmpty()) {
            return;
        }

        Expression expression = returnStmt.getExpression().get();

        if (expression.isNullLiteralExpr()) {
            return;
        }

        RawNode resolvedNode = context.getExpressionResolver().resolve(
                rawTree,
                step,
                expression,
                context
        );

        if (resolvedNode != null) {
            resolvedReturnNodes.add(resolvedNode);
        }
    }

    private void handleIfStatement(RawTree rawTree,
                                   AnalysisStep step,
                                   IfStmt ifStmt,
                                   EngineContext context,
                                   List<RawNode> resolvedReturnNodes) {
        AnalysisStep thenStep = copyStep(step);
        collectFromStatement(rawTree, thenStep, ifStmt.getThenStmt(), context, resolvedReturnNodes);

        if (ifStmt.getElseStmt().isPresent()) {
            AnalysisStep elseStep = copyStep(step);
            collectFromStatement(rawTree, elseStep, ifStmt.getElseStmt().get(), context, resolvedReturnNodes);
        }
    }

    private void collectFromStatement(RawTree rawTree,
                                      AnalysisStep step,
                                      Statement statement,
                                      EngineContext context,
                                      List<RawNode> resolvedReturnNodes) {
        if (statement.isBlockStmt()) {
            BlockStmt blockStmt = statement.asBlockStmt();
            collectReturnNodes(rawTree, step, blockStmt.getStatements(), context, resolvedReturnNodes);
            return;
        }

        collectReturnNodes(rawTree, step, new NodeList<>(statement), context, resolvedReturnNodes);
    }

    private AnalysisStep copyStep(AnalysisStep step) {
        return new AnalysisStep(
                step.getMethod(),
                step.getPayload(),
                step.getCurrentRawNode(),
                new LinkedHashMap<>(step.getBindings())
        );
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
}

package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolutionResult;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_InterprocReturnResolver")
public class InterprocReturnResolver {

    private final AnalysisTrace analysisTrace;

    @Autowired
    public InterprocReturnResolver(AnalysisTrace analysisTrace) {
        this.analysisTrace = Objects.requireNonNull(analysisTrace, "analysisTrace is null");
    }

    public ExpressionResolutionResult resolveReturnValue(RawTree rawTree,
                                                         MethodDeclaration targetMethod,
                                                         Map<String, ValueBinding> targetBindings,
                                                         RawNode entryAnchor,
                                                         EngineContext context) {
        analysisTrace.log("INTERPROC: return resolver start for method = "
                + targetMethod.getNameAsString());

        if (targetMethod.getBody().isEmpty()) {
            analysisTrace.log("INTERPROC: return resolver failed, method body is absent");
            return ExpressionResolutionResult.empty();
        }

        AnalysisStep syntheticStep = new AnalysisStep(
                targetMethod,
                StatementsPayload.from(targetMethod),
                entryAnchor,
                new LinkedHashMap<>(targetBindings)
        );

        ExpressionResolutionResult result = collectReturnValues(
                rawTree,
                syntheticStep,
                targetMethod.getBody().get().getStatements(),
                context
        );

        if (result.isEmpty()) {
            analysisTrace.log("INTERPROC: return resolver failed, no meaningful return node");
        } else {
            analysisTrace.log("INTERPROC: return resolver final nodes = "
                    + result.getNodes().stream().map(node -> String.valueOf(node.getId())).toList());
        }

        return result;
    }

    private ExpressionResolutionResult collectReturnValues(RawTree rawTree,
                                                           AnalysisStep step,
                                                           NodeList<Statement> statements,
                                                           EngineContext context) {
        ExpressionResolutionResult merged = ExpressionResolutionResult.empty();

        for (Statement statement : statements) {
            if (statement.isExpressionStmt()) {
                handleExpressionStatement(rawTree, step, statement.asExpressionStmt().getExpression(), context);
                continue;
            }

            if (statement.isReturnStmt()) {
                merged = merged.merge(
                        handleReturnStatement(rawTree, step, statement.asReturnStmt(), context)
                );
                continue;
            }

            if (statement.isIfStmt()) {
                merged = merged.merge(
                        handleIfStatement(rawTree, step, statement.asIfStmt(), context)
                );
                continue;
            }

            if (statement.isBlockStmt()) {
                AnalysisStep nestedStep = copyStep(step);
                merged = merged.merge(
                        collectReturnValues(rawTree, nestedStep, statement.asBlockStmt().getStatements(), context)
                );
            }
        }

        return merged;
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

                ExpressionResolutionResult result = context.getExpressionResolver().resolveAll(
                        rawTree,
                        step,
                        variable.getInitializer().get(),
                        context
                );

                if (!result.isEmpty()) {
                    bindResolved(rawTree, step, variable.getNameAsString(), result);
                }
            }
            return;
        }

        if (expression.isAssignExpr()) {
            AssignExpr assignExpr = expression.asAssignExpr();

            if (!assignExpr.getTarget().isNameExpr()) {
                return;
            }

            ExpressionResolutionResult result = context.getExpressionResolver().resolveAll(
                    rawTree,
                    step,
                    assignExpr.getValue(),
                    context
            );

            if (!result.isEmpty()) {
                bindResolved(rawTree, step, assignExpr.getTarget().asNameExpr().getNameAsString(), result);
            }
        }
    }

    private ExpressionResolutionResult handleReturnStatement(RawTree rawTree,
                                                             AnalysisStep step,
                                                             ReturnStmt returnStmt,
                                                             EngineContext context) {
        if (returnStmt.getExpression().isEmpty()) {
            return ExpressionResolutionResult.empty();
        }

        Expression expression = returnStmt.getExpression().get();

        if (expression.isNullLiteralExpr()) {
            analysisTrace.log("INTERPROC: return resolver ignored return null");
            return ExpressionResolutionResult.empty();
        }

        ExpressionResolutionResult result = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                expression,
                context
        );

        if (!result.isEmpty()) {
            analysisTrace.log("INTERPROC: return resolver resolved return -> nodes="
                    + result.getNodes().stream().map(node -> String.valueOf(node.getId())).toList());
        }

        return result;
    }

    private ExpressionResolutionResult handleIfStatement(RawTree rawTree,
                                                         AnalysisStep step,
                                                         IfStmt ifStmt,
                                                         EngineContext context) {
        AnalysisStep thenStep = copyStep(step);
        ExpressionResolutionResult thenResult = collectFromStatement(
                rawTree,
                thenStep,
                ifStmt.getThenStmt(),
                context
        );

        ExpressionResolutionResult elseResult = ExpressionResolutionResult.empty();
        if (ifStmt.getElseStmt().isPresent()) {
            AnalysisStep elseStep = copyStep(step);
            elseResult = collectFromStatement(
                    rawTree,
                    elseStep,
                    ifStmt.getElseStmt().get(),
                    context
            );
        }

        return thenResult.merge(elseResult);
    }

    private ExpressionResolutionResult collectFromStatement(RawTree rawTree,
                                                            AnalysisStep step,
                                                            Statement statement,
                                                            EngineContext context) {
        if (statement.isBlockStmt()) {
            BlockStmt blockStmt = statement.asBlockStmt();
            return collectReturnValues(rawTree, step, blockStmt.getStatements(), context);
        }

        NodeList<Statement> singleStatement = new NodeList<>();
        singleStatement.add(statement);
        return collectReturnValues(rawTree, step, singleStatement, context);
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
                              ExpressionResolutionResult result) {
        ValueBinding binding = createBindingWithAliases(rawTree, variableName, result);
        step.bind(variableName, binding);

        analysisTrace.log("INTERPROC: return resolver bound local " + variableName
                + " -> nodes="
                + binding.getNodes().stream().map(node -> String.valueOf(node.getId())).toList());
    }

    private ValueBinding createBindingWithAliases(RawTree rawTree,
                                                  String variableName,
                                                  ExpressionResolutionResult result) {
        java.util.Set<RawNode> nodes = new java.util.LinkedHashSet<>();

        for (RawNode resolvedNode : result.getNodes()) {
            RawNode nodeToBind = shouldCreateAlias(variableName, resolvedNode)
                    ? rawTree.addAlias(resolvedNode, variableName)
                    : resolvedNode;

            nodeToBind.setUsageKind(UsageKind.INTERMEDIATE);
            nodes.add(nodeToBind);
        }

        return new ValueBinding(nodes, result.isUncertain());
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

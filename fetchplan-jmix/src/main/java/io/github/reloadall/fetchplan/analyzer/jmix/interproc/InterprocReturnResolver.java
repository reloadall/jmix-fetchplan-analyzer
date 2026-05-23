package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
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

        ReturnWalkResult walkResult = collectReturnValues(
                rawTree,
                syntheticStep,
                targetMethod.getBody().get().getStatements(),
                context
        );

        ExpressionResolutionResult result = walkResult.returnValues();

        if (result.isEmpty()) {
            if (!walkResult.sideEffects().isEmpty()) {
                analysisTrace.log("INTERPROC: return resolver found only side effects, no resolvable return value");
            } else {
                analysisTrace.log("INTERPROC: return resolver failed, no meaningful return node");
            }
        } else {
            analysisTrace.log("INTERPROC: return resolver final nodes = "
                    + result.getNodes().stream().map(node -> String.valueOf(node.getId())).toList());
        }

        return result;
    }

    private ReturnWalkResult collectReturnValues(RawTree rawTree,
                                                 AnalysisStep step,
                                                 NodeList<Statement> statements,
                                                 EngineContext context) {
        ExpressionResolutionResult returnValues = ExpressionResolutionResult.empty();
        ExpressionResolutionResult sideEffects = ExpressionResolutionResult.empty();

        for (Statement statement : statements) {
            if (statement.isExpressionStmt()) {
                sideEffects = sideEffects.merge(
                        handleExpressionStatement(rawTree, step, statement.asExpressionStmt().getExpression(), context)
                );
                continue;
            }

            if (statement.isReturnStmt()) {
                ReturnWalkResult nested = handleReturnStatement(rawTree, step, statement.asReturnStmt(), context);
                returnValues = returnValues.merge(nested.returnValues());
                sideEffects = sideEffects.merge(nested.sideEffects());
                continue;
            }

            if (statement.isIfStmt()) {
                ReturnWalkResult nested = handleIfStatement(rawTree, step, statement.asIfStmt(), context);
                returnValues = returnValues.merge(nested.returnValues());
                sideEffects = sideEffects.merge(nested.sideEffects());
                continue;
            }

            if (statement.isForEachStmt()) {
                ReturnWalkResult nested = handleForEachStatement(rawTree, step, statement.asForEachStmt(), context);
                returnValues = returnValues.merge(nested.returnValues());
                sideEffects = sideEffects.merge(nested.sideEffects());
                continue;
            }

            if (statement.isBlockStmt()) {
                AnalysisStep nestedStep = copyStep(step);
                ReturnWalkResult nested = collectReturnValues(
                        rawTree,
                        nestedStep,
                        statement.asBlockStmt().getStatements(),
                        context
                );
                returnValues = returnValues.merge(nested.returnValues());
                sideEffects = sideEffects.merge(nested.sideEffects());
            }
        }

        return new ReturnWalkResult(returnValues, sideEffects);
    }

    private ExpressionResolutionResult handleExpressionStatement(RawTree rawTree,
                                                                 AnalysisStep step,
                                                                 Expression expression,
                                                                 EngineContext context) {
        if (expression.isVariableDeclarationExpr()) {
            VariableDeclarationExpr variableDeclarationExpr = expression.asVariableDeclarationExpr();
            ExpressionResolutionResult sideEffects = ExpressionResolutionResult.empty();

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

                Expression initializer = variable.getInitializer().get();
                ExpressionResolutionResult boundaryUsages = collectBoundaryArgumentUsages(
                        rawTree,
                        step,
                        initializer,
                        context
                );
                boundaryUsages = preserveTechnicalIdBoundaryAnchor(initializer, boundaryUsages);
                if (!boundaryUsages.isEmpty()) {
                    markTerminal(boundaryUsages);
                    sideEffects = sideEffects.merge(boundaryUsages);
                }

                if (!result.isEmpty()) {
                    bindResolved(rawTree, step, variable.getNameAsString(), result);
                    if (containsTerminalNodes(result)) {
                        sideEffects = sideEffects.merge(result);
                    }
                }
            }
            return sideEffects;
        }

        if (expression.isAssignExpr()) {
            AssignExpr assignExpr = expression.asAssignExpr();

            if (!assignExpr.getTarget().isNameExpr()) {
                return ExpressionResolutionResult.empty();
            }

            ExpressionResolutionResult result = context.getExpressionResolver().resolveAll(
                    rawTree,
                    step,
                    assignExpr.getValue(),
                    context
            );

            ExpressionResolutionResult boundaryUsages = collectBoundaryArgumentUsages(
                    rawTree,
                    step,
                    assignExpr.getValue(),
                    context
            );
            boundaryUsages = preserveTechnicalIdBoundaryAnchor(assignExpr.getValue(), boundaryUsages);
            if (!boundaryUsages.isEmpty()) {
                markTerminal(boundaryUsages);
            }

            if (!result.isEmpty()) {
                bindResolved(rawTree, step, assignExpr.getTarget().asNameExpr().getNameAsString(), result);
            }

            ExpressionResolutionResult terminalResult = containsTerminalNodes(result)
                    ? result
                    : ExpressionResolutionResult.empty();

            return terminalResult.merge(boundaryUsages);
        }

        ExpressionResolutionResult result = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                expression,
                context
        );

        if (result.isEmpty()) {
            result = collectBoundaryArgumentUsages(rawTree, step, expression, context);
        }

        result = preserveTechnicalIdBoundaryAnchor(expression, result);
        if (!result.isEmpty()) {
            markTerminal(result);
            analysisTrace.log("INTERPROC: return resolver preserved expression statement -> nodes="
                    + result.getNodes().stream().map(node -> String.valueOf(node.getId())).toList());
        }

        return result;
    }

    private ExpressionResolutionResult collectBoundaryArgumentUsages(RawTree rawTree,
                                                                    AnalysisStep step,
                                                                    Expression expression,
                                                                    EngineContext context) {
        if (expression == null) {
            return ExpressionResolutionResult.empty();
        }

        if (expression.isNameExpr()) {
            ValueBinding binding = step.getBinding(expression.asNameExpr().getNameAsString());
            if (binding != null && binding.isTerminalOnly() && !binding.isEmpty()) {
                return new ExpressionResolutionResult(binding.getNodes(), binding.isUncertain());
            }
        }

        if (expression.isMethodCallExpr()) {
            ExpressionResolutionResult merged = ExpressionResolutionResult.empty();
            MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
            for (Expression argument : methodCallExpr.getArguments()) {
                ExpressionResolutionResult argumentResult = context.getExpressionResolver().resolveAll(
                        rawTree,
                        step,
                        argument,
                        context
                );

                if (argumentResult.isEmpty()) {
                    argumentResult = collectBoundaryArgumentUsages(rawTree, step, argument, context);
                }

                if (!argumentResult.isEmpty()) {
                    markTerminal(argumentResult);
                    merged = merged.merge(argumentResult);
                }
            }
            return merged;
        }

        if (expression.isNameExpr()) {
            ValueBinding binding = step.getBinding(expression.asNameExpr().getNameAsString());
            if (binding != null && binding.isTerminalOnly() && !binding.isEmpty()) {
                return new ExpressionResolutionResult(binding.getNodes(), binding.isUncertain());
            }
        }

        if (expression.isObjectCreationExpr()) {
            ExpressionResolutionResult merged = ExpressionResolutionResult.empty();
            for (Expression argument : expression.asObjectCreationExpr().getArguments()) {
                ExpressionResolutionResult argumentResult = context.getExpressionResolver().resolveAll(
                        rawTree,
                        step,
                        argument,
                        context
                );

                if (argumentResult.isEmpty()) {
                    argumentResult = collectBoundaryArgumentUsages(rawTree, step, argument, context);
                }

                if (!argumentResult.isEmpty()) {
                    markTerminal(argumentResult);
                    merged = merged.merge(argumentResult);
                }
            }
            return merged;
        }

        return ExpressionResolutionResult.empty();
    }

    private ReturnWalkResult handleReturnStatement(RawTree rawTree,
                                                   AnalysisStep step,
                                                   ReturnStmt returnStmt,
                                                   EngineContext context) {
        if (returnStmt.getExpression().isEmpty()) {
            return new ReturnWalkResult(ExpressionResolutionResult.empty(), ExpressionResolutionResult.empty());
        }

        Expression expression = returnStmt.getExpression().get();

        if (expression.isNullLiteralExpr()) {
            analysisTrace.log("INTERPROC: return resolver ignored return null");
            return new ReturnWalkResult(ExpressionResolutionResult.empty(), ExpressionResolutionResult.empty());
        }

        if (expression.isBinaryExpr()) {
            return new ReturnWalkResult(
                    handleBinaryReturnExpression(rawTree, step, expression.asBinaryExpr(), context),
                    ExpressionResolutionResult.empty()
            );
        }

        ExpressionResolutionResult result = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                expression,
                context
        );

        if (result.isEmpty()) {
            ExpressionResolutionResult boundaryUsages = collectBoundaryArgumentUsages(rawTree, step, expression, context);
            if (!boundaryUsages.isEmpty()) {
                analysisTrace.log("INTERPROC: return resolver preserved boundary return argument usages as side effects -> nodes="
                        + boundaryUsages.getNodes().stream().map(node -> String.valueOf(node.getId())).toList());
            }
            return new ReturnWalkResult(ExpressionResolutionResult.empty(), boundaryUsages);
        }

        analysisTrace.log("INTERPROC: return resolver resolved return -> nodes="
                + result.getNodes().stream().map(node -> String.valueOf(node.getId())).toList());

        return new ReturnWalkResult(result, ExpressionResolutionResult.empty());
    }

    private ExpressionResolutionResult handleBinaryReturnExpression(RawTree rawTree,
                                                                    AnalysisStep step,
                                                                    BinaryExpr binaryExpr,
                                                                    EngineContext context) {
        ExpressionResolutionResult leftResult = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                binaryExpr.getLeft(),
                context
        );
        markTerminal(leftResult);

        ExpressionResolutionResult rightResult = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                binaryExpr.getRight(),
                context
        );
        markTerminal(rightResult);

        return leftResult.merge(rightResult);
    }

    private ReturnWalkResult handleIfStatement(RawTree rawTree,
                                               AnalysisStep step,
                                               IfStmt ifStmt,
                                               EngineContext context) {
        context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                ifStmt.getCondition(),
                context
        );

        AnalysisStep thenStep = copyStep(step);
        ReturnWalkResult thenResult = collectFromStatement(
                rawTree,
                thenStep,
                ifStmt.getThenStmt(),
                context
        );

        ReturnWalkResult elseResult = new ReturnWalkResult(
                ExpressionResolutionResult.empty(),
                ExpressionResolutionResult.empty()
        );
        if (ifStmt.getElseStmt().isPresent()) {
            AnalysisStep elseStep = copyStep(step);
            elseResult = collectFromStatement(
                    rawTree,
                    elseStep,
                    ifStmt.getElseStmt().get(),
                    context
            );
        }

        return new ReturnWalkResult(
                thenResult.returnValues().merge(elseResult.returnValues()),
                thenResult.sideEffects().merge(elseResult.sideEffects())
        );
    }

    private ReturnWalkResult handleForEachStatement(RawTree rawTree,
                                                    AnalysisStep step,
                                                    ForEachStmt forEachStmt,
                                                    EngineContext context) {
        ExpressionResolutionResult iterableResult = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                forEachStmt.getIterable(),
                context
        );

        AnalysisStep bodyStep = copyStep(step);
        if (!iterableResult.isEmpty()) {
            Set<RawNode> elementNodes = new LinkedHashSet<>();
            for (RawNode iterableNode : iterableResult.getNodes()) {
                RawNode elementNode = rawTree.addChild(
                        iterableNode,
                        null,
                        io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind.COLLECTION_ELEMENT,
                        null,
                        UsageKind.INTERMEDIATE
                );
                elementNodes.add(elementNode);
            }

            if (!elementNodes.isEmpty()) {
                String loopVariableName = resolveLoopVariableName(forEachStmt);
                bodyStep.bind(loopVariableName, new ValueBinding(elementNodes, iterableResult.isUncertain()));
            }
        }

        return collectFromStatement(rawTree, bodyStep, forEachStmt.getBody(), context);
    }

    private String resolveLoopVariableName(ForEachStmt forEachStmt) {
        java.util.List<VariableDeclarator> variables = forEachStmt.getVariable().getVariables();
        if (variables.isEmpty()) {
            throw new IllegalStateException("ForEach variable is absent");
        }
        return variables.get(0).getNameAsString();
    }

    private ReturnWalkResult collectFromStatement(RawTree rawTree,
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

    private void markTerminal(ExpressionResolutionResult result) {
        for (RawNode node : result.getNodes()) {
            node.setUsageKind(UsageKind.TERMINAL);
        }
    }

    private boolean containsTerminalNodes(ExpressionResolutionResult result) {
        for (RawNode node : result.getNodes()) {
            if (node.getUsageKind() == UsageKind.TERMINAL) {
                return true;
            }
        }
        return false;
    }

    private ExpressionResolutionResult preserveTechnicalIdBoundaryAnchor(Expression expression,
                                                                         ExpressionResolutionResult result) {
        if (expression == null || result.isEmpty() || !expression.isMethodCallExpr()) {
            return result;
        }

        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        if (!"getId".equals(methodCallExpr.getNameAsString()) || !methodCallExpr.getArguments().isEmpty()) {
            return result;
        }

        java.util.Set<RawNode> anchors = new java.util.LinkedHashSet<>();
        for (RawNode node : result.getNodes()) {
            if (!"id".equals(node.getEntityField())) {
                return result;
            }

            RawNode parent = node.getParent();
            if (parent == null || parent.getFlowKind() != io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind.DIRECT) {
                return result;
            }

            anchors.add(parent);
        }

        if (anchors.isEmpty()) {
            return result;
        }

        analysisTrace.log("INTERPROC: preserved pre-boundary anchor for technical id access -> nodes="
                + anchors.stream().map(node -> String.valueOf(node.getId())).toList());
        return new ExpressionResolutionResult(anchors, result.isUncertain());
    }

    private record ReturnWalkResult(ExpressionResolutionResult returnValues,
                                    ExpressionResolutionResult sideEffects) {
    }
}

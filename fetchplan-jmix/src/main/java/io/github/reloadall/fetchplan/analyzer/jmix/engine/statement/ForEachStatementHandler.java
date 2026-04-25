package io.github.reloadall.fetchplan.analyzer.jmix.engine.statement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementHandleResult;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolutionResult;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocMethodResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_ForEachStatementHandler")
@Order(160)
public class ForEachStatementHandler implements StatementHandler {

    private final InterprocMethodResolver interprocMethodResolver;

    @Autowired
    public ForEachStatementHandler(InterprocMethodResolver interprocMethodResolver) {
        this.interprocMethodResolver = interprocMethodResolver;
    }

    @Override
    public boolean supports(Statement statement) {
        return statement.isForEachStmt();
    }

    @Override
    public StatementHandleResult handle(RawTree rawTree,
                                        AnalysisStep step,
                                        Statement statement,
                                        EngineContext context) {
        ForEachStmt forEachStmt = statement.asForEachStmt();
        StatementsPayload currentPayload = (StatementsPayload) step.getPayload();
        StatementsPayload afterLoopPayload = currentPayload.next();

        Continuation afterLoopContinuation = new Continuation(
                step.getMethod(),
                afterLoopPayload,
                step.getCurrentRawNode(),
                step.copyBindings()
        );

        List<Continuation> continuations = new ArrayList<>();

        if (afterLoopPayload.hasCurrentStatement() || afterLoopPayload.getContinuationOnFinish() != null) {
            continuations.add(afterLoopContinuation);
        }

        ExpressionResolutionResult iterableResult = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                forEachStmt.getIterable(),
                context
        );

        if (iterableResult.isEmpty()) {
            Optional<Continuation> workerFanOutContinuation = tryPlanWorkerDispatchLoop(step, forEachStmt, afterLoopContinuation);
            if (workerFanOutContinuation.isPresent()) {
                continuations.add(workerFanOutContinuation.get());
                return StatementHandleResult.customContinuations(continuations);
            }

            rawTree.addUnknownBreak(step.getCurrentRawNode(), null, UsageKind.NONE);
            return StatementHandleResult.customContinuations(continuations);
        }

        Set<RawNode> elementNodes = new LinkedHashSet<>();
        for (RawNode iterableNode : iterableResult.getNodes()) {
            RawNode elementNode = rawTree.addChild(
                    iterableNode,
                    null,
                    FlowKind.COLLECTION_ELEMENT,
                    null,
                    UsageKind.INTERMEDIATE
            );
            elementNodes.add(elementNode);
        }

        String loopVariableName = resolveLoopVariableName(forEachStmt);
        Map<String, ValueBinding> nextBindings = step.copyBindings();
        nextBindings.put(loopVariableName, new ValueBinding(elementNodes, iterableResult.isUncertain()));

        RawNode anchor = elementNodes.iterator().next();

        StatementsPayload bodyPayload = new StatementsPayload(
                asStatements(forEachStmt.getBody()),
                0,
                afterLoopContinuation
        );

        continuations.add(step.continueWith(
                step.getMethod(),
                bodyPayload,
                anchor,
                nextBindings
        ));

        return StatementHandleResult.customContinuations(continuations);
    }

    private Optional<Continuation> tryPlanWorkerDispatchLoop(AnalysisStep step,
                                                             ForEachStmt forEachStmt,
                                                             Continuation afterLoopContinuation) {
        if (!forEachStmt.getIterable().isNameExpr()) {
            return Optional.empty();
        }

        String iterableName = forEachStmt.getIterable().asNameExpr().getNameAsString();
        List<String> implementations = interprocMethodResolver.resolveCollectionElementImplementations(step.getMethod(), iterableName);
        if (implementations.isEmpty()) {
            return Optional.empty();
        }

        String loopVariableName = resolveLoopVariableName(forEachStmt);
        Map<String, ValueBinding> nextBindings = new LinkedHashMap<>(step.copyBindings());
        nextBindings.put(loopVariableName, ValueBinding.forDispatchTargets(new LinkedHashSet<>(implementations), false));

        StatementsPayload bodyPayload = new StatementsPayload(
                asStatements(forEachStmt.getBody()),
                0,
                afterLoopContinuation
        );

        return Optional.of(step.continueWith(
                step.getMethod(),
                bodyPayload,
                step.getCurrentRawNode(),
                nextBindings
        ));
    }

    private String resolveLoopVariableName(ForEachStmt forEachStmt) {
        List<VariableDeclarator> variables = forEachStmt.getVariable().getVariables();
        if (variables.isEmpty()) {
            throw new IllegalStateException("ForEach variable is absent");
        }
        return variables.get(0).getNameAsString();
    }

    private List<Statement> asStatements(Statement statement) {
        if (statement.isBlockStmt()) {
            return statement.asBlockStmt().getStatements();
        }
        return List.of(statement);
    }
}

package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import com.github.javaparser.ast.expr.Expression;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;

public interface ExpressionHandler {

    boolean supports(Expression expression);

    RawNode resolve(RawTree rawTree,
                    AnalysisStep step,
                    Expression expression,
                    EngineContext context);

}

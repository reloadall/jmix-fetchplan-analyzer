package io.github.reloadall.fetchplan.analyzer.jmix.engine.statement;

import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementHandleResult;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;

public interface StatementHandler {

    boolean supports(Statement statement);

    StatementHandleResult handle(RawTree rawTree,
                                 AnalysisStep step,
                                 Statement statement,
                                 EngineContext context);
}

package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.util.Objects;

import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_EngineContext")
public class EngineContext {

    private final ExpressionResolver expressionResolver;

    @Autowired
    public EngineContext(ExpressionResolver expressionResolver) {
        this.expressionResolver = Objects.requireNonNull(expressionResolver, "expressionResolver is null");
    }

    public ExpressionResolver getExpressionResolver() {
        return expressionResolver;
    }

}

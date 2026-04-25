package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.BaseLine;
import org.springframework.stereotype.Service;

@Service
public class LineTypeGuard {

    public boolean isSpecificLine(BaseLine line) {
        return false;
    }

    public boolean isNotSpecificLine(BaseLine line) {
        return !isSpecificLine(line);
    }
}
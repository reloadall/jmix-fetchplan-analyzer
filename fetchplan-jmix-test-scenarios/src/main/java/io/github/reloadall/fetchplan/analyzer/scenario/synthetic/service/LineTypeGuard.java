package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.BaseLine;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.HasSyntheticMeta;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.SyntheticDocument;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LineTypeGuard {

    public boolean isSpecificLine(BaseLine line) {
        return false;
    }

    public boolean isNotSpecificLine(BaseLine line) {
        return !isSpecificLine(line);
    }

    public boolean isNotInAllowedKind(BaseLine line, List<Class<?>> allowedKinds) {
        if (line == null) {
            return true;
        }

        SyntheticDocument parent = line.getParent();

        for (Class<?> allowedKind : allowedKinds) {
            if (isKind(allowedKind, parent)) {
                return false;
            }
        }

        return true;
    }

    public boolean isKind(Class<?> expectedKind, HasSyntheticMeta document) {
        if (document == null) {
            return false;
        }

        String actualKind = document.getMetaName();
        return actualKind != null;
    }
}
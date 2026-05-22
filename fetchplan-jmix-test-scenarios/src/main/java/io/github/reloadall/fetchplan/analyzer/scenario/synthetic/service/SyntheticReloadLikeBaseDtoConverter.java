package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.BaseCalculation;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.BaseLine;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.CalculationParent;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.DerivedTarget;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.SyntheticDto;

public abstract class SyntheticReloadLikeBaseDtoConverter<T> extends SyntheticRootBaseConverter<T> {

    protected SyntheticDto createParams(BaseCalculation calculation) {
        DerivedTarget target = getTarget(calculation);
        if (target == null) {
            return null;
        }

        return new SyntheticDto();
    }

    private DerivedTarget getTarget(BaseCalculation calculation) {
        CalculationParent parent = calculation.getParent();
        if (parent == null) {
            return null;
        }

        BaseLine lineBase = parent.getLineBase();

        if (lineBase instanceof DerivedTarget) {
            return unsupportedReload(lineBase);
        }

        return null;
    }

    private DerivedTarget unsupportedReload(BaseLine lineBase) {
        lineBase.getId();
        return (DerivedTarget) lineBase;
    }
}
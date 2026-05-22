package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.HasSyntheticDocument;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.SyntheticDto;

public abstract class SyntheticBaseConverter<T extends HasSyntheticDocument> extends SyntheticBaseDtoConverter<T> {

    protected SyntheticDto createParams(HasSyntheticDocument document) {
        document.getAgreement()
                .getSides()
                .getCounterparty()
                .getName();

        return new SyntheticDto();
    }
}
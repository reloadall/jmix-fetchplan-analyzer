package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RootDocument;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.SyntheticDto;
import org.springframework.stereotype.Service;

@Service
public class SyntheticConcreteConverterA extends SyntheticBaseConverterA<RootDocument> {

    public SyntheticDto createDto(RootDocument document) {
        return createParams(document);
    }
}
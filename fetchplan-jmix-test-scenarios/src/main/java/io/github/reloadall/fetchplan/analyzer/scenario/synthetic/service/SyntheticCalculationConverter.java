package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.SyntheticCalculation;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.SyntheticDto;
import org.springframework.stereotype.Service;

@Service
public class SyntheticCalculationConverter extends SyntheticReloadLikeBaseDtoConverter<SyntheticCalculation> {

    @Override
    public SyntheticDto createDto(SyntheticCalculation line) throws Exception {
        return createParams(line);
    }
}
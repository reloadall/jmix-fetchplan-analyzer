package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.SyntheticDto;

public abstract class SyntheticRootBaseConverter<T> {

    public abstract SyntheticDto createDto(T line) throws Exception;
}
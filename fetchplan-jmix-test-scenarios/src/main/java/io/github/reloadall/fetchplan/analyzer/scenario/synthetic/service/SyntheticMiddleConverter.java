package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.HasSyntheticDocument;

public abstract class SyntheticMiddleConverter<T extends HasSyntheticDocument>
        extends SyntheticGrandparentDtoConverter<T> {

    protected void touchMarker() {
        // harmless unrelated code path for separate-file grandparent traversal scenario
    }
}
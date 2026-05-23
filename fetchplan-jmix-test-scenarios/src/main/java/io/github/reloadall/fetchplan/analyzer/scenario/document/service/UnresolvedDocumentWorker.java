package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Contract;

public interface UnresolvedDocumentWorker {

    void process(Contract contract);
}
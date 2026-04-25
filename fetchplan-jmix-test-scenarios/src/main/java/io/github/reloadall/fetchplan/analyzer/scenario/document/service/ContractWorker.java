package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document;
import org.springframework.stereotype.Service;

@Service
public class ContractWorker implements DocumentWorker {

    @Override
    public void process(Document document) {
        document.getContract().getNumber();
    }
}
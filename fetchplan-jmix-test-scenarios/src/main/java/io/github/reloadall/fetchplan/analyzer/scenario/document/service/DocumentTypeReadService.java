package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document;
import org.springframework.stereotype.Service;

@Service
public class DocumentTypeReadService {

    public void readType(Document document) {
        document.getType().getCode();
        document.getType().getName();
    }
}
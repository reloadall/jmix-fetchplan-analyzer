package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.DocumentLine;
import org.springframework.stereotype.Service;

@Service
public class LineReadService {

    public void readLine(DocumentLine line) {
        line.getQuantity();
        line.getProduct().getSku();
        line.getProduct().getCategory().getCode();
    }
}
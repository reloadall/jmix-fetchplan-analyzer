package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Address;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document;

public final class AddressSelector {

    private AddressSelector() {
    }

    public static Address select(Document document) {
        return document.getShippingAddress();
    }
}
package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Address;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document;
import org.springframework.stereotype.Service;

@Service
public class ReturnRebindingFixtureService {

    public void sameClassHelperReturnRebinding(Document document) {
        Address address = resolveShippingAddress(document);
        address.getCity();
    }

    public void explicitThisCallReturnRebinding(Document document) {
        Address address = this.resolveShippingAddress(document);
        address.getCity();
    }

    public void valueCallReturnRebinding(Document document) {
        Address address = loadShippingAddress(document);
        address.getCity();
    }

    public void valueCallReturnRebindingShouldNotUseSideEffectsAsReturnedValue(Document document) {
        Address address = helperWithSideEffectOnly(document);
        address.getCity();
    }

    public void valueCallReturnRebindingShouldUseActualReturnedValue(Document document) {
        Address address = helperReturningShippingAddress(document);
        address.getCity();
    }

    public void valueCallReturnRebindingShouldPreserveSideEffectsAndReturnedLeaf(Document document) {
        Address address = helperWithSideEffectAndReturn(document);
        address.getCity();
    }

    public void castContinuation(Document document) {
        Object address = document.getShippingAddress();
        Address castedAddress = (Address) address;
        castedAddress.getCity();
    }

    public void returnedAssociationWithoutDeeperCallerAccess(Document document) {
        Address address = resolveShippingAddress(document);
    }

    public void returnedAssociationWithDeeperCallerAccess(Document document) {
        Address address = resolveShippingAddress(document);
        address.getCity();
    }

    Address resolveShippingAddress(Document document) {
        return document.getShippingAddress();
    }

    Address loadShippingAddress(Document document) {
        return resolveShippingAddress(document);
    }

    Address helperWithSideEffectOnly(Document document) {
        document.getType().getCode();
        return null;
    }

    Address helperReturningShippingAddress(Document document) {
        return document.getShippingAddress();
    }

    Address helperWithSideEffectAndReturn(Document document) {
        document.getType().getCode();
        return document.getShippingAddress();
    }
}
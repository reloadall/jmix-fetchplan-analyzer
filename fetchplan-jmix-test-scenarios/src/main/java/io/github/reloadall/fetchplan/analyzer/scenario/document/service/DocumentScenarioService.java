package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Address;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Contract;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.DocumentLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentScenarioService {

    private final DocumentTypeReadService documentTypeReadService;
    private final ContractReadService contractReadService;
    private final LineReadService lineReadService;

    @Autowired
    public DocumentScenarioService(DocumentTypeReadService documentTypeReadService,
                                   ContractReadService contractReadService,
                                   LineReadService lineReadService) {
        this.documentTypeReadService = documentTypeReadService;
        this.contractReadService = contractReadService;
        this.lineReadService = lineReadService;
    }

    public void inspectDocument(Document document) {
        document.getNumber();

        documentTypeReadService.readType(document);

        Contract contract = document.getContract();
        contractReadService.readContract(contract);

        for (DocumentLine line : document.getLines()) {
            lineReadService.readLine(line);
        }

        Address shipping = resolveShippingAddress(document);
        shipping.getCity();
    }

    public void inspectDocumentBranch(Document document) {
        if (document.getType() != null) {
            document.getType().getCode();
        } else {
            document.getType().getName();
        }
    }

    public void inspectFirstLine(Document document) {
        DocumentLine line = document.getLines().get(0);
        line.getQuantity();
        line.getProduct().getSku();
    }

    public void inspectDocumentWithThisCall(Document document) {
        Address shipping = this.resolveShippingAddress(document);
        shipping.getCity();
    }

    public void inspectDocumentWithValueCall(Document document) {
        Address shipping = loadShippingAddress(document);
        shipping.getCity();
    }

    public void inspectDocumentWithLocalAlias(Document document) {
        io.github.reloadall.fetchplan.analyzer.scenario.document.entity.DocumentType type = document.getType();
        type.getCode();
    }

    public void inspectDocumentWithAliasChain(Document document) {
        io.github.reloadall.fetchplan.analyzer.scenario.document.entity.DocumentType first = document.getType();
        io.github.reloadall.fetchplan.analyzer.scenario.document.entity.DocumentType second = first;
        second.getName();
    }

    public void inspectDocumentWithCast(Document document) {
        Object address = document.getShippingAddress();
        Address castedAddress = (Address) address;
        castedAddress.getCity();
    }

    public void inspectDocumentWithStreamMap(Document document) {
        document.getLines()
                .stream()
                .map(DocumentLine::getProduct)
                .map(io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Product::getSku);
    }

    public void inspectDocumentWithUnknownBreak(Document document) {
        Address address = AddressSelector.select(document);
        address.getCity();
    }

    Address resolveShippingAddress(Document document) {
        return document.getShippingAddress();
    }

    Address loadShippingAddress(Document document) {
        return resolveShippingAddress(document);
    }

}
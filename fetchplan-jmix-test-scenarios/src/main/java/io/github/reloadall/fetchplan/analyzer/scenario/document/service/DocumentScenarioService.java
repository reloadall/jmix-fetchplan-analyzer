package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Address;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Contract;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Currency;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.DocumentLine;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentScenarioService {

    private final DocumentTypeReadService documentTypeReadService;
    private final ContractReadService contractReadService;
    private final LineReadService lineReadService;
    private final RecordRepository recordRepository;
    private final List<DocumentWorker> workers;
    private UnresolvedDocumentWorker unresolvedWorker;

    @Autowired
    public DocumentScenarioService(DocumentTypeReadService documentTypeReadService,
                                   ContractReadService contractReadService,
                                   LineReadService lineReadService,
                                   RecordRepository recordRepository,
                                   List<DocumentWorker> workers) {
        this.documentTypeReadService = documentTypeReadService;
        this.contractReadService = contractReadService;
        this.lineReadService = lineReadService;
        this.recordRepository = recordRepository;
        this.workers = workers;
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

    public void inspectDocumentWithStreamMapComputedGetter(Document document) {
        document.getLines()
                .stream()
                .map(DocumentLine::getCodeAsEnum)
                .toList();
    }

    public void inspectDocumentWithCollectionForEachLambda(Document document) {
        document.getLines()
                .forEach(line -> line.getProduct().getSku());
    }

    public void inspectDocumentWithStreamForEachLambda(Document document) {
        document.getLines()
                .stream()
                .forEach(line -> line.getProduct().getSku());
    }

    public void inspectDocumentWithStreamForEachBlockLambda(Document document) {
        document.getLines()
                .stream()
                .forEach(line -> {
                    line.getProduct().getSku();
                    line.getQuantity();
                });
    }

    public void inspectDocumentWithUnknownBreak(Document document) {
        Address address = AddressSelector.select(document);
        address.getCity();
    }

    public void inspectDocumentWithWorkers(Document document) {
        for (DocumentWorker worker : workers) {
            worker.process(document);
        }
    }

    public void inspectDocumentWithUnresolvedWorker(Document document) {
        unresolvedWorker.process(document.getContract());
    }

    public void inspectDocumentWithGetterArguments(Document document) {
        DocumentFinder.findSomething(
                document.getDateStart(),
                document.getDateFinish(),
                IdLike.of(document.getContract()),
                IdLike.of(document.getCurrency())
        );
    }

    public void inspectDocumentWithForwardedRepositoryArguments(Document document) {
        findRecords(
                document.getDateStart(),
                document.getDateFinish(),
                IdLike.of(document.getContract()),
                IdLike.of(document.getCurrency())
        );
    }

    Address resolveShippingAddress(Document document) {
        return document.getShippingAddress();
    }

    Address loadShippingAddress(Document document) {
        return resolveShippingAddress(document);
    }

    private List<RecordView> findRecords(LocalDate dateStart,
                                         LocalDate dateFinish,
                                         IdLike<Contract> contractId,
                                         IdLike<Currency> currencyId) {
        return recordRepository.find(
                dateStart,
                dateFinish,
                contractId,
                currencyId,
                LoadPlanLike.of("records")
        );
    }

}
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

    public void inspectDocumentWithStreamMapLambdaEntityContinuation(Document document) {
        document.getLines()
                .stream()
                .map(line -> line.getProduct())
                .forEach(product -> product.getSku());
    }

    public void inspectDocumentWithStreamMapLambdaLeafToList(Document document) {
        document.getLines()
                .stream()
                .map(line -> line.getProduct().getSku())
                .toList();
    }

    public void inspectDocumentWithStreamMapLambdaThenFilter(Document document) {
        document.getLines()
                .stream()
                .map(line -> line.getProduct())
                .filter(product -> product.getCategory().getCode().equals("A"))
                .forEach(product -> product.getSku());
    }

    public void inspectDocumentWithStreamMapBlockLambdaEntityContinuation(Document document) {
        document.getLines()
                .stream()
                .map(line -> {
                    return line.getProduct();
                })
                .forEach(product -> product.getSku());
    }

    public void inspectDocumentWithStreamMapBlockLambdaLeafToList(Document document) {
        document.getLines()
                .stream()
                .map(line -> {
                    return line.getProduct().getSku();
                })
                .toList();
    }

    public void inspectDocumentWithStreamMapBlockLambdaPreReturnRead(Document document) {
        document.getLines()
                .stream()
                .map(line -> {
                    line.getProduct().getCategory().getCode();
                    return line.getProduct();
                })
                .forEach(product -> product.getSku());
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

    public void inspectDocumentWithStreamFilterLambda(Document document) {
        document.getLines()
                .stream()
                .filter(line -> line.getProduct().getSku() != null)
                .forEach(line -> line.getQuantity());
    }

    public void inspectDocumentWithStreamFilterMethodCallLambda(Document document) {
        document.getLines()
                .stream()
                .filter(line -> line.getProduct().getCategory().getCode().equals("A"))
                .forEach(line -> line.getQuantity());
    }

    public void inspectDocumentWithStreamAnyMatchLambda(Document document) {
        document.getLines()
                .stream()
                .anyMatch(line -> line.getProduct().getSku() != null);
    }

    public void inspectDocumentWithStreamAllMatchLambda(Document document) {
        document.getLines()
                .stream()
                .allMatch(line -> line.getProduct().getCategory().getCode().equals("A"));
    }

    public void inspectDocumentWithStreamNoneMatchLambda(Document document) {
        document.getLines()
                .stream()
                .noneMatch(line -> line.getQuantity() == 0);
    }

    public void inspectDocumentWithStreamCollectToMapMethodRefs(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        DocumentLine::getProduct,
                        DocumentLine::getQuantity
                ));
    }

    public void inspectDocumentWithStreamCollectToMapLambdas(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        line -> line.getProduct().getSku(),
                        line -> line.getQuantity()
                ));
    }

    public void inspectDocumentWithStreamCollectToMapMerge(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        line -> line.getProduct().getSku(),
                        line -> line.getQuantity(),
                        (a, b) -> a
                ));
    }

    public void inspectDocumentWithStreamCollectToMapIdentityValue(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        line -> line.getProduct().getSku(),
                        java.util.function.Function.identity()
                ));
    }

    public void inspectDocumentWithStreamCollectGroupingByLambda(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        line -> line.getProduct().getCategory().getCode()
                ));
    }

    public void inspectDocumentWithStreamCollectGroupingByMethodRef(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        DocumentLine::getProduct
                ));
    }

    public void inspectDocumentWithStreamCollectGroupingByDownstream(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        line -> line.getProduct().getCategory().getCode(),
                        java.util.stream.Collectors.toList()
                ));
    }

    public void inspectDocumentWithStreamCollectGroupingBySupplierDownstream(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        line -> line.getProduct().getCategory().getCode(),
                        java.util.HashMap::new,
                        java.util.stream.Collectors.toList()
                ));
    }

    public void inspectDocumentWithStreamToListTerminal(Document document) {
        document.getLines()
                .stream()
                .toList();
    }

    public void inspectDocumentWithStreamCountTerminal(Document document) {
        document.getLines()
                .stream()
                .count();
    }

    public void inspectDocumentWithStreamCollectToListTerminal(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.toList());
    }

    public void inspectDocumentWithStreamCollectToSetTerminal(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.toSet());
    }

    public void inspectDocumentWithStreamCollectToUnmodifiableListTerminal(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableList());
    }

    public void inspectDocumentWithStreamCollectToCollectionTerminal(Document document) {
        document.getLines()
                .stream()
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }

    public void inspectDocumentWithStreamFlatMapLambda(Document document) {
        document.getContracts()
                .stream()
                .flatMap(contract -> contract.getLines().stream())
                .forEach(line -> line.getProduct().getSku());
    }

    public void inspectDocumentWithStreamFlatMapBlockLambda(Document document) {
        document.getContracts()
                .stream()
                .flatMap(contract -> {
                    return contract.getLines().stream();
                })
                .forEach(line -> line.getProduct().getSku());
    }

    public void inspectDocumentWithStreamFlatMapToList(Document document) {
        document.getContracts()
                .stream()
                .flatMap(contract -> contract.getLines().stream())
                .toList();
    }

    public void inspectDocumentWithStreamSortedComparatorLambda(Document document) {
        document.getLines()
                .stream()
                .sorted(java.util.Comparator.comparing(line -> line.getProduct().getSku()))
                .forEach(line -> line.getQuantity());
    }

    public void inspectDocumentWithStreamSortedComparatorMethodRefToList(Document document) {
        document.getLines()
                .stream()
                .sorted(java.util.Comparator.comparing(DocumentLine::getQuantity))
                .toList();
    }

    public void inspectDocumentWithStreamSortedComparatorComparingIntToList(Document document) {
        document.getLines()
                .stream()
                .sorted(java.util.Comparator.comparingInt(DocumentLine::getQuantity))
                .toList();
    }

    public void inspectDocumentWithStreamSortedComparatorReversedToList(Document document) {
        document.getLines()
                .stream()
                .sorted(java.util.Comparator.comparing((DocumentLine line) -> line.getProduct().getSku()).reversed())
                .toList();
    }

    public void inspectDocumentWithStreamMaxComparatorLambda(Document document) {
        document.getLines()
                .stream()
                .max(java.util.Comparator.comparing(line -> line.getProduct().getSku()));
    }

    public void inspectDocumentWithStreamMinComparatorMethodRef(Document document) {
        document.getLines()
                .stream()
                .min(java.util.Comparator.comparing(DocumentLine::getQuantity));
    }

    public void inspectDocumentWithStreamMaxComparatorComparingInt(Document document) {
        document.getLines()
                .stream()
                .max(java.util.Comparator.comparingInt(DocumentLine::getQuantity));
    }

    public void inspectDocumentWithStreamMinComparatorReversed(Document document) {
        document.getLines()
                .stream()
                .min(java.util.Comparator.comparing((DocumentLine line) -> line.getProduct().getSku()).reversed());
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
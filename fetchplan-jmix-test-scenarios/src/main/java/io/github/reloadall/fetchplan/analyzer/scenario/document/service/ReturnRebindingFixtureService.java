package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Address;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Contract;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Currency;
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

    public void returnedParameterShouldRebindExplicitOrigin(Document document) {
        Address address = sameAddress(document.getShippingAddress());
        address.getCity();
    }

    public void helperBodyReadsShouldBePreservedForNonRebindableValueCall(GroupingAct act) {
        List<? extends GroupingLine> liabilityLines = act.getLiabilityLines();
        List<? extends GroupingLine> paymentLines = act.getPaymentLines();

        Map<GroupingKey, BigDecimal> liabilityTotals = groupTotals(liabilityLines);
        Map<GroupingKey, BigDecimal> paymentTotals = groupTotals(paymentLines);
    }

    public void helperBodyReadsShouldIgnoreReturnedMapUsageForNonRebindableValueCall(GroupingAct act) {
        List<? extends GroupingLine> liabilityLines = act.getLiabilityLines();

        Map<GroupingKey, BigDecimal> totals = groupTotals(liabilityLines);
        totals.keySet();
    }

    public void scalarArgumentsMustNotBecomePathAnchors(Document document) {
        findPaymentTransactions(
                document.getDateStart(),
                document.getDateFinish(),
                Id.of(document.getContract()),
                Id.of(document.getCurrency())
        );
    }

    public void fullChainQueryResultMustNotInheritFilterArgumentOrigins(FullChainAct act) {
        List<Transaction> paymentTransactions = findPaymentTransactions(
                act.getDateStart(),
                act.getDateFinish(),
                Id.of(act.getContract()),
                Id.of(act.getCurrency())
        );

        List<Payment> payments = generatePayments(paymentTransactions, act);
        act.setPaymentLines(payments);
    }

    public void fullChainBoundaryReturnMustNotInheritFilterArgumentOrigins(FullChainAct act) {
        List<Transaction> paymentTransactions = findPaymentTransactionsViaBoundary(
                act.getDateStart(),
                act.getDateFinish(),
                Id.of(act.getContract()),
                Id.of(act.getCurrency())
        );

        List<Payment> payments = generatePayments(paymentTransactions, act);
        act.setPaymentLines(payments);
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

    private Address sameAddress(Address address) {
        return address;
    }

    private Map<GroupingKey, BigDecimal> groupTotals(List<? extends GroupingLine> lines) {
        Map<GroupingKey, BigDecimal> result = new HashMap<>();
        for (GroupingLine line : lines) {
            GroupingKey key = new GroupingKey(
                    line.getRate(),
                    line.getNomenclature(),
                    line.getType()
            );
            BigDecimal currentSum = result.getOrDefault(key, BigDecimal.ZERO);
            result.put(key, currentSum.add(line.getCost()));
        }
        return result;
    }

    private List<Transaction> findPaymentTransactions(LocalDate dateStart,
                                                      LocalDate dateFinish,
                                                      Id<Contract> contractId,
                                                      Id<Currency> currencyId) {
        String.valueOf(dateStart);
        String.valueOf(dateFinish);
        String.valueOf(contractId);
        String.valueOf(currencyId);
        return List.of();
    }

    private List<Transaction> findPaymentTransactionsViaBoundary(LocalDate dateStart,
                                                                 LocalDate dateFinish,
                                                                 Id<Contract> contractId,
                                                                 Id<Currency> currencyId) {
        return transactionQuery(dateStart, dateFinish, contractId, currencyId);
    }

    private List<Transaction> transactionQuery(LocalDate dateStart,
                                               LocalDate dateFinish,
                                               Id<Contract> contractId,
                                               Id<Currency> currencyId) {
        return List.of();
    }

    private List<Payment> generatePayments(List<Transaction> transactions, FullChainAct act) {
        for (Transaction transaction : transactions) {
            transaction.getDocLine();
            transaction.getCost();
        }
        return List.of();
    }

    public static class FullChainAct {
        private LocalDate dateStart;
        private LocalDate dateFinish;
        private Contract contract;
        private Currency currency;
        private List<Payment> paymentLines;

        public LocalDate getDateStart() {
            return dateStart;
        }

        public void setDateStart(LocalDate dateStart) {
            this.dateStart = dateStart;
        }

        public LocalDate getDateFinish() {
            return dateFinish;
        }

        public void setDateFinish(LocalDate dateFinish) {
            this.dateFinish = dateFinish;
        }

        public Contract getContract() {
            return contract;
        }

        public void setContract(Contract contract) {
            this.contract = contract;
        }

        public Currency getCurrency() {
            return currency;
        }

        public void setCurrency(Currency currency) {
            this.currency = currency;
        }

        public List<Payment> getPaymentLines() {
            return paymentLines;
        }

        public void setPaymentLines(List<Payment> paymentLines) {
            this.paymentLines = paymentLines;
        }
    }

    public static class GroupingAct {
        private List<? extends GroupingLine> liabilityLines;
        private List<? extends GroupingLine> paymentLines;

        public List<? extends GroupingLine> getLiabilityLines() {
            return liabilityLines;
        }

        public void setLiabilityLines(List<? extends GroupingLine> liabilityLines) {
            this.liabilityLines = liabilityLines;
        }

        public List<? extends GroupingLine> getPaymentLines() {
            return paymentLines;
        }

        public void setPaymentLines(List<? extends GroupingLine> paymentLines) {
            this.paymentLines = paymentLines;
        }
    }

    public static class GroupingLine {
        private BigDecimal rate;
        private String nomenclature;
        private String type;
        private BigDecimal cost;

        public BigDecimal getRate() {
            return rate;
        }

        public void setRate(BigDecimal rate) {
            this.rate = rate;
        }

        public String getNomenclature() {
            return nomenclature;
        }

        public void setNomenclature(String nomenclature) {
            this.nomenclature = nomenclature;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public BigDecimal getCost() {
            return cost;
        }

        public void setCost(BigDecimal cost) {
            this.cost = cost;
        }
    }

    public static class GroupingKey {
        public GroupingKey(BigDecimal rate, String nomenclature, String type) {
        }
    }

    public static class Id<T> {
        public static <T> Id<T> of(T value) {
            return new Id<>();
        }
    }
    public static class Transaction {
        private String docLine;
        private BigDecimal cost;

        public String getDocLine() {
            return docLine;
        }

        public BigDecimal getCost() {
            return cost;
        }
    }

    public static class Payment {
    }
}
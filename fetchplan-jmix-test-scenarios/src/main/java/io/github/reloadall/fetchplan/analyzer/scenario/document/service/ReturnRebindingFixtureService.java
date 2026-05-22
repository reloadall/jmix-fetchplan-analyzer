package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
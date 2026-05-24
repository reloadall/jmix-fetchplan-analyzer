package io.github.reloadall.fetchplan.analyzer.scenario.document.entity;

import java.util.List;
import java.util.UUID;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@JmixEntity
@Entity(name = "fpa_scenario_Contract")
@Table(name = "FPA_SCENARIO_CONTRACT")
public class Contract {

    @Id
    @JmixGeneratedValue
    private UUID id;

    @Column(name = "NUMBER_")
    private String number;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DOCUMENT_ID")
    private Document document;

    @OneToMany(mappedBy = "contract")
    private List<DocumentLine> lines;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public List<DocumentLine> getLines() {
        return lines;
    }

    public void setLines(List<DocumentLine> lines) {
        this.lines = lines;
    }
}
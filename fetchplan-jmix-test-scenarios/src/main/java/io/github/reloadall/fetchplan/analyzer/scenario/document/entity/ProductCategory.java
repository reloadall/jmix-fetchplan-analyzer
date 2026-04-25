package io.github.reloadall.fetchplan.analyzer.scenario.document.entity;

import java.util.UUID;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@JmixEntity
@Entity(name = "fpa_scenario_ProductCategory")
@Table(name = "FPA_SCENARIO_PRODUCT_CATEGORY")
public class ProductCategory {

    @Id
    @JmixGeneratedValue
    private UUID id;

    @Column(name = "CODE")
    private String code;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
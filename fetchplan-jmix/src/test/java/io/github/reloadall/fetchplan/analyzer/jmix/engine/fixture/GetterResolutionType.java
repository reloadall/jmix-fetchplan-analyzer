package io.github.reloadall.fetchplan.analyzer.jmix.engine.fixture;

import java.util.UUID;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@JmixEntity
@Entity(name = "fpa_test_GetterResolutionType")
@Table(name = "FPA_TEST_GETTER_RESOLUTION_TYPE")
public class GetterResolutionType {

    @Id
    @JmixGeneratedValue
    private UUID id;

    @Column(name = "CODE")
    private String code;

    @Column(name = "NAME")
    private String name;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CodeKind getCodeAsEnum() {
        if (code == null) {
            return null;
        }
        return CodeKind.valueOf(code);
    }

    public String getInstanceName() {
        return getCode() + " - " + getName();
    }

    public String getDisplayLabel() {
        String localCode = getCode();
        return localCode;
    }

    public String getDirectFieldLabel() {
        return code + " - " + name;
    }

    public String getRecursiveLabel() {
        return getRecursiveAlias();
    }

    public String getRecursiveAlias() {
        return getRecursiveLabel();
    }

    public enum CodeKind {
        INTERNAL,
        EXTERNAL
    }
}
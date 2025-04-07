package com.pfe.DFinancialStatement.report_generation.json_structure;

import java.util.List;

public class Actif {
    private String name;
    private List<Field> fields;

    public Actif() {
    }

    public Actif(String name, List<Field> fields) {
        this.name = name;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }
}

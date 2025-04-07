package com.pfe.DFinancialStatement.report_generation.json_structure;

public class Field {
    private String label;
    private double value;

    public Field() {
    }

    public Field(String label, double value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}

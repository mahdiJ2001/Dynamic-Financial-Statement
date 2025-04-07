package com.pfe.DFinancialStatement.report_generation.json_structure;

import java.util.List;

public class Root {
    private List<Actif> actif;
    private List<Passif> passif;

    public List<Actif> getActif() {
        return actif;
    }

    public void setActif(List<Actif> actif) {
        this.actif = actif;
    }

    public List<Passif> getPassif() {
        return passif;
    }

    public void setPassif(List<Passif> passif) {
        this.passif = passif;
    }
}

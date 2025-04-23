package com.pfe.DFinancialStatement.report_generation.service;

import com.pfe.DFinancialStatement.report_generation.json_structure.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ReportGenerationService {

    public byte[] generateFinancialReport(Map<String, Object> inputJson, String companyName, String designName) throws Exception {

        Root root = convertJsonToRoot(inputJson);

        String jrxmlPath = "/" + designName + ".jrxml";
        InputStream reportStream = this.getClass().getResourceAsStream(jrxmlPath);
        if (reportStream == null) {
            throw new IllegalArgumentException("'" + jrxmlPath + "' not found in resources.");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("COMPANY_NAME", companyName);
        parameters.put("REPORT_DATE", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

        InputStream logoStream = this.getClass().getResourceAsStream("/images/logo.png");
        if (logoStream == null) {
            throw new IllegalArgumentException("'logo.png' not found in /images.");
        }
        Image logoImage = ImageIO.read(logoStream);
        parameters.put("LOGO", logoImage);

        List<Root> rootList = Collections.singletonList(root);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(rootList);

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

        return pdfBytes;
    }



    private Root convertJsonToRoot(Map<String, Object> inputJson) {

        List<Map<String, Object>> actifList = (List<Map<String, Object>>) inputJson.get("actif");
        List<Map<String, Object>> passifList = (List<Map<String, Object>>) inputJson.get("passif");


        List<Actif> actives = new ArrayList<>();
        for (Map<String, Object> actifMap : actifList) {
            String name = (String) actifMap.get("name");
            List<Map<String, Object>> fields = (List<Map<String, Object>>) actifMap.get("fields");

            List<Field> fieldList = new ArrayList<>();
            for (Map<String, Object> fieldMap : fields) {
                String label = (String) fieldMap.get("label");
                double value = ((Number) fieldMap.get("value")).doubleValue();
                fieldList.add(new Field(label, value));
            }

            actives.add(new Actif(name, fieldList));
        }

        List<Passif> passives = new ArrayList<>();
        for (Map<String, Object> passifMap : passifList) {
            String name = (String) passifMap.get("name");
            List<Map<String, Object>> fields = (List<Map<String, Object>>) passifMap.get("fields");

            List<Field> fieldList = new ArrayList<>();
            for (Map<String, Object> fieldMap : fields) {
                String label = (String) fieldMap.get("label");
                double value = ((Number) fieldMap.get("value")).doubleValue();
                fieldList.add(new Field(label, value));
            }

            passives.add(new Passif(name, fieldList));
        }

        Root root = new Root();
        root.setActif(actives);
        root.setPassif(passives);

        return root;
    }
}

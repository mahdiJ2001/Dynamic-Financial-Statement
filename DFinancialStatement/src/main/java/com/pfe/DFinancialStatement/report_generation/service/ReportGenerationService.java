package com.pfe.DFinancialStatement.report_generation.service;

import com.pfe.DFinancialStatement.report_generation.json_structure.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ReportGenerationService {

    public byte[] generateFinancialReport(Map<String, Object> inputJson, String companyName) throws Exception {
        // Convert the JSON input to Root structure
        Root root = convertJsonToRoot(inputJson);

        // Load the JRXML report from resources
        InputStream reportStream = this.getClass().getResourceAsStream("/Designed.jrxml");
        if (reportStream == null) {
            throw new IllegalArgumentException("'Designed.jrxml' not found in resources.");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // Prepare report parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("COMPANY_NAME", companyName);  // Use the input company name
        parameters.put("REPORT_DATE", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

        // Load logo image from resources and convert to java.awt.Image
        InputStream logoStream = this.getClass().getResourceAsStream("/images/logo.png");
        if (logoStream == null) {
            throw new IllegalArgumentException("'logo.png' not found in /images.");
        }
        Image logoImage = ImageIO.read(logoStream);
        parameters.put("LOGO", logoImage);

        // Wrap the received Root object in a List for the data source
        List<Root> rootList = Collections.singletonList(root);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(rootList);

        // Fill the report with data
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        // Define the target path for the PDF file
        Path targetPath = Paths.get("src/main/resources/reports/FinancialReport.pdf");

        // Export the report to a byte array
        byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

        // Optionally, save the file to resources (if needed)
        JasperExportManager.exportReportToPdfFile(jasperPrint, targetPath.toString());

        return pdfBytes;
    }

    private Root convertJsonToRoot(Map<String, Object> inputJson) {
        // Extract "actif" and "passif" lists from the incoming JSON
        List<Map<String, Object>> actifList = (List<Map<String, Object>>) inputJson.get("actif");
        List<Map<String, Object>> passifList = (List<Map<String, Object>>) inputJson.get("passif");

        // Convert Actif data
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

        // Convert Passif data
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

        // Return a new Root object with the converted data
        Root root = new Root();
        root.setActif(actives);
        root.setPassif(passives);

        return root;
    }
}

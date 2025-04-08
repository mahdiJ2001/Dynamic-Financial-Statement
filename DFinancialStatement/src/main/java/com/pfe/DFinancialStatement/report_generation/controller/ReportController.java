package com.pfe.DFinancialStatement.report_generation.controller;

import com.pfe.DFinancialStatement.report_generation.json_structure.Root;
import com.pfe.DFinancialStatement.report_generation.service.ReportGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportGenerationService reportGenerationService;

    @Autowired
    public ReportController(ReportGenerationService reportGenerationService) {
        this.reportGenerationService = reportGenerationService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generateFinancialReport(@RequestBody Map<String, Object> inputJson) {
        try {
            // Extract company name from inputJson
            String companyName = (String) inputJson.get("companyName");

            // Pass company name along with other parameters to the service
            byte[] pdfBytes = reportGenerationService.generateFinancialReport(inputJson, companyName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // "inline" to display in browser, or "attachment" to force download
            headers.setContentDispositionFormData("inline", "FinancialReport.pdf");

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error generating the report".getBytes());
        }
    }
}

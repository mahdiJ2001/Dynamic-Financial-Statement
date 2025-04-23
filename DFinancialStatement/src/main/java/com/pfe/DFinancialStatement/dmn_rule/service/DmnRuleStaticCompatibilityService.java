package com.pfe.DFinancialStatement.dmn_rule.service;

import com.pfe.DFinancialStatement.dmn_rule.entity.DmnRule;
import com.pfe.DFinancialStatement.dmn_rule.repository.DmnRuleRepository;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Service
public class DmnRuleStaticCompatibilityService {

    private final DmnRuleRepository dmnRuleRepository;

    public DmnRuleStaticCompatibilityService(DmnRuleRepository dmnRuleRepository) {
        this.dmnRuleRepository = dmnRuleRepository;
    }

    /**
     * Normalizes field names based on the following rules:
     *  - Replaces spaces with underscores.
     *  - Removes accented characters.
     *  - Matching is case-insensitive.
     *  - Multiple underscores are replaced by a single underscore.
     */
    private String normalizeFieldName(String rawName) {
        String withoutAccents = Normalizer.normalize(rawName, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String normalizedName = withoutAccents.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_");
        System.out.println("Normalized field name: " + rawName + " -> " + normalizedName);
        return normalizedName;
    }

    /**
     * Extracts the input labels defined in the DMN XML.
     *
     * This method uses an XPath expression to extract attribute values.
     * You may need to adjust the XPath query depending on how your DMN inputs
     * are structured. For example, if your DMN XML uses a different tag or attribute
     * (such as "inputData" with an attribute "id" or "label"), update the XPath accordingly.
     */
    private Set<String> extractInputFieldsFromXml(String xmlContent) {
        Set<String> inputFields = new HashSet<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlContent)));


            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();

            XPathExpression expr = xpath.compile("//input/@label");
            NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); i++) {
                String label = nodes.item(i).getNodeValue();
                String normalizedLabel = normalizeFieldName(label);
                inputFields.add(normalizedLabel);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        System.out.println("Extracted DMN input fields: " + inputFields);
        return inputFields;
    }

    /**
     * Finds compatible DMN rules by checking that all input fields required by the DMN XML are present
     * in the provided form fields (after normalization).
     *
     * @param formFields A set of strings representing the fields present in the form.
     * @return A list of compatible DMN rules.
     */
    public List<DmnRule> findCompatibleDmns(Set<String> formFields) {
        System.out.println("Finding compatible DMNs using static analysis...");


        Set<String> normalizedFormFields = formFields.stream()
                .map(this::normalizeFieldName)
                .collect(Collectors.toSet());
        System.out.println("Normalized form fields: " + normalizedFormFields);

        List<DmnRule> compatible = new ArrayList<>();


        for (DmnRule rule : dmnRuleRepository.findAll()) {
            System.out.println("Processing DMN rule: " + rule.getRuleKey());
            String xmlContent = rule.getRuleContent();

            Set<String> dmnInputs = extractInputFieldsFromXml(xmlContent);

            if (normalizedFormFields.containsAll(dmnInputs)) {
                compatible.add(rule);
                System.out.println("DMN rule " + rule.getRuleKey() + " is compatible.");
            } else {
                System.out.println("DMN rule " + rule.getRuleKey() + " is not compatible.");
            }
        }

        System.out.println("Found " + compatible.size() + " compatible DMNs.");
        return compatible;
    }
}

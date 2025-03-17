import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.xmlunit.diff.ComparisonType.TEXT_VALUE;

public class XmlComparator {

    private static final String NO_VALUE = "N/A";
    private static final Set<String> IGNORE_NODES = Set.of("toBeIgnored", "tooIgnored");
    private static final Set<String> IGNORE_ATTRIBUTES = Set.of("bikId=\"1111\"");
    private static final Set<String> IGNORE_ATTRIBUTE_COUNT_NODES = Set.of("newItem");

    private static final Set<String> TIME_WITH_DIFFERENT_ENDING = Set.of("bikId=\"1234\"");

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        File oldXmlFile = new File("old_format.xml");
        File newXmlFile = new File("new_format.xml");

        String oldXml = new String(Files.readAllBytes(oldXmlFile.toPath()));
        String newXml = new String(Files.readAllBytes(newXmlFile.toPath()));

        try (PrintWriter writer = new PrintWriter("output.txt")) {
            compareXml(oldXml, newXml, writer);
        }
    }

    private static void compareXml(String oldXml, String newXml, PrintWriter writer) throws ParserConfigurationException, IOException, SAXException {
        int totalNodes = countNodes(oldXml) + countNodes(newXml);

        Diff diff = DiffBuilder.compare(oldXml)
                .withTest(newXml)
                .ignoreWhitespace()
                .ignoreComments()
                .checkForSimilar()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                .withNodeFilter(XmlComparator::filterNode)
                .withDifferenceEvaluator((comparison, comparisonResult) -> {
                    if (comparison.getType() == ComparisonType.ATTR_NAME_LOOKUP) {
                        Node controlNode = comparison.getControlDetails().getTarget();
                        if (controlNode != null && IGNORE_ATTRIBUTE_COUNT_NODES.contains(controlNode.getNodeName())) {
                            return ComparisonResult.SIMILAR;
                        }
                    }

                    if (isTimeDifferenceCase(comparison)) {
                        String oldValue = comparison.getControlDetails().getValue().toString();
                        String newValue = comparison.getTestDetails().getValue().toString();

                        if (oldValue.length() > 6 && newValue.length() > 6) {
                            oldValue = oldValue.substring(0, 6);
                            newValue = newValue.substring(0, 6);
                        }

                        return oldValue.equals(newValue) ? ComparisonResult.SIMILAR : comparisonResult;
                    }

                    return comparisonResult;
                })
                .build();

        List<Difference> differences = new ArrayList<>();
        diff.getDifferences().forEach(differences::add);

        List<String> missingNodes = new ArrayList<>();
        List<String> additionalNodes = new ArrayList<>();
        List<String> missingAttributes = new ArrayList<>();
        List<String> additionalAttributes = new ArrayList<>();

        writer.println("\n==== PORÓWNANIE XML ====");

        int actualDifferencesCount = 0;

        for (Difference d : differences) {
            Comparison comparison = d.getComparison();
            ComparisonType type = comparison.getType();
            String xpath = null;
            Object oldValueObj = comparison.getControlDetails().getValue();
            Object newValueObj = comparison.getTestDetails().getValue();

            if (comparison.getControlDetails().getXPath() != null) {
                xpath = comparison.getControlDetails().getXPath();
            } else if (comparison.getTestDetails().getXPath() != null) {
                xpath = comparison.getTestDetails().getXPath();
            }

            String oldValue = (oldValueObj != null) ? oldValueObj.toString() : NO_VALUE;
            String newValue = (newValueObj != null) ? newValueObj.toString() : NO_VALUE;

            if (type == ComparisonType.TEXT_VALUE) {
                writer.println("Różnica w wartości tekstowej:");
                writer.println("  - XPath: " + xpath);
                writer.println("  - Oczekiwane: " + oldValue);
                writer.println("  - Aktualne: " + newValue);
                actualDifferencesCount++;
            } else if (type == ComparisonType.CHILD_LOOKUP) {
                if (NO_VALUE.equals(oldValue) && !NO_VALUE.equals(newValue)) {
                    additionalNodes.add(xpath + " -> " + newValue);
                } else if (NO_VALUE.equals(newValue) && !NO_VALUE.equals(oldValue)) {
                    missingNodes.add(xpath + " -> " + oldValue);
                }
                actualDifferencesCount++;
            } else if (type == ComparisonType.ATTR_NAME_LOOKUP) {
                if (NO_VALUE.equals(oldValue) && !NO_VALUE.equals(newValue)) {
                    additionalAttributes.add(xpath + " -> " + newValue);
                } else if (NO_VALUE.equals(newValue) && !NO_VALUE.equals(oldValue)) {
                    missingAttributes.add(xpath + " -> " + oldValue);
                }
                actualDifferencesCount++;
            }
        }

        // Obliczanie % zgodności
        double similarityPercentage = 100.0 * (1 - ((double) actualDifferencesCount / totalNodes));
        writer.printf("\nProcent zgodności: %.2f%%\n", similarityPercentage);

        writer.println("#######################################################");
        writer.println("Brakujące węzły (są w starej wersji, brak w nowej)");
        missingNodes.forEach(writer::println);

        writer.println("#######################################################");
        writer.println("Nowe węzły (są w nowej wersji, brak w starej)");
        additionalNodes.forEach(writer::println);

        writer.println("#######################################################");
        writer.println("Brakujące atrybuty (są w starej wersji, brak w nowej)");
        missingAttributes.forEach(writer::println);

        writer.println("#######################################################");
        writer.println("Nowe atrybuty (są w nowej wersji, brak w starej)");
        additionalAttributes.forEach(writer::println);
    }


    private static boolean filterNode(final Node node) {
        if (IGNORE_NODES.contains(node.getNodeName())) {
            return false;
        }

        final NamedNodeMap attributes = node.getAttributes();

        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                final String attributeString = attribute.toString();
                if (IGNORE_ATTRIBUTES.contains(attributeString)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isTimeDifferenceCase(final Comparison comparison) {
        if (comparison.getType() == TEXT_VALUE) {

            final Node controlParent = comparison.getControlDetails().getTarget().getParentNode();
            final NamedNodeMap attributes = controlParent.getAttributes();

            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attribute = attributes.item(i);
                    final String attributeString = attribute.toString();
                    if (TIME_WITH_DIFFERENT_ENDING.contains(attributeString)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int countNodes(String xml) throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        Document document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));

        return countElementsRecursively(document.getDocumentElement());
    }

    private static int countElementsRecursively(Node node) {
        int count = 1; // Liczymy bieżący węzeł (element)

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) { // Liczymy tylko elementy
                count += countElementsRecursively(child);
            }
        }

        return count;
    }
}

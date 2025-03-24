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
import java.util.Map;
import java.util.Set;

import static org.xmlunit.diff.ComparisonType.ATTR_NAME_LOOKUP;
import static org.xmlunit.diff.ComparisonType.TEXT_VALUE;

public class XmlComparator {

    private static final String NO_VALUE = "N/A";
    private static final Set<String> IGNORE_NODES = Set.of("toBeIgnored", "tooIgnored");
    private static final Set<String> IGNORE_ATTRIBUTE_COUNT_NODES = Set.of("newItem");

    private static final Set<String> TIME_WITH_DIFFERENT_ENDING = Set.of("bikId=\"1234\"");

    private static final Map<String, String> SKIP_FOR_ATTRIBUTE_MAP = Map.of(
            "mapping", "not used",
            "mapping1", "skipped"
    );

    private static final Map<String, Set<String>> IGNORE_ATTRIBUTES_PER_NODE = Map.of(
            "time", Set.of("description"),
            "time2", Set.of("description")
    );

    private static String pathToSkip = null;

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
        Diff diff = DiffBuilder.compare(oldXml)
                .withTest(newXml)
                .ignoreWhitespace()
                .ignoreComments()
                .checkForSimilar()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                .withNodeFilter(XmlComparator::filterNode)
                .withDifferenceEvaluator((comparison, comparisonResult) -> {

                    Node testNode = comparison.getTestDetails().getTarget();

                    if (pathToSkip == null && hasSkippingAttribute(testNode)) {
                        pathToSkip = comparison.getControlDetails().getXPath();
                    }

                    if (pathToSkip != null) {
                        if (comparison.getTestDetails().getXPath() != null && comparison.getTestDetails().getXPath().contains(pathToSkip) ||
                        comparison.getControlDetails().getXPath() != null && comparison.getControlDetails().getXPath().contains(pathToSkip)) {
                            return ComparisonResult.SIMILAR;
                        } else {
                            pathToSkip = null;
                        }
                    }


                    if ((comparison.getType() == ComparisonType.ATTR_VALUE || comparison.getType() == ATTR_NAME_LOOKUP)) {
                        final Comparison.Detail controlDetails = comparison.getControlDetails();

                        final String controlParentNodeXPath = controlDetails.getParentXPath();
                        final String controlParentLastNode = getLastNodeFromParentXPath(controlParentNodeXPath);

                        final String controlNodeXPath = controlDetails.getXPath();
                        String controlNodeAttributeName = getAttributeName(controlNodeXPath);


                        final Comparison.Detail testDetails = comparison.getTestDetails();

                        final String testParentNodeXPath = testDetails.getParentXPath();
                        final String testParentLastNode = getLastNodeFromParentXPath(testParentNodeXPath);

                        final String testNodeXPath = testDetails.getXPath();
                        String testNodeAttributeName = getAttributeName(testNodeXPath);

                        if (IGNORE_ATTRIBUTES_PER_NODE.containsKey(controlParentLastNode) &&
                                IGNORE_ATTRIBUTES_PER_NODE.get(controlParentLastNode).contains(controlNodeAttributeName)) {
                            return ComparisonResult.SIMILAR;
                        }

                        if (IGNORE_ATTRIBUTES_PER_NODE.containsKey(testParentLastNode) &&
                                IGNORE_ATTRIBUTES_PER_NODE.get(testParentLastNode).contains(testNodeAttributeName)) {
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

//        List<String> missingNodes = new ArrayList<>();
//        List<String> additionalNodes = new ArrayList<>();
//        List<String> missingAttributes = new ArrayList<>();
//        List<String> additionalAttributes = new ArrayList<>();

        writer.println("\n==== PORÓWNANIE XML ====");

        int actualDifferencesCount = 0;

        for (Difference d : differences) {
            Comparison comparison = d.getComparison();
            ComparisonType type = comparison.getType();
            String xpath = null;


            if (comparison.getControlDetails().getXPath() != null) {
                xpath = comparison.getControlDetails().getXPath();
            } else if (comparison.getTestDetails().getXPath() != null) {
                xpath = comparison.getTestDetails().getXPath();
            }

            if (type == ComparisonType.TEXT_VALUE) {
                Object oldValueObj = comparison.getControlDetails().getValue();
                Object newValueObj = comparison.getTestDetails().getValue();

                String oldValue = (oldValueObj != null) ? oldValueObj.toString() : NO_VALUE;
                String newValue = (newValueObj != null) ? newValueObj.toString() : NO_VALUE;

                writer.println("Różnica w wartości tekstowej (w wezle rodzic):");
                writer.println("  - XPath: " + xpath);
                writer.println("  - Oczekiwane: " + oldValue);
                writer.println("  - Aktualne: " + newValue);
                actualDifferencesCount++;
            } else if (type == ComparisonType.CHILD_LOOKUP) {
                final Node oldNode = comparison.getControlDetails().getTarget();
                final String oldValue = oldNode != null ? oldNode.getTextContent() : NO_VALUE;

                final Node newNode = comparison.getTestDetails().getTarget();
                final String newValue = newNode != null ? newNode.getTextContent() : NO_VALUE;

                writer.println("Różnica w wartości tekstowej (w wezle dziecko):");
                writer.println("  - XPath: " + xpath);
                writer.println("  - Oczekiwane: " + oldValue);
                writer.println("  - Aktualne: " + newValue);
                actualDifferencesCount++;
            }
        }

        // Obliczanie % zgodności
        int oldXmlNodeCount = countNodes(oldXml);
        int correctNodeCount = oldXmlNodeCount - actualDifferencesCount;
        double similarityPercentage = ((double) (correctNodeCount) / oldXmlNodeCount) * 100;
        writer.printf("\nProcent zgodności: %.2f%%\n", similarityPercentage);
//
//        writer.println("#######################################################");
//        writer.println("Brakujące węzły (są w starej wersji, brak w nowej)");
//        missingNodes.forEach(writer::println);
//
//        writer.println("#######################################################");
//        writer.println("Nowe węzły (są w nowej wersji, brak w starej)");
//        additionalNodes.forEach(writer::println);
//
//        writer.println("#######################################################");
//        writer.println("Brakujące atrybuty (są w starej wersji, brak w nowej)");
//        missingAttributes.forEach(writer::println);
//
//        writer.println("#######################################################");
//        writer.println("Nowe atrybuty (są w nowej wersji, brak w starej)");
//        additionalAttributes.forEach(writer::println);
    }

    private static String getAttributeName(String controlNodeXPath) {
        return controlNodeXPath.contains("@") ? controlNodeXPath.substring(controlNodeXPath.indexOf('@') + 1) : "";
    }

    private static String getLastNodeFromParentXPath(String controlNodeParentXPath) {
        final int lastSlashIndex = controlNodeParentXPath.lastIndexOf('/');
        int firstBracketIndex = controlNodeParentXPath.indexOf('[', lastSlashIndex);

        return (firstBracketIndex != -1)
                ? controlNodeParentXPath.substring(lastSlashIndex + 1, firstBracketIndex)
                : controlNodeParentXPath.substring(lastSlashIndex + 1);
    }

    private static boolean hasSkippingAttribute(Node node) {
        if (node == null || !node.hasAttributes()) {
            return false;
        }

        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = attribute.getNodeName();
            String attributeValue = attribute.getNodeValue();

            if (SKIP_FOR_ATTRIBUTE_MAP.containsKey(attributeName) &&
                    SKIP_FOR_ATTRIBUTE_MAP.get(attributeName).equals(attributeValue)) {
                return true;
            }
        }
        return false;
    }


    private static boolean filterNode(final Node node) {
        return !IGNORE_NODES.contains(node.getNodeName());
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

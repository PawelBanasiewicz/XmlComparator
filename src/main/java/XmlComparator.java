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
import java.util.*;

import static org.xmlunit.diff.ComparisonType.ATTR_NAME_LOOKUP;
import static org.xmlunit.diff.ComparisonType.TEXT_VALUE;

public class XmlComparator {

    private static final String NO_VALUE = "NULL";
    private static final Set<String> IGNORE_NODES = Set.of("toBeIgnored", "tooIgnored");
    private static final Set<String> IGNORE_ATTRIBUTE_COUNT_NODES = Set.of("newItem");

    private static final Set<String> TIME_WITH_DIFFERENT_ENDING = Set.of("bikId=\"1234\"");

    private static final Map<String, String> SKIP_FOR_ATTRIBUTE_MAP = Map.of(
            "mapping", "not used",
            "mapping1", "skipped"
    );

    private static final Map<String, Set<String>> IGNORE_ATTRIBUTES_PER_NODE_MAP = Map.of(
            "time", Set.of("description"),
            "time2", Set.of("description")
    );

    private static String pathToSkip = null;

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        File oldXmlFile = new File("old_format.xml");
        File newXmlFile = new File("new_format.xml");

        String oldXml = new String(Files.readAllBytes(oldXmlFile.toPath()));
        String newXml = new String(Files.readAllBytes(newXmlFile.toPath()));

        try (final PrintWriter writer = new PrintWriter("output.txt")) {
            compareXml(oldXml, newXml, writer);
        }
    }

    private static void compareXml(final String oldXml, final String newXml, final PrintWriter writer) throws ParserConfigurationException, IOException, SAXException {
        final Diff diff = DiffBuilder.compare(oldXml)
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
                        final String controlParentLastNode = getLastNodeFromXPath(controlParentNodeXPath);

                        final String controlNodeXPath = controlDetails.getXPath();
                        final String controlNodeAttributeName = getAttributeNameFromXPath(controlNodeXPath);

                        final Comparison.Detail testDetails = comparison.getTestDetails();

                        final String testParentNodeXPath = testDetails.getParentXPath();
                        final String testParentLastNode = getLastNodeFromXPath(testParentNodeXPath);

                        final String testNodeXPath = testDetails.getXPath();
                        final String testNodeAttributeName = getAttributeNameFromXPath(testNodeXPath);

                        if (IGNORE_ATTRIBUTES_PER_NODE_MAP.containsKey(controlParentLastNode) &&
                                IGNORE_ATTRIBUTES_PER_NODE_MAP.get(controlParentLastNode).contains(controlNodeAttributeName)) {
                            return ComparisonResult.SIMILAR;
                        }

                        if (IGNORE_ATTRIBUTES_PER_NODE_MAP.containsKey(testParentLastNode) &&
                                IGNORE_ATTRIBUTES_PER_NODE_MAP.get(testParentLastNode).contains(testNodeAttributeName)) {
                            return ComparisonResult.SIMILAR;
                        }
                    }

                    if (isTimeDifferenceCase(comparison)) {
                        String controlValue = comparison.getControlDetails().getValue().toString();
                        String testValue = comparison.getTestDetails().getValue().toString();

                        if (controlValue.length() > 6 && testValue.length() > 6) {
                            controlValue = controlValue.substring(0, 6);
                            testValue = testValue.substring(0, 6);
                        }

                        return controlValue.equals(testValue) ? ComparisonResult.SIMILAR : comparisonResult;
                    }

                    return comparisonResult;
                })
                .build();

        final List<Difference> differences = new ArrayList<>();
        diff.getDifferences().forEach(differences::add);

        final List<ReportDifference> textDifferences = new ArrayList<>();
        final List<ReportDifference> attributeDifferences = new ArrayList<>();
        final List<ReportDifference> orderDifferences = new ArrayList<>();
        final List<ReportDifference> otherDifferences = new ArrayList<>();

        for (final Difference difference : differences) {
            final Comparison comparison = difference.getComparison();
            final Comparison.Detail controlDetails = comparison.getControlDetails();
            final Comparison.Detail testDetails = comparison.getTestDetails();
            final ComparisonType type = comparison.getType();

            String xpath = chooseXPath(comparison);

            if (type == ComparisonType.TEXT_VALUE) {
                final Object controlValueObject = controlDetails.getValue();
                final Object testValueObject = testDetails.getValue();
                final String controlValue = (controlValueObject != null) ? controlValueObject.toString() : NO_VALUE;
                final String testValue = (testValueObject != null) ? testValueObject.toString() : NO_VALUE;

                final Node controlNode = controlDetails.getTarget();
                final Node testNode = testDetails.getTarget();
                final Map<String, String> oldNodeAttributes = getNodeAttributesFromNode(controlNode != null ? controlNode.getParentNode() : null);
                final Map<String, String> newNodeAttributes = getNodeAttributesFromNode(testNode != null ? testNode.getParentNode() : null);

                textDifferences.add(new TextDifference(xpath, oldNodeAttributes, newNodeAttributes, controlValue, testValue));
            } else if (type == ComparisonType.CHILD_LOOKUP) {
                final Node controlNode = controlDetails.getTarget();
                final String controlValue = controlNode != null ? controlNode.getTextContent() : NO_VALUE;

                final Node testNode = testDetails.getTarget();
                final String testValue = testNode != null ? testNode.getTextContent() : NO_VALUE;

                final Map<String, String> controlNodeAttributes = getNodeAttributesFromNode(controlNode);
                final Map<String, String> testNodeAttributes = getNodeAttributesFromNode(testNode);

                textDifferences.add(new TextDifference(xpath, controlNodeAttributes, testNodeAttributes, controlValue, testValue));
            } else if (type == ComparisonType.ATTR_NAME_LOOKUP || type == ComparisonType.ATTR_VALUE) {
                final Object controlValueObject = controlDetails.getValue();
                final Object testValueObject = testDetails.getValue();

                final String controlValue = (controlValueObject != null) ? controlValueObject.toString() : NO_VALUE;
                final String testValue = (testValueObject != null) ? testValueObject.toString() : NO_VALUE;

                final Node controlNode = controlDetails.getTarget();
                final Node testNode = testDetails.getTarget();

                final Map<String, String> controlNodeAttributes = getNodeAttributesFromNode(controlNode);
                final Map<String, String> testNodeAttributes = getNodeAttributesFromNode(testNode);
//                attributeDifferences.add(new NonTextDifference(xpath, controlNodeAttributes, testNodeAttributes, "Atrybut zmieniony: " + controlValue + " → " + testValue));
                attributeDifferences.add(new TextDifference(xpath, controlNodeAttributes, testNodeAttributes, controlValue, testValue));
            } else if (type == ComparisonType.CHILD_NODELIST_SEQUENCE) {
                final Node controlNode = controlDetails.getTarget();
                final Node testNode = testDetails.getTarget();

                final Map<String, String> controlNodeAttributes = getNodeAttributesFromNode(controlNode);
                final Map<String, String> testNodeAttributes = getNodeAttributesFromNode(testNode);
                orderDifferences.add(new NonTextDifference(xpath, controlNodeAttributes, testNodeAttributes, "Different order of nodes"));
            } else {
                final Node controlNode = controlDetails.getTarget();
                final Node testNode = testDetails.getTarget();

                final Map<String, String> controlNodeAttributes = getNodeAttributesFromNode(controlNode);
                final Map<String, String> testNodeAttributes = getNodeAttributesFromNode(testNode);

                otherDifferences.add(new NonTextDifference(xpath, controlNodeAttributes, testNodeAttributes, "Difference type: " + type));
            }
        }

        writer.println("\n============ XML COMPARISON ============");

        int oldXmlNodeCount = countNodes(oldXml);
        int correctNodeCount = oldXmlNodeCount - textDifferences.size();
        double similarityPercentage = ((double) (correctNodeCount) / oldXmlNodeCount) * 100;
        writer.println("Control nodes: " + oldXmlNodeCount);
        writer.println("Mismatch nodes: " + textDifferences.size());

        writer.printf("\nSimilarity: %.2f%%\n", similarityPercentage);

        if (!textDifferences.isEmpty()) {
            writer.println("\n=== Text differences ===");
            textDifferences.forEach(writer::println);
        }

        if (!attributeDifferences.isEmpty()) {
            writer.println("\n=== Attribute differences ===");
            attributeDifferences.forEach(writer::println);
        }

        if (!orderDifferences.isEmpty()) {
            writer.println("\n=== Node order change ===");
            orderDifferences.forEach(writer::println);
        }

        if (!otherDifferences.isEmpty()) {
            writer.println("\n=== Other ===");
            otherDifferences.forEach(writer::println);
        }
    }

    private static String chooseXPath(final Comparison comparison) {
        if (comparison.getControlDetails().getXPath() != null) {
            return comparison.getControlDetails().getXPath();
        }

        if (comparison.getTestDetails().getXPath() != null) {
            return comparison.getTestDetails().getXPath();
        }

        return null;
    }

    private static Map<String, String> getNodeAttributesFromNode(final Node node) {
        if (node == null || !node.hasAttributes()) {
            return null;
        }

        final NamedNodeMap attributes = node.getAttributes();
        final Map<String, String> attributeMap = new HashMap<>();

        for (int i = 0; i < attributes.getLength(); i++) {
            final Node attribute = attributes.item(i);
            attributeMap.put(attribute.getNodeName(), attribute.getNodeValue());
        }

        return attributeMap;
    }

    private static String getAttributeNameFromXPath(final String nodeXPath) {
        return nodeXPath.contains("@") ? nodeXPath.substring(nodeXPath.indexOf('@') + 1) : "";
    }

    private static String getLastNodeFromXPath(final String nodeParentXPath) {
        final int lastSlashIndex = nodeParentXPath.lastIndexOf('/');
        final int firstBracketIndex = nodeParentXPath.indexOf('[', lastSlashIndex);

        return (firstBracketIndex != -1)
                ? nodeParentXPath.substring(lastSlashIndex + 1, firstBracketIndex)
                : nodeParentXPath.substring(lastSlashIndex + 1);
    }

    private static boolean hasSkippingAttribute(final Node node) {
        if (node == null || !node.hasAttributes()) {
            return false;
        }

        final NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            final Node attribute = attributes.item(i);
            final String attributeName = attribute.getNodeName();
            final String attributeValue = attribute.getNodeValue();

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
                    final Node attribute = attributes.item(i);
                    final String attributeString = attribute.toString();
                    if (TIME_WITH_DIFFERENT_ENDING.contains(attributeString)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int countNodes(final String xml) throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        final Document document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));

        return countElementsRecursively(document.getDocumentElement());
    }

    private static int countElementsRecursively(final Node node) {
        int count = 1; // Liczymy bieżący węzeł (element)

        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) { // Liczymy tylko elementy
                count += countElementsRecursively(child);
            }
        }

        return count;
    }
}

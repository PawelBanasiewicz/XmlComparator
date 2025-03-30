package comparator;

import comparator.diffrence.NonTextDifference;
import comparator.diffrence.ReportDifference;
import comparator.diffrence.TextDifference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.Difference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DifferenceAnalyzer {

    private static final String NO_VALUE = "NULL";

    public static DifferenceResult analyzeDifferences(final List<Difference> differences) {
        final List<ReportDifference> textDifferences = new ArrayList<>();
        final List<ReportDifference> attributeDifferences = new ArrayList<>();
        final List<ReportDifference> orderDifferences = new ArrayList<>();
        final List<ReportDifference> otherDifferences = new ArrayList<>();

        groupDifferences(differences, textDifferences, attributeDifferences, orderDifferences, otherDifferences);

        return new DifferenceResult(textDifferences, attributeDifferences, orderDifferences, otherDifferences);
    }

    private static void groupDifferences(final List<Difference> differences,
                                         final List<ReportDifference> textDifferences,
                                         final List<ReportDifference> attributeDifferences,
                                         final List<ReportDifference> orderDifferences,
                                         final List<ReportDifference> otherDifferences) {
        for (final Difference difference : differences) {
            final Comparison comparison = difference.getComparison();
            final String xPath = chooseXPath(comparison);

            switch (comparison.getType()) {
                case TEXT_VALUE -> {
                    final String controlValue = extractTextValue(comparison.getControlDetails());
                    final String testValue = extractTextValue(comparison.getTestDetails());

                    final Node controlNode = comparison.getControlDetails().getTarget();
                    final Node testNode = comparison.getTestDetails().getTarget();

                    final Map<String, String> controlNodeAttributes = getNodeAttributesFromNode(getParentNode(controlNode));
                    final Map<String, String> testNodeAttributes = getNodeAttributesFromNode(getParentNode(testNode));

                    textDifferences.add(new TextDifference(xPath, controlNodeAttributes, testNodeAttributes, controlValue, testValue));
                }
                case CHILD_LOOKUP -> {
                    final String controlValue = extractTextValue(comparison.getControlDetails());
                    final String testValue = extractTextValue(comparison.getTestDetails());

                    final Node controlNode = comparison.getControlDetails().getTarget();
                    final Node testNode = comparison.getTestDetails().getTarget();

                    final Map<String, String> controlNodeAttributes = getNodeAttributesFromNode(controlNode);
                    final Map<String, String> testNodeAttributes = getNodeAttributesFromNode(testNode);
                    textDifferences.add(new TextDifference(xPath, controlNodeAttributes, testNodeAttributes, controlValue, testValue));
                }
                case ATTR_NAME_LOOKUP, ATTR_VALUE -> {
                    final String controlValue = extractTextValue(comparison.getControlDetails());
                    final String testValue = extractTextValue(comparison.getTestDetails());

                    final Node controlNode = comparison.getControlDetails().getTarget();
                    final Node testNode = comparison.getTestDetails().getTarget();

                    final Map<String, String> controlNodeAttributes = getNodeAttributesFromNode(controlNode);
                    final Map<String, String> testNodeAttributes = getNodeAttributesFromNode(testNode);
                    attributeDifferences.add(new TextDifference(xPath, controlNodeAttributes, testNodeAttributes, controlValue, testValue));
                }
                case CHILD_NODELIST_SEQUENCE -> {
                    final Node controlNode = comparison.getControlDetails().getTarget();
                    final Node testNode = comparison.getTestDetails().getTarget();

                    final Map<String, String> controlNodeAttributes = getNodeAttributesFromNode(controlNode);
                    final Map<String, String> testNodeAttributes = getNodeAttributesFromNode(testNode);
                    orderDifferences.add(new NonTextDifference(xPath, controlNodeAttributes, testNodeAttributes, "Different order of nodes"));
                }
                default -> {
                    final Node controlNode = comparison.getControlDetails().getTarget();
                    final Node testNode = comparison.getTestDetails().getTarget();

                    final Map<String, String> controlNodeAttributes = getNodeAttributesFromNode(controlNode);
                    final Map<String, String> testNodeAttributes = getNodeAttributesFromNode(testNode);
                    otherDifferences.add(new NonTextDifference(xPath, controlNodeAttributes, testNodeAttributes, "Difference type: " + comparison.getType()));
                }
            }
        }
    }

    private static String extractTextValue(final Comparison.Detail detail) {
        final Object value = detail.getValue();
        return (value != null) ? value.toString() : NO_VALUE;
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

    private static Node getParentNode(final Node node) {
        return (node != null) ? node.getParentNode() : null;
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
}

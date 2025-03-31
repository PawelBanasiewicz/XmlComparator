package comparator;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.xmlunit.diff.ComparisonType.ATTR_NAME_LOOKUP;
import static org.xmlunit.diff.ComparisonType.TEXT_VALUE;

public class DifferenceFinder {
    private static final Set<String> IGNORE_NODES = Set.of("toBeIgnored", "tooIgnored");
    private static final Set<String> TIME_WITH_DIFFERENT_ENDING = Set.of("bikId=\"1234\"");

    private static final Map<String, String> ATTRIBUTE_WITH_VALUE_TO_SKIP = Map.of(
            "mapping", "not used",
            "mapping1", "skipped"
    );

    private static final Map<String, Set<String>> NODE_WITH_ATTRIBUTES_TO_SKIP = Map.of(
            "time", Set.of("description"),
            "time2", Set.of("description")
    );

    private static String pathToSkip = null;

    public static List<Difference> findDifferences(final Document controlXml, final Document testXml) {
        final Diff diff = DiffBuilder.compare(controlXml)
                .withTest(testXml)
                .ignoreWhitespace()
                .ignoreComments()
                .checkForSimilar()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                .withNodeFilter(node -> XmlUtils.isNodeInControlDocument(node, controlXml))
                .withDifferenceEvaluator(DifferenceFinder::customDifferenceEvaluator)
                .build();

        final List<Difference> differences = new ArrayList<>();
        diff.getDifferences().forEach(differences::add);

        return differences;
    }

    private static ComparisonResult customDifferenceEvaluator(final Comparison comparison, final ComparisonResult comparisonResult) {
        if (shouldSkipPath(comparison)) {
            return ComparisonResult.SIMILAR;
        }

        if (isAttributeIgnored(comparison)) {
            return ComparisonResult.SIMILAR;
        }

        if (isTimeDifferenceCase(comparison)) {
            return compareTimes(comparison);
        }

        return comparisonResult;
    }

    private static boolean shouldSkipPath(final Comparison comparison) {
        if (pathToSkip == null && hasSkippingAttribute(comparison.getTestDetails().getTarget())) {
            pathToSkip = comparison.getControlDetails().getXPath();
        }

        if (pathToSkip != null) {
            if (comparison.getTestDetails().getXPath().contains(pathToSkip) ||
                    comparison.getControlDetails().getXPath().contains(pathToSkip)) {
                return true;
            }
            pathToSkip = null;
        }

        return false;
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

            if (ATTRIBUTE_WITH_VALUE_TO_SKIP.containsKey(attributeName) &&
                    ATTRIBUTE_WITH_VALUE_TO_SKIP.get(attributeName).equals(attributeValue)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAttributeIgnored(final Comparison comparison) {
        if (comparison.getType() == ComparisonType.ATTR_VALUE || comparison.getType() == ATTR_NAME_LOOKUP) {
            final Comparison.Detail controlDetails = comparison.getControlDetails();
            final String controlParentLastNode = getLastNodeFromXPath(controlDetails.getParentXPath());
            final String controlAttributeName = getAttributeNameFromXPath(controlDetails.getXPath());

            final Comparison.Detail testDetails = comparison.getTestDetails();
            final String testParentLastNode = getLastNodeFromXPath(testDetails.getParentXPath());
            final String testAttributeName = getAttributeNameFromXPath(testDetails.getXPath());

            return isAttributeInIgnoreList(controlParentLastNode, controlAttributeName) ||
                    isAttributeInIgnoreList(testParentLastNode, testAttributeName);
        }

        return false;
    }

    private static boolean isTimeDifferenceCase(final Comparison comparison) {
        if (comparison.getType() != TEXT_VALUE) {
            return false;
        }

        final Node controlParent = comparison.getControlDetails().getTarget().getParentNode();
        final NamedNodeMap attributes = controlParent.getAttributes();

        if (attributes == null) {
            return false;
        }

        for (int i = 0; i < attributes.getLength(); i++) {
            final Node attribute = attributes.item(i);
            final String attributeString = attribute.toString();
            if (TIME_WITH_DIFFERENT_ENDING.contains(attributeString)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAttributeInIgnoreList(final String node, final String attributeName) {
        return NODE_WITH_ATTRIBUTES_TO_SKIP.containsKey(node) &&
                NODE_WITH_ATTRIBUTES_TO_SKIP.get(node).contains(attributeName);
    }

    private static ComparisonResult compareTimes(final Comparison comparison) {
        String controlValue = comparison.getControlDetails().getValue().toString();
        String testValue = comparison.getTestDetails().getValue().toString();

        final int numberOfDigitsToCheck = 6;

        if (controlValue.length() > numberOfDigitsToCheck && testValue.length() > numberOfDigitsToCheck) {
            controlValue = controlValue.substring(0, numberOfDigitsToCheck);
            testValue = testValue.substring(0, numberOfDigitsToCheck);
        }

        return controlValue.equals(testValue) ? ComparisonResult.SIMILAR : ComparisonResult.DIFFERENT;
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
}

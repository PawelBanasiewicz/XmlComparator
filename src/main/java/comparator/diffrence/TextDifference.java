package comparator.diffrence;

import java.util.Map;

public class TextDifference extends ReportDifference {

    private final String expectedValue;
    private final String actualValue;

    public TextDifference(String nodeXPath, Map<String, String> controlNodeAttributes, Map<String, String> testNodeAttributes,
                          String expectedValue, String actualValue) {
        super(nodeXPath, controlNodeAttributes, testNodeAttributes);
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    @Override
    public String toString() {
        return SEPARATOR_LINE +
                "\nAttributes: " + printAttributes() +
                "\nXPath: " + nodeXPath +
                "\nExpected: " + expectedValue +
                "\nActual: " + actualValue;
    }
}

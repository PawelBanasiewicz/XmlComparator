package comparator.diffrence;

import java.util.Map;

public class NonTextDifference extends ReportDifference {

    private final String description;

    public NonTextDifference(String nodeXPath, Map<String, String> controlNodeAttributes, Map<String, String> testNodeAttributes, String description) {
        super(nodeXPath, controlNodeAttributes, testNodeAttributes);
        this.description = description;
    }

    public String toString() {
        return SEPARATOR_LINE +
                "\nDescription: " + description +
                "\nAttributes: " + printAttributes() +
                "\nXPath: " + nodeXPath;
    }
}

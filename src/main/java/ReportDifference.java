import java.util.Map;

public abstract class ReportDifference {

    protected final String SEPARATOR_LINE = "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++";

    protected final String nodeXPath;
    protected final Map<String, String> controlNodeAttributes;
    protected final Map<String, String> testNodeAttributes;

    public ReportDifference(String nodeXPath, Map<String, String> controlNodeAttributes, Map<String, String> testNodeAttributes) {
        this.nodeXPath = nodeXPath;
        this.controlNodeAttributes = controlNodeAttributes;
        this.testNodeAttributes = testNodeAttributes;
    }

    protected String printAttributes() {
        final Map<String, String> attributesToPrint = controlNodeAttributes != null ? controlNodeAttributes : testNodeAttributes;

        if (attributesToPrint == null || attributesToPrint.isEmpty()) {
            return "[]";
        }
        return attributesToPrint.toString();
    }

    public abstract String toString();
}

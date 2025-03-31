package comparator.similarity;

import comparator.XmlUtils;
import org.w3c.dom.Document;

public class SimilarityCalculator {

    public static SimilarityResult calculate(final Document controlXmlDocument, final int textDifferencesSize) {
        final int controlXmlNodeCount = XmlUtils.countNodes(controlXmlDocument);
        final int correctNodeCount = controlXmlNodeCount - textDifferencesSize;
        final double similarity = ((double) (correctNodeCount) / controlXmlNodeCount) * 100;
        return new SimilarityResult(controlXmlNodeCount, correctNodeCount, textDifferencesSize, similarity);
    }
}

package comparator.similarity;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class SimilarityCalculator {

    private static final DocumentBuilderFactory DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance();

    public static SimilarityResult calculate(final String controlXmlContent, final int textDifferencesSize) {
        try {
            final int controlXmlNodeCount = countNodes(controlXmlContent);
            final int correctNodeCount = controlXmlNodeCount - textDifferencesSize;
            final double similarity = ((double) (correctNodeCount) / controlXmlNodeCount) * 100;
            return new SimilarityResult(controlXmlNodeCount, correctNodeCount, textDifferencesSize, similarity);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException("Something went wrong with calculating similarity");
        }
    }

    private static int countNodes(final String xml) throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilder documentBuilder = DOCUMENT_FACTORY.newDocumentBuilder();
        final Document document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));

        return countElementsRecursively(document.getDocumentElement());
    }

    private static int countElementsRecursively(final Node node) {
        int count = 1;

        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                count += countElementsRecursively(child);
            }
        }

        return count;
    }
}

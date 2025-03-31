package comparator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public enum XmlUtils {
    ;
    private static final DocumentBuilderFactory DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance();

    public static Document parseXML(final File xmlFile) throws IOException {
        try {
            final DocumentBuilder documentBuilder = DOCUMENT_FACTORY.newDocumentBuilder();
            return documentBuilder.parse(xmlFile);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Error parsing XML file", e);
        }
    }

    public static int countNodes(final Document document) {
        return countElementsRecursively(document.getDocumentElement());
    }

    private static int countElementsRecursively(Node node) {
        int count = 1;

        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                count += countElementsRecursively(child);
            }
        }
        return count;
    }
}

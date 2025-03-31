package comparator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
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

    public static boolean isNodeInControlDocument(Node node, final Document controlDocument) {
        if (node == null || controlDocument == null) {
            return false;
        }

        if (node.getNodeType() == Node.TEXT_NODE) {
            node = node.getParentNode();
        }

        final String nodeXPath = getXPath(node);

        return existsAtXPath(controlDocument, nodeXPath);
    }

    private static String getXPath(Node node) {
        final StringBuilder xpath = new StringBuilder();

        while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            final int index = getNodeIndex(node);
            xpath.insert(0, "/" + node.getNodeName() + "[" + index + "]");
            node = node.getParentNode();
        }

        return xpath.toString();
    }

    private static int getNodeIndex(final Node node) {
        final Node parent = node.getParentNode();
        if (parent == null) {
            return 1;
        }

        final NodeList siblings = parent.getChildNodes();
        int index = 0;

        for (int i = 0; i < siblings.getLength(); i++) {
            final Node sibling = siblings.item(i);
            if (sibling.getNodeType() == Node.ELEMENT_NODE && sibling.getNodeName().equals(node.getNodeName())) {
                index++;
            }
            if (sibling == node) {
                return index;
            }
        }
        return 1;
    }

    private static boolean existsAtXPath(final Document controlDocument, final String xpath) {
        try {
            final XPathExpression xPathExpression = XPathFactory.newInstance().newXPath().compile(xpath);
            return xPathExpression.evaluate(controlDocument, XPathConstants.NODE) != null;
        } catch (XPathExpressionException e) {
            return false;
        }
    }

    public static int countNodes(final Document document) {
        return countElementsRecursively(document.getDocumentElement());
    }

    private static int countElementsRecursively(final Node node) {
        if (node == null) {
            return 0;
        }

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

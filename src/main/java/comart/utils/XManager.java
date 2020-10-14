package comart.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;

import org.xml.sax.SAXException;

public class XManager {

    private static final HashMap<String,XNode> _xmls= new HashMap<>();

    /**
     * load XML and parse from file system.
     * 
     * @param resource
     *             XML resource path in <tt>CLASS_PATH</tt>
     * @return parsed XML in <tt>XNode</tt> type.
     * @throws ParserConfigurationException
     *             if a parser cannot be created which satisfies the requested
     *             configuration.
     * @throws SAXException
     *             for SAX errors.
     * @throws IOException
     *             If any IO errors occur.
     */
    public static synchronized XNode getXml(String resource)
            throws ParserConfigurationException, SAXException, IOException
    {
        XNode doc;

        /*
         * must be synchronized below codes for multi-threaded application.
         */
        synchronized (_xmls) {
            doc = (XNode) _xmls.get(resource);
            if (doc == null) {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                XHandler handler = new XHandler();
                URL furl = XManager.class.getResource(resource);
                saxParser.parse(new File(furl.getFile()), handler);
                doc = handler.getLast();
                _xmls.put(resource, doc);
            }
        }
        return doc.duplicate();
    }

    /**
     * parse XML string to <tt>XNode</tt> type.
     * 
     * @param xmlstr
     *            XML string to be parsed.
     * @return parsed XML in <tt>XNode</tt> type.
     * @throws UnsupportedEncodingException
     *             If <tt>UTF-8</tt> encoding failed.
     * @throws SAXException
     *             for SAX errors.
     * @throws IOException
     *             If any IO errors occur.
     * @throws ParserConfigurationException
     *             if a parser cannot be created which satisfies the requested
     *             configuration.
     */
    public static XNode toDocument(String xmlstr)
            throws UnsupportedEncodingException, SAXException, IOException,
            ParserConfigurationException
    {
        return toDocument(new StringInputStream(xmlstr, "UTF-8"));
    }

    /**
     * parse XML from <tt>InputStream</tt> to <tt>XNode</tt> object.
     * 
     * Note, this method does not close input stream.
     * caller must close input stream after execution of this method.
     * 
     * @param is
     *            input stream object to be parsed.
     * @return parsed XML in <tt>XNode</tt> type.
     * @throws ParserConfigurationException
     *             if a parser cannot be created which satisfies the requested
     *             configuration.
     * @throws SAXException
     *             for SAX errors.
     * @throws IOException
     *             If any IO errors occur.
     */
    public static XNode toDocument(InputStream is)
            throws ParserConfigurationException, SAXException, IOException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        XHandler handler = new XHandler();
        saxParser.parse(is, handler);
        return handler.getLast();
    }

    /**
     * generate XML string from <tt>XNode</tt> object.
     * 
     * @param doc
     *            <tt>XNode</tt> object
     * @return generated XML string.
     */
    public static String docToString(XNode doc)
    {
        StringBuilder sb = new StringBuilder();
        // sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sb.append(doc.toString());
        return sb.toString();
    }

    /**
     * set content to certain XML tag. if name consist of multiple tag sequence
     * delimited with period, this method searches recursively to child nodes
     * for each tag sequence.
     * 
     * @param node
     *            XML node
     * @param name
     *            tag sequence delimited with period.
     * @param value
     *            content value to be set.
     * @return succeed or not.
     */
    public static boolean setContent(XNode node, String name, String value)
    {
        if (node != null) {
            String names[] = name.split("\\.");
            if (names.length > 0) {
                for (String name1 : names) {
                    node = node.getFirstElem(name1);
                    if (node == null)
                        break;
                }
                if (node != null) {
                    node.setChildren(
                            node.getChildren().stream()
                                    .filter(c -> !c.isText())
                                    .collect(Collectors.toList()));
                    node.addChild(new XNode(value, 1));
                    return true;
                }
            }
        }
        return false;
    }
    
    public static String getContent(XNode node, String name)
    {
        if (node != null) {
            String names[] = name.split("\\.");
            if (names.length > 0) {
                for (String name1 : names) {
                    node = node.getFirstElem(name1);
                    if (node == null)
                        break;
                }
                if (node != null) {
                    return node.getChildren().stream()
                            .filter(c -> c.isText())
                            .map(c -> c.getName())
                            .collect(Collectors.joining());
                }
            }
        }
        return null;
    }
    
    public static XNode getNode(XNode node, String name)
    {
        if (node != null) {
            String names[] = name.split("\\.");
            if (names.length > 0) {
                for (String name1 : names) {
                    node = node.getFirstElem(name1);
                    if (node == null)
                        break;
                }
                if (node != null) {
                    return node;
                }
            }
        }
        return null;
    }
    
    public static XNode htmlToXml(String html
            ) throws IOException, ParserConfigurationException, SAXException
    {
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties props = cleaner.getProperties();

        TagNode tnode = cleaner.clean(html);
        String cleaned = new PrettyXmlSerializer(props).getAsString(tnode);

        return XManager.toDocument(cleaned);
    }
}

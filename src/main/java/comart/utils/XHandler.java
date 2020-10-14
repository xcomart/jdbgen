package comart.utils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XHandler extends DefaultHandler {
	private final Stack<XNode> _stk = new Stack<>();
	
    @Override
	public void startElement(String namespaceURI, String lName, String qName,
			Attributes attrs) throws SAXException {
		String eName = lName;
		if ("".equals(eName))
			eName = qName;
		XNode node = new XNode(eName);
		_stk.push(node);

		// if attributes exists, add attributes to node.
		if( attrs != null && attrs.getLength() > 0 ) {
			for(int i=0; i<attrs.getLength(); i++) {
				String name = attrs.getLocalName(i);
				if( "".equals(name) )
					name = attrs.getQName(i);
				String value = attrs.getValue(i);
				node.addAttribute(name, value);
			}
		}
	}

    @Override
	public void characters(char buf[], int offset, int len
			) throws SAXException {
		// append text content to current node.
		String s = new String(buf, offset, len);
		XNode cur = (XNode)_stk.top();
		if (cur != null)
			cur.addChild(new XNode(s, 1));
	}

    @Override
	public void endElement(String namespaceURI, String sName, String qName)
			throws SAXException {
		XNode child = (XNode)_stk.pop();
		XNode parent = (XNode)_stk.top();
		if( parent != null ) {
			parent.addChild(child);
		} else {
			_stk.push(child);
		}
	}
	
	public XNode getLast()
	{
		return (XNode)_stk.top();
	}
}

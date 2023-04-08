/*
 * MIT License
 * 
 * Copyright (c) 2020 Dennis Soungjin Park
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package comart.utils;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XHandler extends DefaultHandler {
    private final Stack<XNode> stack = new Stack<>();
    
    @Override
    public void startElement(String namespaceURI, String lName, String qName,
            Attributes attrs) throws SAXException {
        String eName = lName;
        if (StringUtils.isBlank(eName))
            eName = qName;
        XNode node = new XNode(eName);
        stack.push(node);

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
        XNode cur = (XNode)stack.top();
        if (cur != null)
            cur.addChild(new XNode(s, 1));
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName)
            throws SAXException {
        XNode child = (XNode)stack.pop();
        XNode parent = (XNode)stack.top();
        if( parent != null ) {
            parent.addChild(child);
        } else {
            stack.push(child);
        }
    }
    
    public XNode getLast()
    {
        return (XNode)stack.top();
    }
}

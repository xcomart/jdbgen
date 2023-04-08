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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang3.ObjectUtils;

public class XNode {
    private int nodeType = 0;
    //private StringBuilder _content = new StringBuilder();
    private String name;
    private List<XNode> children = null;
    private HashMap<String,String> attributes = null;
    
    public XNode(String name)
    {
        this.name = name;
    }
    
    public XNode(String content, int type)
    {
        this.name = content;
        this.nodeType = type;
    }
    
    public synchronized void addChild(XNode child)
    {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }
    
    public synchronized void addAttribute(String key, String value)
    {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key.toUpperCase(), value);
    }
    
    public String getAttribute(String attrname)
    {
        if (attributes != null)
            return (String)attributes.get(attrname.toUpperCase());
        else
            return null;
    }
    
    public HashMap<String,String> getAttributes()
    {
        return attributes;
    }
    
    public List<XNode> getChildren()
    {
        return children;
    }
    
    public void setChildren(List<XNode> children) {
        this.children = children;
    }
    
    @Override
    public String toString()
    {
        if (nodeType == 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("<").append(name);
            if (attributes != null) {
                attributes.forEach((k, v) -> {
                    sb.append(" ").append(k);
                    sb.append("=\"").append(v);
                    sb.append("\"");
                });
            }
            if (ObjectUtils.isEmpty(children)) {
                sb.append(" />");
            } else {
                sb.append(">");
                children.forEach(c -> sb.append(c.toString()));
                sb.append("</").append(name).append(">");
            }
            return sb.toString();
        } else {
            return name;
        }
    }
    
    /**
     * duplicate this object recursively. returned object completely new object.
     * so change returned object will not influence original object.
     * 
     * @return duplicated new object.
     */
    public XNode duplicate()
    {
        if (nodeType == 0) {
            XNode res = new XNode(name);
            if (ObjectUtils.isNotEmpty(children))
                children.forEach(c -> res.addChild(c.duplicate()));

            if (attributes != null)
                attributes.forEach((k, v) -> res.addAttribute(k, v));
            return res;
        } else {
            return new XNode(name, nodeType);
        }
    }

    private void getElementByName(String name, ArrayList<XNode> result)
    {
        if (nodeType == 0) {
            if (name.equalsIgnoreCase(this.name))
                result.add(this);
            
            if (ObjectUtils.isNotEmpty(children))
                children.forEach(c -> c.getElementByName(name, result));
        }
    }

    /**
     * find all elements which tag name is <code>name</code>. depth first search
     * method is used in search. if no element found, result is empty
     * <code>ArrayList</code> object.
     * 
     * @param name
     *            tag name to be searched
     * @return array of <code>XNode</code> which tag name is <code>name</code>
     */
    public ArrayList<XNode> getElementByName(String name)
    {
        ArrayList<XNode> result = new ArrayList<>();
        getElementByName(name, result);
        return result;
    }

    /**
     * get first matched element in depth first search. if no element found,
     * result is null.
     * 
     * @param name
     *            tag name to be searched.
     * @return first matched element.
     */
    public XNode getFirstElem(String name)
    {
        if (nodeType == 0) {
            if (name.equalsIgnoreCase(this.name)) {
                return this;
            }
            if (ObjectUtils.isNotEmpty(children)) {
                for (int i = 0; i < children.size(); i++) {
                    XNode node = children.get(i).getFirstElem(name);
                    if (node != null)
                        return node;
                }
            }
        }
        return null;
    }
    
    public XNode getElementById(String id)
    {
        return getFirstElementAttrMatch("id", id);
    }
    
    public XNode getFirstElementAttrMatch(String key, String value)
    {
        if (nodeType == 0) {
            String thisval = getAttribute(key);
            if (value.equalsIgnoreCase(thisval))
                return this;
            if (ObjectUtils.isNotEmpty(children)) {
                for (int i = 0; i < children.size(); i++) {
                    XNode child = (XNode) children.get(i);
                    XNode node = child.getFirstElementAttrMatch(key, value);
                    if (node != null)
                        return node;
                }
            }
        }
        return null;
    }
    
    public synchronized XNode replaceNode(XNode find, XNode rep)
    {
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                XNode child = (XNode) children.get(i);
                XNode node;
                if (child == find) {
                    node = child;
                    children.remove(child);
                    children.add(i, rep);
                } else {
                    node = child.replaceNode(find, rep);
                }
                if (node != null)
                    return node;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isText() {
        return this.nodeType != 0;
    }
    
    public boolean traverse(Function<XNode, Boolean> walker) {
        if (walker.apply(this)) {
            if (children != null)
                if (!children.stream().noneMatch(c -> (!c.traverse(walker))))
                    return false;
            return true;
        } else {
            return false;
        }
    }
}

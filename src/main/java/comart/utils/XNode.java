package comart.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;

public class XNode {
    private int nodeType = 0;
    //private StringBuilder _content = new StringBuilder();
    private String _name;
    private List<XNode> _children = null;
    private HashMap<String,String> _attributes = null;
    
    public XNode(String name)
    {
        this._name = name;
    }
    
    public XNode(String content, int type)
    {
        this._name = content;
        this.nodeType = type;
    }
    
    public synchronized void addChild(XNode child)
    {
        if (_children == null) {
            _children = new ArrayList<>();
        }
        _children.add(child);
    }
    
    public synchronized void addAttribute(String key, String value)
    {
        if (_attributes == null) {
            _attributes = new HashMap<>();
        }
        _attributes.put(key.toUpperCase(), value);
    }
    
    public String getAttribute(String attrname)
    {
        if (_attributes != null)
            return (String)_attributes.get(attrname.toUpperCase());
        else
            return null;
    }
    
    public HashMap<String,String> getAttributes()
    {
        return _attributes;
    }
    
    public List<XNode> getChildren()
    {
        return _children;
    }
    
    public void setChildren(List<XNode> children) {
        this._children = children;
    }
    
    @Override
    public String toString()
    {
        if (nodeType == 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("<").append(_name);
            if (_attributes != null) {
                _attributes.forEach((k, v) -> {
                    sb.append(" ").append(k);
                    sb.append("=\"").append(v);
                    sb.append("\"");
                });
            }
            if (ObjectUtils.isEmpty(_children)) {
                sb.append(" />");
            } else {
                sb.append(">");
                _children.forEach(c -> sb.append(c.toString()));
                sb.append("</").append(_name).append(">");
            }
            return sb.toString();
        } else {
            return _name;
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
            XNode res = new XNode(_name);
            if (ObjectUtils.isNotEmpty(_children))
                _children.forEach(c -> res.addChild(c.duplicate()));

            if (_attributes != null)
                _attributes.forEach((k, v) -> res.addAttribute(k, v));
            return res;
        } else {
            return new XNode(_name, nodeType);
        }
    }

    private void getElementByName(String name, ArrayList<XNode> result)
    {
        if (nodeType == 0) {
            if (name.equalsIgnoreCase(this._name))
                result.add(this);
            
            if (ObjectUtils.isNotEmpty(_children))
                _children.forEach(c -> c.getElementByName(name, result));
        }
    }

    /**
     * find all elements which tag name is <tt>name</tt>. depth first search
     * method is used in search. if no element found, result is empty
     * <tt>ArrayList</tt> object.
     * 
     * @param name
     *            tag name to be searched
     * @return array of <tt>XNode</tt> which tag name is <tt>name</tt>
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
            if (name.equalsIgnoreCase(this._name)) {
                return this;
            }
            if (ObjectUtils.isNotEmpty(_children)) {
                for (int i = 0; i < _children.size(); i++) {
                    XNode node = _children.get(i).getFirstElem(name);
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
            if (ObjectUtils.isNotEmpty(_children)) {
                for (int i = 0; i < _children.size(); i++) {
                    XNode child = (XNode) _children.get(i);
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
        if (_children != null) {
            for (int i = 0; i < _children.size(); i++) {
                XNode child = (XNode) _children.get(i);
                XNode node = null;
                if (child == find) {
                    node = child;
                    _children.remove(child);
                    _children.add(i, rep);
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
        return _name;
    }

    public void setName(String name) {
        this._name = name;
    }
    
    public boolean isText() {
        return this.nodeType != 0;
    }
}

package comart.utils;

import java.io.Serializable;
import java.util.ArrayList;

public class Stack<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final ArrayList<T> _arr = new ArrayList<>();

    public synchronized void push(T o)
    {
        _arr.add(o);
    }
    
    public synchronized T pop()
    {
        if (_arr.size() > 0)
            return _arr.remove(_arr.size()-1);
        else
            return null;
    }
    
    public synchronized T top()
    {
        if (_arr.size() > 0)
            return _arr.get(_arr.size()-1);
        else
            return null;
    }
    
    public synchronized int size()
    {
        return _arr.size();
    }
}

class xIterator implements java.util.Iterator {
    
    public boolean hasNext() { return false; }
    
    public java.lang.Object next() throws java.util.NoSuchElementException {
        return null;
    }
    
    public void remove() throws java.lang.UnsupportedOperationException,
        java.lang.IllegalStateException {
        throw new java.lang.UnsupportedOperationException();
    }
    
    public xIterator() { super(); }
}

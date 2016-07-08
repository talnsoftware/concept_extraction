package eu.multisensor.utils;

class NIFToken {

    public String text;
    public int end;
    public int start;
    
    @Override
    public String toString() {
        return "(" + start + "," + end + ")=" + text;
    }
}
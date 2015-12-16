package com.getui.logful.server.entity;

public class LayoutItem {

    public static final int TYPE_NUMBER = 1;

    public static final int TYPE_STRING = 2;

    private String abbreviation;

    private String fullName;

    private int type;

    public static LayoutItem create(String abbr, String full, int type) {
        LayoutItem item = new LayoutItem();
        item.setAbbreviation(abbr);
        item.setFullName(full);
        item.setType(type);
        return item;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

}

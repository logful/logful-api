package com.getui.logful.server.entity;

import com.getui.logful.server.Constants;

import java.util.concurrent.ConcurrentHashMap;

public class Layout {

    private ConcurrentHashMap<String, LayoutItem> itemMap = new ConcurrentHashMap<>();
    private static final String TYPE_NUMBER_SIGN = "%n";
    private static final String TYPE_STRING_SIGN = "%s";

    public Layout(String template) {
        String[] elements = template.split("\\|");
        for (String element : elements) {
            String[] attributes = element.split(Constants.DEFAULT_ATTRIBUTE_SEPARATOR);
            if (attributes.length == 3) {
                String abbreviation = attributes[0];
                String fullName = attributes[1];
                int type = getType(attributes[2]);
                LayoutItem layoutItem = LayoutItem.create(abbreviation, fullName, type);
                itemMap.put(abbreviation, layoutItem);
            } else {
                throw new IllegalArgumentException("Layout template format is not correct");
            }
        }
    }

    public LayoutItem getItem(String abbr) {
        return itemMap.get(abbr);
    }

    private int getType(String typeString) {
        if (typeString.equalsIgnoreCase(TYPE_STRING_SIGN)) {
            return LayoutItem.TYPE_STRING;
        } else if (typeString.equalsIgnoreCase(TYPE_NUMBER_SIGN)) {
            return LayoutItem.TYPE_NUMBER;
        } else {
            throw new IllegalArgumentException("Layout template element type set error");
        }
    }

}

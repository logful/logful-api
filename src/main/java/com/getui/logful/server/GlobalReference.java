package com.getui.logful.server;

import com.getui.logful.server.entity.Layout;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class GlobalReference {

    private static final int MAX_CAPACITY = 1000;
    private HashMap<String, Layout> layoutMap = new HashMap<>();

    private static class ClassHolder {
        static GlobalReference instance = new GlobalReference();
    }

    public static GlobalReference reference() {
        return ClassHolder.instance;
    }

    public static Layout getLayout(String template) {
        if (StringUtils.isEmpty(template)) {
            return null;
        }
        GlobalReference reference = reference();
        Layout layout = reference.layoutMap.get(template);
        if (layout == null) {
            layout = new Layout(template);
            int size = reference.layoutMap.size();
            if (size >= MAX_CAPACITY) {
                int itemToRemove = size - MAX_CAPACITY;
                for (int i = 0; i < itemToRemove; i++) {
                    Map.Entry<String, Layout> entry = reference.layoutMap.entrySet().iterator().next();
                    reference.layoutMap.remove(entry.getKey());
                }
            }
            reference.layoutMap.put(template, layout);
        }
        return layout;
    }

}

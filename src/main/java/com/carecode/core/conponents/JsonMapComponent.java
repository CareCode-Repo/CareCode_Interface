package com.carecode.core.conponents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonMapComponent extends HashMap<String, Object> {

    public JsonMapComponent() {
        super();
    }

    public List<Map<String, Object>> getArrayListData(String key) {
        if (key != null && containsKey(key))
            return (List<Map<String, Object>>) get(key);
        return null;
    }

    public Map<String, Object> getHashMapData(String key) {
        if(key != null && containsKey(key))
            return (Map<String, Object>) get(key);
        return null;
    }

    public String getStringData(String key) {
        if (key != null && containsKey(key))
            return (String) get(key);
        return null;
    }
}

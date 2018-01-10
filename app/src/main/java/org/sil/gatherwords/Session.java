package org.sil.gatherwords;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by andrewthomas on 1/5/18.
 */

public class Session {
    Map<String, String> metaData;

    Session() {
        metaData = new HashMap<>();
    }

    public void setData(String key, String value) {
        metaData.put(key, value);
    }

    public String getData(String key) {
        return metaData.get(key);
    }
}

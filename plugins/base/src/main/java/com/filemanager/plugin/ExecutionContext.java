package com.filemanager.plugin;

import java.util.Map;

public class ExecutionContext {
    private final Map<String, Object> attributes;

    public ExecutionContext() {
        this.attributes = new java.util.HashMap<>();
    }

    public ExecutionContext(Map<String, Object> attributes) {
        this.attributes = new java.util.HashMap<>(attributes);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public boolean containsAttribute(String key) {
        return attributes.containsKey(key);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public Map<String, Object> getAttributes() {
        return new java.util.HashMap<>(attributes);
    }
}

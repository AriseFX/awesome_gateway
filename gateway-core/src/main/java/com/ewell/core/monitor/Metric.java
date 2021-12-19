package com.ewell.core.monitor;

import java.util.HashMap;
import java.util.Map;

public class Metric<T extends Number> {
    private MetricType type;
    private String help;
    private String name;
    private Map<String, String> tags = new HashMap<>();
    private T value;


    public Metric(MetricType type, String help, String name, T value) {
        this.type = type;
        this.help = help;
        this.name = name;
        this.value = value;
    }

    public Metric<T> tag(String key, String value) {
        tags.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return "# HELP " + name + " " + help + "\n" +
                "# TYPE " + name + " " + type + "\n" +
                nameTags() + " " + getValue() + "\n";
    }

    private String getValue() {
        if (value == null) {
            return "0";
        } else {
            return String.valueOf(value);
        }
    }

    private String nameTags() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("{");
        for (Map.Entry<String, String> tagvalue : tags.entrySet()) {
            sb.append(tagvalue.getKey());
            sb.append("=\"");
            sb.append(tagvalue.getValue());
            sb.append("\",");
        }
        sb.append("}");
        return sb.toString();
    }
}

enum MetricType {
    gauge, counter
}
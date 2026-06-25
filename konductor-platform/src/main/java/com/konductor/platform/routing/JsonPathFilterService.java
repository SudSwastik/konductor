package com.konductor.platform.routing;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.DocumentContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JsonPathFilterService {

    public Map<String, Object> filter(Map<String, Object> payload, List<String> fieldPaths) {
        if (fieldPaths == null || fieldPaths.isEmpty()) {
            return payload;
        }

        DocumentContext ctx = JsonPath.parse(payload);
        Map<String, Object> result = new LinkedHashMap<>();

        for (String path : fieldPaths) {
            try {
                Object value = ctx.read(path);
                result.put(stripPrefix(path), value);
            } catch (PathNotFoundException ignored) {
                // path absent in this payload — skip silently
            }
        }

        return result;
    }

    private String stripPrefix(String path) {
        return path.startsWith("$.") ? path.substring(2) : path;
    }
}

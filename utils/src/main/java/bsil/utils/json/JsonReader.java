package bsil.utils.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * a facade made to simplify access to JsonDatas
 */
public class JsonReader {

    // because of logback bug...
    private static final transient String LINE_SEPARATOR_SYSPROP = "line.separator";
    public static final String UNDEFINED = "UNDEFINED";
    private final String json;

    // for reading purpose
    private transient DocumentContext jsonPathContext;

    public JsonReader(final String json){
        this.json=json;
    }

    public Optional<String> jsonStringValue(final String path) {
        enforceLineSeparator();
        return Optional.ofNullable(jsonPathContext().read(path))
                       .map(Object::toString)
                       .filter(JsonReader::isNotNullValue)
                       .map(JsonReader::dismissStringMarks);
    }

    private static boolean isNotNullValue(final String value) {
        return !"null".equals(value);
    }

    private static String dismissStringMarks(final String value) {
        return nonNull(value) && (value.startsWith("\"") && value.endsWith("\""))
                   ? value.substring(1, value.length() - 1)
                   : value;
    }

    public List<String> jsonStringValues(final String path) {
        enforceLineSeparator();
        return jsonPathContext().read(path, new ListTypeRef())
                                .stream()
                                .map(JsonReader::dismissStringMarks)
                                .collect(Collectors.toList());
    }

    public Map<String, String> jsonStringsMap(final String path) {
        enforceLineSeparator();
        final Map<String, String> map= new HashMap<>();
        jsonPathContext().read(path, new MapTypeRef()).entrySet()
                         .stream()
                         .map(entry->new AbstractMap.SimpleEntry<>(entry.getKey(), asStringOrNull(entry.getValue())))
                         .map(this::cleanedStringsMapEntry)
                         .forEach(entry->map.put(entry.getKey(),entry.getValue()));
        return map;
    }

    private String asStringOrNull(final JsonElement elt) {
        return elt.isJsonNull() ? null : elt.toString();
    }

    private Map.Entry<String,String> cleanedStringsMapEntry(final Map.Entry<String, String> entry) {
        return new AbstractMap.SimpleEntry<>(dismissStringMarks(entry.getKey()),
                                             dismissStringMarks(entry.getValue()));
    }

    // logback core bug workaround with line separator...
    private static void enforceLineSeparator() {
        if (isNull(System.getProperty(LINE_SEPARATOR_SYSPROP))) {
            System.setProperty(LINE_SEPARATOR_SYSPROP, System.lineSeparator());
        }
    }

    private DocumentContext jsonPathContext() {
        if (isNull(this.jsonPathContext)) this.jsonPathContext = JsonPath.parse(this.json, config());
        return this.jsonPathContext;
    }

    private static Configuration config() {
        final Gson gson = CustomGsonBuilder.create();
        final JsonProvider jsonProvider = new GsonJsonProvider(gson);
        final MappingProvider mappingProvider = new GsonMappingProvider(gson);
        return Configuration.builder()
                            .jsonProvider(jsonProvider)
                            .mappingProvider(mappingProvider)
                            .build();
    }

    private static class ListTypeRef extends TypeRef<List<String>> {
    }

    private static class MapTypeRef extends TypeRef<Map<String, JsonElement>> {
    }

    public List<String> jsonList(final String path) {
        return this.jsonStringValues(path);
    }

    public String jsonOrNull(final String path) {
        return this.jsonStringValue(path).orElse(null);
    }

    public String jsonOrUndefined(final String path) {
        return this.jsonStringValue(path).orElse(UNDEFINED);
    }

}

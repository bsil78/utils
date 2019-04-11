package bsil78.utils;


import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class JsonResponse implements Serializable {

    private static final long serialVersionUID = 2019032922; /* version from date and hour */

    // because of logback bug...
    private static transient final String LINE_SEPARATOR_SYSPROP = "line.separator";

    // optimization : init. once at load
    // for writing json purpose only
    private static transient final JsonParser jsonMapper = new JsonParser();
    private static transient final GsonBuilder gsonBuilder;

    static {
        forceJavaVersionProperty();
        gsonBuilder=new GsonBuilder().disableInnerClassSerialization()
                         .excludeFieldsWithModifiers(Modifier.STATIC,
                                                     Modifier.TRANSIENT,
                                                     Modifier.VOLATILE);


    }

    // ensure setup for a class is made once only
    private static final transient Collection<String> knownConverters = new HashSet<>();

    // transfered datas when uses as DTO have to be ciphered
    private byte[] cipheredContent;
    // for helping reading errors as Error object
    private boolean isError;
    // for reading purpose
    private transient DocumentContext jsonPathContext;

    // for serialization purpose only
    private JsonResponse(){}

    // for static factory calls only
    private JsonResponse(final Object content) {
        final String jsonContent = jsonOf(content);
        final String paddedJsonContent = padIfNedded(jsonContent);
        this.cipheredContent = Ciphering.cipher(paddedJsonContent);
        this.isError = content instanceof Error;
    }


    private static String jsonOf(final Object toJsonify) {
        return Optional.ofNullable(toJsonify)
                       .map(value -> {
                           forceJavaVersionProperty();
                           return gsonBuilder.create().toJson(toJsonify);
                       })
                       .orElse("");
    }

    // for ciphering purpose only
    private String padIfNedded(final String jsonContent) {
        return jsonContent+StringUtils.repeat(" ",Ciphering.neededPaddingFor(jsonContent));
    }

    // missing property makes gson to crash, this enforce it
    private static void forceJavaVersionProperty() {
        final String javaVersion = "java.version";
        if (System.getProperty(javaVersion) == null) System.setProperty(javaVersion, "11");
    }

    /**
     * if any Classe need specific Json serialization, declare it here
     * once declared through this, it cannot be removed or overidden for any instance of JsonResponse created after
     *
     * @param converter function that takes an instance and gives a json String of it
     * @param type Class to jsonify
     */
    public static <T> void setupJsonConverterForClass(final Function<T,String> converter, final Class<T> type){
        if(!knownConverters.contains(type.getCanonicalName())) {
            gsonBuilder.registerTypeHierarchyAdapter(type, new MyTypeAdapter<>(converter));
            knownConverters.add(type.getCanonicalName());
        }
    }

    /**
     * factory method, not to use for Throwable (like Exception...)
     * @param content object to convert to json
     */
    public static JsonResponse ofObject(final Object content) {
        if(content instanceof Throwable){
            throw new IllegalArgumentException("Bad usage. Must use JsonResponse.ofThrowable(exception) instead.");
        }
        return new JsonResponse(content);
    }

    public static JsonResponse ofThrowable(final Throwable exception) {
        return new JsonResponse(Error.ofThrowable(exception));
    }

    /**
     * helper factory to produce Error as JsonResponse directly
     * @param error subject of error (like message in exceptions)
     * @param details context of error (like stackstrace in exceptions)
     */
    public static JsonResponse ofError(final String error, final String details) {
        return new JsonResponse(new Error(error,details));
    }

    /**
     * @return json content as a string
     */
    public String jsonContent() {
        return Ciphering.uncipher(requireNonNull(cipheredContent));
    }

    @Override
    public String toString(){
       return jsonContent();
    }

    /**
     * @return an error if it is an error, or empty
     */
    public Optional<Error> error() {
        return isError ? Optional.of(errorFromJsonObject(jsonObjectFromJson())) : Optional.empty();
    }

    private JsonObject jsonObjectFromJson() {
        return jsonMapper.parse(jsonContent()).getAsJsonObject();
    }

    private Error errorFromJsonObject(final JsonObject json) {
        return new Error(json.get("error").getAsString(), json.get("details").getAsString());
    }

    public Optional<String> jsonStringValue(final String path) {
        enforceLineSeparator();
        return Optional.ofNullable(jsonPathContext().read(path)).map(Object::toString);
    }

    // logback core bug workaround with line separator...
    private static void enforceLineSeparator() {
        if (Objects.isNull(System.getProperty(LINE_SEPARATOR_SYSPROP))) {
            System.setProperty(LINE_SEPARATOR_SYSPROP, System.lineSeparator());
        }
    }

    private DocumentContext jsonPathContext() {
        if(jsonPathContext==null) jsonPathContext=JsonPath.parse(jsonContent());
        return jsonPathContext;
    }


    /**
     * Standardized error object to simplify error management in json format
     */
    public static final class Error {

        private final String error;
        private final String details;

        public Error(final String message, final String details){
            this.error= requireNonNull(message);
            this.details = StringUtils.defaultIfBlank(details,"");
        }

        public static Error ofThrowable(final Throwable exception){
            return new Error(exception.getMessage(), ExceptionUtils.getStackTrace(exception));
        }

        public String message() {
            return error;
        }

        public String details() {
            return details;
        }
    }

    /**
     * helper for declaring gson type adapter
     * @param <T> type to convert to json
     */
    private static final class MyTypeAdapter<T> extends TypeAdapter<T> {
        private final Function<T, String> converter;

        private MyTypeAdapter(final Function<T, String> converter) {
            this.converter = converter;
        }

        @Override
        public void write(final JsonWriter out, final T value)
        throws IOException {
            out.value(converter.apply(value));
        }

        @Override
        public T read(final JsonReader in) {
            throw new UnsupportedOperationException("One must not read json with this type adapter");
        }
    }
}

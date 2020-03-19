package bsil.utils.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;

/**
 * Let configure a custom gson with NULL FRIENDLY, NO STATIC, NO TRANSIENT, NO VOLATILE fields
 */
public enum CustomGsonBuilder {;

    private static final transient GsonBuilder gsonBuilder;

    // ensure setup for a class is made once only
    private static final transient Collection<String> knownConverters = new HashSet<>();

    static {
        gsonBuilder = new GsonBuilder()
                          .enableComplexMapKeySerialization()
                          .serializeNulls()
                          .excludeFieldsWithModifiers(Modifier.STATIC,
                                                      Modifier.TRANSIENT,
                                                      Modifier.VOLATILE);


    }


    public static Gson create() {
        return gsonBuilder.create();
    }

    public static GsonBuilder instance() {
        return gsonBuilder;
    }

    /**
     * if any Classe need specific Json serialization, declare it here
     * once declared through this, it cannot be removed or overidden for any instance of JsonResponse created after
     * @param converter function that takes an instance and gives a json String of it
     * @param type      Class to jsonify
     */
    public static <T> void setupJsonConverterForClass(final Function<T, String> converter, final Class<T> type) {
        if (!knownConverters.contains(type.getCanonicalName())) {
            instance().registerTypeHierarchyAdapter(type, new MyTypeAdapter<>(converter));
            knownConverters.add(type.getCanonicalName());
        }
    }


    /**
     * helper for declaring gson type adapter
     * @param <T> type to convert to json
     */
    private static final class MyTypeAdapter<T> extends TypeAdapter<T> {
        private final Function<? super T, String> converter;

        private MyTypeAdapter(final Function<? super T, String> converter) {
            this.converter = converter;
        }

        @Override
        public void write(final JsonWriter out, final T value)
        throws IOException {
            out.value(this.converter.apply(value));
        }

        @Override
        public T read(final JsonReader in) {
            throw new UnsupportedOperationException("One must not read json with this type adapter");
        }
    }

}

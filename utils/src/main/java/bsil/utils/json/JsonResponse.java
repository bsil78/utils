package bsil.utils.json;


import bsil.utils.ciphering.Cipher;
import bsil.utils.ciphering.CipheringConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Synchronized;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.text.MessageFormat.format;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public final class JsonResponse implements Serializable {

    private static final long serialVersionUID = 2019032922; /* version from date and hour */


    // optimization : init. once at load
    // for writing json purpose only
    private static final Pattern BAD_JSON = Pattern.compile(".*(?<badField>\"[^\"]+\":(|\\{})),.*");


    // transfered datas when uses as DTO have to be ciphered
    private final byte[] cipheredContent;
    // for helping reading errors as Error object
    private final boolean isError;

    //synchro nized
    private static Cipher ciphering;

    @Synchronized
    public static void setupCiphering(final CipheringConfig cipheringConfig) {
        ciphering = cipheringConfig.cipheringTransformation().isPaddedAlgorithm()
                        ? Cipher.createPaddedCiphering(cipheringConfig, JsonResponse::generatePadding)
                        : Cipher.createCiphering(cipheringConfig);
    }

    // for static factory calls only
    private JsonResponse(final @NotNull Object content) {
        final Object notNullContent = ofNullable(content).orElseThrow(() -> new CannotConvertToJson(null, "null"));
        final String jsonContent = jsonOf(notNullContent);
        if ("null".equals(jsonContent)) {
            throw new CannotConvertToJson(content, "null");
        }
        this.cipheredContent = cipher(jsonContent);
        this.isError = content instanceof Error;
    }

    @Synchronized
    private byte[] cipher(final String jsonContent) {
        return ofNullable(ciphering)
                   .map(currentCiphering -> currentCiphering.cipher(jsonContent))
                   .orElseThrow(CipheringWasNotSetUp::new);
    }


    private static String jsonOf(final Object toJsonify) {
        return ofNullable(toJsonify).flatMap(JsonResponse::tryConvertToJson)
                                    .map(checkBadJson(toJsonify))
                                    .orElse("");
    }


    private static Optional<String> tryConvertToJson(final Object toJsonify) {
        final String json = CustomGsonBuilder.create().toJson(requireNonNull(toJsonify));
        return Optional.of(json);
    }

    private static Function<String, String> checkBadJson(final Object toJsonify) {
        return json -> Optional.of(json)
                               .filter(value -> !BAD_JSON.matcher(requireNonNull(value)).matches())
                               .orElseThrow(() -> new CannotConvertToJson(toJsonify, json));
    }

    // for ciphering purpose only
    private static String generatePadding(final Integer len) {
        return StringUtils.repeat(" ", len);
    }


    /**
     * factory method, not to use for Throwable (like Exception...)
     * @param content object to convert to json
     */
    public static JsonResponse ofObject(final Object content) {
        if (content instanceof Throwable) {
            throw new IllegalArgumentException("Bad usage. Must use JsonResponse.ofThrowable(exception) instead.");
        }
        return new JsonResponse(content);
    }

    public static JsonResponse ofThrowable(final Throwable exception) {
        return new JsonResponse(Error.ofThrowable(exception));
    }

    /**
     * helper factory to produce Error as JsonResponse directly
     * @param error   subject of error (like message in exceptions)
     * @param details context of error (like stackstrace in exceptions)
     */
    public static JsonResponse ofError(final String error, final String details) {
        return new JsonResponse(new Error(error, details));
    }

    /**
     * @return json content as a string
     */
    public String jsonContent() {
        return uncipher(requireNonNull(this.cipheredContent));
    }

    @Synchronized
    private String uncipher(final byte[] cipheredContent) {
        return ofNullable(ciphering).map(currentCiphering -> currentCiphering.uncipher(cipheredContent, String::trim))
                                    .orElseThrow(CipheringWasNotSetUp::new);
    }

    @Override
    public String toString() {
        return jsonContent();
    }

    /**
     * @return an error if it is an error, or empty
     */
    public Optional<Error> error() {
        return this.isError ? Optional.of(errorFromJsonObject(jsonObjectFromJson())) : Optional.empty();
    }

    private JsonObject jsonObjectFromJson() {
        return JsonParser.parseString(jsonContent()).getAsJsonObject();
    }

    private Error errorFromJsonObject(final JsonObject json) {
        return new Error(json.get("error").getAsString(), json.get("details").getAsString());
    }

    private void readObject(final ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        throw new NotSerializableException(this.getClass().getCanonicalName());
    }

    private void writeObject(final ObjectOutputStream out)
    throws IOException {
        throw new NotSerializableException(this.getClass().getCanonicalName());
    }


    /**
     * Standardized error object to simplify error management in json format
     */
    public static final class Error {

        private final String message;
        private final String details;

        private Error(final String message, final String details) {
            this.message = requireNonNull(message);
            this.details = StringUtils.defaultIfBlank(details, "");
        }

        public static Error ofThrowable(final Throwable exception) {
            return new Error(exception.getMessage(), ExceptionUtils.getStackTrace(exception));
        }

        public String message() {
            return this.message;
        }

        public String details() {
            return this.details;
        }
    }


    public static class CannotConvertToJson extends RuntimeException {
        public CannotConvertToJson(final Object toJsonify, final String json) {
            super(tryFindRootCause(toJsonify, json)
                      .orElseGet(() -> "Cannot convert the given object to json : " + toJsonify.getClass()
                                                                                               .getCanonicalName()));
        }

        private static Optional<String> tryFindRootCause(final Object toJsonify, @NotNull final String json) {
            requireNonNull(json);
            if (isNull(toJsonify)) {
                return Optional.of("Cannot serialize null");
            }
            final Class<?> aClass = toJsonify.getClass();
            return Stream.of(anonymousInstanceCheck(aClass),
                             scopeLimitedObjectCheck(aClass),
                             unkownError(aClass, json))
                         .filter(RootCauseChecker::checkRootCause)
                         .map(RootCauseChecker::toMessageOfRootCause)
                         .filter(value -> !value.isEmpty())
                         .findFirst();
        }

        private static RootCauseChecker unkownError(final Class<?> aClass, final String json) {
            final Matcher matcher = BAD_JSON.matcher(json);
            final Optional<String> badField = matcher.matches()
                                                  ? ofNullable(matcher.group("badField"))
                                                  : Optional.empty();
            return RootCauseChecker.of(badField::isPresent,
                                       () -> format("Cannot convert field {0} to json for {1}",
                                                    badField.orElseGet(() -> "[UNKNOWN of " + json + "]"),
                                                    aClass.getName()));
        }

        private static RootCauseChecker scopeLimitedObjectCheck(final Class<?> aClass) {
            return RootCauseChecker.of(() -> isNull(aClass.getCanonicalName()),
                                       () -> format("Cannot convert scope limited object {0} to json",
                                                    aClass.getName()));
        }

        private static RootCauseChecker anonymousInstanceCheck(final Class<?> aClass) {
            final String simpleName = aClass.getSimpleName();
            return RootCauseChecker.of(simpleName::isEmpty,
                                       () -> format("Cannot convert {0} anonymous class instance to json",
                                                    aClass.getName()));
        }

        private static final class RootCauseChecker {
            private final BooleanSupplier checker;
            private final Supplier<String> messageSupplier;

            private RootCauseChecker(final BooleanSupplier checker, final Supplier<String> messageSupplier) {
                this.messageSupplier = requireNonNull(messageSupplier);
                this.checker = checker;
            }

            public static RootCauseChecker of(final BooleanSupplier checker, final Supplier<String> messageSupplier) {
                return new RootCauseChecker(checker, messageSupplier);
            }

            public String toMessageOfRootCause() {
                return this.messageSupplier.get();
            }

            private boolean checkRootCause() {
                return this.checker.getAsBoolean();
            }

        }
    }

    public byte[] serialize()
    throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (final ObjectOutput oos = new ObjectOutputStream(outputStream)) {
            oos.writeObject(this);
            return outputStream.toByteArray();
        }
    }

    public static JsonResponse deserialize(final byte[] transferedDatas)
    throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(transferedDatas);
        try (final ObjectInput ois = new ObjectInputStream(inputStream)) {
            return (JsonResponse) ois.readObject();
        }
    }

    private static class CipheringWasNotSetUp extends RuntimeException {
    }
}

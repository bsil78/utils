package bsil.utils;

import java.util.HashMap;
import java.util.Map;

import static java.lang.System.getSecurityManager;
import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

@SuppressWarnings("AccessOfSystemProperties")
public enum SystemPropertiesHelper {
    ;

    private static final Map<String, String> properties = new HashMap<>();

    public static synchronized String getProperty(final String property) {
        requireNonNull(property);
        return
            ofNullable(getSecurityManager()).map(
                sm -> {
                    try {
                        sm.checkPropertiesAccess();
                        return System.getProperty(property);
                    }
                    catch (final SecurityException ex) {
                        return null;
                    }
                }
            ).orElseGet(() ->
                            ofNullable(properties.get(property))
                                .orElseGet(
                                    () -> ofNullable(getenv(envVarNameFromPropertyName(property)))
                                              .orElseGet(()->getenv(envVarNameFromPropertyName(property).toUpperCase()))
                                )
            );
    }

    private static String envVarNameFromPropertyName(final String property) {
        return property.replace('.', '_');
    }

    public static synchronized void setProperty(final String property, final String value) {
        requireNonNull(property);
        ofNullable(getSecurityManager())
            .ifPresentOrElse(
                sm -> {
                    try {
                        sm.checkPropertiesAccess();
                        System.setProperty(property, value);
                    }
                    catch (final SecurityException ex) {
                        // extincted exception
                    }
                },
                () -> properties.put(property, value));
    }

}

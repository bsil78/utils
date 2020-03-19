package bsil.utils.ciphering;


import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public final class CipheringConfig {

    private static final Map<Predicate<CipheringTransformation>, Predicate<String>> keyControls = new HashMap<>();

    static {
        addAESkeyControl();
    }

    private static void addAESkeyControl() {
        keyControls.put(
            transformation -> "AES".equals(transformation.algorithm()),
            key -> Stream.of(128, 192, 256).anyMatch(sizeOfKey(key))
        );
    }

    private static Predicate<Integer> sizeOfKey(final String key) {
        final int keyBitsLength = bitsLengthOfKey(key);
        return allowedBitsLength -> allowedBitsLength == keyBitsLength;
    }

    private static int bitsLengthOfKey(final String key) {
        return key.length() * 8;
    }

    private final String key;
    private final CipheringTransformation transformation;


    private CipheringConfig(final String key, final String transformation)
    throws NoSuchAlgorithmException, NoSuchPaddingException {
        this.key = requireNonNull(key);
        this.transformation = CipheringTransformation.ofName(transformation);
        verifyKeyConstraints();

    }

    public static CipheringConfig ofKeyAndTransformation(final String key, final String transformation)
    throws NoSuchAlgorithmException, NoSuchPaddingException {
        return new CipheringConfig(key, transformation);
    }

    private void verifyKeyConstraints() {
        if (!keyControls.entrySet().stream()
                        .filter(e -> isMatchingTransformation(this.transformation, e))
                        .allMatch(e -> isKeyMeetingRequirements(this.key, e))) {
            throw new KeyDoesNotMeetCipheringTransformationRequirements();
        }
    }

    private boolean isKeyMeetingRequirements(final String key,
                                             final Map.Entry<Predicate<CipheringTransformation>,
                                                                ? extends Predicate<String>> e) {
        return e.getValue().test(key);
    }

    private boolean isMatchingTransformation(final CipheringTransformation transformation,
                                             final Map.Entry<? extends Predicate<CipheringTransformation>,
                                                                Predicate<String>> e) {
        return e.getKey().test(transformation);
    }

    public String cipheringKey() {
        return this.key;
    }

    public CipheringTransformation cipheringTransformation() {
        return this.transformation;
    }

    public static final class KeyDoesNotMeetCipheringTransformationRequirements extends RuntimeException {

        private KeyDoesNotMeetCipheringTransformationRequirements() {
        }
    }
}

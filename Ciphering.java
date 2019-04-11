package bsil78.utils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.regex.Pattern;

import static java.text.MessageFormat.format;

/**
 * Utility type made to simplify Ciphering
 */
public enum Ciphering {;

    public static final String CIPHERING_KEY_PROPERTY = "ciphering.key";
    public static final String CIPHERING_TRANSFORMATION_PROPERTY = "ciphering.transformation";
    private static final Pattern[] SUPPORTED_PADDED_ALGORITHMS = {Pattern.compile("AES")};

    public static byte[] cipher(final String content) {
        return cipheringOperationResultFor(content.getBytes(), Cipher.ENCRYPT_MODE,
                                           (aText, cipher) -> cipher.doFinal(aText));
    }


    public static String uncipher(final byte[] cipheredContent) {
        return new String(cipheringOperationResultFor(cipheredContent,
                                           Cipher.DECRYPT_MODE,
                                           (aText, cipher) -> cipher.doFinal(cipheredContent)));
    }

    public static int neededPaddingFor(final String aText){
        if(!isPaddedAlgorithm()) return 0;
        final int keySize = secretKey().getEncoded().length;
        return keySize-(aText.getBytes().length% keySize);
    }

    private static boolean isPaddedAlgorithm() {
        final String algorithm = algorithmOf(cipherTransformation());
        return Arrays.stream(SUPPORTED_PADDED_ALGORITHMS)
                     .anyMatch(pattern -> pattern.matcher(algorithm).matches());
    }

    private static byte[] cipheringOperationResultFor(final byte[] content,
                                                      final int cipherMode,
                                                      final BiFunctionWithException<byte[],Cipher,byte[]> operation)
    {
        try {
            return operation.apply(content, initializedCipher(cipherMode));
        }
        catch (final Exception e) {
            final String cipherModeWord = (cipherMode == Cipher.ENCRYPT_MODE) ? "" : "un";
            throw new InternalError(format("Something gone wrong while {0}ciphering", cipherModeWord), e);
        }
    }

    private static Cipher initializedCipher(final int cipherMode)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        final Cipher cipher = Cipher.getInstance(cipherTransformation());
        cipher.init(cipherMode, secretKey());
        return cipher;
    }

    private static SecretKeySpec secretKey() {
        return new SecretKeySpec(getCipherKey().getBytes(), algorithmOf(cipherTransformation()));
    }

    private static String algorithmOf(final String transformation) {
        return transformation.split("/")[0];
    }

    @FunctionalInterface
    private interface BiFunctionWithException<T, U, V> {
        V apply(T p1, U p2) throws Exception;
    }

    private static String getCipherKey() {
        return (System.getProperty(CIPHERING_KEY_PROPERTY));
    }

    private static String cipherTransformation() {
        return System.getProperty(CIPHERING_TRANSFORMATION_PROPERTY);
    }
}

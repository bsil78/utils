package bsil.utils.ciphering;

import org.apache.commons.lang.ArrayUtils;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.text.MessageFormat.format;
import static java.util.Objects.requireNonNull;

/**
 * Utility type made to simplify Ciphering
 *  supported padded algorithms : AES
 */
public final class Cipher {

    private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";

    private final String key;
    private final CipheringTransformation transformation;
    private final IntFunction<String> paddingGenerator;
    private byte[] lastGeneratedIv;
    private byte[] lastExtractedIv;

    private Cipher(final String key, final CipheringTransformation transformation,
                   final IntFunction<String> paddingGenerator){
        this.key= requireNonNull(key);
        this.transformation= requireNonNull(transformation);
        this.paddingGenerator = requireNonNull(paddingGenerator);
    }

    private Cipher(final String key, final CipheringTransformation transformation) {
        this.key = requireNonNull(key);
        this.transformation = requireNonNull(transformation);
        if(transformation.isPaddedAlgorithm()){
            throw new IllegalArgumentException(
                format("Given transformation {0} require a padding. Use createPaddedCiphering factory method.",
                       transformation)
            );
        }
        this.paddingGenerator = null;
    }



    public static Cipher createCiphering(final CipheringConfig cipheringConfig) {
        return new Cipher(cipheringConfig.cipheringKey(),
                          cipheringConfig.cipheringTransformation());
    }

    public static Cipher createPaddedCiphering(final CipheringConfig cipheringConfig,
                                               final IntFunction<String> paddingGenerator) {
        return new Cipher(cipheringConfig.cipheringKey(),
                          cipheringConfig.cipheringTransformation(),
                          paddingGenerator);
    }



    public byte[] cipher(final String content) {
        requireNonNull(content);
        final var toCipher = (this.transformation.isPaddedAlgorithm())? addPaddingTo(content):content;
        final byte[] cipheredContent = cipheringOperationResultFor(toCipher.getBytes(UTF_8), javax.crypto.Cipher.ENCRYPT_MODE,
                                                                   (aText, cipher) -> cipher.doFinal(aText));
        if(isIVRequired()){
            return ArrayUtils.addAll(this.lastGeneratedIv, cipheredContent);
        } else {
            return cipheredContent;
        }
    }

    private String addPaddingTo(final String content) {
        return content + this.paddingGenerator.apply(neededPaddingFor(content));
    }


    public String uncipher(final byte[] cipheredContent, final Function<? super String, String> finalizer) {
        return finalizer.apply(new String(getUncipheredBytes(cipheredContent),UTF_8));
    }

    private byte[] getUncipheredBytes(final byte[] cipheredContent) {
        return cipheringOperationResultFor(cipheredContent,
                                           javax.crypto.Cipher.DECRYPT_MODE,
                                           (aText, cipher) -> {
                                               final byte[] content = textPartOfCipheredContent(aText, cipher);
                                               return cipher.doFinal(content);
                                            });
    }

    private byte[] textPartOfCipheredContent(final byte[] aText, final javax.crypto.Cipher cipher) {
        return isIVRequired() ? bytesWithoutIV(aText, cipher) : aText;
    }

    private byte[] bytesWithoutIV(final byte[] aText, final javax.crypto.Cipher cipher) {
        return Arrays.copyOfRange(aText, cipher.getBlockSize(), aText.length);
    }

    private byte[] extractIVbytes(final byte[] aText, final javax.crypto.Cipher cipher) {
        return Arrays.copyOfRange(aText, 0, cipher.getBlockSize());
    }

    private int neededPaddingFor(final String aText){
        if(!this.transformation.isPaddedAlgorithm()) return 0;
        final int keyLen = secretKey().getEncoded().length;
        return keyLen-(aText.getBytes().length% keyLen);
    }



    private byte[] cipheringOperationResultFor(final byte[] content,
                                                      final int cipherMode,
                                                      final ThrowingBiFunction<byte[], ? super javax.crypto.Cipher,
                                                                                  byte[], Exception> operation)
    {
        try {
            final javax.crypto.Cipher cipher=this.transformation.newCipher();
            if(javax.crypto.Cipher.DECRYPT_MODE == cipherMode && isIVRequired()){
                this.lastExtractedIv = extractIVbytes(content,cipher);
            }
            initializedCipher(cipher,cipherMode);
            return operation.apply(content, cipher);
        }
        catch (final Exception e) {
            final String cipherModeWord = (javax.crypto.Cipher.ENCRYPT_MODE == cipherMode) ? "" : "un";
            throw new InternalError(format("Something gone wrong while {0}ciphering", cipherModeWord), e);
        }
    }


    private void initializedCipher(final javax.crypto.Cipher cipher, final int cipherMode)
        throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException
    {
        final AlgorithmParameterSpec params = paramSpec(cipher, cipherMode);
        if(Objects.nonNull(params)) {
            cipher.init(cipherMode, secretKey(), params);
        }else {
          cipher.init(cipherMode, secretKey());
        }
    }

    private AlgorithmParameterSpec paramSpec(final javax.crypto.Cipher cipher, final int cipherMode)
        throws NoSuchAlgorithmException
    {
        AlgorithmParameterSpec params=null;
        if(isIVRequired()){
          byte[] iv=null;
          if(javax.crypto.Cipher.ENCRYPT_MODE == cipherMode) {
              iv = new byte[cipher.getBlockSize()];
              SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM).nextBytes(iv);
              this.lastGeneratedIv = iv;
          }
          if(javax.crypto.Cipher.DECRYPT_MODE == cipherMode) {
              iv= this.lastExtractedIv;
          }
          assert null != iv;
          params = new IvParameterSpec(iv);
        }
        return params;
    }

    private boolean isIVRequired() {
        return "CTR".equals(this.transformation.mode());
    }

    private SecretKeySpec secretKey() {
        return new SecretKeySpec(this.key.getBytes(), this.transformation.algorithm());
    }



    @FunctionalInterface
    private interface ThrowingBiFunction<T, U, V, E extends Exception> {
        V apply(T p1, U p2) throws E;
    }



}

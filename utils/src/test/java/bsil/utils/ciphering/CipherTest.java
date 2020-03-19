package bsil.utils.ciphering;

import bsil.utils.SystemPropertiesHelper;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;
import java.util.function.Predicate;

import static bsil.utils.ciphering.Cipher.createCiphering;
import static bsil.utils.ciphering.Cipher.createPaddedCiphering;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class CipherTest {

    private static final String AESKey = "MyCipheringKey!!"; // 16 chars => 128 bits
    private static final String AES_W_PADDING = "AES/ECB/PKCS5Padding";
    private static final String AES_NO_PADDING = "AES/CTR/NoPadding";
    private static final int L128BITS = 128 / 8;
    private static final int L192BITS = 192 / 8;
    private static final int L256BITS = 32;

    public static void setupPaddedAESCipheringForTest() {
        SystemPropertiesHelper.setProperty(CipheringConfigHelper.CIPHERING_KEY_PROPERTY, AESKey);
        SystemPropertiesHelper.setProperty(CipheringConfigHelper.CIPHERING_TRANSFORMATION_PROPERTY, AES_W_PADDING);
    }

    static void setupNotPaddedAESCipheringForTest() {
        SystemPropertiesHelper.setProperty(CipheringConfigHelper.CIPHERING_KEY_PROPERTY, AESKey);
        SystemPropertiesHelper.setProperty(CipheringConfigHelper.CIPHERING_TRANSFORMATION_PROPERTY, AES_NO_PADDING);
    }

    @Test
    final void should_helper_return_config_with_given_system_properties()
    throws NoSuchAlgorithmException, NoSuchPaddingException {
        setupPaddedAESCipheringForTest();
        final CipheringConfig cipheringConfig = CipheringConfigHelper.config();
        assertThat(cipheringConfig.cipheringKey()).isEqualTo(AESKey);
        assertThat(cipheringConfig.cipheringTransformation().toString()).isEqualTo(AES_W_PADDING);
    }


    @RepeatedTest(10)
    final void should_AES_ciphering_transformation_check_bad_key_length() {
        final int keyLength = getKeyLength(this::isNotAESLengthCondition);
        assertThatThrownBy(() -> CipheringConfig.ofKeyAndTransformation(generateKeyOfLength(keyLength), AES_W_PADDING))
            .isInstanceOf(CipheringConfig.KeyDoesNotMeetCipheringTransformationRequirements.class);
    }

    @RepeatedTest(10)
    final void should_AES_ciphering_transformation_check_good_key_length()
    throws NoSuchPaddingException, NoSuchAlgorithmException
    {
        final int keyLength = getKeyLength(this::isAESLengthCondition);
        final String key = generateKeyOfLength(keyLength);
        final CipheringConfig config = CipheringConfig.ofKeyAndTransformation(key, AES_W_PADDING);
        assertThat(config).isNotNull().extracting("cipheringKey").isEqualTo(key);
    }

    private boolean isNotAESLengthCondition(final Integer length) {
        return !isAESLengthCondition(length);
    }

    private boolean isAESLengthCondition(final int length) {
        return L128BITS == length || L192BITS == length || L256BITS == length;
    }

    private int getKeyLength(Predicate<Integer> lengthCondition) {
        int length = -1;
        while(0 > length || !lengthCondition.test(length))
            length= RandomUtils.nextInt(50);
        return length;
    }

    private String generateKeyOfLength(final int keyLength) {
        return (AESKey + AESKey + AESKey + AESKey).substring(0, keyLength);
    }

    @Test
    final void should_ciphering_transformation_give_right_algorithm()
    throws NoSuchAlgorithmException, NoSuchPaddingException {
        setupPaddedAESCipheringForTest();
        final CipheringConfig cipheringConfig = CipheringConfigHelper.config();
        assertThat(cipheringConfig.cipheringTransformation().algorithm()).isEqualTo("AES");
    }

    @RepeatedTest(10)
    final void should_cipher_and_uncipher_PaddedAES()
    throws NoSuchAlgorithmException, NoSuchPaddingException {
        setupPaddedAESCipheringForTest();
        Cipher ciphering=createPaddedCiphering(CipheringConfigHelper.config(), len-> StringUtils.repeat(" ", len));
        testCiphering(ciphering, String::trim);
    }

    @RepeatedTest(10)
    final void should_cipher_and_uncipher_NotPaddedAES()
    throws NoSuchAlgorithmException, NoSuchPaddingException {
        setupNotPaddedAESCipheringForTest();
        Cipher ciphering = createCiphering(CipheringConfigHelper.config());
        testCiphering(ciphering, Function.identity());
    }

    private void testCiphering(final Cipher ciphering, final Function<String,String> finalizer) {
        //Given
        final String originalString = RandomStringUtils.randomAlphanumeric(RandomUtils.nextInt(1000));

        //When
        final String cipheredThenUnciphered = ciphering.uncipher(ciphering.cipher(originalString), finalizer);

        //Then
        assertThat(cipheredThenUnciphered).isEqualTo(originalString);
    }


}

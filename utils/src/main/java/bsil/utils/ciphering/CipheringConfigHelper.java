package bsil.utils.ciphering;

import bsil.utils.SystemPropertiesHelper;

import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;

public enum CipheringConfigHelper {;

    public static final String CIPHERING_KEY_PROPERTY = "ciphering.key";
    public static final String CIPHERING_TRANSFORMATION_PROPERTY = "ciphering.transformation";

    private static String cipheringKey() {
        return SystemPropertiesHelper.getProperty(CIPHERING_KEY_PROPERTY);
    }

    private static String cipheringTransformation() {
        return SystemPropertiesHelper.getProperty(CIPHERING_TRANSFORMATION_PROPERTY);
    }

    public static CipheringConfig config()
    throws NoSuchPaddingException, NoSuchAlgorithmException {
        return CipheringConfig.ofKeyAndTransformation(cipheringKey(), cipheringTransformation());
    }

}

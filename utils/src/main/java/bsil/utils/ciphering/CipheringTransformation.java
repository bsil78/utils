package bsil.utils.ciphering;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public final class CipheringTransformation {

    private static final Pattern NOTPADDED_ALGORITHMS_PATTERN = Pattern.compile("^.*/NoPadding$");
    private final String transformation;
    private Cipher cipher;

    private CipheringTransformation(final String transformation)
        throws NoSuchPaddingException, NoSuchAlgorithmException
    {
        this.transformation = requireNonNull(transformation);
        this.cipher=Cipher.getInstance(this.transformation);
    }

    public static CipheringTransformation ofName(final String transformation)
    throws NoSuchPaddingException, NoSuchAlgorithmException {
        return new CipheringTransformation(transformation);
    }


    public boolean isPaddedAlgorithm() {
        return !NOTPADDED_ALGORITHMS_PATTERN.matcher(this.transformation).matches();
    }

    public Cipher newCipher()
    throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.cipher=Cipher.getInstance(this.transformation);
        return this.cipher;
    }

    @Override
    public String toString() {
        return this.transformation;
    }

    public String algorithm() {
        return this.cipher.getAlgorithm().split("/")[0];
    }

    public String mode() {
        return Arrays.stream(this.cipher.getAlgorithm().split("/")).limit(2).skip(1).findFirst().orElse("");
    }
}

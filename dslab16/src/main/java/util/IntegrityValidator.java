package util;

import org.bouncycastle.util.encoders.Base64;

import javax.crypto.Mac;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class IntegrityValidator {
    public static Key secretKey;

    private static Mac getMac() throws InvalidKeyException, NoSuchAlgorithmException {
        Mac hMac = Mac.getInstance("HmacSHA256");
        hMac.init(secretKey);

        return hMac;
    }

    private static byte[] generateHMACByte(String message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hMac = getMac();
        hMac.update(message.getBytes());
        byte[] hash = hMac.doFinal();

        return hash;
    }

    public static String generateHMAC(String message) throws InvalidKeyException, NoSuchAlgorithmException {
        return new String(Base64.encode(generateHMACByte(message)));
    }

    public static boolean isMessageUntampered(String hmacStringB64, String message) throws InvalidKeyException, NoSuchAlgorithmException {

        return MessageDigest.isEqual(Base64.decode(hmacStringB64), generateHMACByte(message));
    }

    public static void loadHMACKey(Config config) throws IOException {
        File hmac = new File(config.getString("hmac.key"));
        secretKey = Keys.readSecretKey(hmac);
    }
}

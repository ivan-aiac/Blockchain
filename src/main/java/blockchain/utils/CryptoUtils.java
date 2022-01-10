package blockchain.utils;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class CryptoUtils {
    public static KeyPair generateKeys(int keyLength) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(keyLength);
        return keyGen.generateKeyPair();
    }

    public static byte[] signData(PrivateKey privateKey, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature rsa = Signature.getInstance("SHA1withRSA");
        rsa.initSign(privateKey);
        rsa.update(data);
        return rsa.sign();
    }

    public static boolean verifySignature(byte[] encodedKey, byte[] signature, byte[] data) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        Signature sig = Signature.getInstance("SHA1withRSA");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        sig.initVerify(kf.generatePublic(spec));
        sig.update(data);
        return sig.verify(signature);
    }
}

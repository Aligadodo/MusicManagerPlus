/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class DecryptUtils {

    // AES-ECB decrypt
    public static byte[] AESECBDecrypt(byte[] src, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(src);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException e) {
            System.out.println("Error");
            throw new RuntimeException(e);
        }
    }

    // Base64 Decrypt
    public static byte[] base64Decrypt(byte[] src) {
        Base64.Decoder decoder = Base64.getDecoder();
        return decoder.decode(src);
    }

    // RC4-KSA algorithm
    public static byte[] RC4KSA(byte[] k) {
        byte[] s = new byte[256];
        for (int i = 0; i <= 255; i++) {
            s[i] = (byte) i;
        }

        int j = 0;
        for (int i = 0; i <= 255; i++) {
            j = (j + s[i] + k[i % k.length]) & 255;
            byte swap = s[i];
            s[i] = s[j];
            s[j] = swap;
        }
        return s;
    }

    // RC4-PRGA algorithm
    public static void RC4PRGA(byte[] src, byte[] s) {

        byte[] k = new byte[256];

        for (int i = 0; i <= 255; i++) {
            k[i] = s[(s[i] + s[(i + s[i]) & 255]) & 255];
        }

        for (int j = 0; j < src.length; j++) {
            src[j] ^= k[(j + 1) % 256];
        }
    }

}

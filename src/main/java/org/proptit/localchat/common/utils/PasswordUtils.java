package org.proptit.localchat.common.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }


    public static boolean checkPassword(String plainPw, String hashedPw) {
        if (plainPw == null || hashedPw == null || hashedPw.isBlank()) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainPw, hashedPw);
        } catch (IllegalArgumentException ex) {
            // Invalid hash format (e.g. plain text password stored in DB).
            return false;
        }
    }
}
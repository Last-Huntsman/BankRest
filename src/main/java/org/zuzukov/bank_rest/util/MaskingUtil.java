package org.zuzukov.bank_rest.util;

public final class MaskingUtil {
    private MaskingUtil() {}

    public static String maskCardLast4(String last4) {
        return "**** **** **** " + last4;
    }
}



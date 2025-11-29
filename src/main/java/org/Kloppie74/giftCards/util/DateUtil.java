package org.Kloppie74.giftCards.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
    public static String expireDaysFromNow(int days) {
        long millis = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000;
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(millis));
    }
}
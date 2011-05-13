package uk.ac.ebi.gxa.utils;

import java.util.Date;

public class DateUtil {
    public static Date copyOf(Date date) {
        return date == null ? null : new Date(date.getTime());
    }
}

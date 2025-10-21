package com.nganhangdethi.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class DateUtils {

    private DateUtils() {} // Lớp tiện ích

    public static final String DEFAULT_DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";
    public static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy";

    private static final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

    public static String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        return dateTimeFormatter.format(new Date(timestamp.getTime()));
    }

    public static String formatDate(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        return dateFormatter.format(new Date(timestamp.getTime()));
    }

    public static String formatDateTime(Date date) {
        if (date == null) return "N/A";
        return dateTimeFormatter.format(date);
    }

    public static String formatDate(Date date) {
        if (date == null) return "N/A";
        return dateFormatter.format(date);
    }

    public static Timestamp parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) return null;
        try {
            Date parsedDate = dateTimeFormatter.parse(dateTimeString);
            return new Timestamp(parsedDate.getTime());
        } catch (ParseException e) {
            System.err.println("Error parsing date-time string '" + dateTimeString + "': " + e.getMessage());
            return null;
        }
    }
}
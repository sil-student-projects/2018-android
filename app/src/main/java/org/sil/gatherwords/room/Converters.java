package org.sil.gatherwords.room;

import android.arch.persistence.room.TypeConverter;

import java.util.Date;

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static String sessionStatusToString(Session.Status status){
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static Session.Status stringToSessionStatus(String statusName) {
        return statusName == null ? null : Session.Status.valueOf(statusName);
    }

    @TypeConverter
    public static String wordStatusToString(Word.Status status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static Word.Status stringToWordStatus(String statusName) {
        return statusName == null ? null : Word.Status.valueOf(statusName);
    }
}

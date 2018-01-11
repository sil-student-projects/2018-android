package org.sil.gatherwords.room;

import android.arch.persistence.room.TypeConverter;

import java.util.Date;

/**
 * Created by andrewthomas on 1/11/18.
 */

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}

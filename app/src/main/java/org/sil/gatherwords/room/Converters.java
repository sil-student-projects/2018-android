package org.sil.gatherwords.room;

import android.arch.persistence.room.TypeConverter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
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
    public static byte[] bitmapToBlob(Bitmap picture) {
        if (picture == null) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        picture.compress(Bitmap.CompressFormat.PNG, 100, bos);

        return bos.toByteArray();
    }

    @TypeConverter
    public static Bitmap byteArrayToBitmap(byte[] bArray) {
        if (bArray == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
    }
}

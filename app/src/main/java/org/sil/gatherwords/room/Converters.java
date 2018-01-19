package org.sil.gatherwords.room;

import android.arch.persistence.room.TypeConverter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    @TypeConverter
    public static String jsonObjectToString(JSONObject object) {
        if (object == null) {
            return null;
        }
        return object.toString();
    }

    @TypeConverter
    public static JSONObject stringToJsonObject(String string) {
        if (string == null) {
            return null;
        }
        JSONObject object = null;
        try {
            object = new JSONObject(string);
        } catch (JSONException e) {
            Log.e("stringToJsonObject()", "JSONException when converting String to JSON", e);
        }
        return object;
    }

    @TypeConverter
    public static String stringArrayToString(String[] strings) {
        if (strings == null) {
            return null;
        }
        JSONArray temp = new JSONArray();
        try {
            temp = new JSONArray(strings);
        } catch (JSONException e) {
            Log.e("stringArrayToString()", "JSONException while converting String[] to string", e);
        }
        return temp.toString();
    }

    @TypeConverter
    public static String[] stringToStringArray(String s) {
        if (s == null) {
            return null;
        }
        JSONArray temp;
        List<String> stringList = new ArrayList<>();
        try {
            temp = new JSONArray(s);
            for (int i = 0; i < temp.length(); i++) {
                stringList.add(temp.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return stringList.toArray(new String[0]);
    }

}

package org.sil.gatherwords.room;

import android.arch.persistence.room.TypeConverter;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    /**
     * Convert audio byte[] into a File for the media player
     *
     * Not necessarily a Room type converter, but still needed
     * @param bytes The audio byte[]
     * @return Audio file
     */
    public static File byteArrayToFile(byte[] bytes, String filepath) {
        File file = new File(filepath);
        try {
            // Create the file if it doesn't exist
            if (!file.exists() && !file.createNewFile()) {
                // The file does not exist and could not be created
                // jump out of the try block
                throw new IOException("Cannot create file");
            }

            // Check that we can write to the file
            if (!file.canWrite() && !file.setWritable(true)) {
                // We can't write to the file but it exists and cannot make writable
                throw new IOException("Cannot write to file");
            }

            // Write the array to the file and close the stream
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(bytes);
            outputStream.close();
        } catch (IOException e) {
            Log.e("byteArrayToFile", "IOException while writing to temporary file", e);
        }
        return file;
    }
}

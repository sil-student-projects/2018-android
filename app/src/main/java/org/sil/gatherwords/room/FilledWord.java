package org.sil.gatherwords.room;

import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Relation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.sil.gatherwords.Util;

import java.io.File;
import java.util.List;

public class FilledWord {
    private static final String TAG = FilledWord.class.getSimpleName();

    public int id;
    public int sessionID;
    public String audio;
    public String picture;
    public String semanticDomain;

    @Ignore
    public Bitmap imageData;

    @Relation(parentColumn = "id", entityColumn = "wordID")
    public List<Meaning> meanings;

    public void loadImageDataScaled(Context context) {
        if (picture == null || imageData != null) {
            return;
        }

        // Load reduced size to save memory.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        File imageFile = Util.getDataFile(context, picture);
        imageData = BitmapFactory.decodeFile(
            imageFile.getAbsolutePath(),
            options
        );

        if (imageData == null) {
            Log.e(TAG, "Failed to decode bitmap: " + imageFile);
        }
    }
}

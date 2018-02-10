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

    public long id;
    public long sessionID;
    public String picture;
    public Long semanticDomainID;

    @Ignore
    public Bitmap imageData;

    @Relation(parentColumn = "id", entityColumn = "wordID")
    public List<Meaning> meanings;

    // Should have at most 1 entry.
    @Relation(parentColumn = "semanticDomainID",
              entityColumn = "id",
              entity = SemanticDomain.class,
              projection = {"name"})
    public List<String> semanticDomains;

    public String getSemanticDomain() {
        if (semanticDomains == null || semanticDomains.isEmpty()) {
            return null;
        }
        else {
            return semanticDomains.get(0);
        }
    }

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

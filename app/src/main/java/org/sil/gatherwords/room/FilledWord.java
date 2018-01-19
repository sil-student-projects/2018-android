package org.sil.gatherwords.room;

import android.arch.persistence.room.Relation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class FilledWord extends Word {
//    public long id;
//    public long sessionID;
    @Relation(parentColumn = "id", entityColumn = "wordID")
    public List<Meaning> meanings;

    @Override
    public String toString() {
        JSONObject object = new JSONObject();
        JSONObject senses = new JSONObject();
        //TODO build lexeme string
        JSONObject lexeme = new JSONObject();
//        senses.put("audio", audio != null ? audio : ""); // audio was being reworked
        try {
            JSONObject gloss = new JSONObject();
            for (Meaning meaning : meanings) {
                gloss.put("en", meaning.data); // TODO Fix gloss
            }
            senses.put("gloss", gloss);
//            senses.put("pictures", PATH_TO_PICTURES);

            object.put("lexeme", lexeme);
            object.put("senses", senses);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object.toString();
    }
}

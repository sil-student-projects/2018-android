package org.sil.gatherwords.room;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DatabaseSeeder {
    private static final String TAG = DatabaseSeeder.class.getSimpleName();

    public static void seed(Context context) {
        new SeedTask(context).execute();
    }

    private static class SeedTask extends AsyncTask<Void, Void, Void> {
        private AppDatabase db;
        private AssetManager assets;

        SeedTask(Context context) {
            db = AppDatabase.get(context);
            assets = context.getAssets();
        }

        @Override
        protected Void doInBackground(Void... v) {
            SemanticDomainDAO sdDAO = db.semanticDomainDAO();
            if (sdDAO.count() == 0) {
                seedSemanticDomains();
            }

            return null;
        }

        private void seedSemanticDomains() {
            InputStream is = null;
            BufferedReader br = null;

            try {
                is = assets.open("semanticDomain/semanticDomains_en.json");
                br = new BufferedReader(new InputStreamReader(is));
                SemanticDomain[] semanticDomains = new Gson().fromJson(br, SemanticDomain[].class);

                SemanticDomainDAO sdDAO = db.semanticDomainDAO();
                sdDAO.insertSemanticDomains(semanticDomains);
            } catch (IOException e) {
                Log.e(TAG, "IOException while reading the semantic domain file", e);
            } finally {
                // Assume the input stream is not null because it is used to construct the buffered reader
                if (br != null) {
                    try {
                        is.close();
                        br.close();
                    } catch (IOException e) {
                        Log.e(TAG, "IOException when closing buffered reader and stream", e);
                    }
                }
            }
        }
    }
}

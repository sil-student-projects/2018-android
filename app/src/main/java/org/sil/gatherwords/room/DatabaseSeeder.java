package org.sil.gatherwords.room;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DatabaseSeeder {
    private static String TAG = DatabaseSeeder.class.getSimpleName();

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

            List<SemanticDomain> semanticDomains = new ArrayList<>();

            try {
                is = assets.open("semanticDomain/semanticDomains_en.json");
                br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }

                JSONObject jsonObjectCluster = new JSONObject(sb.toString());
                Iterator<String> keys = jsonObjectCluster.keys();

                while(keys.hasNext()) {
                    String key = keys.next();
                    if (jsonObjectCluster.get(key) instanceof JSONObject) {
                        JSONObject domainObject = (JSONObject) jsonObjectCluster.get(key);

                        SemanticDomain semanticDomain = new SemanticDomain();
                        semanticDomain.guid        = domainObject.getString("guid");
                        semanticDomain.key         = domainObject.getString("key");
                        semanticDomain.abbr        = domainObject.getString("abbr");
                        semanticDomain.name        = domainObject.getString("name");
                        semanticDomain.description = domainObject.getString("description");

                        semanticDomains.add(semanticDomain);
                    }
                }

            } catch (IOException e) {
                Log.e(TAG, "IOException while reading the semantic domain file", e);
            } catch (JSONException e) {
                Log.e(TAG, "JSONException while parsing the semantic domain file", e);
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

            if (!semanticDomains.isEmpty()) {
                SemanticDomainDAO sdDAO = db.semanticDomainDAO();
                sdDAO.insertSemanticDomains(semanticDomains);
            }
        }
    }
}

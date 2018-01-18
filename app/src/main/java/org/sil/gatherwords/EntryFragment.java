package org.sil.gatherwords;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.FilledWord;
import org.sil.gatherwords.room.Meaning;
import org.sil.gatherwords.room.MeaningDao;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class EntryFragment extends Fragment {
    private static final String ARG_WORD_ID = "wordID";
    private static final String ARG_POSITION = "position";
    private static final String ARG_TOTAL = "total";

    private long m_wordID;  // Database ID of the word displayed word.
    private int m_position; // The index of the displayed word (0-indexed).
    private int m_total;    // Total number of words.

    public static EntryFragment newInstance(long wordID, int position, int total) {
        EntryFragment fragment = new EntryFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_WORD_ID, wordID);
        args.putInt(ARG_POSITION, position);
        args.putInt(ARG_TOTAL, total);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            m_wordID = args.getLong(ARG_WORD_ID);
            m_position = args.getInt(ARG_POSITION);
            m_total = args.getInt(ARG_TOTAL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View entryPage = inflater.inflate(R.layout.fragment_entry, container, false);

        new LoadWordTask(entryPage, m_position, m_total).execute(m_wordID);
        TextView pageStatus = entryPage.findViewById(R.id.page_status);
        pageStatus.setText(
            getString(R.string.entry_status_line, "Word", m_position + 1, m_total)
        );

        return entryPage;
    }

    /**
     * Will run after the view is created and after the picture is taken.
     */
    @Override
    public void onResume() {
        super.onResume();
        new UpdateAfterPicture(getView()).execute(m_wordID);
    }

    /**
     * Update lexical entries upon stopping the fragment (occurs after user swipes to another screen).
     */
    @Override
    public void onStop() {
        super.onStop();

        try {
            View rootView = getView();
            if (rootView == null) {
                Log.e(
                    EntryFragment.class.getSimpleName(),
                    "Root view not found"
                );
                return;
            }

            ListView entryFields = rootView.findViewById(R.id.entry_fields);
            // The last item in the list view is always the semantic domain, which will cause an error if you try
            // and store it as a Meaning. Hence the -1(s)
            Meaning[] meanings = new Meaning[entryFields.getCount() - 1];
            for (int i = 0; i < entryFields.getCount() - 1; i++) {
                View entryField = entryFields.getChildAt(i);
                TextView langCodeField = entryField.findViewById(R.id.lang_code);
                EditText langDataField = entryField.findViewById(R.id.lang_data);

                if (langCodeField == null || langDataField == null) {
                    continue;
                }

                meanings[i] = new Meaning(
                    m_wordID,
                    langCodeField.getText().toString(),
                    langDataField.getText().toString()
                );
            }

            new UpdateMeaningTask(getContext()).execute(meanings);

            // Update the semantic domain. Blank should insert None as consisten with default
            //      value in database
            AutoCompleteTextView aCTV = entryFields.findViewById(R.id.semantic_domain_auto_complete);
            if ( !aCTV.getText().toString().equals("") ) {
                new UpdateWordTask(getContext(), "None").execute(m_wordID);
            } else {
                new UpdateWordTask(getContext(), aCTV.getText().toString()).execute(m_wordID);
            }

        } catch (Exception e) {
            Log.e("EntryFragment.java", "Null pointer exception - failed to find ListView and/or EditText object", e);
        }
    }

    /**
     * Gets the current wordId
     *
     * It seems m_wordID is not set when returning from taking a picture.
     * @return The wordID of the current Word
     */
    public long getWordID() {
        return getArguments().getLong(ARG_WORD_ID);
    }

    private static class LoadWordTask extends AsyncTask<Long, Void, FilledWord> {
        WeakReference<View> entryPageRef;
        WordDao wDAO;
        int position;
        int total;

        LoadWordTask(View entryPage, int pos, int tot) {
            entryPageRef = new WeakReference<>(entryPage);
            wDAO = AppDatabase.get(entryPage.getContext()).wordDao();
            position = pos;
            total = tot;
        }

        @Override
        protected FilledWord doInBackground(Long... wordIDs) {
            if (wordIDs == null || wordIDs.length != 1) {
                Log.e(LoadWordTask.class.getSimpleName(), "Did not receive exactly 1 wordID");
                return null;
            }

            return wDAO.getFilled(wordIDs[0]);
        }

        @Override
        protected void onPostExecute(FilledWord word) {
            View entryPage = entryPageRef.get();
            if (entryPage == null || word == null) {
                return;
            }

            Context context = entryPage.getContext();

            // TODO: Fill with real data from `word`.


            ListView entryFields = entryPage.findViewById(R.id.entry_fields);
            entryFields.setAdapter(new EntryFieldAdapter(
                LayoutInflater.from(context),
                word.meanings, word.semanticDomain, entryPage
            ));
        }
    }

    /**
     * Performs background task to update meanings for lexical entries in database.
     */
    private static class UpdateMeaningTask extends AsyncTask<Meaning, Void, Void> {
        private AppDatabase db;

        //Constructor.
        UpdateMeaningTask(Context context) {
            db = AppDatabase.get(context);
        }

        @Override
        protected Void doInBackground(Meaning... meanings) {
            MeaningDao mDAO = db.meaningDao();

            db.beginTransaction();
            try {
                for (Meaning meaning : meanings) {
                    if (meaning == null) {
                        continue;
                    }

                    Meaning currentState = mDAO.getByType(meaning.wordID, meaning.type);
                    if (currentState == null) {
                        // New entry? Insert.
                        mDAO.insertMeanings(meaning);
                    }
                    else {
                        // Pull the ID for update key.
                        meaning.id = currentState.id;
                        mDAO.updateMeanings(meaning);
                    }
                }

                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(
                    EntryFragment.class.getSimpleName(),
                    "Error updating meanings",
                    e
                );
            } finally {
                // Commit or rollback the database.
                db.endTransaction();
            }

            mDAO.updateMeanings(meanings);
            return null;
        }
    }

    /**
     * Update the Image viewer with the stored image
     */
    private static class UpdateAfterPicture extends AsyncTask<Long, Void, Word> {

        private WeakReference<View> entryPageRef;
        private WordDao wd;

        UpdateAfterPicture(View entryPage) {
            entryPageRef = new WeakReference<>(entryPage);
            wd = AppDatabase.get(entryPage.getContext()).wordDao();
        }

        @Override
        protected Word doInBackground(Long... wordIDs) {
            if (wordIDs == null || wordIDs.length != 1) {
                Log.e(UpdateAfterPicture.class.getSimpleName(), "Did not receive exactly 1 wordID");
                return null;
            }

            return wd.get(wordIDs[0]);
        }

        @Override
        protected void onPostExecute(Word word) {
            View entryPage = entryPageRef.get();
            if (entryPage == null || word == null) {
                return;
            }

            ImageView picture = entryPage.findViewById(R.id.pic_viewer);
            if (word.picture != null) {
                picture.setVisibility(View.VISIBLE);
                picture.setImageBitmap(word.picture);
            } else {
                picture.setVisibility(View.GONE);
            }
        }
    }

    private static class EntryFieldAdapter extends BaseAdapter {
        private int TYPE_COUNT = 2;
        private LayoutInflater inflater;
        private List<Meaning> meaningList;
        private String semanticDomain;
        private View entryPage;

        EntryFieldAdapter(LayoutInflater flate, List<Meaning> meanList, String sd, View page) {
            inflater = flate;
            meaningList = meanList;
            entryPage = page;
            semanticDomain = sd;
        }

        @Override
        public int getCount() {
            return meaningList.size() + 1;
        }

        @Override
        public Meaning getItem(int i) {
            return meaningList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_COUNT;
        }


        @Override
        public View getView(int i, View convertView, ViewGroup container) {
            // For each meaning
            if ( i != meaningList.size() ) {
                Meaning meaning = getItem(i);
                convertView = inflater.inflate(R.layout.entry_field_item,
                        container, false);
                if (meaning != null) {
                    TextView langCodeField = convertView.findViewById(R.id.lang_code);
                    langCodeField.setText(meaning.type);
                    EditText langDataField = convertView.findViewById(R.id.lang_data);
                    langDataField.setText(meaning.data);
                }
            // For the final item, the semantic domain
            } else {
                convertView = inflater.inflate( R.layout.entry_field_semantic_domain_line,
                                                    container, false );
                AutoCompleteTextView aCTV = convertView.findViewById(R.id.semantic_domain_auto_complete);
                aCTV.setAdapter( new ArrayAdapter<String>(convertView.getContext(),
                                    android.R.layout.simple_dropdown_item_1line, getSemanticDomains() ));
                if ( !semanticDomain.equals("None") ) {
                    aCTV.setText(semanticDomain);
                } else { // Not sure if this will ever run, but added as a precaution. If "" is read, "None" is inserted into db
                    aCTV.setText("");
                }
            }

            return convertView;
        }

        /* Returns a List of semantic domains from the JSON file of semantic domains from Language Forge

         */
        private List<String> getSemanticDomains() {
            InputStream is = null;
            BufferedReader br = null;
            String file = null;
            AssetManager assets = ((EntryActivity) entryPage.getContext()).getAssets();
            List<String> semanticDomainList = new ArrayList<>();
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
                file = sb.toString();

                JSONObject jsonObjectCluster = new JSONObject( file.trim() );
                Iterator<String> keys = jsonObjectCluster.keys();

                while( keys.hasNext() ) {
                    String key = keys.next();
                    if ( jsonObjectCluster.get(key) instanceof JSONObject ) {
                        semanticDomainList.add( ((JSONObject) jsonObjectCluster.get(key)).getString("name") );
                    }
                }

            } catch (IOException e) {
                Log.e("InsertSessionsTask", "IOException while reading the semantic domain file", e);
            } catch (JSONException e) {
                Log.e("InsertSessionsTask", "JSONException while parsing the semantic domain file", e);
            } finally {
                // Assume the input stream is not null because it is used to construct the buffered reader
                if (br != null) {
                    try {
                        is.close();
                        br.close();
                    } catch (IOException e) {
                        Log.e("InsertSessionsTask", "IOException when closing buffered reader and stream", e);
                    }
                }
            }
            return semanticDomainList;
        }
    }

    // Currently just used to update semantic domain
    private static class UpdateWordTask extends AsyncTask<Long, Void, Void> {

        private WordDao wd;
        private String semanticDomainToSet;

        UpdateWordTask(Context context, String semanticDomain) {

            wd = AppDatabase.get(context).wordDao();
            semanticDomainToSet = semanticDomain;
        }

        @Override
        protected Void doInBackground(Long... ids) {
            if ( ids == null || ids.length != 1) {
                Log.e(UpdateAfterPicture.class.getSimpleName(), "Did not receive exactly 1 wordID");
                return null;
            }
            Long id = ids[0];
            Word word = wd.get(id);
            word.semanticDomain = semanticDomainToSet;
            wd.updateWords(word);
            return null;
        }
    }
}

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
import org.sil.gatherwords.room.MeaningDAO;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


public class EntryFragment extends Fragment {
    private static final String TAG = EntryFragment.class.getSimpleName();

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
        // TODO: Hard coded "Word" is intended to be the word this page represents.
        // What word would that be?
        pageStatus.setText(
            getString(R.string.entry_status_line, "Word", m_position + 1, m_total)
        );

        return entryPage;
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
                    TAG,
                    "Root view not found"
                );
                return;
            }

            ListView entryFields = rootView.findViewById(R.id.entry_fields);

            List<Meaning> meanings = new ArrayList<>(entryFields.getCount());
            for (int i = 0; i < entryFields.getCount(); i++) {
                View entryField = entryFields.getChildAt(i);
                if (entryField == null) {
                    // TODO: Does this mean the field was deleted before we saved the data?
                    //      ... yes, yes it does.
                    continue;
                }

                TextView langCodeField = entryField.findViewById(R.id.lang_code);
                EditText langDataField = entryField.findViewById(R.id.lang_data);

                if (langCodeField == null || langDataField == null) {
                    continue;
                }

                meanings.add(new Meaning(
                    m_wordID,
                    langCodeField.getText().toString(),
                    langDataField.getText().toString()
                ));
            }

            FilledWord word = new FilledWord();
            word.id = m_wordID;
            word.meanings = meanings;

            // Update the semantic domain. Blank should insert None as consistent with default
            //      value in database.
            AutoCompleteTextView aCTV = entryFields.findViewById(R.id.semantic_domain_auto_complete);
            String semanticDomain = aCTV.getText().toString();
            if (semanticDomain.isEmpty()) {
                semanticDomain = "None";
            }
            word.semanticDomain = semanticDomain;

            new UpdateWordTask(getContext()).execute(word);

        } catch (Exception e) {
            Log.e(TAG, "Null pointer exception - failed to find ListView and/or EditText object", e);
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
        WordDAO wDAO;
        int position;
        int total;

        LoadWordTask(View entryPage, int pos, int tot) {
            entryPageRef = new WeakReference<>(entryPage);
            wDAO = AppDatabase.get(entryPage.getContext()).wordDAO();
            position = pos;
            total = tot;
        }

        @Override
        protected FilledWord doInBackground(Long... wordIDs) {
            if (wordIDs == null || wordIDs.length != 1) {
                Log.e(TAG, "Did not receive exactly 1 wordID");
                return null;
            }

            // Needed for context to load bitmap.
            View entryPage = entryPageRef.get();
            if (entryPage == null) {
                return null;
            }

            FilledWord word = wDAO.getFilled(wordIDs[0]);
            if (word != null){
                word.loadImageDataScaled(entryPage.getContext());
            }

            return word;
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
                word
            ));
        }
    }

    /**
     * Performs background task to update meanings for lexical entries in database.
     */
    private static class UpdateWordTask extends AsyncTask<FilledWord, Void, Void> {
        private AppDatabase db;

        //Constructor.
        UpdateWordTask(Context context) {
            db = AppDatabase.get(context);
        }

        @Override
        protected Void doInBackground(FilledWord... words) {
            if (words == null || words.length != 1) {
                Log.e(TAG, "Did not receive exactly 1 FilledWord");
                return null;
            }

            MeaningDAO mDAO = db.meaningDAO();
            WordDAO wDAO = db.wordDAO();

            db.beginTransaction();
            FilledWord filledWord = words[0];
            boolean updateDate = false;
            try {
                for (Meaning meaning : filledWord.meanings) {
                    Meaning currentState = mDAO.getByType(meaning.wordID, meaning.type);
                    if (currentState == null) {
                        // New entry? Insert.
                        mDAO.insertMeanings(meaning);
                        updateDate = true;
                    }
                    else {
                        // Pull the ID for update key.
                        meaning.id = currentState.id;
                        if (!meaning.data.equals(currentState.data)) {
                            updateDate = true;
                        }
                        mDAO.updateMeanings(meaning);
                    }
                }

                Word word = wDAO.get(filledWord.id);
                if (!word.semanticDomain.equals(filledWord.semanticDomain)) {
                    updateDate = true;
                    word.semanticDomain = filledWord.semanticDomain;
                }

                if (updateDate) {
                    // Something changed for this word.
                    word.updatedAt = new Date();
                }

                wDAO.updateWords(word);

                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(
                    TAG,
                    "Error updating meanings",
                    e
                );
            } finally {
                // Commit or rollback the database.
                db.endTransaction();
            }

            return null;
        }
    }

    private static class EntryFieldAdapter extends BaseAdapter {
        private static final String MEANING_VIEW_TAG = "meaning";
        private static final String SEMANTIC_VIEW_TAG = "semantic";
        private static final String IMAGE_VIEW_TAG = "image";

        // static so we only parse once.
        private static List<String> semanticDomainList = null;

        private LayoutInflater inflater;
        private FilledWord word;

        EntryFieldAdapter(LayoutInflater flate, FilledWord filledWord) {
            inflater = flate;
            word = filledWord;
        }

        @Override
        public int getCount() {
            // +1 for semantic domain.

            if (word.picture == null) {
                return word.meanings.size() + 1;
            }

            // Last element is an ImageView.
            return word.meanings.size() + 2;
        }

        @Override
        public Meaning getItem(int pos) {
            return word.meanings.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup container) {
            if (pos == word.meanings.size()) {
                return getSemanticDomainView(convertView, container);
            }
            else if (pos == word.meanings.size() + 1) {
                // Photo shown last.
                return getImageView(convertView);
            }

            return getMeaningView(pos, convertView, container);
        }

        private View getMeaningView(int pos, View convertView, ViewGroup container) {
            if (convertView == null || !convertView.getTag().equals(MEANING_VIEW_TAG)) {
                convertView = inflater.inflate(
                    R.layout.entry_field_item,
                    container,
                    false
                );
                convertView.setTag(MEANING_VIEW_TAG);
            }

            Meaning meaning = getItem(pos);
            if (meaning != null) {
                TextView langCodeField = convertView.findViewById(R.id.lang_code);
                langCodeField.setText(meaning.type);

                EditText langDataField = convertView.findViewById(R.id.lang_data);
                langDataField.setText(meaning.data);
            }

            return convertView;
        }

        private View getImageView(View convertView) {
            if (convertView == null || !convertView.getTag().equals(IMAGE_VIEW_TAG)) {
                convertView = new ImageView(inflater.getContext());
                convertView.setTag(IMAGE_VIEW_TAG);
            }

            ImageView display = (ImageView)convertView;
            display.setImageBitmap(word.imageData);

            return display;
        }

        private View getSemanticDomainView(View convertView, ViewGroup container) {
            if (convertView == null || !convertView.getTag().equals(SEMANTIC_VIEW_TAG)) {
                convertView = inflater.inflate(
                    R.layout.entry_field_semantic_domain_line,
                    container,
                    false
                );
                convertView.setTag(SEMANTIC_VIEW_TAG);
            }

            AutoCompleteTextView aCTV = convertView.findViewById(R.id.semantic_domain_auto_complete);
            aCTV.setAdapter(new ArrayAdapter<>(
                convertView.getContext(),
                android.R.layout.simple_dropdown_item_1line,
                getSemanticDomains()
            ));

                if ( !word.semanticDomain.equals("None") ) {
                    aCTV.setText(word.semanticDomain);
                } else { // Not sure if this will ever run, but added as a precaution. If "" is read, "None" is inserted into db
                    aCTV.setText("");
                }

            return convertView;
        }

        /* Returns a List of semantic domains from the JSON file of semantic domains from Language Forge

         */
        private List<String> getSemanticDomains() {
            if (semanticDomainList != null) {
                return semanticDomainList;
            }

            InputStream is = null;
            BufferedReader br = null;

            AssetManager assets = inflater.getContext().getAssets();
            semanticDomainList = new ArrayList<>();
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
                String file = sb.toString();

                JSONObject jsonObjectCluster = new JSONObject( file.trim() );
                Iterator<String> keys = jsonObjectCluster.keys();

                while( keys.hasNext() ) {
                    String key = keys.next();
                    if ( jsonObjectCluster.get(key) instanceof JSONObject ) {
                        semanticDomainList.add( ((JSONObject) jsonObjectCluster.get(key)).getString("name") );
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

            return semanticDomainList;
        }
    }
}

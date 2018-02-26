package org.sil.gatherwords;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
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

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.FilledWord;
import org.sil.gatherwords.room.Meaning;
import org.sil.gatherwords.room.MeaningDAO;
import org.sil.gatherwords.room.SemanticDomain;
import org.sil.gatherwords.room.SemanticDomainDAO;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDAO;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;


public class EntryFragment extends Fragment {
    private static final String TAG = EntryFragment.class.getSimpleName();

    private static final String ARG_WORD_ID = "wordID";
    private static final String ARG_POSITION = "position";
    private static final String ARG_TOTAL = "total";

    private int m_wordID;  // Database ID of the word displayed word.
    private int m_position; // The index of the displayed word (0-indexed).
    private int m_total;    // Total number of words.

    public static EntryFragment newInstance(int wordID, int position, int total) {
        EntryFragment fragment = new EntryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_WORD_ID, wordID);
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
            m_wordID = args.getInt(ARG_WORD_ID);
            m_position = args.getInt(ARG_POSITION);
            m_total = args.getInt(ARG_TOTAL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View entryPage = inflater.inflate(R.layout.fragment_entry, container, false);

        new LoadWordTask(entryPage).execute(m_wordID);
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
            EntryFieldAdapter adapter = (EntryFieldAdapter) entryFields.getAdapter();

            new UpdateWordTask(getContext()).execute(adapter.word);
        } catch (Exception e) {
            Log.e(TAG, "Null pointer exception - failed to find ListView and/or EditText object", e);
        }
    }

    /**
     * Gets the current wordID
     *
     * It seems m_wordID is not set when returning from taking a picture.
     * @return The wordID of the current Word
     */
    public int getWordID() {
        return getArguments().getInt(ARG_WORD_ID);
    }

    private static class LoadWordTask extends AsyncTask<Integer, Void, FilledWord> {
        WeakReference<View> entryPageRef;
        WordDAO wDAO;

        LoadWordTask(View entryPage) {
            entryPageRef = new WeakReference<>(entryPage);
            wDAO = AppDatabase.get(entryPage.getContext()).wordDAO();
        }

        @Override
        protected FilledWord doInBackground(Integer... wordIDs) {
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
            SemanticDomainDAO sdDAO = db.semanticDomainDAO();

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
                SemanticDomain semanticDomain = sdDAO.getByName(
                    filledWord.semanticDomain
                );
                if (semanticDomain == null) {
                    if (word.semanticDomainID != null) {
                        updateDate = true;
                        word.semanticDomainID = null;
                    }
                }
                else if (word.semanticDomainID == null || semanticDomain.id != word.semanticDomainID) {
                    updateDate = true;
                    word.semanticDomainID = semanticDomain.id;
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

    static class EntryFieldAdapter extends BaseAdapter {
        private static final String MEANING_VIEW_TAG = "meaning";
        private static final String SEMANTIC_VIEW_TAG = "semantic";
        private static final String AUDIO_VIEW_TAG = "audio";
        private static final String IMAGE_VIEW_TAG = "image";

        private LayoutInflater inflater;
        FilledWord word;

        EntryFieldAdapter(LayoutInflater flate, FilledWord filledWord) {
            inflater = flate;
            word = filledWord;
        }

        @Override
        public int getCount() {
            // +1 for semantic domain.
            // +1 for audio status.

            if (word.picture == null) {
                return word.meanings.size() + 2;
            }

            // Last element is an ImageView.
            return word.meanings.size() + 3;
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
                return getAudioView(convertView, container);
            }
            else if (pos == word.meanings.size() + 2) {
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

                // Remove the existing listener since this
                // view can be reused.
                if (langDataField.getTag() != null) {
                    langDataField.removeTextChangedListener(
                        (TextWatcher) langDataField.getTag()
                    );
                }

                langDataField.setText(meaning.data);

                TextWatcher watcher = new MeaningTextWatcher(meaning);
                langDataField.addTextChangedListener(watcher);
                langDataField.setTag(watcher);
            }

            return convertView;
        }

        private View getAudioView(View convertView, ViewGroup container) {
            if (convertView == null || !convertView.getTag().equals(AUDIO_VIEW_TAG)) {
                convertView = inflater.inflate(
                        R.layout.entry_audio_status,
                        container,
                        false
                );
                convertView.setTag(AUDIO_VIEW_TAG);
            }

            TextView audioStatus = convertView.findViewById(R.id.audio_status);
            if (word.audio == null) {
                audioStatus.setText(R.string.no_audio);
            }
            else {
                audioStatus.setText(R.string.has_audio);
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

            // Remove the existing listener since this
            // view can be reused.
            if (aCTV.getTag() != null) {
                aCTV.removeTextChangedListener(
                    (TextWatcher) aCTV.getTag()
                );
            }

            aCTV.setText(word.semanticDomain);
            new FillSemanticDomainTask(aCTV).execute();

            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable s) {
                    word.semanticDomain = s.toString();
                }
            };

            aCTV.addTextChangedListener(watcher);
            aCTV.setTag(watcher);

            return convertView;
        }
    }

    private static class FillSemanticDomainTask extends AsyncTask<Void, Void, List<SemanticDomain>> {
        private WeakReference<AutoCompleteTextView> aCTVRef;

        FillSemanticDomainTask(AutoCompleteTextView aCTV) {
            aCTVRef = new WeakReference<>(aCTV);
        }

        @Override
        protected List<SemanticDomain> doInBackground(Void... voids) {
            AutoCompleteTextView aCTV = aCTVRef.get();
            if (aCTV == null) {
                return null;
            }

            AppDatabase db = AppDatabase.get(aCTV.getContext());
            return db.semanticDomainDAO().getAll();
        }

        @Override
        protected void onPostExecute(List<SemanticDomain> semanticDomains) {
            AutoCompleteTextView aCTV = aCTVRef.get();
            if (semanticDomains == null || aCTV == null) {
                return;
            }

            // TODO: Prevent text not in this list from being entered.
            aCTV.setAdapter(new ArrayAdapter<>(
                aCTV.getContext(),
                android.R.layout.simple_dropdown_item_1line,
                semanticDomains
            ));
        }
    }

    private static class MeaningTextWatcher implements TextWatcher {
        private Meaning meaning;

        MeaningTextWatcher(Meaning m) {
            meaning = m;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            meaning.data = s.toString();
        }
    }
}

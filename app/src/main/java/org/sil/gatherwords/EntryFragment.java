package org.sil.gatherwords;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.FilledWord;
import org.sil.gatherwords.room.Meaning;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDao;

import java.lang.ref.WeakReference;
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
            if (wordIDs == null || wordIDs.length != 1)  {
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
            TextView pageStatus = entryPage.findViewById(R.id.page_status);
            pageStatus.setText(
                context.getString(R.string.entry_status_line, "Word", position + 1, total)
            );


            ListView entryFields = entryPage.findViewById(R.id.entry_fields);
            entryFields.setAdapter( new EntryFieldAdapter( ((EntryActivity) entryPage.getContext() ).getLayoutInflater(), word.meanings ) );

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
        private LayoutInflater inflater;
        private List<Meaning> meaningList;

        EntryFieldAdapter(LayoutInflater flate, List<Meaning> meanList) {
            inflater = flate;
            meaningList = meanList;
        }

        @Override
        public int getCount() {
            return meaningList.size();
        }

        @Override
        public Meaning getItem(int i) {
            return meaningList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = inflater.inflate(
                        R.layout.entry_field_item,
                        container,
                        false
                );

            };

            Meaning meaning = getItem(i);
                    if (meaning != null) {
                        TextView langCodeField = convertView.findViewById(R.id.lang_code);
                        langCodeField.setText(meaning.type);

                        EditText langDataField = convertView.findViewById(R.id.lang_data);
                        langDataField.setText(meaning.data);
                    }

            return convertView;
        }
    }
}

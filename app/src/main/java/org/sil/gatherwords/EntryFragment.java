package org.sil.gatherwords;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.sil.gatherwords.room.AppDatabase;
import org.sil.gatherwords.room.Word;
import org.sil.gatherwords.room.WordDao;

import java.lang.ref.WeakReference;


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

    private static class LoadWordTask extends AsyncTask<Long, Void, Word> {
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
        protected Word doInBackground(Long... wordIDs) {
            if (wordIDs == null || wordIDs.length != 1)  {
                Log.e(LoadWordTask.class.getSimpleName(), "Did not receive exactly 1 wordID");
                return null;
            }

            return wDAO.get(wordIDs[0]);
        }

        @Override
        protected void onPostExecute(Word word) {
            View entryPage = entryPageRef.get();
            if (entryPage == null || word == null) {
                return;
            }

            Context context = entryPage.getContext();

            // TODO: Fill with real data from `word`.

            ListView entryFields = entryPage.findViewById(R.id.entry_fields);
            String[] langs = {"lang1", "lang2"};
            entryFields.setAdapter(new ArrayAdapter<String>(context, 0, langs) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    if (convertView == null) {
                        convertView = LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.entry_field_item,
                            parent,
                            false
                        );
                    }

                    TextView langCodeField = convertView.findViewById(R.id.lang_code);
                    langCodeField.setText(getItem(position));

                    return convertView;
                }
            });
        }
    }
}

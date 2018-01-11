package org.sil.gatherwords;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class EntryFragment extends Fragment {
    private static final String ARG_POSITION = "position";
    private static final String ARG_TOTAL = "total";

    private int m_position; // The index of the displayed word (0-indexed).
    private int m_total;    // Total number of words.

    public static EntryFragment newInstance(int position, int total) {
        EntryFragment fragment = new EntryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        args.putInt(ARG_TOTAL, total);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            m_position = getArguments().getInt(ARG_POSITION);
            m_total = getArguments().getInt(ARG_TOTAL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View entryPage = inflater.inflate(R.layout.fragment_entry, container, false);
        TextView pageStatus = entryPage.findViewById(R.id.page_status);
        pageStatus.setText(
            getString(R.string.entry_status_line, "Word", m_position + 1, m_total)
        );

        ListView entryFields = entryPage.findViewById(R.id.entry_fields);
        String[] langs = {"lang1", "lang2"};
        entryFields.setAdapter(new ArrayAdapter<String>(getContext(), 0, langs) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(
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

        return entryPage;
    }

}

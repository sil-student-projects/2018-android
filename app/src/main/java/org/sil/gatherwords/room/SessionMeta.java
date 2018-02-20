package org.sil.gatherwords.room;

import android.arch.persistence.room.Embedded;

import java.util.Date;

public class SessionMeta {
    public int id;
    public Date date;
    public String speaker;
    public String label;

    @Embedded
    public Progress progress;
}

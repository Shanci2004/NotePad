package com.example.android.notepad;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MyCursorAdapter extends SimpleCursorAdapter {
    private Context mContext;
    private int mLayout;
    private Cursor mCursor;
    private final LayoutInflater mInflater;

    public MyCursorAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to) {
        super(context, layout, cursor, from, to);
        mContext = context;
        mLayout = layout;
        mCursor = cursor;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(mLayout, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // 获取标签列的索引
        int tagIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TAG);
        String tag = cursor.getString(tagIndex);

        // 根据标签设置背景颜色
        int backgroundColor;
        switch (tag) {
            case "bill":
                backgroundColor = Color.parseColor("#FFF59D"); // 浅黄色
                break;
            case "study":
                backgroundColor = Color.parseColor("#90CAF9"); // 浅蓝色
                break;
            default: // normal 或其他
                backgroundColor = Color.WHITE;
                break;
        }
        view.setBackgroundColor(backgroundColor);

        // 设置其他数据，标题、时间
        TextView titleView = view.findViewById(android.R.id.text1);
        TextView modifiedDateView = view.findViewById(R.id.modified_date);
        TextView tagView = view.findViewById(R.id.tag_label);

        int titleIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
        int modifiedDateIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);

        String title = cursor.getString(titleIndex);
        String modifiedDate = cursor.getString(modifiedDateIndex);

        if (titleView != null) {
            titleView.setText(title);
        }
        if (modifiedDateView != null) {
            modifiedDateView.setText(modifiedDate);
        }
        if (tag != null){
            tagView.setText(tag);
        }

        switch (tag) {
            case "bill":
                tagView.setBackgroundColor(Color.parseColor("#FFA726")); // 橙色
                break;
            case "study":
                tagView.setBackgroundColor(Color.parseColor("#29B6F6")); // 蓝色
                break;
            case "normal":
            default:
                tagView.setBackgroundColor(Color.parseColor("#BDBDBD")); // 灰色
                break;
        }
    }
}

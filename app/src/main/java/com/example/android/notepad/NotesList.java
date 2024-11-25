/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toolbar;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;

public class NotesList extends ListActivity {

    private SearchView searchView;//搜索框控件实例
    private ListView lv_notesList;
    private FrameLayout ll_noteList;
    private Toolbar toolbar;
    //优化ui及功能
    private FloatingActionButton mainFab;
    private LinearLayout subMenu;
    private boolean isMenuOpen = false;
    private FloatingActionButton colorFab;
    private LinearLayout colorMenu;
    private boolean isColorMenuOpen = false;

    // For logging and debugging
    private static final String TAG = "NotesList";

    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, //2
            NotePad.Notes.COLUMN_NAME_TAG,   //3
    };

    private static final int COLUMN_INDEX_TITLE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.noteslist_layout);
        initView();

        //恢复背景颜色
        restoreBackgroundColor();

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        getListView().setOnCreateContextMenuListener(this);

        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        // The names of the cursor columns to display in the view, initialized to the title column
        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE};

        // The view IDs that will display the cursor columns, initialized to the TextView in
        // noteslist_item.xml
        int[] viewIDs = { android.R.id.text1, R.id.modified_date};

        // Creates the backing adapter for the ListView.
        MyCursorAdapter adapter
            = new MyCursorAdapter(
                      this,                             // The Context for the ListView
                      R.layout.noteslist_item,          // Points to the XML for a list item
                      cursor,                           // The cursor to get items from
                      dataColumns,
                      viewIDs
              );

        // Sets the ListView's adapter to be the cursor adapter that was just created.
        setListAdapter(adapter);
    }

    public void initView(){
        ll_noteList= (FrameLayout) findViewById(R.id.noteList_layout);
        lv_notesList= (ListView) findViewById(android.R.id.list);//绑定listView;
        searchView= (SearchView) findViewById(R.id.Search);
        mainFab = (FloatingActionButton) findViewById(R.id.mainFab);
        subMenu = (LinearLayout) findViewById(R.id.subMenu);
        toolbar = findViewById(R.id.toolbar);
        colorFab = (FloatingActionButton) findViewById(R.id.fabOption_color);
        colorMenu = (LinearLayout) findViewById(R.id.colorMenu);

        setActionBar(toolbar);  //设置toolbar为当前activity的actionbar

        searchView.setIconifiedByDefault(false);    //设置searchview的searchbox默认显示，否则需要点击放大镜icon才会显示searchbox
        searchView.setSubmitButtonEnabled(true);    //在searchview的右侧设置一个提交按钮（箭头），focus输入的box后会出现，其实不需要，因为输入后listview设置为自动更新了，只是好看而已
        // Get the SearchView and set the searchable configuration
        searchView.setQueryHint("输入标题查找");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Perform final search when user submits
                performSearch(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Perform search as user types
                performSearch(newText);
                return true;
            }
        });

        mainFab.setOnClickListener(v -> toggleSubMenu());

        // 其它小圆圈按钮的点击事件
        findViewById(R.id.fabOption_add).setOnClickListener(v -> handleOption_Add());
        findViewById(R.id.fabOption_paste).setOnClickListener(v -> handleOption_Paste());

        colorFab.setOnClickListener(v -> toggleColorMenu());
        //颜色按钮点击事件
        setupColorButtons();
    }

    private void toggleSubMenu() {
        if (isMenuOpen) {
            // 子菜单向下缩小并收起
            subMenu.animate()
                    .translationY(mainFab.getHeight()) // 移动靠近主按钮
                    .scaleX(0)       // 水平缩小到 0
                    .scaleY(0)       // 垂直缩小到 0
                    .setDuration(300) // 动画时长
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            subMenu.setVisibility(View.GONE); // 动画结束后隐藏
                        }
                    });
        } else {
            // 子菜单向上展开并放大
            subMenu.setVisibility(View.VISIBLE); // 显示视图
            subMenu.setScaleX(0); // 初始化为缩小状态
            subMenu.setScaleY(0);
            subMenu.setTranslationY(mainFab.getHeight()); // 从主按钮位置开始

            subMenu.animate()
                    .translationY(0) // 向上移动距离等于自身高度
                    .scaleX(1)       // 水平放大到 1
                    .scaleY(1)       // 垂直放大到 1
                    .setDuration(300) // 动画时长
                    .setListener(null); // 无额外操作
        }
        isMenuOpen = !isMenuOpen; // 切换菜单状态
    }


    // Option1 的点击事件
    private void handleOption_Add() {
        toggleSubMenu();   // 收起主菜单
        startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
    }

    // Option2 的点击事件
    private void handleOption_Paste() {
        toggleSubMenu();   // 收起主菜单
        startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
    }

    //处理颜色按钮显示
    private void toggleColorMenu() {
        if (isColorMenuOpen) {
            colorMenu.animate()
                    .translationX(colorFab.getWidth())
                    .scaleX(0)
                    .scaleY(0)
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            colorMenu.setVisibility(View.GONE);
                        }
                    });
        } else {
            colorMenu.setVisibility(View.VISIBLE);
            colorMenu.setScaleX(0);
            colorMenu.setScaleY(0);
            colorMenu.setTranslationX(colorFab.getWidth());

            colorMenu.animate()
                    .translationX(0)
                    .scaleX(1)
                    .scaleY(1)
                    .setDuration(300)
                    .setListener(null);
        }
        isColorMenuOpen = !isColorMenuOpen;
    }


    private void setupColorButtons() {
        int[] buttonIds = {R.id.colorButton1, R.id.colorButton2, R.id.colorButton3, R.id.colorButton4, R.id.colorButton5};
        int[] colors = {
                Color.parseColor("#A8E6CF"),
                Color.parseColor("#FFD3B6"),
                Color.parseColor("#D4C5F9"),
                Color.parseColor("#ECEFF1"),
                Color.WHITE
        };

        for (int i = 0; i < buttonIds.length; i++) {
            int color = colors[i];
            findViewById(buttonIds[i]).setOnClickListener(v -> {
                // 设置背景颜色
                lv_notesList.setBackgroundColor(color);

                // 保存背景颜色到 SharedPreferences
                saveBackgroundColor(color);

                // 收起 colorMenu 和 subMenu
                toggleColorMenu(); // 收起颜色菜单
                toggleSubMenu();   // 收起主菜单
            });
        }
    }

    // 保存背景颜色到 SharedPreferences
    private void saveBackgroundColor(int color) {
        SharedPreferences preferences = getSharedPreferences("app_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("background_color", color); // 将背景颜色保存为整数
        editor.apply();
    }

    // 恢复背景颜色
    private void restoreBackgroundColor() {
        SharedPreferences preferences = getSharedPreferences("app_preferences", MODE_PRIVATE);
        int savedColor = preferences.getInt("background_color", Color.WHITE); // 默认白色
        lv_notesList.setBackgroundColor(savedColor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);


        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.setEnabled(false);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                Menu.NONE,                  // A unique item ID is not required.
                Menu.NONE,                  // The alternatives don't need to be in order.
                null,                       // The caller's name is not excluded from the group.
                specifics,                  // These specific options must appear first.
                intent,                     // These Intent objects map to the options in specifics.
                Menu.NONE,                  // No flags are required.
                items                       // The menu items generated from the specifics-to-
                                            // Intents mapping
            );
                // If the Edit menu item exists, adds shortcuts for it.
                if (items[0] != null) {

                    // Sets the Edit menu item shortcut to numeric "1", letter "e"
                    items[0].setShortcut('1', 'e');
                }
            } else {
                // If the list is empty, removes any existing alternative actions from the menu
                menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
            }

        // Displays the menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                /*
                 * Launches a new Activity using an Intent. The intent filter for the Activity
                 * has to have action ACTION_INSERT. No category is set, so DEFAULT is assumed.
                 * In effect, this starts the NoteEditor Activity in NotePad.
                 */
                startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
                return true;
            case R.id.menu_paste:
                /*
                 * Launches a new Activity using an Intent. The intent filter for the Activity
                 * has to have action ACTION_PASTE. No category is set, so DEFAULT is assumed.
                 * In effect, this starts the NoteEditor Activity in NotePad.
                 */
                startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
                return true;
//            case R.id.menu_day:
//
//            case R.id.menu_night:


            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * Gets the data associated with the item at the selected position. getItem() returns
         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
         * the adapter associated all of the data for a note with its list item. As a result,
         * getItem() returns that data as a Cursor.
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(), 
                                        Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        switch (item.getItemId()) {
            case R.id.context_open:
                // Launch activity to view/edit the currently selected item
                startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
                return true;
//BEGIN_INCLUDE(copy)
            case R.id.context_copy:
                // Gets a handle to the clipboard service.
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);

                // Copies the notes URI to the clipboard. In effect, this copies the note itself
                clipboard.setPrimaryClip(ClipData.newUri(   // new clipboard item holding a URI
                        getContentResolver(),               // resolver to retrieve URI info
                        "Note",                             // label for the clip
                        noteUri)                            // the URI
                );

                // Returns to the caller and skips further processing.
                return true;
//END_INCLUDE(copy)
            case R.id.context_delete:

                // Deletes the note from the provider by passing in a URI in note ID format.
                // Please see the introductory note about performing provider operations on the
                // UI thread.
                getContentResolver().delete(
                        noteUri,  // The URI of the provider
                        null,     // No where clause is needed, since only a single note ID is being
                        // passed in.
                        null      // No where clause is used, so no where arguments are needed.
                );

                // Returns to the caller and skips further processing.
                return true;
            default:
                return super.onContextItemSelected(item);
        }

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }

    private void performSearch(String query) {
        // 定义搜索条件
        String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%"};

        // 执行查询
        Cursor cursor = getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        // 确保游标数据加载正确
        if (cursor != null) {
            // 获取当前的适配器
            SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();

            // 用新的游标替换旧游标
            adapter.changeCursor(cursor);

            // 通知 ListView 数据已更改
            adapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "Search query returned no results or encountered an error.");
        }
    }

}

# NotePad
实现以下功能：
* NoteList界面中笔记条目增加时间戳显示
* 添加笔记查询功能（按照标题查询）
* 笔记分类添加标签
* UI美化

## 一 NoteList界面中笔记条目增加时间戳显示
### 1、首先对notes_list_item.xml中加入一个TextView由于显示时间戳
设置一个TextView并与原本标题的TextView（id为"@android:id/text1"）在一个垂直分布的线性布局下。
```
<TextView
    android:id="@+id/modified_date"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:gravity="right"
    android:text="time"
    android:textSize="15sp" />
```
这是新的notes_list_item.xml的样式，左下角显示时间戳
![image](images/2.png)
### 2、在NotesList的Activity中设置ListView新的item的配置显示，增加时间戳的配置。
#### (1)查询时间戳
首先为了能够获取到时间戳，在NotesList的PROJECTION中添加`NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, //2`以查询到修改时间。
#### (2)装配adapter以符合新的item布局
由于添加了修改时间，我们通过修改dataColumns和viewIDs添加一个对时间戳的from和to，来配置到新的item布局中。
```
String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE};
int[] viewIDs = { android.R.id.text1, R.id.modified_date};
```
#### (3)存在问题：时间戳显示并非年月日时分秒，而是毫秒数
由于Notes表格中存入的是System.currentTimeMillis()，显示的是一串长数字（如下图），代表从1970年1月1日开始计算的总毫秒数。
![image](images/1.png)
为了直观看到笔记编辑的条目时间，我们追寻到创建和编辑笔记时，在何时添加的修改时间，因此我们在NoteEditor中寻找代码逻辑，最终发现在NoteEditor中，不论是`mState == STATE_EDIT`还是`mState == STATE_INSERT`都会调用updateNote方法，在updateNote方法中，下列代码存入了当前时间的系统时间毫秒数
```
ContentValues values = new ContentValues();
values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
```
#### (4)修改时间戳的存储
我们通过SimpleDateFormat对这个时间毫秒数进行format化，对`System.currentTimeMillis()`进行规范化后，将规范化后的formattedDate存入到数据库中
```
SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
String formattedDate = dateFormat.format(new Date(System.currentTimeMillis()));
ContentValues values = new ContentValues();
values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, formattedDate);
```
### 结果
最终实现时间戳的显示为"yyyy-MM-dd HH:mm:ss"格式
![image](images/3.png)
## 二 添加笔记查询功能（按照标题查询）
### 1、首先在NotesList的布局中添加一个SearchView
#### (1)存在问题：NotesList是继承ListActivity的Activity，它并没有布局
分析代码发现，在NotesList中并没有setContentView方法来绑定一个布局，布局文件中也没有对应的布局，因此我们需要自己创建一个NotesList布局，里面包含ListView以及我们要实现的搜索功能的组件SearchView
##### 仍存在问题一：通过自己设置布局来绑定到NotesList，启动app会报错闪退
**解决** ListActivity自身就有一个布局，其布局中包含一个ListView。如果要对ListActivity设置布局，就需要对我们设置的布局里面的ListView的id设置为`android:id="@android:id/list"`。
```
        <androidx.appcompat.widget.SearchView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:id="@+id/Search" />

        <ListView
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:id="@android:id/list" />
```
##### 仍存在问题二：启动app仍然会报错闪退
**发现原因** 根据LogCat的报错发现，SearchView并不支持AppCompat或Material以外的主题。
**解决** 因此，在AndroidManifest.xml文件中，添加theme
```
<application
    android:icon="@drawable/app_notes"
    android:label="@string/app_name"
    android:theme="@style/Theme.AppCompat.DayNight.DarkActionBar">
```
##### 仍存在问题三：启动app后发现，在这个主题下，ActionBar消失了
明明采用的是DarkActionBar的主题，可是对应的ActionBar却消失
**解决** 这里我们采用ToolBar来代替ActionBar
首先在noteslist_layout.xml中加入一个ToolBar，在线性布局的垂直布局下，第一栏最顶部添加ToolBar
```
<android.widget.Toolbar
    android:id="@+id/toolbar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/LightGray"
    android:theme="@style/Theme.AppCompat.DayNight.DarkActionBar"/>
```
然后在NotesList中实现一个initView()方法，在initView方法中获取toolbar然后通过setActionBar()方法，将toolbar与ActionBar设置，以匹配ActionBar，将其对应的菜单也实现在ToolBar上
```
toolbar = findViewById(R.id.toolbar);
setActionBar(toolbar);
```
#### 最终布局效果
![image](images/4.png)
### 2、在NotesList实现SearchView的功能
#### (1)细节设置
通过setIconifiedByDefault()方法设置searchview的searchbox默认显示，否则需要点击放大镜icon才会显示searchbox。
通过setSubmitButtonEnabled()方法添加一个提交按钮（箭头），当用户focus输入的box后会出现这个箭头。
```
searchView.setIconifiedByDefault(false);
searchView.setSubmitButtonEnabled(true);
```
![image](images/5.png)
#### (2)对SearchView的搜索功能编辑
对于SearchView需要实现onQueryTextSubmit()方法用于对输入的文本提交的进行的逻辑操作以及onQueryTextChange()方法用于对输入的文本发生改变的进行的逻辑操作
对于onQueryTextSubmit()方法我们实现了performSearch()方法，然后取消对searchView的聚焦。
而对于onQueryTextChange()方法我们只进行performSearch()方法。这样的逻辑更加符合搜索的1）确认搜索以及2）输入时匹配
```
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
```
在performSearch()方法中，我们进行了模糊查询，定义了模糊查询标题的查询条件
```
String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
String[] selectionArgs = new String[]{"%" + query + "%"};
```
通过ContentResolver的query方法，将获取的结果存入cursor中，然后将新获取的cursor替换，同时通知ListView数据已更改
```
Cursor cursor = getContentResolver().query(
        NotePad.Notes.CONTENT_URI,
        PROJECTION,
        selection,
        selectionArgs,
        NotePad.Notes.DEFAULT_SORT_ORDER
);
if (cursor != null) {
    SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
    adapter.changeCursor(cursor);
    adapter.notifyDataSetChanged();
} else {
    Log.e(TAG, "Search query returned no results or encountered an error.");
}
```
### 结果
最终实现查询，可以进行模糊查询，也可以精准查询。
![image](images/7.png)
## 三 笔记分类添加标签
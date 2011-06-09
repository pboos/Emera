/*
 * Copyright (C) 2011 Tonchidot Corporation. All rights reserved.
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

package ch.pboos.emera;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class ContactsProvider extends ContentProvider {
    private static final String packageName = "ch.pboos.emera";
    public static final Uri CONTENT_URI = Uri.parse("content://" + packageName + "/contacts");

    private static final String TAG = ContactsProvider.class.getSimpleName();
    private static final String DATABASE_NAME = "contacter.db";
    private static final int DATABASE_VERSION = 1;
    private static final String CONTACTS_TABLE = "history";

    // Column names
    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_DATE = "timestamp";
    public static final String KEY_LATITUDE = "lat";
    public static final String KEY_LONGITUDE = "lon";
    public static final String KEY_VCARD = "vcard";

    // Column indexes
    public static final int NAME_COLUMN = 1;
    public static final int DATE_COLUMN = 2;
    public static final int LATITUDE_COLUMN = 3;
    public static final int LONGITUDE_COLUMN = 4;
    public static final int VCARD_COLUMN = 5;

    private static final String DATABASE_CREATE = "CREATE TABLE " + CONTACTS_TABLE + " (" + KEY_ID
            + " INTEGER primary key autoincrement, " + KEY_NAME + " VARCHAR(100) not null, "
            + KEY_DATE + " INTEGER not null, " + KEY_LATITUDE + " FLOAT, " + KEY_LONGITUDE
            + " FLOAT not null, " + KEY_VCARD + " TEXT not null);";

    private SQLiteDatabase db;

    public static class HistoryDbHelper extends SQLiteOpenHelper {
        public HistoryDbHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase _db) {
            _db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase _db, int oldVersion, int newVersion) {
            _db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_TABLE);
            onCreate(_db);
        }
    }

    // Constants to differntiate URI requests
    private static final int CONTACTS = 1;
    private static final int CONTACT_ID = 2;

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(packageName, "contacts", CONTACTS);
        uriMatcher.addURI(packageName, "contacts/#", CONTACT_ID);
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case CONTACTS:
                return "vnd.android.cursor.dir/vnd.emera.contact";
            case CONTACT_ID:
                return "vnd.android.cursor.item/vnd.emera.contact";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        HistoryDbHelper dbHelper = new HistoryDbHelper(context, DATABASE_NAME, null,
                DATABASE_VERSION);
        db = dbHelper.getWritableDatabase();
        return db != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(CONTACTS_TABLE);
        switch (uriMatcher.match(uri)) {
            case CONTACT_ID:
                qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                break;
        }

        String orderBy = TextUtils.isEmpty(sortOrder) ? KEY_DATE + " DESC" : sortOrder;
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowId = db.insert(CONTACTS_TABLE, "contact", values);
        if (rowId > 0) {
            Uri insertedUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(insertedUri, null);
            return insertedUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        int count;
        switch (uriMatcher.match(uri)) {
            case CONTACTS:
                count = db.delete(CONTACTS_TABLE, where, whereArgs);
                break;
            case CONTACT_ID:
                String segment = uri.getPathSegments().get(1);
                count = db.delete(CONTACTS_TABLE,
                        KEY_ID + "=" + segment
                                + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""),
                        whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count;
        switch (uriMatcher.match(uri)) {
            case CONTACTS:
                count = db.update(CONTACTS_TABLE, values, where, whereArgs);
                break;

            case CONTACT_ID:
                String segment = uri.getPathSegments().get(1);
                count = db.update(CONTACTS_TABLE, values,
                        KEY_ID + "=" + segment
                                + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""),
                        whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    // public long insertEntry(HistoryEntry toInsert) {
    // ContentValues values = new ContentValues();
    // values.put(KEY_NAME, toInsert.name);
    // values.put(KEY_DATE, toInsert.timestamp);
    // values.put(KEY_LATITUDE, toInsert.latitude);
    // values.put(KEY_LONGITUDE, toInsert.longitude);
    // values.put(KEY_VCARD, toInsert.vcard);
    // return db.insert(CONTACTS_TABLE, null, values);
    // }
    //
    // public boolean removeEntry(long id) {
    // return db.delete(CONTACTS_TABLE, KEY_ID + "=" + id, null) > 0;
    // }
    //
    // public Cursor getAllEntries() {
    // return getEntries(null);
    // }
    //
    // public Cursor getEntries(String limit) {
    // return db.query(CONTACTS_TABLE, new String[] {
    // KEY_ID, KEY_NAME, KEY_DATE, KEY_LATITUDE, KEY_LONGITUDE, KEY_VCARD
    // }, null, null, null, null, KEY_DATE + " DESC", limit);
    // }
    //
    // public Cursor getEntriesLast10Minutes() {
    // Calendar past = Calendar.getInstance();
    // past.add(Calendar.MINUTE, -10);
    //
    // return db.query(CONTACTS_TABLE, new String[] {
    // KEY_ID, KEY_NAME, KEY_DATE, KEY_LATITUDE, KEY_LONGITUDE, KEY_VCARD
    // }, KEY_DATE + ">" + past.getTimeInMillis(), null, null, null, KEY_DATE +
    // " DESC");
    // }
    //
    // public HistoryEntry getEntry(long id) {
    // Cursor cursor = db.query(CONTACTS_TABLE, new String[] {
    // KEY_ID, KEY_NAME, KEY_DATE, KEY_LATITUDE, KEY_LONGITUDE, KEY_VCARD
    // }, KEY_ID + "=" + id, null, null, null, null);
    // if (cursor.moveToNext()) {
    // HistoryEntry entry = HistoryEntry.createFromCursor(cursor);
    // cursor.close();
    // return entry;
    // }
    // cursor.close();
    // return null;
    // }
    //
    // public boolean updateEntry(long id, HistoryEntry newEntry) {
    // ContentValues values = new ContentValues();
    // values.put(KEY_ID, id);
    // values.put(KEY_NAME, newEntry.name);
    // values.put(KEY_DATE, newEntry.timestamp);
    // values.put(KEY_LATITUDE, newEntry.latitude);
    // values.put(KEY_LONGITUDE, newEntry.longitude);
    // values.put(KEY_VCARD, newEntry.vcard);
    // return db.update(CONTACTS_TABLE, values, KEY_ID + "=" + id, null) > 0;
    // }
}

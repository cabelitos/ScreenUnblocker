package com.iscaro.screenunblocker.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;


public class ScreenUnblockerContentProvider extends ContentProvider {

    private SqlHelper mDb;

    private static final int KNOWN_NETWORKS = 0;
    private static final int KNOWN_NETWORKS_ID = 1;
    private static final String AUTHORITY = "com.iscaro.screen_unblocker.content_provider";
    public static final Uri KNOWN_NETWORKS_URI = Uri.parse("content://" + AUTHORITY + "/" +
            SqlHelper.KNOWN_NETWORKS_TABLE);

    private static final UriMatcher mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        mMatcher.addURI(AUTHORITY, SqlHelper.KNOWN_NETWORKS_TABLE, KNOWN_NETWORKS);
        mMatcher.addURI(AUTHORITY, SqlHelper.KNOWN_NETWORKS_TABLE + "/#", KNOWN_NETWORKS_ID);
    }

    private String[] appendToSelectionArgs(String toAppend, String[] selectionArgs) {
        if (selectionArgs == null) {
            return new String[] { toAppend };
        } else {
            String [] array = new String[selectionArgs.length + 1];
            array[0] = toAppend;
            System.arraycopy(selectionArgs, 0, array, 1, selectionArgs.length);
            return array;
        }
    }

    private String appendToSelection(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SqlHelper.COLUMN_ID + " = ?";
        } else {
            return SqlHelper.COLUMN_ID + " = ?" + selection;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int type = mMatcher.match(uri);
        SQLiteDatabase db = mDb.getWritableDatabase();
        int deleted;
        String newSelection;
        String [] newSelectionArgs;
        switch (type) {
            case KNOWN_NETWORKS:
                deleted = db.delete(SqlHelper.KNOWN_NETWORKS_TABLE, selection, selectionArgs);
                break;
            case KNOWN_NETWORKS_ID:
                String id = uri.getLastPathSegment();
                newSelection = appendToSelection(selection);
                newSelectionArgs = appendToSelectionArgs(id, selectionArgs);
                deleted = db.delete(SqlHelper.KNOWN_NETWORKS_TABLE, newSelection, newSelectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Incorrect URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return deleted;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int type = mMatcher.match(uri);
        SQLiteDatabase db = mDb.getWritableDatabase();
        long id;
        switch (type) {
            case KNOWN_NETWORKS:
                id = db.insert(SqlHelper.KNOWN_NETWORKS_TABLE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Incorrect URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(SqlHelper.KNOWN_NETWORKS_TABLE + "/" + id);
    }

    @Override
    public boolean onCreate() {
        mDb = new SqlHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        int type = mMatcher.match(uri);
        SQLiteDatabase db = mDb.getReadableDatabase();

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(SqlHelper.KNOWN_NETWORKS_TABLE);
        String newSelection = selection;
        String [] newSelectionArgs = selectionArgs;

        switch (type) {
            case KNOWN_NETWORKS:
                break;
            case KNOWN_NETWORKS_ID:
                String id = uri.getLastPathSegment();
                newSelection = appendToSelection(selection);
                newSelectionArgs = appendToSelectionArgs(id, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Incorrect URI: " + uri);
        }
        Cursor  cursor = builder.query(db, projection, newSelection, newSelectionArgs,
                null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {        int updated;
        int type = mMatcher.match(uri);
        SQLiteDatabase db = mDb.getWritableDatabase();
        String newSelection;
        String [] newSelectionArgs;
        switch (type) {
            case KNOWN_NETWORKS:
                updated = db.update(SqlHelper.KNOWN_NETWORKS_TABLE, values,
                        selection, selectionArgs);
                break;
            case KNOWN_NETWORKS_ID:
                String id = uri.getLastPathSegment();
                newSelection = appendToSelection(selection);
                newSelectionArgs = appendToSelectionArgs(id, selectionArgs);
                updated = db.update(SqlHelper.KNOWN_NETWORKS_TABLE, values,
                        newSelection, newSelectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Incorrect URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return updated;
    }
}

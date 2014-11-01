package com.iscaro.screenunblocker.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by iscaro on 10/19/14.
 */
public class SqlHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ScreenUnblocker";
    private static final int DATABASE_VERSION = 1;

    public static final String KNOWN_NETWORKS_TABLE = "known_networks";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SSID = "ssid";
    public static final String COLUMN_ADDRESS = "address";

    public SqlHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String create = "CREATE TABLE " + KNOWN_NETWORKS_TABLE + " ( " + COLUMN_ID + " integer " +
                "primary key autoincrement," + COLUMN_SSID + " text not null, " +
                COLUMN_ADDRESS + " text not null, unique ( " + COLUMN_ID + "," + COLUMN_ADDRESS + " ));";
        db.execSQL(create);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + KNOWN_NETWORKS_TABLE);
    }
}

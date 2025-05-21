package com.example.myapplication.wifi;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * WiFi连接历史数据库帮助类
 * 用于管理WiFi连接历史记录的SQLite数据库
 */
public class WiFiHistoryDatabaseHelper extends SQLiteOpenHelper {
    // 数据库名称和版本
    public static final String DATABASE_NAME = "WiFiHistory.db";
    public static final int DATABASE_VERSION = 1;
    // 表名
    public static final String TABLE_NAME = "wifi_history";

    // 表结构字段
    public static final String COLUMN_ID = "_id";           // 自增主键ID
    public static final String COLUMN_SSID = "ssid";       // WiFi名称
    public static final String COLUMN_IP = "ip_address";   // IP地址
    public static final String COLUMN_CONNECT_TIME = "connect_time"; // 连接时间（时间戳）

    /**
     * 构造函数
     * @param context 应用上下文
     */
    public WiFiHistoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * 创建数据库表
     * @param db SQLite数据库实例
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL语句：创建WiFi历史记录表
        String createTableSQL = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + // 自增主键
                COLUMN_SSID + " TEXT NOT NULL, " +                   // WiFi名称（非空）
                COLUMN_IP + " TEXT, " +                               // IP地址（可为空）
                COLUMN_CONNECT_TIME + " LONG NOT NULL)";             // 连接时间（非空）
        db.execSQL(createTableSQL); // 执行SQL创建表
    }

    /**
     * 数据库升级时调用
     * @param db         SQLite数据库实例
     * @param oldVersion 旧版本号
     * @param newVersion 新版本号
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 升级策略：删除旧表并重新创建
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db); // 重新创建表
    }
}
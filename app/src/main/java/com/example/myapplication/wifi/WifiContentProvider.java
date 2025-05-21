package com.example.myapplication.wifi;

import android.Manifest;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * WiFi信息内容提供者，用于通过ContentProvider接口向外提供当前连接的WiFi信息
 */
public class WifiContentProvider extends ContentProvider {

    // 内容提供者的唯一标识
    public static final String AUTHORITY = "com.example.myapplication.wifi.wificontentprovider";
    // 内容URI，外部应用通过此URI访问WiFi信息
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/wifi");

    // URI匹配码，用于标识访问WiFi信息的请求
    private static final int WIFI = 1;
    // URI匹配器，用于解析传入的URI
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        // 初始化URI匹配器，将"wifi"路径映射到WIFI匹配码
        uriMatcher.addURI(AUTHORITY, "wifi", WIFI);
    }

    private WifiManager wifiManager; // WiFi管理器实例

    /**
     * 初始化ContentProvider，获取系统WiFi服务
     * @return 初始化成功返回true
     */
    @Override
    public boolean onCreate() {
        wifiManager = (WifiManager) getContext().getSystemService(getContext().WIFI_SERVICE);
        return true;
    }

    /**
     * 处理查询请求，返回当前连接的WiFi信息
     * @param uri 请求的URI
     * @param projection 需要返回的列
     * @param selection 查询条件
     * @param selectionArgs 查询条件参数
     * @param sortOrder 排序方式
     * @return 包含WiFi信息的Cursor
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        // 检查应用是否拥有必要的权限
        if (!checkPermissions()) {
            return null; // 权限不足时返回空
        }

        // 验证URI是否合法
        if (uriMatcher.match(uri) != WIFI) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // 获取当前连接的WiFi信息
        WifiInfo wifiInfo = null;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            wifiInfo = wifiManager.getConnectionInfo();
        }

        // 创建返回结果的Cursor，定义返回的列
        MatrixCursor cursor = new MatrixCursor(new String[]{"wifi_name", "rssi", "ip_address", "link_speed"});

        // 如果WiFi已启用且获取到连接信息，则添加数据到Cursor
        if (wifiManager.isWifiEnabled() && wifiInfo != null) {
            MatrixCursor.RowBuilder row = cursor.newRow();
            String ssid = processSSID(wifiInfo.getSSID()); // 处理SSID格式，去除引号
            row.add(ssid); // 添加WiFi名称
            row.add(wifiInfo.getRssi()); // 添加信号强度
            row.add(intToIp(wifiInfo.getIpAddress())); // 添加IP地址
            row.add(wifiInfo.getLinkSpeed()); // 添加连接速度
        }

        return cursor; // 返回包含WiFi信息的Cursor
    }

    /**
     * 检查应用是否拥有必要的权限
     * @return 有权限时返回true，否则返回false
     */
    private boolean checkPermissions() {
        // 检查定位权限（Android 6.0+需要）
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Android 10+ 需要额外的ACCESS_WIFI_STATE权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_WIFI_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        return true;
    }

    /**
     * 处理SSID字符串，去除可能包含的引号
     * @param ssid 原始SSID字符串
     * @return 处理后的SSID字符串
     */
    private String processSSID(String ssid) {
        if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            return ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    /**
     * 将整数形式的IP地址转换为点分十进制字符串格式
     * @param ip 整数IP地址
     * @return 点分十进制字符串（如"192.168.1.1"）
     */
    private String intToIp(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >> 24) & 0xFF);
    }

    // 以下方法未实现，因为本ContentProvider仅用于查询WiFi信息
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
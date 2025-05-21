package com.example.myapplication.wifi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private TextView wifiInfoTextView;
    private ListView wifiListView;
    private Button scanButton;
    private Button historyButton;
    private Button contentProviderButton; // 新增：通过ContentProvider获取信息的按钮
    private WifiManager wifiManager;
    private ArrayAdapter<String> listAdapter;
    private List<ScanResult> scanResults = new ArrayList<>();
    private ConnectivityManager.NetworkCallback networkCallback;
    private WiFiHistoryDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main7);

        // 初始化UI组件
        wifiInfoTextView = findViewById(R.id.wifi_info_text);
        wifiListView = findViewById(R.id.wifi_list_view);
        scanButton = findViewById(R.id.scan_button);
        historyButton = findViewById(R.id.history_button);
        contentProviderButton = findViewById(R.id.content_provider_button); // 新增按钮

        // 初始化WiFi管理器和列表适配器
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        wifiListView.setAdapter(listAdapter);
        initDatabase(); // 初始化数据库

        // 检查位置权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            initWiFi();
        }

        // 设置按钮点击事件
        scanButton.setOnClickListener(v -> startScan());
        historyButton.setOnClickListener(v -> showHistoryDialog());
        contentProviderButton.setOnClickListener(v -> queryWifiContentProvider()); // 新增按钮点击事件

        // 设置WiFi列表项点击事件
        wifiListView.setOnItemClickListener((parent, view, position, id) -> {
            String ssid = listAdapter.getItem(position);
            if (ssid != null && !ssid.equals("未发现可用WiFi")) {
                showPasswordDialog(ssid);
            }
        });

        // 注册WiFi扫描结果广播接收器
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                    displayScanResults();
                }
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    /**
     * 初始化数据库
     */
    private void initDatabase() {
        dbHelper = new WiFiHistoryDatabaseHelper(this);
    }

    /**
     * 初始化WiFi功能
     */
    private void initWiFi() {
        //现在已经不行了，Android10以下可以用
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
//        updateCurrentWiFiInfo();
        queryWifiContentProvider();
    }

    /**
     * 开始扫描WiFi
     */
    private void startScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请先授予位置权限", Toast.LENGTH_SHORT).show();
            return;
        }

        //扫描附近的 WiFi 热点。
        wifiManager.startScan();
    }

    /**
     * 显示扫描结果
     */
    private void displayScanResults() {
        //返回扫描到的热点列表（List<ScanResult>）
        scanResults = wifiManager.getScanResults();

        List<String> wifiList = new ArrayList<>();
        Set<String> ssidSet = new HashSet<>();

        for (ScanResult result : scanResults) {
            String ssid = result.SSID;
            if (ssid != null && !ssid.isEmpty() && !ssidSet.contains(ssid)) {
                ssidSet.add(ssid);
                wifiList.add(ssid);
            }
        }

        listAdapter.clear();
        listAdapter.addAll(wifiList.isEmpty() ? List.of("未发现可用WiFi") : wifiList);
        listAdapter.notifyDataSetChanged();
    }

    /**
     * 显示WiFi密码输入对话框
     * @param ssid WiFi名称
     */
    private void showPasswordDialog(String ssid) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("连接到 " + ssid)
                .setMessage("请输入密码：")
                .setView(input)
                .setPositiveButton("连接", (dialog, which) -> {
                    String password = input.getText().toString();
                    connectToWifi(ssid, password);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 连接到指定WiFi
     * @param ssid WiFi名称
     * @param password WiFi密码
     */
    private void connectToWifi(String ssid, String password) {
        // 构建WiFi网络规范
        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build();

        // 构建网络请求
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();

        //ConnectivityManager 负责网络连接的建立和管理
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // 取消之前的网络回调
        if (networkCallback != null) {
            cm.unregisterNetworkCallback(networkCallback);
        }

        // 设置新的网络回调
        //通过 ConnectivityManager 请求连接
        //通过 NetworkCallback 监听连接成功或失败事件
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                cm.bindProcessToNetwork(network);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "连接成功: " + ssid, Toast.LENGTH_SHORT).show();
                    updateCurrentWiFiInfo();
                    // 保存连接历史
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    String ip = intToIp(wifiInfo.getIpAddress());
                    saveConnectionHistory(ssid, ip);
                });
            }
        };

        // 请求网络连接
        cm.requestNetwork(request, networkCallback);
    }

    /**
     * 更新当前WiFi连接信息显示
     */
    private void updateCurrentWiFiInfo() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = removeQuotes(wifiInfo.getSSID());
        String ip = intToIp(wifiInfo.getIpAddress());
        String speed = wifiInfo.getLinkSpeed() + " Mbps";

        if (ssid == null || ssid.equals("<unknown ssid>")) {
            wifiInfoTextView.setText("未连接WiFi");
        } else {
            wifiInfoTextView.setText("当前连接: " + ssid + "\nIP地址: " + ip + "\n速度: " + speed);
        }
    }

    /**
     * 移除SSID中的引号
     * @param ssid 带引号的SSID
     * @return 不带引号的SSID
     */
    private String removeQuotes(String ssid) {
        if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            return ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    /**
     * 将整数IP转换为点分十进制格式
     * @param ip 整数IP
     * @return 点分十进制IP字符串
     */
    private String intToIp(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >> 24) & 0xFF);
    }

    // === 历史记录相关方法 ===

    /**
     * 保存WiFi连接历史记录到数据库
     * @param ssid WiFi名称
     * @param ip IP地址
     */
    private void saveConnectionHistory(String ssid, String ip) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(WiFiHistoryDatabaseHelper.COLUMN_SSID, ssid);
        values.put(WiFiHistoryDatabaseHelper.COLUMN_IP, ip);
        values.put(WiFiHistoryDatabaseHelper.COLUMN_CONNECT_TIME, System.currentTimeMillis());
        db.insert(WiFiHistoryDatabaseHelper.TABLE_NAME, null, values);
        db.close();
    }

    /**
     * 获取WiFi连接历史记录
     * @return 历史记录列表，每个记录包含SSID、IP和连接时间
     */
    private List<Map<String, String>> getConnectionHistory() {
        List<Map<String, String>> historyList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] columns = {
                WiFiHistoryDatabaseHelper.COLUMN_SSID,
                WiFiHistoryDatabaseHelper.COLUMN_IP,
                WiFiHistoryDatabaseHelper.COLUMN_CONNECT_TIME
        };
        Cursor cursor = db.query(
                WiFiHistoryDatabaseHelper.TABLE_NAME,
                columns,
                null,
                null,
                null,
                null,
                WiFiHistoryDatabaseHelper.COLUMN_CONNECT_TIME + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            do {
                Map<String, String> item = new HashMap<>();
                item.put("ssid", cursor.getString(cursor.getColumnIndex(WiFiHistoryDatabaseHelper.COLUMN_SSID)));
                item.put("ip", cursor.getString(cursor.getColumnIndex(WiFiHistoryDatabaseHelper.COLUMN_IP)));
                item.put("time", sdf.format(new Date(cursor.getLong(cursor.getColumnIndex(WiFiHistoryDatabaseHelper.COLUMN_CONNECT_TIME)))));
                historyList.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return historyList;
    }

    /**
     * 显示WiFi连接历史记录对话框
     */
    private void showHistoryDialog() {
        List<Map<String, String>> historyList = getConnectionHistory();
        if (historyList.isEmpty()) {
            Toast.makeText(this, "暂无连接历史", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> displayList = new ArrayList<>();
        for (Map<String, String> item : historyList) {
            displayList.add(
                    "SSID: " + item.get("ssid") + "\n" +
                            "IP: " + item.get("ip") + "\n" +
                            "时间: " + item.get("time") + "\n" +
                            "------------------------"
            );
        }

        new AlertDialog.Builder(this)
                .setTitle("WiFi连接历史记录")
                .setItems(displayList.toArray(new String[0]), null)
                .setNegativeButton("关闭", null)
                .show();
    }

    /**
     * 通过ContentProvider查询WiFi信息
     */
    private void queryWifiContentProvider() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请先授予位置权限", Toast.LENGTH_SHORT).show();
            return;
        }

        Cursor cursor = getContentResolver().query(
                WifiContentProvider.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String ssid = cursor.getString(cursor.getColumnIndex("wifi_name"));

            // 判断SSID是否为空或未知
            if (ssid == null || ssid.isEmpty() || ssid.equals("<unknown ssid>")) {
                wifiInfoTextView.setText("未连接到WiFi");
            } else {
                // SSID有效时，继续获取其他信息
                int rssi = cursor.getInt(cursor.getColumnIndex("rssi"));
                String ip = cursor.getString(cursor.getColumnIndex("ip_address"));
                int linkSpeed = cursor.getInt(cursor.getColumnIndex("link_speed"));

                String info = "通过ContentProvider获取：\n" +
                        "SSID: " + ssid + "\n" +
                        "信号强度: " + rssi + " dBm\n" +
                        "IP地址: " + ip + "\n" +
                        "连接速度: " + linkSpeed + " Mbps";
                wifiInfoTextView.setText(info);
            }
        } else {
            wifiInfoTextView.setText("未获取到WiFi信息（通过ContentProvider）");
        }
//        if (cursor != null && cursor.moveToFirst()) {
//            String ssid = cursor.getString(cursor.getColumnIndex("wifi_name"));
//            int rssi = cursor.getInt(cursor.getColumnIndex("rssi"));
//            String ip = cursor.getString(cursor.getColumnIndex("ip_address"));
//            int linkSpeed = cursor.getInt(cursor.getColumnIndex("link_speed"));
//
//            String info = "通过ContentProvider获取：\n" +
//                    "SSID: " + ssid + "\n" +
//                    "信号强度: " + rssi + " dBm\n" +
//                    "IP地址: " + ip + "\n" +
//                    "连接速度: " + linkSpeed + " Mbps";
//            wifiInfoTextView.setText(info);
//        } else {
//            wifiInfoTextView.setText("未获取到WiFi信息（通过ContentProvider）");
//        }
        cursor.close();
    }

    /**
     * 权限请求结果回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initWiFi();
            } else {
                Toast.makeText(this, "需要位置权限才能扫描WiFi", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * Activity销毁时释放资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.unregisterNetworkCallback(networkCallback);
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
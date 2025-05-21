package com.example.myapplication.wifi;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

/**
 * 后台服务类，用于定期获取WiFi连接信息并通过广播发送更新
 */
public class WifiInfoService extends Service {

    private static final String TAG = "WifiInfoService";
    private static final int UPDATE_INTERVAL = 5000; // 更新间隔为5秒
    private WifiManager wifiManager; // WiFi管理器
    private boolean isRunning = false; // 服务运行状态标志
    private Thread updateThread; // 更新线程

    /**
     * 服务创建时调用，初始化WiFi管理器
     */
    @Override
    public void onCreate() {
        super.onCreate();
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        Log.d(TAG, "Service created");
    }

    /**
     * 服务启动时调用，开始定期更新WiFi信息
     * @param intent 启动服务的Intent
     * @param flags 启动标志
     * @param startId 启动ID
     * @return 服务启动模式
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        if (!isRunning) {
            startUpdateThread(); // 启动更新线程
        }

        return START_STICKY; // 服务被杀死后自动重启
    }

    /**
     * 启动更新线程
     */
    private void startUpdateThread() {
        isRunning = true;
        updateThread = new Thread(this::updatePeriodically);
        updateThread.start();
    }

    /**
     * 周期性更新WiFi信息的线程方法
     */
    private void updatePeriodically() {
        while (isRunning) {
            try {
                Thread.sleep(UPDATE_INTERVAL); // 休眠指定时间
                if (checkLocationPermission()) {
                    sendUpdateBroadcast(); // 发送WiFi信息更新广播
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Update thread interrupted", e);
                isRunning = false;
                break;
            }
        }
    }

    /**
     * 发送WiFi信息更新广播
     */
    private void sendUpdateBroadcast() {
        if (!wifiManager.isWifiEnabled() || !checkLocationPermission()) {
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Intent intent = new Intent("com.example.myapplication.wifi.WIFI_INFO_UPDATED");

        // 添加WiFi信息到广播中
        if (wifiInfo != null) {
            intent.putExtra("ssid", wifiInfo.getSSID());
            intent.putExtra("ipAddress", wifiInfo.getIpAddress());
            intent.putExtra("rssi", wifiInfo.getRssi());
            intent.putExtra("linkSpeed", wifiInfo.getLinkSpeed());
        }

        sendBroadcast(intent); // 发送广播
    }

    /**
     * 检查是否有定位权限（Android 6.0+需要定位权限才能获取WiFi信息）
     * @return 有权限时返回true，否则返回false
     */
    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 绑定服务时调用（本服务不支持绑定）
     * @param intent 绑定Intent
     * @return null
     */
    @Override
    public IBinder onBind(Intent intent) { return null; }

    /**
     * 服务销毁时调用，停止更新线程
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        isRunning = false;
        if (updateThread != null) {
            updateThread.interrupt(); // 中断更新线程
        }
    }
}
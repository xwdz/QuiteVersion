package com.xwdz.quietversion.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;

import com.xwdz.quietversion.QuietVersion;
import com.xwdz.quietversion.Utils;
import com.xwdz.quietversion.callback.OnProgressListener;
import com.xwdz.quietversion.callback.OnUINotify;
import com.xwdz.quietversion.ui.UIAdapter;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author huangxingwei (xwdz9989@gmail.com)
 * @since 2018/5/3
 */
public class VersionHandler {

    private static final String TAG = VersionHandler.class.getSimpleName();

    private ApkInstall mApkInstall;
    private DownloadTask mDownloadTask;
    private UIAdapter mUIAdapter;
    private ExecutorService mExecutorService;

    private StartDownloadReceiver mDownloadReceiver;
    private Context mContext;

    private FragmentActivity mFragmentActivity;
    private QuietVersion.QuiteEntry mQuiteEntry;
    /**
     * 本地是否存在缓存Apk
     */
    private boolean mApkLocalIsExist;


    public static VersionHandler get(Context context, QuietVersion.QuiteEntry entry) {
        return new VersionHandler(context, entry);
    }


    private VersionHandler(Context context, QuietVersion.QuiteEntry entry) {
        mContext = context;
        mExecutorService = Executors.newFixedThreadPool(3);
        checkURLNotNull(entry.getUrl());

        mQuiteEntry = entry;
        createModule();
        mDownloadTask.setUrl(mQuiteEntry.getUrl());
        mDownloadTask.setOnProgressListener(mOnProgressListener);
        mDownloadTask.setFilePath(mQuiteEntry.getApkPath());
        mApkLocalIsExist = mQuiteEntry.checkApkExits();
        handlerApk();
    }


    private void createModule() {
        mApkInstall = new ApkInstall(mContext);
        mUIAdapter = new UIAdapter(mContext);
        mDownloadTask = new DownloadTask();
        mDownloadReceiver = new StartDownloadReceiver();
        mContext.registerReceiver(mDownloadReceiver, new IntentFilter(START_DOWNLOAD_ACTION));
        Utils.LOG.i(TAG, "组件初始化完毕 ...");
    }

    private void handlerApk() {
        if (CheckUpgradeVersion.get().check(mQuiteEntry.getRemoteVersionCode())) {
            if (mApkLocalIsExist && !mQuiteEntry.isForceDownload()) {
                Utils.LOG.i(TAG, "读取到本地缓存APk = " + mQuiteEntry.getApkPath() + " 开始安装...");
                mApkInstall.install(mQuiteEntry.getApkPath());
            } else {
                final OnUINotify onUINotify = mQuiteEntry.getOnUINotify();
                if (onUINotify != null) {
                    final String note = mQuiteEntry.getNote();
                    onUINotify.show(note);

                    try {
                        if (mFragmentActivity != null) {
                            FragmentManager fragmentManager = mFragmentActivity.getSupportFragmentManager();
                            if (fragmentManager != null) {
                                onUINotify.show(note, fragmentManager);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Utils.LOG.e(TAG, "get fragmentManager error = " + e);
                    }
                } else {
                    mUIAdapter.showUpgradeDialog(mQuiteEntry.getNote(), mQuiteEntry.getActivityClass());
                }

            }
        } else {
            Utils.LOG.i(TAG, "未发现最新Apk版本 " + mQuiteEntry.getUrl());
        }
    }

    /**
     * 执行下载Apk操作
     */
    private void doDownload() {
        if (CheckUpgradeVersion.get().check(mQuiteEntry.getRemoteVersionCode())) {
            /* 是否强制每次都从网络上下载最新apk */
            if (!mQuiteEntry.isForceDownload()) {
                if (mApkLocalIsExist) {
                    Utils.LOG.i(TAG, "读取到本地缓存APk = " + mQuiteEntry.getApkPath() + " 开始安装...");
                    mApkInstall.install(mQuiteEntry.getApkPath());
                    return;
                }
            }

            mExecutorService.execute(mDownloadTask);
            Utils.LOG.i(TAG, "开始下载服务器apk ...");
        } else {
            Utils.LOG.i(TAG, "未发现最新Apk版本 " + mQuiteEntry.getUrl());
        }
    }


    private void checkURLNotNull(String url) {
        if (TextUtils.isEmpty(url)) {
            throw new NullPointerException("remote apk url cannot be null !");
        }
    }

    public static void startDownloadApk(Context context) {
        Intent intent = new Intent(START_DOWNLOAD_ACTION);
        intent.putExtra(KEY_START_DOWN, FLAG_START_DOWN);
        context.sendBroadcast(intent);
    }


    private final OnProgressListener mOnProgressListener = new OnProgressListener() {

        @Override
        public void onTransfer(int percent, long currentLength, long total) {
            updateProgress(mContext, total, currentLength, percent);
        }

        @Override
        public void onFinished(File file) {
            Utils.LOG.i(TAG, "download file done ...");
            mApkInstall.install(file.getAbsolutePath());
        }
    };


    private static final String START_DOWNLOAD_ACTION = "com.xwdz.checkupdate.core.VersionHandler";
    private static final String KEY_START_DOWN = "start_download";
    private static final int FLAG_START_DOWN = 1;

    public class StartDownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int flag = intent.getIntExtra(KEY_START_DOWN, 0);
            if (flag == FLAG_START_DOWN) {
                doDownload();
            }
        }
    }


    public static final String UPDATE_PROGRESSBAR_ACTION = "com.xwdz.quietversion.ui.ProgressDialogActivity";

    private static final String KEY_TOTAL = "total";
    private static final String KEY_CURRENT_LENGTH = "currentlength";
    private static final String KEY_PERCENT = "percent";

    public abstract static class ProgressReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long total = intent.getLongExtra(KEY_TOTAL, 0);
            long currentLength = intent.getLongExtra(KEY_CURRENT_LENGTH, 0);
            int percent = intent.getIntExtra(KEY_PERCENT, 0);

            onUpdateProgress(total, currentLength, percent);

        }

        public abstract void onUpdateProgress(long total, long currentLength, int percent);
    }


    public static void registerProgressbarReceiver(Context context, ProgressReceiver progressReceiver) {
        if (progressReceiver != null) {
            context.getApplicationContext().registerReceiver(progressReceiver, new IntentFilter(VersionHandler.UPDATE_PROGRESSBAR_ACTION));
        }
    }

    public static void unregisterProgressbarReceiver(Context context, ProgressReceiver progressReceiver) {
        if (progressReceiver != null) {
            context.getApplicationContext().unregisterReceiver(progressReceiver);
        }
    }

    private static void updateProgress(Context context, long total, long currentLength, int percent) {
        Intent intent = new Intent(UPDATE_PROGRESSBAR_ACTION);
        intent.putExtra(KEY_TOTAL, total);
        intent.putExtra(KEY_CURRENT_LENGTH, currentLength);
        intent.putExtra(KEY_PERCENT, percent);
        context.sendBroadcast(intent);
    }


    public void recycle() {
        if (mDownloadReceiver != null) {
            mContext.getApplicationContext().unregisterReceiver(mDownloadReceiver);
        }
    }
}
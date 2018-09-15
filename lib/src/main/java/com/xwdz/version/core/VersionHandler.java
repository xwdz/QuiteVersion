package com.xwdz.version.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import com.xwdz.version.Utils;
import com.xwdz.version.callback.OnCheckVersionRules;
import com.xwdz.version.callback.OnProgressListener;
import com.xwdz.version.entry.ApkSource;
import com.xwdz.version.ui.UIAdapter;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author huangxingwei (xwdz9989@gmail.com)
 * @since 2018/5/3
 */
public class VersionHandler {

    private static final String TAG = VersionHandler.class.getSimpleName();

    private DownloadTask mDownloadTask;
    private UIAdapter mUIAdapter;
    private ExecutorService mExecutorService;

    private StartDownloadReceiver mDownloadReceiver;
    private Context mContext;

    private ApkSource mApkSource;
    /**
     * 本地是否存在缓存Apk
     */
    private boolean mApkLocalIsExist;

    private VersionConfigs mVersionConfigs;


    public static VersionHandler get(Context context, ApkSource entry) {
        return new VersionHandler(context, entry);
    }


    private VersionHandler(Context context, ApkSource apkSource) {
        mVersionConfigs = VersionConfigs.getImpl();
        mVersionConfigs.initContext(context, apkSource.getUrl());
        mContext = context;
        mExecutorService = Executors.newFixedThreadPool(3);
        checkURLNotNull(apkSource.getUrl());

        mApkSource = apkSource;
        createModule();
        mDownloadTask.setUrl(mApkSource.getUrl());
        mDownloadTask.setOnProgressListener(mOnProgressListener);
        mDownloadTask.setFilePath(mVersionConfigs.getApkPath());
        mApkLocalIsExist = mVersionConfigs.checkApkExits();
        handlerApk();
    }


    private void createModule() {
        mUIAdapter = new UIAdapter(mContext);
        mDownloadTask = new DownloadTask();
        mDownloadReceiver = new StartDownloadReceiver();
        mContext.registerReceiver(mDownloadReceiver, new IntentFilter(START_DOWNLOAD_ACTION));
        Utils.LOG.i(TAG, "组件初始化完毕 ...");
    }

    private void handlerApk() {
        OnCheckVersionRules onCheckVersionRules = mVersionConfigs.getOnCheckVersionRules();
        if (onCheckVersionRules != null) {
            boolean handler = onCheckVersionRules.check(mApkSource);
            if (handler) {
                if (mApkLocalIsExist && !mVersionConfigs.isForceDownload()) {
                    String path = mVersionConfigs.getApkPath();
                    Utils.LOG.i(TAG, "读取到本地缓存APk = " + path + " 开始安装...");
                    ApkInstallUtils.doInstall(mContext, path);
                } else {
//                    final OnUIDialogNotify onUIDialogNotify = mVersionConfigs.getOnUIDialogNotify();
//                    if (onUIDialogNotify != null) {
//                        onUIDialogNotify.show(mApkSource,);
//                        try {
//                            if (mFragmentActivity != null) {
//                                FragmentManager fragmentManager = mFragmentActivity.getSupportFragmentManager();
//                                if (fragmentManager != null) {
//                                    onUIDialogNotify.show(note, fragmentManager);
//                                }
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            Utils.LOG.e(TAG, "get fragmentManager error = " + e);
//                        }
//                    } else {
                    mUIAdapter.showUpgradeDialog(mApkSource, mVersionConfigs.getUIActivityClass());
//                    }
                }
            } else {
                Utils.LOG.i(TAG, "当前暂未发现新版本...");
            }
        } else {
            Utils.LOG.i(TAG, "未发现最新Apk版本 " + mApkSource.getUrl());
        }
    }

    /**
     * 执行下载Apk操作
     */
    private void doDownload() {
        OnCheckVersionRules onCheckVersionRules = mVersionConfigs.getOnCheckVersionRules();
        if (onCheckVersionRules != null) {
            if (!mVersionConfigs.isForceDownload()) {
                if (mApkLocalIsExist) {
                    Utils.LOG.i(TAG, "读取到本地缓存APk = " + mVersionConfigs.getApkPath() + " 开始安装...");
                    ApkInstallUtils.doInstall(mContext, mVersionConfigs.getApkPath());
                    return;
                }
            }

            mExecutorService.execute(mDownloadTask);
            Utils.LOG.i(TAG, "开始下载服务器apk ...");
        }
    }


    private void checkURLNotNull(String url) {
        if (TextUtils.isEmpty(url)) {
            throw new NullPointerException("remote apk url cannot be null !");
        }
    }

    private final OnProgressListener mOnProgressListener = new OnProgressListener() {

        @Override
        public void onTransfer(int percent, long currentLength, long total) {
            updateProgress(mContext, total, currentLength, percent);
        }

        @Override
        public void onFinished(File file) {
            Utils.LOG.i(TAG, "download file done ...");
            ApkInstallUtils.doInstall(mContext, file.getAbsolutePath());
        }
    };


    private static final String START_DOWNLOAD_ACTION = "com.xwdz.version.core.VersionHandler";
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


    private static final String UPDATE_PROGRESSBAR_ACTION = "com.xwdz.qversion.ui.DefaultProgressDialogActivity";

    private static final String KEY_TOTAL = "total";
    private static final String KEY_CURRENT_LENGTH = "current.length";
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


    public static void startDownloader(Context context) {
        Intent intent = new Intent(START_DOWNLOAD_ACTION);
        intent.putExtra(KEY_START_DOWN, FLAG_START_DOWN);
        context.sendBroadcast(intent);
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

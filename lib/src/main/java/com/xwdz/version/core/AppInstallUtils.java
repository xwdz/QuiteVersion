package com.xwdz.version.core;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;

import com.xwdz.version.callback.onErrorListener;
import com.xwdz.version.utils.LOG;

import java.io.File;

/**
 * 执行Apk安装操作行为
 *
 * @author huangxingwei (xwdz9989@gmail.com)
 * @since v0.0.1
 */
public class AppInstallUtils {

    private static final String TAG = AppInstallUtils.class.getSimpleName();

    private AppInstallUtils() {
    }


    public static Intent getSystemInstallIntent(Context context, String apkPath, onErrorListener onErrorListener) {
        File   file   = new File(apkPath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }


    public static void doInstall(Context context, String apkPath, onErrorListener onErrorListener) {
        if (!apkPath.endsWith(".apk")) {
            LOG.e(TAG, "install error path = " + apkPath);
            return;
        }

        try {
            File   file   = new File(apkPath);
            Intent intent = new Intent(Intent.ACTION_VIEW);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            if (onErrorListener != null) {
                onErrorListener.listener(e);
            }
        }

    }
}

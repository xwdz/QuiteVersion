### QuiteVersion

用于Android App检测自动更新


### 实现效果
![WeChatSight112.gif](https://upload-images.jianshu.io/upload_images/2651056-8c50665d70685c18.gif?imageMogr2/auto-orient/strip)

### 添加依赖

```
    implementation 'com.xwdz:QuiteVersion:0.0.4'
    implementation 'com.xwdz:okHttpUtils:1.0.4'
    //如已依赖可忽略
    implementation 'com.squareup.okhttp3:okhttp:3.5.0'
```

### 特点

- 任何地方都可以调用
- 支持自定义界面
- 支持强制下载最新Apk
- 自调起安装界面
- 简单
- 支持OKHttp拦截器
- 适配7.0


### 简单使用

```
    DialogTest dialogTest = DialogTest.newInstance();
        Quite.getInstance(this)
                //or POST
                .GET("http://www.baidu.com")
                //强制每次更新都从网络下载最新Apk
                .setForceDownload(true)
                .setApkPath()
                .setApkName()
                .addHeader()
                .setShowUIActivity()
                .addParams()
                .addInterceptor()
                .addNetworkInterceptor()
                //UI容器需实现OnUINotify接口
                .setNotifyHandler(dialogTest)
                .setOnNetworkParserListener(new OnNetworkParserListener() {
                    @Override
                    public ApkSource parser(String response) {
                        return new ApkSource(
                                kugou,
                                "更新内容如下\n1.你好\n2.我不好",
                                123123123,
                                123,
                                9999
                        );
                    }
                })
                .apply();


Quite.getInstance(this).recycle()
```

### 注意

####  开发者必须实现此接口,返回QuiteVersion需要的Apk信息,如果返回null,则视为没有新版本更新

```
setOnNetworkParserListener(new OnNetworkParserListener() {
                          @Override
                          public ApkSource parser(String response) {
                              return null;
                          }
                      })
```


####  QuiteVersion 执行更新App策略
     - **ApkSource.remoteVersionCode > 当前版本code**
     - **todo**


#### setApkName 以及setApkPath 说明

- **QuiteVersion 默认实现路径**为`context.getExternalFilesDir("apk").getAbsolutePath() + File.separator + apkFilename`

- **QuiteVersion 默认实现文件名称为`Url最后一个/ 至 .apk`,如酷狗 http://download.kugou.com/download/kugou_android`
      `ApkName为kugou_android.apk`**


#### 自定义容器Dialog

1. 继承`AbstractActivity`实现自己的UI,重写如下三个方法,通过`setShowUIActivity(xxx.class)`注入.

```
//自己定义的UI layout
public abstract int getContentLayoutId();
//数据初始化
public abstract void setUpData();
//当执行下载任务的时候回回调到此方法
public abstract void updateProgress(int percent, long currentLength, long total);
```
默认的[ProgressDialogActivity](https://github.com/xwdz/QuiteVersion/blob/master/lib/src/main/java/com/xingwei/checkupdate/ui/ProgressDialogActivity.java)也是基于此实现。
可通过`String note = getIntent().getStringExtra("note")`拿到更新文本。


2. 实现`OnUINotify`接口
**在不指定`.setNotifyHandler()方法以及`setShowUIActivity`方法时默认实现效果参照文章开头`**
**在自定义容器中实现此接口,在接口方法`show`中调用真正的`show`方法**,[详见simple-code](https://github.com/xwdz/QuiteVersion/blob/master/app/src/main/java/com/update/testabc/DialogTest.java)

**注意:自定义容器只能使用一种方式。**


#### 自定义容器中，点击开始下载时,需要调用如下代码

```
VersionHandler.startDownloadApk(getContext());
```


#### 自定义容器中注册接受下载进度条组件

```
private final VersionHandler.ProgressReceiver mProgressReceiver = new VersionHandler.ProgressReceiver() {
        @Override
        public void onUpdateProgress(long total, long currentLength, int percent) {
            Utils.LOG.i("tag", "current = " + currentLength);
        }
    };


//在容器创建等合适的时候调用注册代码
VersionHandler.registerProgressbarReceiver(getContext(), mProgressReceiver);

//容器销毁的时候调用注销代码
VersionHandler.unregisterProgressbarReceiver(getContext(), mProgressReceiver);
```

[@酸菜xwdz](http://huangxingwei.cn)
[Github](https://github.com/xwdz/QuiteVersion)


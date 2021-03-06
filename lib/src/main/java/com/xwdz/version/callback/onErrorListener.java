package com.xwdz.version.callback;

import com.xwdz.version.utils.LOG;

/**
 * @author xingwei.huang (xwdz9989@gmail.com)
 * @since v1.0.0
 */
public interface onErrorListener {
    void listener(Throwable throwable);

    onErrorListener sDef = new onErrorListener() {
        @Override
        public void listener(Throwable throwable) {
            LOG.e(LOG.TAG, "error:" + throwable);
        }
    };
}

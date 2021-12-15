package com.cloudling.customtask;

import android.os.Handler;
import android.os.Looper;

/**
 * 描述: 定时任务处理
 * 联系: 1966353889@qq.com
 * 日期: 2021/12/15
 */
public class CustomTask<Support, Result> {
    private Handler mHandler;
    private Runnable mRunnable;
    /**
     * 首次任务延迟执行的时间
     */
    private long mDelay;
    /**
     * 两次任务之间的间隔时间
     */
    private long mInterval;
    /**
     * 具体的任务
     */
    private SingleTask<Support, Result> mTask;
    /**
     * 重试次数（单次任务完成后自动重试）
     */
    private int mRepeat;

    private OnResultListener<Result> listener;

    private CustomTask(Builder<Support, Result> builder) {
        mDelay = builder.delay;
        mInterval = builder.interval;
        mTask = builder.task;
        mRepeat = builder.repeat;
        listener = builder.listener;
        mHandler = new Handler(Looper.getMainLooper());
        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (mTask != null) {
                    if (mRepeat > 0) {
                        mRepeat--;
                    }
                    mTask.start(new Scheduler<Result>() {
                        @Override
                        public void process(boolean carryOn, Result result) {
                            if (carryOn) {
                                if (mRepeat > 0 || mRepeat == -1) {
                                    start(mInterval);
                                } else {
                                    if (listener != null) {
                                        listener.onComplete(result);
                                    }
                                }
                            } else {
                                if (listener != null) {
                                    listener.onComplete(result);
                                }
                            }
                        }
                    });
                }
            }
        };
    }

    /**
     * 开启任务（内部调用）
     */
    private void start(long delay) {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            if (delay > 0) {
                mHandler.postDelayed(mRunnable, delay);
            } else {
                mHandler.post(mRunnable);
            }
        }
    }

    /**
     * 开启任务（外部调用）
     */
    public void start() {
        if (mHandler != null && mRunnable != null && (mRepeat > 0 || mRepeat == -1)) {
            mHandler.removeCallbacks(mRunnable);
            if (mDelay > 0) {
                mHandler.postDelayed(mRunnable, mDelay);
            } else {
                mHandler.post(mRunnable);
            }
        }
    }


    /**
     * 取消任务
     */
    public void cancel() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            if (mTask != null) {
                mTask.cancel();
            }
        }
    }

    /**
     * 销毁任务（一般在页面关闭之后调用）
     */
    public void onDestroy() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            if (mTask != null) {
                mTask.onDestroy();
            }
        }
    }

    public static class Builder<Support, Result> {
        /**
         * 首次任务延迟执行的时间
         */
        private long delay;
        /**
         * 两次任务之间的间隔时间
         */
        private long interval;
        /**
         * 具体的任务
         */
        private SingleTask<Support, Result> task;
        /**
         * 重试次数（单次任务完成后自动重试）
         */
        private int repeat = -1;
        private OnResultListener<Result> listener;

        public Builder<Support, Result> delay(long delay) {
            this.delay = delay;
            return this;
        }

        public Builder<Support, Result> interval(long interval) {
            this.interval = interval;
            return this;
        }

        public Builder<Support, Result> task(SingleTask<Support, Result> task) {
            this.task = task;
            return this;
        }

        public Builder<Support, Result> repeat(int repeat) {
            this.repeat = repeat;
            return this;
        }

        public Builder<Support, Result> callback(OnResultListener<Result> listener) {
            this.listener = listener;
            return this;
        }

        public CustomTask<Support, Result> build() {
            return new CustomTask<>(this);
        }
    }

    public interface SingleTask<Support, Result> {
        /**
         * 开始任务
         */
        void start(Scheduler<Result> scheduler);

        /**
         * 取消任务
         */
        void cancel();

        /**
         * 销毁任务（一般在页面退出时调用）
         */
        void onDestroy();

        /**
         * 需要从外部获取的支持参数（比如Context、goods_id）
         */
        Support getSupport();
    }

    public interface OnResultListener<Result> {
        void onComplete(Result result);
    }

    public interface Scheduler<Result> {
        /**
         * 任务完成回调
         *
         * @param carryOn 是否接着重试（辅助TimingTask进行重试）
         * @param result  任务完成后回调的处理结果
         */
        void process(boolean carryOn, Result result);
    }
}

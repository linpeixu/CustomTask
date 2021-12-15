# CustomTask

Android开启定时任务通用的封装
先看接入步骤：
Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```java
    allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
Step 2. Add the dependency
```java
    dependencies {
                        implementation 'com.github.linpeixu:CustomTask:1.0.3'
	        //或者implementation 'com.gitlab.linpeixu:customtask:1.0.2'
	}
```

背景：

最近接到一个需求，在特定条件下定时轮询接口查询相关信息，直到拿到想要的信息为止，为了方便以后使用，在这里做了一个封装，假设需要定时查询订单状态，先看使用方法：

```java
CustomTask<Long, Long> customTask = new CustomTask.Builder<Long, Long>()
                .delay(3000)//延迟3秒执行，根据需要自行更改
                .interval(5000)//轮询间隔5秒，根据需要自行更改
                .task(new CheckOrderStateTask() {//具体的任务执行逻辑
                    @Override
                    public Long getSupport() {
                       /*重写getSupport()返回CheckOrderStateTask执行需要用到的参数*/
                        return getIntent().getLongExtra("id", 0);
                    }
                })
                .callback(new CustomTask.OnResultListener<Long>() {
                    @Override
                    public void onComplete(Long aLong) {
                       /*任务执行完成后的回调（此时应为拿到想要的结果）*/
                        ToastUtil.getInstance().show(String.valueOf(aLong));
                    }
                }).build();
        customTask.start();
```
CustomTask代码如下：

```java
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
```
CheckOrderStateTask代码如下：

```java
public class CheckOrderStateTask implements CustomTask.SingleTask<Long, Long> {
    /**
     * 网络请求订阅存储对象
    */
    private CompositeDisposable mDisposables;
    private final OrderApi orderApi;

    public CheckOrderStateTask() {
        mDisposables = new CompositeDisposable();
        orderApi = ...
    }


    @Override
    public void start(CustomTask.Scheduler<Long> scheduler) {
        if (getSupport() != null) {
            Disposable rx = RxUtils.rx(orderApi.checkOrderStatus(getSupport()), new OnNextOnError<Response<JsonObject>>() {
                @Override
                public void onError(Response response) {
                    if (response != null) {
                        if (response.status == -1) {
                            /*订单已取消*/
                            scheduler.process(false, -1L);
                        }  else {
                            scheduler.process(true, -1L);
                        }
                    }
                }

                @Override
                public void onNext(Response<JsonObject> response) {
                    long order_id = ...;
                    scheduler.process(order_id <= 0, order_id);
                }
            });
            mDisposables.add(rx);
        } else {
            scheduler.process(false, -1L);
        }
    }

    @Override
    public void cancel() {
        if (mDisposables != null) {
            mDisposables.dispose();
        }
    }


    @Override
    public void onDestroy() {
        if (mDisposables != null) {
            mDisposables.dispose();
            mDisposables = null;
        }
    }

    @Override
    public Long getSupport() {
        return null;
    }


}
```
这里的网络请求操作用到的是[Android快速集成网络库功能（rxJava+retrofit+okhttp）](https://blog.csdn.net/qq_33866343/article/details/106135963)，感兴趣的可以看看。

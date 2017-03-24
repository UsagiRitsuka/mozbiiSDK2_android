package ufro.com.mozbiisdkandroid2;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by Usagi on 2017/3/24.
 */

public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }
}

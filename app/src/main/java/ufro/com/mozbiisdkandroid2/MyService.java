package ufro.com.mozbiisdkandroid2;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Usagi on 2017/3/24.
 */

public class MyService extends Service{
    final private String TAG = MyService.class.getSimpleName();
    private MozbiiBleWrapper mozbiiBleWrapper;
    @Override
    public void onCreate() {
        super.onCreate();
        if(mozbiiBleWrapper == null){
            Log.v(TAG, "new wrapper");
            mozbiiBleWrapper = new MozbiiBleWrapper(this);
        }
    }

    @Override
    public void onDestroy() {
        mozbiiBleWrapper.stopScan();
        mozbiiBleWrapper.disConnectAll();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void setNumOfDevice(int numOfDevice){
        mozbiiBleWrapper.setNumOfDevice(numOfDevice);
    }

    public void setOnMozibiiListener(OnMozibiiListener listener){
        mozbiiBleWrapper.setOnMozibiiListener(listener);
    }

    public boolean isBleEnable(){
        return mozbiiBleWrapper.isBleEnable();
    }

    public void enableBle(){
        mozbiiBleWrapper.enableBle();
    }

    public void startScan(){
        mozbiiBleWrapper.startScan();
    }

    public void disConnectAll(){
        mozbiiBleWrapper.disConnectAll();
    }

    public int getNumOfConnDevice(){
        return mozbiiBleWrapper.getNumOfConnDevice();
    }

    public void stopScan(){
        mozbiiBleWrapper.stopScan();
    }


    class MyBinder extends Binder{
        public MyService getService(){
            return MyService.this;
        }
    }
}

package ufro.com.mozbiisdkandroid2;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.security.Permission;

public class MainActivity extends AppCompatActivity implements OnMozibiiListener{
    private final int PERMISSION_REQUEST= 1;
    private ServiceConnection myConnection;
    private MyService myService;
    final private String TAG = MainActivity.class.getSimpleName();
    private boolean serviceBound = false;
    private RecyclerView recyclerView;
    private RecycleAdapter recycleAdapter;
    private Context context;
    private int requireNum = 2;

    private Button conBtn;
    private Button disconBtn;

    private Runnable stopScan = new Runnable() {
        @Override
        public void run() {
            myService.stopScan();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST);
            }
        }
        context = this;

        if(myConnection == null){
            myConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    Log.v(TAG, "onServiceConnected");
                    myService = ((MyService.MyBinder)iBinder).getService();
                    myService.setNumOfDevice(requireNum);
                    myService.setOnMozibiiListener(MainActivity.this);
                    if(!myService.isBleEnable()){
                        myService.enableBle();
                    }

                    if(!myService.isBleEnable()){
                        // need check ble
                        showMsg();
                    } else {
                        myService.startScan();
                        ThreadManager.getInstance().postToBackgroungThread(stopScan, 20000);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    Log.v(TAG, "onServiceDisconnected");
                    serviceBound = false;
                    myService.disConnectAll();
                }
            };
        }

        conBtn = (Button)findViewById(R.id.connect);
        disconBtn = (Button)findViewById(R.id.disconnect);
        disconBtn.setEnabled(false);

        conBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!serviceBound) {
                    doBindService();
                    serviceBound = true;
                }
            }
        });

        disconBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(serviceBound) {
                    unbindService(myConnection);
                    serviceBound = false;
                }
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.recycle_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recycleAdapter = new RecycleAdapter();
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recycleAdapter.setNumOfDevices(requireNum);
        recyclerView.setAdapter(recycleAdapter);
    }

    private boolean doBindService(){
        Intent intent = new Intent(this, MyService.class);
        return serviceBound = bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
    }

    private void showMsg(){
        Toast.makeText(this, "need chech ble", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            switch (requestCode){
                case PERMISSION_REQUEST:
                    if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                        startInstalledAppDetailsActivity();
                    }
                    break;
            }
        }
    }

    // 開啟系統權限設定頁面
    private void startInstalledAppDetailsActivity() {
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(i);
    }

    @Override
    public void onMozbiiConnected(int index, String address) {
        ThreadManager.getInstance().postToUIThread(new Runnable() {
            @Override
            public void run() {
                if(myService.getNumOfConnDevice() == requireNum){
                    conBtn.setEnabled(false);
                    disconBtn.setEnabled(true);
                    ThreadManager.getInstance().removeCallbacks(stopScan);
                }
            }
        });

    }

    @Override
    public void onMozbiiDisconnected(final int index, final String address) {
        Log.v(TAG, "disconnect: " + index);
        ThreadManager.getInstance().postToUIThread(new Runnable() {
            @Override
            public void run() {
            recycleAdapter.setColorCode(-1, index);
            if(serviceBound) {
                myService.startScan();
                ThreadManager.getInstance().postToBackgroungThread(stopScan, 20000);
            }
            }
        });
    }

    @Override
    public void onMozbiiColorDetected(final int index, final int color) {
        ThreadManager.getInstance().postToUIThread(new Runnable() {
            @Override
            public void run() {
            recycleAdapter.setColorCode(color, index);
            }
        });

    }

    @Override
    public void onMozbiiColorIndexChanged(int index, int indexOfColor) {
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
        if(serviceBound) {
            unbindService(myConnection);
            serviceBound = false;
        }
    }
}

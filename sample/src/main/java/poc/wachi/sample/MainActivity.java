package poc.wachi.sample;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;


import ufro.com.mozbiisdkandroid2.MozbiiBleWrapper;
import ufro.com.mozbiisdkandroid2.OnMozbiiBatterryListener;
import ufro.com.mozbiisdkandroid2.OnMozbiiClickedListener;
import ufro.com.mozbiisdkandroid2.OnMozbiiListener;

public class MainActivity extends PermissionActivity implements OnMozbiiListener, OnMozbiiClickedListener, OnMozbiiBatterryListener {
    private final String TAG = MainActivity.class.getSimpleName();
    private MozbiiBleWrapper wrapper;
    private boolean isScanning = false;

    private RelativeLayout bgParant;
    private Button actBtn;
    private TextView status;
    private TextView battery;
    private TextView serial;
    private TextView firmwareVer;
    private enum STATUS{
        CONNECT,
        DISCONNECT,
        PENDING,
        SCANNING
    };

    private STATUS enumStatus = STATUS.PENDING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        wrapper = new MozbiiBleWrapper(this);
        wrapper.setOnMozbiiListener(this);
        wrapper.setOnMozbiiClickedListener(this);
        wrapper.setOnMozbiiBatterryListener(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(wrapper.isBleEnable()) {
            wrapper.enableBle();
        }

        findView();
        setupComponent();
    }

    private void findView(){
        bgParant = (RelativeLayout)findViewById(R.id.bg);
        actBtn = (Button)findViewById(R.id.btn);
        status = (TextView)findViewById(R.id.status);
        battery = (TextView)findViewById(R.id.battery);
        serial = (TextView)findViewById(R.id.serial);
        firmwareVer = (TextView)findViewById(R.id.firmware);
    }

    private void setupComponent(){
        actBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(enumStatus == STATUS.PENDING || enumStatus == STATUS.DISCONNECT){
                    status.setText(getString(R.string.status_scanning));
                    actBtn.setText(getString(R.string.stop_scan));
                    enumStatus = STATUS.SCANNING;
                    startScan();
                } else if(enumStatus == STATUS.SCANNING){
                    status.setText(getString(R.string.status_pending));
                    actBtn.setText(getString(R.string.start_scan));
                    enumStatus = STATUS.PENDING;
                    stopScan();
                } else if(enumStatus == STATUS.CONNECT){
                    status.setText(getString(R.string.status_pending));
                    actBtn.setText(getString(R.string.start_scan));
                    enumStatus = STATUS.PENDING;
                    wrapper.disConnect();
                }
            }
        });
    }

    void startScan() {
        if(!isScanning) {
            isScanning = true;
            wrapper.startScan();
        }
    }

    private void stopScan(){
        if(isScanning) {
            isScanning = false;
            wrapper.stopScan();
        }
    }

    @Override
    public void onMozbiiConnected() {
        Log.v(TAG, "onMozbiiConnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String serialStr = wrapper.getSerial();
                String firmwareVersion = wrapper.getFirmwareVersion();
                status.setText(getString(R.string.status_connected));
                actBtn.setText(getString(R.string.disconnect));
                serial.setText(TextUtils.isEmpty(serialStr) ? "non" : serialStr);
                firmwareVer.setText(TextUtils.isEmpty(firmwareVersion) ? "non" : firmwareVersion);

                enumStatus = STATUS.CONNECT;
            }
        });

        stopScan();
    }

    @Override
    public void onMozbiiDisconnected() {
        Log.v(TAG, "onMozbiiDisconnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                status.setText(getString(R.string.status_disconnected));
                actBtn.setText(getString(R.string.start_scan));
                battery.setText(getString(R.string.unknown));
                serial.setText(getString(R.string.unknown));
                firmwareVer.setText(getString(R.string.unknown));
                enumStatus = STATUS.DISCONNECT;
            }
        });

    }

    @Override
    public void onMozbiiColorIndexChanged(int index) {
        Log.v(TAG, "onMozbiiColorIndexChanged");
    }

    @Override
    public void onMozbiiColorDetected(final int color) {
        Log.v(TAG, "onMozbiiColorDetected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                status.setTextColor(color);
            }
        });
    }

    @Override
    public void onMozbiiColorArrayDetected(final int[] colorArray, final int index) {
        Log.v(TAG, "onMozbiiColorArrayDetected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bgParant.setBackgroundColor(colorArray[index]);
            }
        });
    }

    @Override
    public void onUpButtonClicked() {
        Log.v(TAG, "onUpButtonClicked");
    }

    @Override
    public void onDownButtonClicked() {
        Log.v(TAG, "onDownButtonClicked");
    }

    @Override
    public void onDetectButtonClicked() {
        Log.v(TAG, "onDetectButtonClicked");
    }

    @Override
    public void onMozbiiBatteryLevelChanged(final int level) {
        Log.v(TAG, "onMozbiiBatteryLevelChanged: " + level);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                battery.setText(Integer.toString(level));
            }
        });
    }

    @Override
    public void onMozbiiBatteryLevelLow() {
        Log.v(TAG, "onMozbiiBatteryLevelLow");
    }
}

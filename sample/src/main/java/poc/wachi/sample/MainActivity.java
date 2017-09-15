package poc.wachi.sample;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.w3c.dom.Text;

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
    private TextView retryTimes;

    private enum STATUS{
        CONNECT,
        DISCONNECT,
        PENDING,
        SCANNING
    };

    private STATUS enumStatus = STATUS.PENDING;

    private RelativeLayout[] colorViews = new RelativeLayout[12];
    private static GoogleAnalytics analytics;
    private static Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        wrapper = new MozbiiBleWrapper(this);
        wrapper.setOnMozbiiListener(this);
        wrapper.setOnMozbiiClickedListener(this);
        wrapper.setOnMozbiiBatterryListener(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(!wrapper.isBleEnable()) {
            wrapper.enableBle();
        }
        analytics = GoogleAnalytics.getInstance(this);

        String packageName = getPackageName();
        String campaignData = "https://play.google.com/store/apps/details?id=com.example.application&referrer=utm_source%3D"+
                "SMAPLE_TEST02" + "%26utm_medium%3Dandroid%26anid%3Dadmob";
        analytics = GoogleAnalytics.getInstance(this);
        if (tracker == null) {
            tracker = analytics.newTracker(
                getResources().getIdentifier("tracker", "xml", getPackageName())
            );

            tracker.send(new HitBuilders.ScreenViewBuilder()
                .setCampaignParamsFromUrl(campaignData)
                .build()
            );
        }

        sendEventGA("category_test", "action_test", "label_test");

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
        retryTimes = (TextView)findViewById(R.id.try_times);

        colorViews[0] = (RelativeLayout)findViewById(R.id.color01);
        colorViews[1] = (RelativeLayout)findViewById(R.id.color02);
        colorViews[2] = (RelativeLayout)findViewById(R.id.color03);
        colorViews[3] = (RelativeLayout)findViewById(R.id.color04);
        colorViews[4] = (RelativeLayout)findViewById(R.id.color05);
        colorViews[5] = (RelativeLayout)findViewById(R.id.color06);
        colorViews[6] = (RelativeLayout)findViewById(R.id.color07);
        colorViews[7] = (RelativeLayout)findViewById(R.id.color08);
        colorViews[8] = (RelativeLayout)findViewById(R.id.color09);
        colorViews[9] = (RelativeLayout)findViewById(R.id.color10);
        colorViews[10] = (RelativeLayout)findViewById(R.id.color11);
        colorViews[11] = (RelativeLayout)findViewById(R.id.color12);
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
                setColorSet();
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
                wrapper.stopScan();
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
    public void onMozbiiColorDetected(final int color) {
        Log.v(TAG, "onMozbiiColorDetected: " + color);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bgParant.setBackgroundColor(color);
                colorViews[wrapper.getIndex()].setBackgroundColor(color);
            }
        });
    }

    @Override
    public void onTopButtonClicked() {
        Log.v(TAG, "onTopButtonClicked");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bgParant.setBackgroundColor(wrapper.getColorArray()[wrapper.getIndex()]);
            }
        });
    }

    @Override
    public void onBottomButtonClicked() {
        Log.v(TAG, "onBottomButtonClicked");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bgParant.setBackgroundColor(wrapper.getColorArray()[wrapper.getIndex()]);
            }
        });
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

    private void setColorSet(){
        int[] colors = wrapper.getColorArray();
        for(int i = 0; i < colors.length; i++){
            colorViews[i].setBackgroundColor(colors[i]);
        }
    }

    private void sendEventGA(String category, String action, String lable){
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(lable)
                .build());
    }
}

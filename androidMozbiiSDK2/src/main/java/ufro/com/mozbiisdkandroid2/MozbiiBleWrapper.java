package ufro.com.mozbiisdkandroid2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

/**
 * Created by Usagi on 2017/3/17.
 */

public class MozbiiBleWrapper{
    final private String TAG = MozbiiBleWrapper.class.getSimpleName();

    final private UUID COLOR_PEN_INFO = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    final private UUID COLOR_PEN = UUID.fromString("00001f1f-0000-1000-8000-00805f9b34fb");
    final private UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    final private UUID INDEX = UUID.fromString("00001f13-0000-1000-8000-00805f9b34fb");
    // this characteristic didn't have read permission
    final private UUID RGB_COLOR = UUID.fromString("00001f14-0000-1000-8000-00805f9b34fb");
    final private UUID CUR_RGB_COLOR = UUID.fromString("00001F16-0000-1000-8000-00805F9B34FB");
    final private UUID EVENT = UUID.fromString("00001f15-0000-1000-8000-00805f9b34fb");
    final private UUID SERIAL_NUMBER = UUID.fromString("00002A25-0000-1000-8000-00805F9B34FB");
    final private UUID FIRMWARE_VERSION = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");
    final private UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    final private String XPLORE = "SP104";
    final private String PILLAR = "SP100";
    // EVENT characteristic value
    final private int UP_BTN_CLICKED = 3;
    final private int DOWN_BTN_CLICKED = 5;
    final private int DETECT_BTN_CLICKED = 17;
    final private int POWER_LOW = 101; // TODO: 不確定，沒看過1(connected), 3, 5, 17以外的event
    private String penSerial;
    private String firmwareVersion;

    // for api level < 21 (Lollipop), 21之後就deprecate了
    private BluetoothAdapter.LeScanCallback leScanCallback = null;
    // for api level >= 21 (Lollipop)
    private ScanCallback scanCallback = null;

    private Context context;
    static private BluetoothAdapter bluetoothAdapter;
    static private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private OnMozbiiListener onMozbiiListener;
    private OnMozbiiClickedListener onMozbiiClickedListener;
    private OnMozbiiBatterryListener onMozbiiBatterryListener;
    private long time = 0;
    private boolean isConnecting = false;

    private int[] colorArray = new int[12];
    // mRGBCharGatt會打三次CharacteristicChange，會傳size 12 的 array，每次更新四個顏色，全部更新完才能打callback回去
    private int counterOfColorSet = 0;
    private int indexOfColorSet = 0; // 不能保證index會先到，目前看起來是會

    private String packageName;
    private static GoogleAnalytics analytics;
    private static Tracker tracker;
    public MozbiiBleWrapper(Context context){
        this.context = context;
        packageName = context.getPackageName();

        analytics = GoogleAnalytics.getInstance(context);
        if (tracker == null) {
            tracker = analytics.newTracker(
                context.getResources().getIdentifier("mozbii_tracker", "xml", context.getPackageName())
            );


        }

        init();
    }

    private void init() {
        Log.v(TAG, "init");

        BluetoothManager btMrg = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (btMrg == null) {
            return;
        }

        bluetoothAdapter = btMrg.getAdapter();
        if (bluetoothAdapter == null){
            // Device does not support Bluetooth
            return;
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            leScanCallback = getLeScanCallback();
        } else {
            scanCallback = getScanCallback();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }
    public boolean isBleEnable(){
        return bluetoothAdapter.isEnabled();
    }

    public boolean enableBle(){
        return bluetoothAdapter.enable();
    }

    public void startScan(){
//        tracker.send(new HitBuilders.EventBuilder()
//                .setCategory("MozbiiAction-" + (TextUtils.isEmpty(packageName) ? "NULL" : packageName))
//                .setAction("startScan")
//                .build());
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.v(TAG, "startScan(sdk less than Lollipop)");
            bluetoothAdapter.startLeScan(new UUID[]{COLOR_PEN}, leScanCallback);
        } else{
            Log.v(TAG, "startScan(sdk at least Lollipop) add");
            List<ScanFilter> scanFilterList = new ArrayList<ScanFilter>();
            scanFilterList.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(COLOR_PEN)).build());
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            bluetoothLeScanner.startScan(scanFilterList, scanSettings, scanCallback);
        }
    }

    public void stopScan(){
        Log.v(TAG, "stopScan");
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            bluetoothAdapter.stopLeScan(leScanCallback);
        } else{
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    public void disConnect(){
        if(bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    private BluetoothAdapter.LeScanCallback getLeScanCallback(){
        return new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                onDeviceScaned(device);
            }
        };
    }

    private ScanCallback getScanCallback(){
        ScanCallback scanCallback = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    Log.v(TAG, "onScanResult");
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        time = System.currentTimeMillis();
                        onDeviceScaned(result.getDevice());
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                }
            };
        }

        return scanCallback;
    }


    synchronized private void onDeviceScaned(BluetoothDevice device){
        Log.v(TAG, "onDeviceScaned: " + device.getName());
        if(!isConnecting) {
            isConnecting = true;
            Log.v(TAG, "device name: " + device.getName());
            time = System.currentTimeMillis();
            device.connectGatt(context, false, getBluetoothGattCallback());
        }
    }

    private BluetoothGattCallback getBluetoothGattCallback(){
        return new BluetoothGattCallback(){
            private List<BluetoothGattCharacteristic> characteristicList = new ArrayList<>();

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                Log.v(TAG, "onConnectionStateChange, cost: " + Double.toString((System.currentTimeMillis() - time) / 1000d));
                time = System.currentTimeMillis();
                if(status == BluetoothGatt.GATT_SUCCESS){
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        // 連線但還不能使用比的功能，因此還不能在此回傳已經連線的訊息
                        bluetoothGatt = gatt;
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (null != onMozbiiListener) {
                            tracker.setScreenName(packageName + "-disconnected");
                            tracker.send(new HitBuilders.ScreenViewBuilder().build());
                            onMozbiiListener.onMozbiiDisconnected();
                        }

                        gatt.close();
                        Log.v(TAG, "gatt close");
                    }
                } else{
                    Log.v(TAG, "onConnectionStateChange fail");
                    if (null != onMozbiiListener) {
                        tracker.setScreenName(packageName + "-disconnected");
                        tracker.send(new HitBuilders.ScreenViewBuilder().build());
                        onMozbiiListener.onMozbiiDisconnected();
                    }

                    gatt.close();
                }

                isConnecting = false;
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.v(TAG, "onServicesDiscovered GATT_SUCCESS, cost: " + Double.toString((System.currentTimeMillis() - time) / 1000d));
                    time = System.currentTimeMillis();

                    BluetoothGattService gattService = gatt.getService(COLOR_PEN);
                    if(gattService != null){
                        characteristicList.add(gattService.getCharacteristic(INDEX));
                        characteristicList.add(gattService.getCharacteristic(RGB_COLOR));
                        characteristicList.add(gattService.getCharacteristic(EVENT));
                    }

                    gattService = gatt.getService(BATTERY_SERVICE_UUID);
                    if(gattService != null){
                        characteristicList.add(gattService.getCharacteristic(BATTERY_LEVEL));
                    }

                    gattService = gatt.getService(COLOR_PEN_INFO);
                    if(gattService != null) {
                        characteristicList.add(gattService.getCharacteristic(FIRMWARE_VERSION));
                        gatt.readCharacteristic(gattService.getCharacteristic(SERIAL_NUMBER));
                    }
                } else if(status == BluetoothGatt.GATT_FAILURE){
                    Log.v(TAG, "GATT_FAILURE");
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                Log.v(TAG, "onCharacteristicRead");

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if(SERIAL_NUMBER.equals(characteristic.getUuid())){
                        penSerial = characteristic.getStringValue(0);
                        checkIsFinishConnected(gatt);
                    } else if(FIRMWARE_VERSION.equals(characteristic.getUuid())){
                        firmwareVersion = characteristic.getStringValue(0);
                        checkIsFinishConnected(gatt);
                    }
                    else {
                        Log.v(TAG, "onCharacteristicRead GATT_SUCCESS, cost: " + Double.toString((System.currentTimeMillis() - time) / 1000d));
                        time = System.currentTimeMillis();
                        gatt.setCharacteristicNotification(characteristic, true);
                        writeDescriptor(characteristic, gatt);
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                if(INDEX.equals(characteristic.getUuid())){
                    indexChange(characteristic);
                } else if(CUR_RGB_COLOR.equals(characteristic.getUuid())){
                    Log.v(TAG, "CUR_RGB_COLOR");
                    colorDetected(characteristic);
                } else if(RGB_COLOR.equals(characteristic.getUuid())) {
                    Log.v(TAG, "RGB_COLOR");
                    colorArrayDetected(characteristic);
                } else if(BATTERY_LEVEL.equals(characteristic.getUuid())){
                    if(onMozbiiBatterryListener != null){
                        onMozbiiBatterryListener.onMozbiiBatteryLevelChanged(characteristic.getValue()[0]);
                    }
                } else if(EVENT.equals(characteristic.getUuid())){
                    onEvent(characteristic);
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                Log.v(TAG, "onDescriptorWrite");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    isConnecting = false;
                    Log.v(TAG, "onDescriptorWrite, cost: " + Double.toString((System.currentTimeMillis() - time) / 1000d));
                    time = System.currentTimeMillis();
                    checkIsFinishConnected(gatt);
                }

            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
                Log.v(TAG, "onDescriptorRead");
            }

            /**
             * 原本設定pillar使用只傳一個顏色的UUID, Xplore使用傳12個顏色的UUID
             * 因此這裡會判斷要刪除哪個, 不過現在不需要了, 一律使用12個顏色的UUID
             */
            private void removeCharacteristic(){
                if (characteristicList.size() > 0) {
                    for (int i = 0; i < characteristicList.size(); i++) {
                        BluetoothGattCharacteristic ch = characteristicList.get(i);
                        if (PILLAR.equals(penSerial)) {
                            if (CUR_RGB_COLOR.equals(ch.getUuid())) {
                                characteristicList.remove(ch);
                            }
                        } else if (XPLORE.equals(penSerial)) {
                            if (RGB_COLOR.equals(ch.getUuid())) {
                                characteristicList.remove(ch);
                            }
                        }
                    }
                }
            }

            private void checkIsFinishConnected(BluetoothGatt gatt){
                if (characteristicList.size() > 0) {
                    boolean isReadSuccess = gatt.readCharacteristic(characteristicList.get(0));
                    // RGB_COLOR 為不可讀，不會跑onCharacteristicRead()，因此需在此直接set notification & write description
                    if(!isReadSuccess){
                        Log.v(TAG, "readCharacteristic() return: " + isReadSuccess);
                        gatt.setCharacteristicNotification(characteristicList.get(0), true);
                        writeDescriptor(characteristicList.get(0), gatt);
                    }

                    characteristicList.remove(0);
                } else {
                    // 到此才算真正可以使用筆的功能
                    Log.v(TAG, "real finish connected");
                    if (null != onMozbiiListener) {
                        onMozbiiListener.onMozbiiConnected();
                        tracker.setScreenName(packageName + "-connected!!!");
                        tracker.send(new HitBuilders.ScreenViewBuilder().build());
                    }
                }
            }
        };
    }

    // official description, 告訴device一旦發生even就送otification
    private void writeDescriptor(BluetoothGattCharacteristic characteristic, BluetoothGatt gatt){
        // official
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (descriptor != null) {
            byte[] val = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
            descriptor.setValue(val);
            gatt.writeDescriptor(descriptor);
        }
    }

    private void indexChange(BluetoothGattCharacteristic characteristic){
        Integer index = characteristic.getIntValue(FORMAT_UINT8, 0);
        if(null != index && null != onMozbiiListener) {
            indexOfColorSet = index - 1;
            onMozbiiListener.onMozbiiColorIndexChanged(indexOfColorSet);
        }
    }

    private void colorDetected(BluetoothGattCharacteristic characteristic){
        Integer r = characteristic.getIntValue(FORMAT_UINT8, 1);
        Integer g = characteristic.getIntValue(FORMAT_UINT8, 2);
        Integer b = characteristic.getIntValue(FORMAT_UINT8, 3);

        if(r != null && g != null && b != null && null != onMozbiiListener) {
            onMozbiiListener.onMozbiiColorDetected(Color.rgb(r, g, b));
        }
    }

    private void colorArrayDetected(BluetoothGattCharacteristic characteristic){
        byte[] rawValue = characteristic.getValue();
        int red = 0;
        int green = 0;
        int blue = 0;

        // Parse color array
        for (int i = 0; i < (rawValue.length) / 4; i++) {
            if (rawValue[i * 4] > 0) {
                red = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, i * 4 + 1);
                green = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, i * 4 + 2);
                blue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, i * 4 + 3);

                try {
                    int color = Color.rgb(red, green, blue);
                    colorArray[rawValue[i * 4] - 1] = color;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        if(++counterOfColorSet >= 3) {
            Log.v(TAG, "colorArrayDetected, index = " + indexOfColorSet);
            counterOfColorSet = 0;
            onMozbiiListener.onMozbiiColorArrayDetected(colorArray, indexOfColorSet);
        }
    }

    private void onEvent(BluetoothGattCharacteristic characteristic){
        int value = characteristic.getIntValue(FORMAT_UINT8, 0);
        Log.v(TAG, "Event: " + value);
        switch (value){
            case UP_BTN_CLICKED:
                if(onMozbiiClickedListener != null){
                    onMozbiiClickedListener.onUpButtonClicked();
                }
                break;

            case DOWN_BTN_CLICKED:
                if(onMozbiiClickedListener != null){
                    onMozbiiClickedListener.onDownButtonClicked();
                }
                break;

            case DETECT_BTN_CLICKED:
                if(onMozbiiClickedListener != null){
                    onMozbiiClickedListener.onDetectButtonClicked();
                }
                break;

            case POWER_LOW:
                if(onMozbiiBatterryListener != null) {
                    onMozbiiBatterryListener.onMozbiiBatteryLevelLow();
                }
                break;
        }
    }

    public String getSerial(){
        return penSerial;
    }

    public String getFirmwareVersion(){
        return firmwareVersion;
    }


    public void setOnMozbiiListener(OnMozbiiListener onMozbiiListener){
        this.onMozbiiListener = onMozbiiListener;
    }

    public void setOnMozbiiClickedListener(OnMozbiiClickedListener onMozbiiClickedListener) {
        this.onMozbiiClickedListener = onMozbiiClickedListener;
    }

    public void setOnMozbiiBatterryListener(OnMozbiiBatterryListener onMozbiiBatterryListener) {
        this.onMozbiiBatterryListener = onMozbiiBatterryListener;
    }
}

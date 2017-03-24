package ufro.com.mozbiisdkandroid2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

/**
 * Created by Usagi on 2017/3/17.
 */

public class MozbiiBleWrapper{
    final private String TAG = MozbiiBleWrapper.class.getSimpleName();
    final private UUID COLOR_PEN = UUID.fromString("00001f1f-0000-1000-8000-00805f9b34fb");
    final private UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");

    final private UUID INDEX = UUID.fromString("00001f13-0000-1000-8000-00805f9b34fb");
    final private UUID RGB_COLOR = UUID.fromString("00001f14-0000-1000-8000-00805f9b34fb");
    final private UUID EVENT = UUID.fromString("00001f15-0000-1000-8000-00805f9b34fb");
    final private UUID CUR_RGB_COLOR = UUID.fromString("00001F16-0000-1000-8000-00805F9B34FB");
    final private UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    // for api level < 21 (Lollipop), 21之後就deprecate了
    private BluetoothAdapter.LeScanCallback leScanCallback = null;
    // for api level >= 21 (Lollipop)
    private ScanCallback scanCallback = null;

    private Context context;
    static private BluetoothAdapter bluetoothAdapter;
    static private BluetoothLeScanner bluetoothLeScanner;
    private int numOfDevice = 0;
    private List<BluetoothGatt> gattList;
    private OnMozibiiListener onMozibiiListener;

    private int scanTimeLimit = 60000; // ms

    private List<BluetoothGattCharacteristic> characteristicList;

    public MozbiiBleWrapper(Context context){
        this.context = context;
        init();
    }

    private void init() {
        Log.v(TAG, "init");

        characteristicList = new ArrayList<BluetoothGattCharacteristic>();
        gattList = new ArrayList<BluetoothGatt>();


        BluetoothManager btMrg = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (btMrg == null) {
            return;
        }

        bluetoothAdapter = btMrg.getAdapter();
        if (bluetoothAdapter == null){
            // TODO Device does not support Bluetooth
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
        Log.v(TAG, "startScan");
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            bluetoothAdapter.startLeScan(new UUID[]{COLOR_PEN}, leScanCallback);
        } else{
            List<ScanFilter> scanFilterList = new ArrayList<ScanFilter>();
            scanFilterList.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(COLOR_PEN)).build());
            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
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

    public void disConnect(int index){
        if(gattList.size() > index){
            gattList.get(index).disconnect();
        }
    }

    public void disConnectAll(){
        for(int i = 0; i < gattList.size(); i++){
            gattList.get(i).disconnect();
        }

    }

    private BluetoothAdapter.LeScanCallback getLeScanCallback(){
        return new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if(gattList.size() < numOfDevice){
                    if(!isConnected(device.getAddress())) {
                        gattList.add(device.connectGatt(context, false, getBluetoothGattCallback()));
                    }
                    if(gattList.size() == numOfDevice){
                        stopScan();
                    }
                } else {
                    stopScan();
                }
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
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Log.v(TAG, "onScan, list size: " + gattList.size() + "  addr: " + result.getDevice().getAddress());
                        if(gattList.size() < numOfDevice){
                            if(!isConnected(result.getDevice().getAddress())) {
                                gattList.add(result.getDevice().connectGatt(context, false, getBluetoothGattCallback()));
                            }

                        }
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                    Log.v(TAG, "onBatchScanResults");
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                }
            };
        }

        return scanCallback;
    }

    private boolean isConnected(String address){
        if(gattList != null){
            for(BluetoothGatt gatt: gattList){
                if(gatt.getDevice().getAddress().equals(address)){
                    return true;
                }
            }
        }

        return false;
    }

    private BluetoothGattCallback getBluetoothGattCallback(){
        return new BluetoothGattCallback(){
            private List<BluetoothGattCharacteristic> characteristicList = new ArrayList<>();


            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                if(INDEX.equals(characteristic.getUuid())){
                    indexChange(gatt, characteristic);
                } else if(CUR_RGB_COLOR.equals(characteristic.getUuid())){
                    colorDetected(gatt, characteristic);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                Log.v(TAG, "onCharacteristicRead");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    gatt.setCharacteristicNotification(characteristic, true);
                    writeDescriptor(characteristic, gatt);

                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.v(TAG, "onServicesDiscovered");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService gattColorService = gatt.getService(COLOR_PEN);
                    if(gattColorService != null){
                        Log.v(TAG, "gattColorService != null");
                        characteristicList.add(gattColorService.getCharacteristic(INDEX));
                        characteristicList.add(gattColorService.getCharacteristic(CUR_RGB_COLOR));

                        if(characteristicList.size() > 0){
                            Log.v(TAG, "PriorityQueue.size > 0");
                            gatt.readCharacteristic(characteristicList.get(0));
                            characteristicList.remove(0);
                        }
                    }

                } else if(status == BluetoothGatt.GATT_FAILURE){
                    Log.v(TAG, "GATT_FAILURE");
                }
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                Log.v(TAG, "onConnectionStateChange");
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    // 連線但還不能使用比的功能，因此還不能在此回傳已經連線的訊息
                    gatt.discoverServices();
                } else if(newState == BluetoothGatt.STATE_DISCONNECTED){
                    onMozibiiListener.onMozbiiDisconnected(gattList.indexOf(gatt), gatt.getDevice().getAddress());
                    gatt.close();
                    Log.v(TAG, "gatt close");
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                Log.v(TAG, "onDescriptorWrite");
                if(characteristicList.size() > 0){
                    gatt.readCharacteristic(characteristicList.get(0));
                    characteristicList.remove(0);
                } else {
                    // 到此才算真正可以使用筆的功能
                    Log.v(TAG, "real finish connected");
                    onMozibiiListener.onMozbiiConnected(gattList.indexOf(gatt), gatt.getDevice().getAddress());
                }

            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
                Log.v(TAG, "onDescriptorRead");
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

    private void indexChange(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        Integer index = characteristic.getIntValue(FORMAT_UINT8, 0);
        if(null != index) {
            onMozibiiListener.onMozbiiColorIndexChanged(gattList.indexOf(gatt),
                    characteristic.getIntValue(FORMAT_UINT8, 0) - 1);
        }
    }

    private void colorDetected(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        Integer r = characteristic.getIntValue(FORMAT_UINT8, 1);
        Integer g = characteristic.getIntValue(FORMAT_UINT8, 2);
        Integer b = characteristic.getIntValue(FORMAT_UINT8, 3);

        if(r != null && g != null && b != null) {
            onMozibiiListener.onMozbiiColorDetected(gattList.indexOf(gatt), Color.rgb(r, g, b));
        }
    }

    /**
     * 設定想要連線的裝置數
     * @param numOfDevice
     */
    public void setNumOfDevice(int numOfDevice){
        this.numOfDevice = numOfDevice;
        if(gattList != null && gattList.size() > 0){
            disConnectAll();
        }

        gattList = new ArrayList<>(numOfDevice);
    }

    public int getNumOfConnDevice(){
        return (null == gattList) ? 0 : gattList.size();
    }

    @Nullable
    public void setOnMozibiiListener(OnMozibiiListener onMozibiiListener){
        this.onMozibiiListener = onMozibiiListener;
    }
}

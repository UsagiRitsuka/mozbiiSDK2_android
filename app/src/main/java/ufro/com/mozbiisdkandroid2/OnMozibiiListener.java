package ufro.com.mozbiisdkandroid2;

import java.util.UUID;

/**
 * Created by Usagi on 2017/3/22.
 */

public interface OnMozibiiListener {
    /**
     *
     * @param index 目前是第幾個device連線(start from 0)
     * @param address device address
     */
    void onMozbiiConnected(int index, String address);
    void onMozbiiDisconnected(int index, String address);

    void onMozbiiColorDetected(int index, int color);
    void onMozbiiColorIndexChanged(int index, int indexOfColor);

    /**
     * 電池是另一個service
     * 暫時先不放在這
     */
//    void onMozbiiBatteryStatusChanged(int battery);

}

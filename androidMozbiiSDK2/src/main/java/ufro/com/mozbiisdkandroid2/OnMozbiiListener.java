package ufro.com.mozbiisdkandroid2;

import java.util.UUID;

/**
 * Created by Usagi on 2017/3/22.
 */

public interface OnMozbiiListener {
    /**
     * 當筆成功連線並且成功註冊完所有功能後會callback
     */
    void onMozbiiConnected();

    /**
     * 當連線中斷時會callback
     */
    void onMozbiiDisconnected();

    /**
     * 將目前偵測到的顏色傳回
     * @param color
     */
    void onMozbiiColorDetected(int color);

}

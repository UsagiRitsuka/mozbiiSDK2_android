package ufro.com.mozbiisdkandroid2;

import java.util.UUID;

/**
 * Created by Usagi on 2017/3/22.
 */

public interface OnMozbiiListener {
    void onMozbiiConnected();

    void onMozbiiDisconnected();

    /**
     * 只接收目前偵測到的顏色，暫時沒有使用
     * @param color
     */
    void onMozbiiColorDetected(int color);

    /**
     * 目前xplore, pillare偵測顏色皆使用此接口
     * 共12種顏色
     * @param colorArray 會傳12種顏色回來
     * @param index 目前偵測到的顏色的index, range: 0~11
     */
    void onMozbiiColorArrayDetected(int colorArray[], int index);

    /**
     * Called when color index changed. Like button up or down clicked, or
     * captured a new color.
     * @param index : color index of Mozbii, should be 0-11.
     */
    void onMozbiiColorIndexChanged(int index);
}

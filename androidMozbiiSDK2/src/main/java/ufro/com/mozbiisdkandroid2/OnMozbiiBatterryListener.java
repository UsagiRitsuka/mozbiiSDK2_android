package ufro.com.mozbiisdkandroid2;

/**
 * Created by Usagi on 2017/5/12.
 */

public interface OnMozbiiBatterryListener {
    /**
     * level value range: 0~100
     * @param level
     */
    void onMozbiiBatteryLevelChanged(int level);
    /**
     * Low is below 20%
     */
    void onMozbiiBatteryLevelLow();
}

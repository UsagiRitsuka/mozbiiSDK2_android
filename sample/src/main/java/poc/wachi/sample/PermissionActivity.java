package poc.wachi.sample;

/**
 * Created by Usagi on 2017/5/10.
 */

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Usagi on 2017/4/27.
 */

public abstract class PermissionActivity extends AppCompatActivity {
    private final int PERMISSION_REQUEST= 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED){
                requestPermission();
            }
        }
    }

    private void requestPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST:
                if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    // todo do something
                }
                break;
        }
    }


}
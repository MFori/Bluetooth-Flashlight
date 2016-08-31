package cz.martinforejt.bluetoothflashlight;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

/**
 * Created by Martin Forejt on 21.08.2016.
 * forejt.martin97@gmail.com
 */
public class FlashManager {
    private Context context;
    private Boolean hasFlash = null;
    public boolean isFlashOn = false;

    // pre lollipop
    private Camera camera = null;
    private Camera.Parameters params;

    // lollipop
    private CameraManager cameraManager = null;
    private CameraCaptureSession mSession;
    private CaptureRequest.Builder mBuilder;
    private CameraDevice mCameraDevice;
    private String cameraId;


    public FlashManager(Context context) {
        this.context = context;
    }

    public void flash(boolean ON) {
        if(hasFlash()) {
            //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (camera == null) getCamera();
            params = camera.getParameters();
            params.setFlashMode(ON ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            if (ON) {
                camera.startPreview();
                isFlashOn = true;
            } else {
                camera.stopPreview();
                isFlashOn = false;
            }
        /*} else {
            if (cameraManager == null) getCamera2();
            try {
                cameraManager.setTorchMode(cameraId, ON);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }*/
        }
    }



    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void getCamera2() {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
            cameraManager = null;
        }
    }

    private void getCamera() {
        try {
            camera = Camera.open();
            params = camera.getParameters();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if device support camera flash
     *
     * @return bool
     */
    public boolean hasFlash() {
        if (hasFlash == null) {
            hasFlash = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        }

        return hasFlash;
    }

    public void destroy() {
        if(camera != null) {
            camera.release();
            camera = null;
        }
    }

}

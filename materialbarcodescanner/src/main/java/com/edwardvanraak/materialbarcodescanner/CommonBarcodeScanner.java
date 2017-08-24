package com.edwardvanraak.materialbarcodescanner;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class CommonBarcodeScanner {
    private static final Logger logger = LoggerFactory.getLogger(CommonBarcodeScanner.class);

    @Nullable
    private MaterialBarcodeScannerBuilder materialBarcodeScannerBuilder;


    CommonBarcodeScanner(@NonNull MaterialBarcodeScannerBuilder materialBarcodeScannerBuilder) {
        this.materialBarcodeScannerBuilder = materialBarcodeScannerBuilder;
    }

    void enableTorch() throws SecurityException {
        materialBarcodeScannerBuilder.getCameraSource().setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        try {
            materialBarcodeScannerBuilder.getCameraSource().start();
        }
        catch (IOException e) {
            logger.error("Oops!", e);
        }
    }

    void disableTorch() throws SecurityException {
        materialBarcodeScannerBuilder.getCameraSource().setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        try {
            materialBarcodeScannerBuilder.getCameraSource().start();
        }
        catch (IOException e) {
            logger.error("Oops!", e);
        }
    }

    boolean isFlashAvailable() {
        return materialBarcodeScannerBuilder.getActivity().getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    void updateCenterTrackerForDetectedState() {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                if (materialBarcodeScannerBuilder.getScannerMode() == MaterialBarcodeScanner.SCANNER_MODE_CENTER) {
                    ImageView centerTracker = materialBarcodeScannerBuilder.getActivity().findViewById(R.id.barcode_square);
                    centerTracker.setImageResource(materialBarcodeScannerBuilder.getTrackerDetectedResourceID());
                }
            }
        };

        materialBarcodeScannerBuilder.getActivity().runOnUiThread(runnable);
    }

    void clean() {
        materialBarcodeScannerBuilder = null;
    }
}
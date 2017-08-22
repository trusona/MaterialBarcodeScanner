package com.edwardvanraak.materialbarcodescanner;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.edwardvanraak.materialbarcodescanner.MaterialBarcodeScannerActivity.RC_HANDLE_GMS;
import static junit.framework.Assert.assertNotNull;

public class MaterialBarcodeScannerFragment extends Fragment {
    private static final Logger logger = LoggerFactory.getLogger(MaterialBarcodeScannerFragment.class);
    private static final int defaultLayout = R.layout.fragment_barcode_capture;

    private MaterialBarcodeScannerBuilder materialBarcodeScannerBuilder;
    private GraphicOverlay<BarcodeGraphic> barcodeGraphicOverlay;
    private CameraSourcePreview cameraSourcePreview;
    private BarcodeDetector barcodeDetector;
    private boolean detectionConsumed;
    private boolean flashOn;
    private int layout;


    public MaterialBarcodeScannerFragment() {
        detectionConsumed = false;
        flashOn = false;
    }

    public static MaterialBarcodeScannerFragment newInstance(@Nullable Integer layout) {
        Bundle bundle = new Bundle();
        bundle.putInt("layout", layout == null ? defaultLayout : layout);

        MaterialBarcodeScannerFragment fragment = new MaterialBarcodeScannerFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMaterialBarcodeScanner(MaterialBarcodeScanner materialBarcodeScanner) {
        materialBarcodeScannerBuilder = materialBarcodeScanner.getMaterialBarcodeScannerBuilder();
        barcodeDetector = materialBarcodeScanner.getMaterialBarcodeScannerBuilder().getBarcodeDetector();
        startCameraSource();
        setupLayout();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        layout = getArguments().getInt("layout");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        return inflater.inflate(layout, viewGroup, false);
    }

    private void setupLayout() {
        if (!materialBarcodeScannerBuilder.getText().isEmpty()) {
            String topText = materialBarcodeScannerBuilder.getText();
            TextView topTextView = getActivity().findViewById(R.id.topText);
            topTextView.setText(topText);
        }

        setupButtons();
        setupCenterTracker();
    }

    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext());

        if (code == ConnectionResult.SUCCESS) {
            barcodeGraphicOverlay = getActivity().findViewById(R.id.graphicOverlay);

            BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(barcodeGraphicOverlay,
                newDetectionListener, materialBarcodeScannerBuilder.getTrackerColor());

            barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());
            CameraSource cameraSource = materialBarcodeScannerBuilder.getCameraSource();

            if (cameraSource != null) {
                try {
                    cameraSourcePreview = getActivity().findViewById(R.id.preview);
                    cameraSourcePreview.start(cameraSource, barcodeGraphicOverlay);
                }
                catch (IOException e) {
                    logger.error("Unable to start camera source.", e);
                    cameraSource.release();
                }
            }
        }
        else {
            GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, RC_HANDLE_GMS).show();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    private void updateCenterTrackerForDetectedState() {
        if (materialBarcodeScannerBuilder.getScannerMode() == MaterialBarcodeScanner.SCANNER_MODE_CENTER) {
            ImageView centerTracker = getActivity().findViewById(R.id.barcode_square);
            centerTracker.setImageResource(materialBarcodeScannerBuilder.getTrackerDetectedResourceID());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (cameraSourcePreview != null) {
            cameraSourcePreview.stop();
        }
    }

    private void setupCenterTracker() {
        if (materialBarcodeScannerBuilder.getScannerMode() == MaterialBarcodeScanner.SCANNER_MODE_CENTER) {
            ImageView centerTracker = getActivity().findViewById(R.id.barcode_square);
            centerTracker.setImageResource(materialBarcodeScannerBuilder.getTrackerResourceID());
            barcodeGraphicOverlay.setVisibility(View.INVISIBLE);
        }
    }

    private void setupButtons() {
        final LinearLayout flashOnButton = getActivity().findViewById(R.id.flashIconButton);
        final ImageView flashToggleIcon = getActivity().findViewById(R.id.flashIcon);
        assertNotNull(flashOnButton);
        flashOnButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (flashOn) {
                    flashToggleIcon.setBackgroundResource(R.drawable.ic_flash_on_white_24dp);
                    disableTorch();
                }
                else {
                    flashToggleIcon.setBackgroundResource(R.drawable.ic_flash_off_white_24dp);
                    enableTorch();
                }
                flashOn ^= true;
            }
        });
        if (materialBarcodeScannerBuilder.isFlashEnabledByDefault()) {
            flashToggleIcon.setBackgroundResource(R.drawable.ic_flash_off_white_24dp);
        }
    }

    private void enableTorch() throws SecurityException {
        materialBarcodeScannerBuilder.getCameraSource().setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        try {
            materialBarcodeScannerBuilder.getCameraSource().start();
        }
        catch (IOException e) {
            logger.error("Oops!", e);
        }
    }

    private void disableTorch() throws SecurityException {
        materialBarcodeScannerBuilder.getCameraSource().setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        try {
            materialBarcodeScannerBuilder.getCameraSource().start();
        }
        catch (IOException e) {
            logger.error("Oops!", e);
        }
    }

    protected NewDetectionListener newDetectionListener = new NewDetectionListener() {

        @Override
        public void onNewDetection(Barcode barcode) {
            if (!detectionConsumed) {
                detectionConsumed = true;
                logger.debug("Barcode detected! => {}", barcode.displayValue);

                EventBus.getDefault().postSticky(barcode);

                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        updateCenterTrackerForDetectedState();
                    }
                });

                if (materialBarcodeScannerBuilder.isBleepEnabled()) {
                    new SoundPlayer(getContext(), R.raw.bleep);
                }

                //barcodeGraphicOverlay.postDelayed(finisher, 50);
            }
        }
    };
}
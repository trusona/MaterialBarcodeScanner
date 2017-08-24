package com.edwardvanraak.materialbarcodescanner;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

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

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.OnClickListener;
import static com.edwardvanraak.materialbarcodescanner.MaterialBarcodeScannerActivity.RC_HANDLE_GMS;

public class MaterialBarcodeScannerFragment extends Fragment {
    private static final Logger logger = LoggerFactory.getLogger(MaterialBarcodeScannerFragment.class);
    private static final int defaultLayout = R.layout.fragment_barcode_capture;

    @Nullable
    private CommonBarcodeScanner commonBarcodeScanner;

    @Nullable
    private CameraSourcePreview cameraSourcePreview;

    private MaterialBarcodeScannerBuilder materialBarcodeScannerBuilder;
    private GraphicOverlay<BarcodeGraphic> barcodeGraphicOverlay;
    private BarcodeDetector barcodeDetector;
    protected boolean detectionConsumed;
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
        logger.info("@onMaterialBarcodeScanner");
        materialBarcodeScannerBuilder = materialBarcodeScanner.getMaterialBarcodeScannerBuilder();
        commonBarcodeScanner = new CommonBarcodeScanner(materialBarcodeScannerBuilder);
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
        setupButtons();
        setupCenterTracker();
    }

    private void startCameraSource() throws SecurityException {
        logger.info("@startCameraSource()");
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext());

        if (code == ConnectionResult.SUCCESS) {
            barcodeGraphicOverlay = getActivity().findViewById(R.id.graphicOverlay);

            // TODO: Sending null here might fix the issue in which the barcode was sent twice
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

    @Override
    public void onStart() {
        logger.info("@onStart");
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        logger.info("@onStop");
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onPause() {
        logger.info("@onPause");
        super.onPause();

        if (cameraSourcePreview != null) {
            cameraSourcePreview.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clean();
    }

    private void setupCenterTracker() {
        logger.info("@setupCenterTracker()");

        if (materialBarcodeScannerBuilder.getScannerMode() == MaterialBarcodeScanner.SCANNER_MODE_CENTER) {
            ImageView centerTracker = getActivity().findViewById(R.id.barcode_square);
            centerTracker.setImageResource(materialBarcodeScannerBuilder.getTrackerResourceID());
            barcodeGraphicOverlay.setVisibility(INVISIBLE);
        }
    }

    private void setupButtons() {
        logger.info("@setupButtons()");
        LinearLayout flashButton = getActivity().findViewById(R.id.flashIconButton);

        if (commonBarcodeScanner.isFlashAvailable()) {
            OnClickListener onClickListener = new OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (flashOn) {
                        commonBarcodeScanner.disableTorch();
                    }
                    else {
                        commonBarcodeScanner.enableTorch();
                    }
                    flashOn ^= true;
                }
            };

            flashButton.setOnClickListener(onClickListener);
        }
        else {
            flashButton.setVisibility(GONE);
        }
    }

    private void clean() {
        logger.info("@clean()");
        EventBus.getDefault().removeStickyEvent(MaterialBarcodeScanner.class);

        if (commonBarcodeScanner != null) {
            commonBarcodeScanner.clean();
            commonBarcodeScanner = null;
        }

        if (cameraSourcePreview != null) {
            cameraSourcePreview.release();
            cameraSourcePreview = null;
        }
    }

    private NewDetectionListener newDetectionListener = new NewDetectionListener() {

        @Override
        public void onNewDetection(Barcode barcode) {
            logger.info("@onNewDetection(Barcode)");

            if (!detectionConsumed) {
                detectionConsumed = true;
                logger.debug("Barcode detected! => {}", barcode.displayValue);

                EventBus.getDefault().postSticky(barcode);
                commonBarcodeScanner.updateCenterTrackerForDetectedState();

                if (materialBarcodeScannerBuilder.isBleepEnabled()) {
                    new SoundPlayer(getContext(), R.raw.bleep);
                }
            }
        }
    };
}
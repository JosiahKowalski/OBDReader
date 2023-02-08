package com.example.obdreader.BluetoothLeObd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.obdreader.DeviceListAdapter;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.BluetoothPeripheralManager;
import com.welie.blessed.WriteType;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class BluetoothLe {
    private BluetoothGatt bluetoothGatt;
    private BluetoothPeripheral bluetoothDevice;
    private static final UUID NOTIFIABLE_CHARACTERISTIC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothGattCharacteristic writable;
    private Context context;
    private boolean isScanning = false;
    private DeviceListAdapter deviceListAdapter;

    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        public void onServicesDiscovered(BluetoothPeripheral peripheral) {
            BluetoothGattCharacteristic notifiable;
            List<BluetoothGattService> services = peripheral.getServices();
            for (BluetoothGattService service : services) {
                notifiable = null;
                writable = null;
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    if (isCharacteristicNotifiable(characteristic)) {
                        notifiable = characteristic;
                    }
                    if (isCharacteristicWritable(characteristic)) {
                        writable = characteristic;
                    }
                    if (notifiable != null && writable != null) {
                        break;
                    }
                }
                if (notifiable != null && writable != null){
                    peripheral.setNotify(notifiable, true);
                }
            }
        }


        public void onCharacteristicUpdate(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("BluetoothLe", "Received: " + Arrays.toString(value));
        }

        public void onNotificationStateUpdate(BluetoothPeripheral peripheral, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (peripheral.isNotifying(characteristic)) {
                    Log.d("BluetoothLe", "Enabled notifications for " + characteristic.getUuid());
                } else {
                    Log.d("BluetoothLe", "Disabled notifications for " + characteristic.getUuid());
                }
            } else {
                Log.d("BluetoothLe", "Error changing notification state for " + characteristic.getUuid() + " status: " + status);
            }
        }
    };

    private final BluetoothCentralManager central;

    public BluetoothLe(@NonNull Context context, @NonNull DeviceListAdapter deviceListAdapter) {
        this.context = context;
        this.deviceListAdapter = deviceListAdapter;

        BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {
            @Override
            public void onDiscoveredPeripheral(@NonNull BluetoothPeripheral peripheral, @NonNull ScanResult scanResult) {
                if (peripheral.getName().isEmpty()) return;
                deviceListAdapter.addDevice(peripheral);
            }
        };

        central = new BluetoothCentralManager(context, bluetoothCentralManagerCallback, new Handler(Looper.getMainLooper()));
    }


    // region Public Methods

    public BluetoothPeripheral getSavedDevice(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("device", Context.MODE_PRIVATE);
        String name = preferences.getString("name", null);
        String address = preferences.getString("address", null);
        if (name == null || address == null)
            return null;
        bluetoothDevice = central.getPeripheral(address);
        return bluetoothDevice;
   }

   public void saveDevice(Context context, BluetoothPeripheral device) {
        SharedPreferences preferences = context.getSharedPreferences("device", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("name", device.getName());
        editor.putString("address", device.getAddress());
        editor.apply();
        bluetoothDevice = device;
    }

    // new String[] {"OBDII", "OBD2", "OBDII-ELM327", "ELM327", "OBDBLE"}
    public void startScanWithNames(String [] names){
        if (isScanning) return;
        isScanning = true;
        central.scanForPeripheralsWithNames(names);
    }

    public void startScan(){
        if (isScanning) return;
        isScanning = true;
        central.scanForPeripherals();
    }

    public void connect(BluetoothPeripheral device) {
        bluetoothDevice = device;
//        bluetoothDevice.createBond();
        central.connectPeripheral(bluetoothDevice, peripheralCallback);
    }

    public void connectSavedDevice(){
        if (getSavedDevice(context) == null) return;
        central.connectPeripheral(bluetoothDevice, peripheralCallback);
    }

    public void disconnect() {
        if (bluetoothDevice != null) {
            bluetoothDevice.cancelConnection();
        }
    }

    public void stopScan() {
        if (isScanning) {
            central.stopScan();
            isScanning = false;
        }
    }

    public boolean write(String command, WriteType writeType) {
        if (writable != null && bluetoothDevice != null)
           return bluetoothDevice.writeCharacteristic(writable, command.getBytes(), writeType);
        else return false;
    }

    public boolean write(byte [] command, WriteType writeType) {
        if (writable != null && bluetoothDevice != null)
            return bluetoothDevice.writeCharacteristic(writable, command, writeType);
        else return false;
    }

    public void setListAdapter(DeviceListAdapter deviceListAdapter) {
        this.deviceListAdapter = deviceListAdapter;
    }

    // endregion



    //region Private Methods


//    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
//        @SuppressLint("MissingPermission")
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                gatt.discoverServices();
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                gatt.close();
//            }
//        }
//        @SuppressLint("MissingPermission")
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                BluetoothGattCharacteristic notifiable;
//                List<BluetoothGattService> services = gatt.getServices();
//                for (BluetoothGattService service : services) {
//                    notifiable = null;
//                    writable = null;
//                    Log.d("service", service.getUuid().toString());
//                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
//                    for (BluetoothGattCharacteristic characteristic : characteristics) {
//                        if (isCharacteristicNotifiable(characteristic)) {
//                            notifiable = characteristic;
//                        }
//                        if (isCharacteristicWritable(characteristic)) {
//                            writable = characteristic;
//                        }
//                        if (notifiable != null && writable != null) {
//                            break;
//                        }
//                    }
//                    if (notifiable != null && writable != null){
//                        BluetoothGattDescriptor descriptor = notifiable.getDescriptor(NOTIFIABLE_CHARACTERISTIC_UUID);
//                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                        gatt.setCharacteristicNotification(notifiable, true);
//                    }
//                }
//
//            } else {
//                Log.w("TAG", "onServicesDiscovered received: " + status);
//            }
//        }
//
//        // on characteristic changed
//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            Log.d("characteristic", characteristic.getUuid().toString());
//            Log.d("characteristic", Arrays.toString(characteristic.getValue()));
//            // this is where results will come in from the device
//
//        }
//    };
    //endregion

    //region Helper Methods
    /**
     * @return Returns <b>true</b> if property is supports write, <b>false</b> otherwise.
     */
    private boolean isCharacteristicWritable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
    }
    /**
     * @return Returns <b>true</b> if property is supports notification, <b>false</b> otherwise.
     */
    private boolean isCharacteristicNotifiable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }
    //endregion

}

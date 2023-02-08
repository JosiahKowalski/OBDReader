package com.example.obdreader;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.github.pires.obd.commands.protocol.*;
import com.github.pires.obd.commands.control.*;
import com.github.pires.obd.enums.ObdProtocols;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.PeripheralType;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ObdReading {

    public static BluetoothPeripheral mDevice;
//    public static ArrayList<BluetoothDevice> mDevices = new ArrayList<>();
    private static BluetoothConnector.BluetoothSocketWrapper socket;
    private static BluetoothGatt mBluetoothGatt;
    private static Context fragmentContext;

    public static void connectDevice(Context context, BluetoothDevice device, BluetoothAdapter adapter) {
        // implementation of connecting to the device
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // 00001101-0000-1000-8000-00805f9b34fb - blue
            // 00001101-0000-1000-8000-00805f9b34fb - black - test clear one when it arrives
            // 00001101-0000-1000-8000-00805f9b34fb maybe this one? if it is, could use this to tell if it is obd or not
            List<UUID> uuids = device.getUuids() != null
                    ? Arrays.stream(device.getUuids()).map(ParcelUuid::getUuid).collect(Collectors.toList())
                    : new ArrayList<>();

            if (uuids.isEmpty()) {
                uuids.add(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
            }

            BluetoothConnector bluetoothConnector = new BluetoothConnector(device, false, adapter, uuids);

            socket = bluetoothConnector.connect();

            new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
            new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());

            // set headers to 7e1 which reads from 7e9 or the transmission
            new CustomObdProtocolCommand("AT SH 7E1").run(socket.getInputStream(), socket.getOutputStream());
            new HeadersOffCommand().run(socket.getInputStream(), socket.getOutputStream());

            getMilesSinceLastClear(context);
            socket.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Toast.makeText(context, "Bluetooth device not connected", Toast.LENGTH_SHORT).show();
        }
    }

    public static void connectDeviceLe(Context context, BluetoothDevice device) {
        fragmentContext = context;
        device.connectGatt(context, false, mGattCallback);

    }


    // BluetoothGattCallback
    private static final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Attempts to discover services after successful connection.
                mBluetoothGatt = gatt;
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattCharacteristic notifiable;
                BluetoothGattCharacteristic writable;
                // get services
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    notifiable = null;
                    writable = null;
                    Log.d("service", service.getUuid().toString());
                    // get characteristics
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {

                        Log.d("characteristic", characteristic.getUuid().toString());
                        if (isCharacteristicNotifiable(characteristic) && isCharacteristicWritable(characteristic)) {
                            notifiable = characteristic;
                            writable = characteristic;
                        }
                    }
                    if (notifiable != null && writable != null){
                        // get descriptor
                        BluetoothGattDescriptor descriptor = notifiable.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        // enable notifications
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        // set notifications
                        gatt.setCharacteristicNotification(notifiable, true);

//                        gatt.writeDescriptor(descriptor);

                        // wait 2 seconds
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        byte[] bytes = "01 31\r".getBytes();
                        writable.setValue(bytes);

                        if (!gatt.writeCharacteristic(writable)) {
                                Log.d("write", "failed");
                        }
                        Log.d("characteristic", "found both");
                        break;
                    }
                }

                // get the service
                // get the characteristic
                // set the notification
            } else {
                Log.w("TAG", "onServicesDiscovered received: " + status);
            }
        }

        // on characteristic changed
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d("characteristic", characteristic.getUuid().toString());
            Log.d("characteristic", Arrays.toString(characteristic.getValue()));
            String hexString = bytesToHex(characteristic.getValue());
            Log.d("characteristic", hexString);

            // convert hex to readable ascii

            String string = HexToString(hexString);
            Log.d("result", string);
            // show value to user
//            Toast.makeText(fragmentContext, Arrays.toString(characteristic.getValue()), Toast.LENGTH_SHORT).show();
        }

    };

    public static boolean isCharacteristicWritable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * @return Returns <b>true</b> if property is Readable
     */
    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic pChar) {
        return ((pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    /**
     * @return Returns <b>true</b> if property is supports notification
     */
    public static boolean isCharacteristicNotifiable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static String HexToString(String hex) {
        String result = new String();
        char[] charArray = hex.toCharArray();
        for(int i = 0; i < charArray.length; i=i+2) {
            String st = ""+charArray[i]+""+charArray[i+1];
            char ch = (char)Integer.parseInt(st, 16);
            result = result + ch;
        }
        return result;
    }

    public static void getMilesSinceLastClear(Context context) throws IOException, InterruptedException {
        DistanceSinceCCCommand distanceSinceCCCommand = new DistanceSinceCCCommand();

        distanceSinceCCCommand.useImperialUnits(true);
        distanceSinceCCCommand.run(socket.getInputStream(), socket.getOutputStream());
        Toast.makeText(context, distanceSinceCCCommand.getFormattedResult(), Toast.LENGTH_SHORT).show();
    }

    public static void connectSavedDevice(Context context, BluetoothAdapter adapter) {
        if (mDevice == null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
            String deviceName = sharedPreferences.getString("deviceName", "");
            String deviceAddress = sharedPreferences.getString("deviceAddress", "");
            if (deviceName.equals("") || deviceAddress.equals("")) {
                Toast.makeText(context, "No saved device", Toast.LENGTH_SHORT).show();
                return;
            }
//            mDevice = adapter.getRemoteDevice(deviceAddress);
        }
        // check if device is bluetooth le or not
        if (mDevice.getType() == PeripheralType.LE) {
//            connectDeviceLe(context, mDevice);
        } else {
//            connectDevice(context, mDevice, adapter);
        }

    }



    public static void saveDevice(Context context, BluetoothPeripheral device) {
        // save device to shared preferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("device", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", device.getName());
        editor.putString("address", device.getAddress());
        editor.apply();
        mDevice = device;
    }

//    public static BluetoothPeripheral getSavedDevice(Context context) {
//        SharedPreferences preferences = context.getSharedPreferences("device", Context.MODE_PRIVATE);
//        String name = preferences.getString("name", null);
//        String address = preferences.getString("address", null);
//        if (name == null || address == null)
//            return null;
//        // use getAdapter to get the adapter with context
//        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
//
//    }
}

class CustomObdProtocolCommand extends ObdProtocolCommand {
    public CustomObdProtocolCommand(String command) {
        super(command);
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Custom OBD Protocol Command";
    }
}

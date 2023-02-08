package com.example.obdreader.BluetoothLeObd;

import android.bluetooth.BluetoothGatt;

import java.util.ArrayList;

public abstract class BluetoothLeObdCommand {
    protected ArrayList<Integer> buffer = null;
    private String command;
    protected boolean useImperialUnits = false;
    protected String rawData = null;
    protected Long responseDelayInMs = null;
    private long start;
    private long end;
    private BluetoothGatt mGatt;

    public BluetoothLeObdCommand(String command, BluetoothGatt gatt) {
        this.command = command;
        this.mGatt = gatt;
    }

    public void run() {
        synchronized (BluetoothLeObdCommand.class) {
            start = System.currentTimeMillis();
            establishConnection();
        }
    }

    private void establishConnection() {

    }

}

package com.example.obdreader;

import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.obdreader.BluetoothLeObd.BluetoothLe;
import com.example.obdreader.databinding.BluetoothDevicesBinding;
import com.welie.blessed.BluetoothPeripheral;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BluetoothDevicesFragment extends Fragment {

    private BluetoothDevicesBinding binding;
    DeviceListAdapter deviceListAdapter;
    // maybe use property
    BluetoothLeScanner scanner;
    boolean isScanning = false;
    private Context context;
    public BluetoothLe bluetoothLe;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = BluetoothDevicesBinding.inflate(inflater, container, false);

        context = getActivity();

        assert context != null;
        RecyclerView recyclerView = binding.bluetoothDevicesRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        deviceListAdapter = new DeviceListAdapter();
        recyclerView.setAdapter(deviceListAdapter);

        bluetoothLe = new BluetoothLe(context, deviceListAdapter);

        return binding.getRoot();

    }

    // start searching for devices when the fragment is shown
    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getContext()).bluetoothLe.startScan();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // stop searching for devices when the fragment is hidden
    @Override
    public void onPause() {
        super.onPause();
        bluetoothLe.stopScan();
    }

}


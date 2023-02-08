package com.example.obdreader;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.obdreader.BluetoothLeObd.BluetoothLe;
import com.welie.blessed.BluetoothPeripheral;

import java.util.ArrayList;
import java.util.List;

// create bluetooth device adapter
public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {
    private final List<BluetoothPeripheral> mDevices = new ArrayList<>();

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothPeripheral device = mDevices.get(position);
        String deviceName = device.getName();
        String deviceAddress = device.getAddress();
        if (deviceName.isEmpty()) {
            return;
        }
        holder.deviceName.setText(deviceName);
        holder.deviceAddress.setText(deviceAddress);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Saved " + deviceName, Toast.LENGTH_SHORT).show();
                // save device

                // navigate to first fragment
                Navigation.findNavController(v).navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public void addDevice(BluetoothPeripheral device) {
        if (!mDevices.contains(device)) {
            mDevices.add(device);
            // notify the adapter that the data has changed
            // needs testing, if doesn't work use (notifyDataSetChanged)
            notifyItemInserted(mDevices.size() - 1);
        }
    }

    public void saveDevice(Context context, BluetoothPeripheral device) {
        SharedPreferences preferences = context.getSharedPreferences("device", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("name", device.getName());
        editor.putString("address", device.getAddress());
        editor.apply();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceAddress;

        DeviceViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceAddress = itemView.findViewById(R.id.device_address);
        }
    }
}

package com.example.obdreader;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.obdreader.BluetoothLeObd.BluetoothLe;
import com.example.obdreader.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<String> mDeviceList = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    private AlertDialog mDialog;
    private BluetoothDevice mDevice;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();
    private BluetoothConnector.BluetoothSocketWrapper socket;
    private NavController navController;
    public BluetoothLe bluetoothLe;
    private DeviceListAdapter deviceListAdapter;
    private static final int BLUETOOTH_CONNECT_PERMISSION_REQUEST_CODE = 1;
    private static final int BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE = 2;

    // required permissions
    String[] PERMISSIONS = {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

//        if (mBluetoothAdapter == null) {
//            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
//            finish();
//        }

        if (!hasPermissions(this, PERMISSIONS)) {
            // Request the permission
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
        } else {
            // Permission has already been granted
//            ObdReading.mDevice = ObdReading.getSavedDevice(this);
            if (ObdReading.mDevice == null) {
                // No saved device, so show the device list
                navController.navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        }


        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                // navigate to second fragment if on first fragment, otherwise navigate back to first fragment
                if (navController.getCurrentDestination().getId() == R.id.FirstFragment) {
                    navController.navigate(R.id.action_FirstFragment_to_SecondFragment);
                } else if (navController.getCurrentDestination().getId() == R.id.BluetoothDevices) {
                    navController.navigate(R.id.action_SecondFragment_to_FirstFragment);
                }
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLUETOOTH_CONNECT_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "BLUETOOTH_CONNECT permission granted", Toast.LENGTH_SHORT).show();
                if (mDevice == null) {
                    // No saved device, so show the device list
                    navController.navigate(R.id.action_FirstFragment_to_SecondFragment);
                }
            } else {
                // Permission denied
                Toast.makeText(this, "BLUETOOTH_CONNECT permission denied, go into settings to enable", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
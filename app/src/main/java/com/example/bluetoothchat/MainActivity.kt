package com.example.bluetoothchat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceAdapter: DeviceAdapter
    private val discoveredDevices = mutableListOf<BluetoothDevice>()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        deviceAdapter =
            DeviceAdapter(discoveredDevices) { device ->
                connectToDevice(device)
            }

        val recyclerViewDevices = findViewById<RecyclerView>(R.id.recyclerViewDevices)
        recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        recyclerViewDevices.adapter = deviceAdapter

        findViewById<Button>(R.id.btnEnableBluetooth).setOnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                showPairedDevices()
            }
        }

        findViewById<Button>(R.id.btnDiscoverDevices).setOnClickListener {
            startDiscovery()
        }

        requestPermissions()

        // Register for broadcasts when a device is discovered
        registerReceiver(
            receiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND).apply {
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            },
        )
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        val uuid: UUID = device.uuids.firstOrNull()?.uuid ?: MY_UUID
        val bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
        try {
            bluetoothSocket.connect()
            (applicationContext as MyApp).bluetoothSocket = bluetoothSocket
            startActivity(Intent(this, ChatActivity::class.java))
        } catch (e: IOException) {
            e.printStackTrace()
            bluetoothSocket.close()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showPairedDevices() {
        val pairedDevices = bluetoothAdapter.bondedDevices
        discoveredDevices.clear()
        if (pairedDevices.isNotEmpty()) {
            discoveredDevices.addAll(pairedDevices)
        }
        deviceAdapter.notifyDataSetChanged()
    }

    private fun startDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST,
            )
            return
        }

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
    }

    private fun requestPermissions() {
        val permissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { permissions ->
                val granted = permissions.entries.all { it.value }
                if (!granted) {
                    // Handle case where permissions are not granted
                }
            }

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
        )
    }

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            if (!discoveredDevices.contains(it)) {
                                discoveredDevices.add(it)
                                deviceAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        // Discovery finished, you can update UI to reflect that
                    }
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    companion object {
        const val REQUEST_ENABLE_BT = 1
        const val LOCATION_PERMISSION_REQUEST = 2
        val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}

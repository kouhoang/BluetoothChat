package com.example.bluetoothchat

import android.app.Application
import android.bluetooth.BluetoothSocket

class MyApp : Application() {
    var bluetoothSocket: BluetoothSocket? = null
}

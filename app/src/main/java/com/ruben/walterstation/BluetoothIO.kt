package com.ruben.walterstation

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class BluetoothIO(private val context: Context, private val activity: Activity)
{
    companion object
    {
        const val MAC_ADDRESS = "B8:27:EB:7A:FF:77"
        val BT_UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee")!!

        fun showExitAlert(context: Context, activity: Activity, errorMessage: String)
        {
            AlertDialog.Builder(context).setTitle("Error").setMessage(errorMessage).setNeutralButton("Exit") { _, _ ->
                activity.finish()
                System.exit(-1)
            }.setIcon(android.R.drawable.ic_dialog_alert).show()
        }
    }

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null
    var connected = false

    private val bluetoothReceiver = object: BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            if (BluetoothDevice.ACTION_FOUND == intent.action)
            {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                if (device.address == MAC_ADDRESS)
                {
                    bluetoothDevice = device
                    activity.unregisterReceiver(this)
                    bluetoothAdapter!!.cancelDiscovery()

                    activity.mainTextView.text = device.name + ": " + device.address
                }
            }
        }
    }

    fun initialize()
    {
        bluetoothAdapter!!.enable()

        if (bluetoothAdapter == null)
            showExitAlert(context, activity, "No Bluetooth adapter found")

        var devices: MutableSet<BluetoothDevice>

        do
        {
            Thread.sleep(500)
            devices = bluetoothAdapter!!.bondedDevices

        } while (devices.isEmpty())

        for (device in devices)
        {
            if (device.address == MAC_ADDRESS)
            {
                bluetoothDevice = device
                activity.mainTextView.text = device.name + ": " + device.address
                break
            }
        }

        if (bluetoothDevice == null)
        {
            val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            activity.registerReceiver(bluetoothReceiver, intentFilter)

            if (!bluetoothAdapter!!.startDiscovery())
                showExitAlert(context, activity, "Couldn't start Bluetooth search")
        }

        if (bluetoothDevice == null)
            showExitAlert(context, activity, "Raspberry Pi not found!")

        bluetoothSocket = bluetoothDevice!!.createRfcommSocketToServiceRecord(BT_UUID)

        if (!bluetoothSocket!!.isConnected)
            bluetoothSocket!!.connect()

        connected = true
    }

    fun write(data: String)
    {
        bluetoothSocket!!.outputStream.write(data.toByteArray())
    }

    fun read(bytes: ByteArray)
    {
        bluetoothSocket!!.inputStream.read(bytes)
    }

    fun close()
    {
        bluetoothSocket!!.close()
        connected = false
    }
}

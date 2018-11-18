package com.ruben.walterstation

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.AsyncTask
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.text.*
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*


class BluetoothTask(private val bluetoothIO: BluetoothIO) :
        AsyncTask<Void, ByteArray, Void>()
{
    private var readBuffer = ByteArray(1024)
    private var activity : WeakReference<Activity> = WeakReference(bluetoothIO.activity)

    private val units = mapOf(
        "CO" to "ppm", "TEMP" to "ºC", "TURBIDITY" to "NTU",
        "LUXES" to "lx", "TEMPH2O" to "ºC")

    override fun doInBackground(vararg p0: Void?): Void?
    {
        while (bluetoothIO.connected)
        {
            bluetoothIO.read(readBuffer)
            publishProgress(readBuffer)
        }

        return null
    }

    override fun onProgressUpdate(vararg values: ByteArray)
    {
        val jsonObj = JSONObject(String(values[0]))
        var str = ""

        for (i in 0 until jsonObj.names().length())
        {
            var name = jsonObj.names().getString(i)
            str += name + "(" + units[name] + "):  " + jsonObj.get(name)

            if (i != jsonObj.names().length() - 1)
                str += "\n"
        }

        val mainActivity = activity.get()!! as MainActivity
        mainActivity.dataReceived.text = str

        if (mainActivity.mapReady)
        {
            val coord = jsonObj.get("GPS").toString().split(",")
            val location = LatLng(coord[0].toDouble(), coord[1].toDouble())
            mainActivity.map!!.addMarker(MarkerOptions().position(location).title("Current position"))
            mainActivity.map!!.moveCamera(CameraUpdateFactory.newLatLng(location))
        }

    }

    override fun onPostExecute(result: Void?)
    {
        Toast.makeText(activity.get(), "Device disconnected!", Toast.LENGTH_LONG).show()
        activity.get()!!.connectedTextView.text = "State: disconnected"
    }
}


class BluetoothIO(private val context: Context, val activity: Activity)
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

    fun initialize()
    {
		if (bluetoothAdapter == null)
            showExitAlert(context, activity, "No Bluetooth adapter found")

        if (!bluetoothAdapter!!.isEnabled)
            bluetoothAdapter!!.enable()

        var devices: MutableSet<BluetoothDevice>

        do
        {
            Thread.sleep(50)
            devices = bluetoothAdapter!!.bondedDevices

        } while (devices.isEmpty())

        for (device in devices)
        {
            if (device.address == MAC_ADDRESS)
            {
                bluetoothDevice = device
                break
            }
        }

        if (bluetoothDevice == null)
            showExitAlert(context, activity, "Raspberry Pi not found in paired devices, pair it first!")

        bluetoothSocket = bluetoothDevice!!.createRfcommSocketToServiceRecord(BT_UUID)

        connected = try {
            bluetoothSocket!!.connect()
            true
        } catch (exception: Exception) { false }
    }

    fun write(data: String)
    {
        if (connected)
        {
            try {
                bluetoothSocket!!.outputStream.write(data.toByteArray())
            } catch (exception: Exception) { close() }
        }
    }

    fun read(bytes: ByteArray)
    {
        if (connected)
        {
            try {
                bluetoothSocket!!.inputStream.read(bytes)
            } catch (exception: Exception) { close() }
        }
    }

    fun close()
    {
        bluetoothSocket!!.close()
        connected = false
    }
}

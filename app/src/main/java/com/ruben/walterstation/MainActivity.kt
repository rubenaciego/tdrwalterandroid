package com.ruben.walterstation

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng


class MainActivity : AppCompatActivity(), OnMapReadyCallback
{
    private val bluetoothIO = BluetoothIO(this, this)
    private var initBluetooth : Thread? = null
    var mapReady = false
    var map : GoogleMap? = null

    private fun getThread() : Thread
    {
        return Thread {
            bluetoothIO.initialize()

            if (bluetoothIO.connected)
            {
                BluetoothTask(bluetoothIO).execute()
                runOnUiThread {
                    Toast.makeText(this, "Device connected!", Toast.LENGTH_LONG).show()
                    connectedTextView.text = "State: connected"
                }
            }
            else
            {
                runOnUiThread {
                    Toast.makeText(this, "Device not found!", Toast.LENGTH_LONG).show()
                    connectedTextView.text = "State: disconnected"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFrag = fragmentManager.findFragmentById(R.id.mapFragment) as MapFragment
        mapFrag.getMapAsync(this)

        initBluetooth = getThread()
        initBluetooth!!.start()

        rescanButton.setOnClickListener {
            if (!bluetoothIO.connected && !initBluetooth!!.isAlive)
            {
                connectedTextView.text = "State: scanning..."

                initBluetooth = getThread()
                initBluetooth!!.start()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap?)
    {
        mapReady = true
        map = googleMap

        val location = LatLng(41.535791, 2.210348)
        googleMap!!.moveCamera(CameraUpdateFactory.newLatLng(location))
    }
}

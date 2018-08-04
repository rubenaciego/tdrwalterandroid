package com.ruben.walterstation

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity()
{
    private val bluetoothIO = BluetoothIO(this, this)
    private var initBluetooth : Thread? = null

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
}

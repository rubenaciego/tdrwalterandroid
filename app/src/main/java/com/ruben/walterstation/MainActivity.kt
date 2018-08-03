package com.ruben.walterstation

import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity()
{
    private val bluetoothIO = BluetoothIO(this, this)
    private val initBluetooth = Thread {
        bluetoothIO.initialize()

        if (bluetoothIO.connected)
            BluetoothTask(bluetoothIO, this).execute()
        else
            Toast.makeText(this, "Device not found!", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initBluetooth.run()

        sendButton.setOnClickListener {
            if (bluetoothIO.connected)
                bluetoothIO.write(dataEditText.text.toString())
        }

        rescanButton.setOnClickListener { if (!bluetoothIO.connected) initBluetooth.run() }
    }

    class BluetoothTask(private val bluetoothIO: BluetoothIO, context: MainActivity) :
            AsyncTask<Void, ByteArray, Void>()
    {
        private var readBuffer = ByteArray(1024)
        private var activity : WeakReference<MainActivity> = WeakReference(context)

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
            activity.get()!!.dataReceived.text = String(values[0])
        }

        override fun onPostExecute(result: Void?)
        {
            Toast.makeText(activity.get(), "Device disconnected!", Toast.LENGTH_LONG).show()
        }
    }
}

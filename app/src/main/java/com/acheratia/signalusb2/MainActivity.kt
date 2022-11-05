package com.acheratia.signalusb2

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
private const val ACTION_USB_PERMISSION = "com.acheratia.signalusb2.USB_PERMISSION"

class MainActivity : AppCompatActivity() {
    lateinit var port: UsbSerialPort
    val handler = Handler(Looper.getMainLooper())
    val runnable = object : Runnable {
        override fun run() {
            //keep alive(null byte)
            port.write(ByteArray(1) { 0 }, 500);
            handler.postDelayed(this,500);
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            connect();
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)

        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers: List<UsbSerialDriver> =
            UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) {
            return
        }

        // Open a connection to the first available driver.
        val driver: UsbSerialDriver = availableDrivers[0]
        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        val device = driver.getDevice();
        if(manager.hasPermission(device)){
            connect();
        }else{
            Toast.makeText(this,"接続を許可してください",Toast.LENGTH_SHORT)
            manager.requestPermission(device, permissionIntent)
        }
    }

    fun connect(){
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers: List<UsbSerialDriver> =
            UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) {
            return
        }

        // Open a connection to the first available driver.
        val driver: UsbSerialDriver = availableDrivers[0]
        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        val device = driver.getDevice();

        val connection = manager.openDevice(device)
            ?: // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            return

        port =
            driver.getPorts().get(0) // Most devices have just one port (port 0)

        port.open(connection)
        port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

        port.write("0".toByteArray(), 500);

        findViewById<Button>(R.id.button_off).setOnClickListener {
            port.write("0".toByteArray(), 500);
        };
        findViewById<Button>(R.id.button_red).setOnClickListener {
            port.write("1".toByteArray(), 500);
        };
        findViewById<Button>(R.id.button_yellow).setOnClickListener {
            port.write("2".toByteArray(), 500);
        };
        findViewById<Button>(R.id.button_green).setOnClickListener {
            port.write("4".toByteArray(), 500);
        };

        handler.post(runnable);
    }
}
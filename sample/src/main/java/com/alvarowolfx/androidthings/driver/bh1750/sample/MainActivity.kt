package com.alvarowolfx.androidthings.driver.bh1750.sample

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import com.alvarowolfx.androidthings.driver.bh1750.Bh1750SensorDriver
import com.google.android.things.contrib.driver.apa102.Apa102
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.pwmservo.Servo
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.google.android.things.pio.PeripheralManager

class MainActivity : Activity() {

    val TAG = this.javaClass.simpleName!!

    val I2C_BUS = "I2C1"
    var mBh1750SensorDriver: Bh1750SensorDriver? = null

    lateinit var mSensorManager: SensorManager

    var mServoLight: Servo? = null
    var mDisplay: AlphanumericDisplay? = null
    var mLedStrip: Apa102? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val service = PeripheralManager.getInstance()
        Log.d(TAG, service.i2cBusList.toString())

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorManager.registerDynamicSensorCallback(object : SensorManager.DynamicSensorCallback() {
            override fun onDynamicSensorConnected(sensor: Sensor) {
                if (sensor.type === Sensor.TYPE_LIGHT) {
                    mSensorManager.registerListener(mLightListener, sensor,
                            SensorManager.SENSOR_DELAY_UI)
                }
            }
        })

        try {
            mBh1750SensorDriver = Bh1750SensorDriver(I2C_BUS)
            mBh1750SensorDriver?.registerLightSensor()

            mServoLight = RainbowHat.openServo()
            mServoLight?.setPulseDurationRange(0.544, 2.4)
            mServoLight?.setAngleRange(0.0, 180.0)
            mServoLight?.setEnabled(true)

            mDisplay = RainbowHat.openDisplay()
            mDisplay?.setEnabled(true)
            mDisplay?.clear()

            mLedStrip = RainbowHat.openLedStrip()
            mLedStrip?.brightness = 1
            mLedStrip?.direction = Apa102.Direction.NORMAL

        }catch (e: Exception){
            Log.d(TAG, e.localizedMessage)
        }
    }


    val mLightListener = object : SensorEventListener {
        override fun onSensorChanged(sensorEvent: SensorEvent?) {
            if (sensorEvent != null) {
                val light = sensorEvent?.values[0]
                updateDisplay(light)
            }

        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }

    fun updateDisplay(light: Float){
        val lightValue = 35 * Math.log10(light.toDouble())
        val newAngle = Math.max(0.0, Math.min(lightValue,180.0))
        mServoLight?.angle = newAngle

        mDisplay?.display(Math.min(light.toInt(), 9999))

        val ledValue = Math.ceil(1.5 * Math.log10(light.toDouble())).toInt()
        val leds = Math.max(0, Math.min(ledValue, RainbowHat.LEDSTRIP_LENGTH))
        val colors = IntArray(RainbowHat.LEDSTRIP_LENGTH)
        val color = when {
            leds > ((2 * colors.size) / 3) -> Color.CYAN
            leds > colors.size / 2 -> Color.BLUE
            else -> Color.MAGENTA
        }

        for (i in 0 until leds) {
            val ri = RainbowHat.LEDSTRIP_LENGTH - 1 - i
            colors[ri] = color
        }
        mLedStrip?.write(colors)

        //Log.i(TAG, "Light: $light")
        //Log.i(TAG, "Angle: $newAngle")

    }

    override fun onDestroy() {
        super.onDestroy()

        mSensorManager.unregisterListener(mLightListener)
        mBh1750SensorDriver?.unregisterLightSensor()
        mBh1750SensorDriver?.close()

        mDisplay?.clear()
        mDisplay?.setEnabled(false)
        mDisplay?.close()


        mLedStrip?.brightness = 0
        mLedStrip?.write(IntArray(RainbowHat.LEDSTRIP_LENGTH))
        mLedStrip?.close()
    }

}

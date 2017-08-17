package com.alvarowolfx.androidthings.driver.bh1750.sample

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import com.alvarowolfx.androidthings.driver.bh1750.Bh1750SensorDriver
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver
import com.google.android.things.contrib.driver.pwmservo.Servo
import com.google.android.things.pio.PeripheralManagerService

class MainActivity : Activity() {

    val TAG = this.javaClass.simpleName!!

    val I2C_BUS = "I2C1"
    val BMP_280_ADDRESS = 0x76
    var mBmx280SensorDriver: Bmx280SensorDriver? = null
    var mBh1750SensorDriver: Bh1750SensorDriver? = null

    lateinit var mSensorManager: SensorManager

    val SERVO_GAUGE_LIGHT_BUS = "PWM1"
    var mServoLight: Servo? = null

    val SERVO_GAUGE_TEMP_BUS = "PWM0"
    var mServoTemp: Servo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val service = PeripheralManagerService()
        Log.d(TAG, service.i2cBusList.toString())
        Log.d(TAG, service.i2sDeviceList.toString())

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorManager.registerDynamicSensorCallback(object : SensorManager.DynamicSensorCallback() {
            override fun onDynamicSensorConnected(sensor: Sensor) {
                if (sensor.type === Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    mSensorManager.registerListener(mTemperatureListener, sensor,
                            SensorManager.SENSOR_DELAY_UI)
                }
                if (sensor.type === Sensor.TYPE_PRESSURE) {
                    mSensorManager.registerListener(mPressureListener, sensor,
                            SensorManager.SENSOR_DELAY_UI)
                }
                if (sensor.type === Sensor.TYPE_LIGHT) {
                    mSensorManager.registerListener(mLightListener, sensor,
                            SensorManager.SENSOR_DELAY_UI)
                }
            }
        })

        try {
            mBh1750SensorDriver = Bh1750SensorDriver(I2C_BUS)
            mBh1750SensorDriver?.registerLightSensor()

            mServoLight = Servo(SERVO_GAUGE_LIGHT_BUS)
            mServoLight?.setPulseDurationRange(0.8, 2.4)
            mServoLight?.setAngleRange(0.0, 180.0)
            mServoLight?.setEnabled(true)

            mBmx280SensorDriver = Bmx280SensorDriver(I2C_BUS, BMP_280_ADDRESS)
            mBmx280SensorDriver?.registerTemperatureSensor()
            //mBmx280SensorDriver?.registerPressureSensor()

            mServoTemp = Servo(SERVO_GAUGE_TEMP_BUS)
            mServoTemp?.setPulseDurationRange(0.544, 2.4)
            mServoTemp?.setAngleRange(0.0, 180.0)
            mServoTemp?.setEnabled(true)

        }catch (e: Exception){
            Log.d(TAG, e.localizedMessage)
        }
    }

    val mTemperatureListener = object : SensorEventListener {
        override fun onSensorChanged(sensorEvent: SensorEvent?) {
            if (sensorEvent != null) {
                val temperature = sensorEvent?.values[0]
                var newAngle = 180.0 * temperature/50.0
                if(newAngle > 180.0) {
                    newAngle = 180.0
                }
                if(newAngle <= 0.0){
                    newAngle = 0.0
                }
                mServoTemp?.angle = newAngle
                Log.i(TAG, "Temperature: $temperature")
                Log.i(TAG, "AngleTemp: $newAngle")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }

    val mPressureListener = object : SensorEventListener {
        override fun onSensorChanged(sensorEvent: SensorEvent?) {
            if (sensorEvent != null) {
                val pressure = sensorEvent?.values[0]
                Log.i(TAG, "Pressure: $pressure")
            }

        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }

    val mLightListener = object : SensorEventListener {
        override fun onSensorChanged(sensorEvent: SensorEvent?) {
            if (sensorEvent != null) {
                val light = sensorEvent?.values[0]
                var newAngle = 45 * Math.log10(light.toDouble())
                if(newAngle > 180.0) {
                    newAngle = 180.0
                }
                if(newAngle <= 0.0){
                    newAngle = 0.0
                }
                mServoLight?.angle = newAngle
                Log.i(TAG, "Light: $light")
                Log.i(TAG, "Angle: $newAngle")
            }

        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mSensorManager.unregisterListener(mPressureListener)
        mSensorManager.unregisterListener(mTemperatureListener)
        mSensorManager.unregisterListener(mLightListener)

        mBmx280SensorDriver?.unregisterPressureSensor()
        mBmx280SensorDriver?.unregisterTemperatureSensor()
        mBh1750SensorDriver?.unregisterLightSensor()

        mBmx280SensorDriver?.close()
        mBh1750SensorDriver?.close()
    }

}

package com.mousecontrol.remote.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class MouseDelta(val dx: Float, val dy: Float)

/**
 * Streams smoothed X/Y mouse deltas derived from the phone's gyroscope.
 *
 * The raw sensor gives angular velocity in rad/s per axis, which is jittery
 * on its own — a low-pass (exponential moving average) filter is applied
 * before the value is scaled into pixel-ish deltas, so small hand tremor
 * gets smoothed out while a deliberate tilt still responds quickly.
 */
class GyroSensorManager(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val isAvailable: Boolean get() = gyroSensor != null

    /**
     * @param sensitivity scales filtered angular velocity into cursor pixels.
     * @param smoothing exponential-filter weight in (0, 1]; lower = smoother but laggier.
     */
    fun deltaFlow(sensitivity: Float = 900f, smoothing: Float = 0.15f): Flow<MouseDelta> = callbackFlow {
        var filteredX = 0f
        var filteredY = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // event.values = [x, y, z] rotation rate (rad/s) around each axis.
                val rawX = -event.values[1] // roll (tilt left/right)  -> cursor X
                val rawY = event.values[0]  // pitch (tilt up/down)    -> cursor Y

                filteredX += smoothing * (rawX - filteredX)
                filteredY += smoothing * (rawY - filteredY)

                trySend(MouseDelta(filteredX * sensitivity, filteredY * sensitivity))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val sensor = gyroSensor
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            close(IllegalStateException("No gyroscope on this device"))
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }
}

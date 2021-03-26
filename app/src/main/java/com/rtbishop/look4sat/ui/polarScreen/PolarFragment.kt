/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.ui.polarScreen

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Surface
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.model.SatPass
import com.rtbishop.look4sat.databinding.FragmentPolarBinding
import com.rtbishop.look4sat.utility.RecyclerDivider
import com.rtbishop.look4sat.utility.formatForTimer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import java.util.*
import kotlin.math.atan2
import kotlin.math.round

@FlowPreview
@AndroidEntryPoint
class PolarFragment : Fragment(R.layout.fragment_polar), SensorEventListener {

    private lateinit var transmitterAdapter: TransAdapter
    private lateinit var binding: FragmentPolarBinding
    private lateinit var satPass: SatPass
    private lateinit var sensorManager: SensorManager
    private val viewModel: PolarViewModel by viewModels()
    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)
    private var magneticDeclination = 0f
    private var polarView: PolarView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPolarBinding.bind(view)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magneticDeclination = viewModel.getMagDeclination()
        observePass()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.shouldUseCompass()) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR).also { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.shouldUseCompass()) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { calculateAzimuth(it) }
    }

    private fun calculateAzimuth(event: SensorEvent) {
//        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
//            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
//            SensorManager.getOrientation(rotationMatrix, orientationValues)
//            val magneticAzimuth = (Math.toDegrees(orientationValues[0].toDouble()) + 360f) % 360f
//            val roundedAzimuth = (round(magneticAzimuth * 10) / 10).toFloat()
//            polarView?.rotation = -(roundedAzimuth + magneticDeclination)
//        }

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val (matrixColumn, sense) = when (val rotation =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    requireContext().display?.rotation
                } else {
                    @Suppress("DEPRECATION")
                    requireActivity().windowManager.defaultDisplay.rotation
                }
            ) {
                Surface.ROTATION_0 -> Pair(0, 1)
                Surface.ROTATION_90 -> Pair(1, -1)
                Surface.ROTATION_180 -> Pair(0, -1)
                Surface.ROTATION_270 -> Pair(1, 1)
                else -> error("Invalid screen rotation value: $rotation")
            }
            val x = sense * rotationMatrix[matrixColumn]
            val y = sense * rotationMatrix[matrixColumn + 3]
            val magneticAzimuth = (Math.toDegrees(-atan2(y, x).toDouble()) + 360f) % 360f
            val roundedAzimuth = (round(magneticAzimuth * 10) / 10).toFloat()
            polarView?.rotation = -(roundedAzimuth + magneticDeclination)
        }
    }

    private fun observePass() {
        val passId = requireArguments().getInt("index")
        viewModel.getPass(passId).observe(viewLifecycleOwner, { pass ->
            satPass = pass
            polarView = PolarView(requireContext()).apply { setPass(pass) }
            binding.frame.addView(polarView)
            observeTransmitters()
        })
    }

    private fun observeTransmitters() {
        viewModel.getSatTransmitters(satPass.catNum)
            .observe(viewLifecycleOwner, { list ->
                transmitterAdapter = TransAdapter(satPass)
                if (list.isNotEmpty()) {
                    transmitterAdapter.setData(list)
                    binding.recycler.apply {
                        setHasFixedSize(true)
                        adapter = transmitterAdapter
                        isVerticalScrollBarEnabled = false
                        layoutManager = LinearLayoutManager(context)
                        (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                        addItemDecoration(RecyclerDivider(R.drawable.rec_divider_dark))
                        visibility = View.VISIBLE
                    }
                    binding.noTransMsg.visibility = View.INVISIBLE
                } else {
                    binding.recycler.visibility = View.INVISIBLE
                    binding.noTransMsg.visibility = View.VISIBLE
                }
                observeTimer()
            })
    }

    private fun observeTimer() {
        viewModel.getAppTimer().observe(viewLifecycleOwner, {
            setPassText(it)
            polarView?.invalidate()
            transmitterAdapter.tickTransmitters()
        })
    }

    private fun setPassText(timeNow: Long) {
        val dateNow = Date(timeNow)
        val satPos = satPass.predictor.getSatPos(dateNow)
        val polarAz = getString(R.string.pat_azimuth)
        val polarEl = getString(R.string.pat_elevation)
        val polarRng = getString(R.string.pat_distance)
        val polarAlt = getString(R.string.pat_altitude)
        binding.azimuth.text = String.format(polarAz, Math.toDegrees(satPos.azimuth))
        binding.elevation.text = String.format(polarEl, Math.toDegrees(satPos.elevation))
        binding.distance.text = String.format(polarRng, satPos.range)
        binding.altitude.text = String.format(polarAlt, satPos.altitude)

        if (!satPass.isDeepSpace) {
            if (dateNow.before(satPass.startDate)) {
                val millisBeforeStart = satPass.startDate.time.minus(timeNow)
                binding.polarTimer.text = millisBeforeStart.formatForTimer()
            } else {
                val millisBeforeEnd = satPass.endDate.time.minus(timeNow)
                binding.polarTimer.text = millisBeforeEnd.formatForTimer()
                if (dateNow.after(satPass.endDate)) {
                    binding.polarTimer.text = 0L.formatForTimer()
                    findNavController().popBackStack()
                }
            }
        } else {
            binding.polarTimer.text = 0L.formatForTimer()
        }
    }
}

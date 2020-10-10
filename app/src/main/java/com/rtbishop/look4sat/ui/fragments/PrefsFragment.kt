/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.utility.PrefsManager
import com.rtbishop.look4sat.utility.Utilities.snack
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrefsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)

        findPreference<EditTextPreference>(PrefsManager.keyLatitude)?.apply {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL or
                        InputType.TYPE_NUMBER_FLAG_SIGNED
            }
            setOnPreferenceChangeListener { _, newValue ->
                val valueStr = newValue.toString()
                if (valueStr.isEmpty() || valueStr == "-" || valueStr.toDouble() < -90.0 || valueStr.toDouble() > 90.0) {
                    getString(R.string.pref_lat_input_error).snack(requireView())
                    return@setOnPreferenceChangeListener false
                }
                return@setOnPreferenceChangeListener true
            }
        }

        findPreference<EditTextPreference>(PrefsManager.keyLongitude)?.apply {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL or
                        InputType.TYPE_NUMBER_FLAG_SIGNED
            }
            setOnPreferenceChangeListener { _, newValue ->
                val valueStr = newValue.toString()
                if (valueStr.isEmpty() || valueStr == "-" || valueStr.toDouble() < -180.0 || valueStr.toDouble() > 180.0) {
                    getString(R.string.pref_lon_input_error).snack(requireView())
                    return@setOnPreferenceChangeListener false
                }
                return@setOnPreferenceChangeListener true
            }
        }

        findPreference<EditTextPreference>(PrefsManager.keyAltitude)?.apply {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL or
                        InputType.TYPE_NUMBER_FLAG_SIGNED
            }
            setOnPreferenceChangeListener { _, newValue ->
                val valueStr = newValue.toString()
                if (valueStr.isEmpty() || valueStr == "-" || valueStr.toDouble() < -413.0 || valueStr.toDouble() > 8850.0) {
                    getString(R.string.pref_alt_input_error).snack(requireView())
                    return@setOnPreferenceChangeListener false
                }
                return@setOnPreferenceChangeListener true
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {}

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}
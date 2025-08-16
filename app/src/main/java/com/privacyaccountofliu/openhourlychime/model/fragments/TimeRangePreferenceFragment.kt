package com.privacyaccountofliu.openhourlychime.model.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TimePicker
import androidx.preference.PreferenceDialogFragmentCompat
import com.privacyaccountofliu.openhourlychime.R
import com.privacyaccountofliu.openhourlychime.model.tools.ToastUtil
import com.privacyaccountofliu.openhourlychime.widget.TimeRangePreference

class TimeRangePreferenceFragment : PreferenceDialogFragmentCompat() {

    private lateinit var startPicker: TimePicker
    private lateinit var endPicker: TimePicker

    @SuppressLint("InflateParams")
    override fun onCreateDialogView(context: Context): View {
        return LayoutInflater.from(context).inflate(R.layout.time_range_dialog, null)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        startPicker = view.findViewById(R.id.startTimePicker)
        endPicker = view.findViewById(R.id.endTimePicker)

        val preference = preference as TimeRangePreference
        startPicker.hour = preference.startTime / 60
        startPicker.minute = preference.startTime % 60
        endPicker.hour = preference.endTime / 60
        endPicker.minute = preference.endTime % 60
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val startMinutes = startPicker.hour * 60 + startPicker.minute
            val endMinutes = endPicker.hour * 60 + endPicker.minute

            if (startMinutes >= endMinutes) {
                ToastUtil.showToast(requireContext(), getString(R.string.toast_3))
                return
            }

            (preference as? TimeRangePreference)?.setTimeRange(startMinutes, endMinutes)
        }
    }

    companion object {
        fun newInstance(key: String): TimeRangePreferenceFragment {
            return TimeRangePreferenceFragment().apply {
                arguments = Bundle(1).apply { putString(ARG_KEY, key) }
            }
        }
    }
}
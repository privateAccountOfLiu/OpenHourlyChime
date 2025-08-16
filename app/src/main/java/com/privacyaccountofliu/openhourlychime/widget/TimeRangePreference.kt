package com.privacyaccountofliu.openhourlychime.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import androidx.preference.DialogPreference
import com.privacyaccountofliu.openhourlychime.R

class TimeRangePreference(context: Context, attrs: AttributeSet) :
    DialogPreference(context, attrs) {

    interface OnTimeRangeChangeListener {
        fun onTimeRangeChanged(startMinutes: Int, endMinutes: Int)
    }

    var startTime: Int = DEFAULT_START
    var endTime: Int = DEFAULT_END
    var onTimeRangeChangeListener: OnTimeRangeChangeListener? = null

    init {
        dialogTitle = context.getString(R.string.time_range_dialog_title)
        positiveButtonText = context.getString(R.string.Yes)
        negativeButtonText = context.getString(R.string.Cancel)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        val value = getPersistedString("$DEFAULT_START-$DEFAULT_END")
        parseAndSetTime(value)
    }

    override fun getSummary(): CharSequence {
        return formatTime(startTime) + " - " + formatTime(endTime)
    }

    fun setTimeRange(start: Int, end: Int) {
        startTime = start
        endTime = end
        persistString("$start-$end")
        notifyChanged()
        onTimeRangeChangeListener?.onTimeRangeChanged(start, end)
    }

    private fun parseAndSetTime(value: String) {
        try {
            val parts = value.split("-")
            if (parts.size == 2) {
                startTime = parts[0].toInt()
                endTime = parts[1].toInt()
            }
        } catch (e: Exception) {
            Log.e("TimeRangePreference", "Error parsing time", e)
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getString(index) ?: "$DEFAULT_START-$DEFAULT_END"
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(superState).apply {
            this.startTime = this@TimeRangePreference.startTime
            this.endTime = this@TimeRangePreference.endTime
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        setTimeRange(state.startTime, state.endTime)
    }

    @SuppressLint("DefaultLocale")
    fun formatTime(minutes: Int): String {
        return String.format("%02d:%02d", minutes / 60, minutes % 60)
    }

    private class SavedState : BaseSavedState {
        var startTime: Int = DEFAULT_START
        var endTime: Int = DEFAULT_END

        constructor(source: Parcel) : super(source) {
            startTime = source.readInt()
            endTime = source.readInt()
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(startTime)
            out.writeInt(endTime)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel) = SavedState(source)
                override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
            }
        }
    }

    companion object {
        private const val DEFAULT_START = 420
        private const val DEFAULT_END = 1320
    }
}
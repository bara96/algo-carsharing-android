package com.example.algorandcarsharing.pickers;

import android.app.DatePickerDialog;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateSetter implements View.OnFocusChangeListener, DatePickerDialog.OnDateSetListener, View.OnClickListener {

    private EditText mEditText;
    private Calendar mCalendar;
    private SimpleDateFormat mFormat;

    public DateSetter(EditText editText){
        this.mEditText = editText;
        mEditText.setOnFocusChangeListener(this);
        mEditText.setOnClickListener(this);
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (hasFocus){
            showPicker(view);
        }
    }

    @Override
    public void onClick(View view) {
        showPicker(view);
    }

    private void showPicker(View view) {
        if (mCalendar == null)
            mCalendar = Calendar.getInstance();

        int year = mCalendar.get(Calendar.YEAR);
        int month = mCalendar.get(Calendar.MONTH);
        int day = mCalendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(view.getContext(), this, year, month, day).show();
    }

    @Override
    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
        mCalendar.set(Calendar.YEAR, i);
        mCalendar.set(Calendar.MONTH, i1);
        mCalendar.set(Calendar.DAY_OF_MONTH, i2);

        if (mFormat == null)
            mFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        this.mEditText.setText(mFormat.format(mCalendar.getTime()));
    }
}

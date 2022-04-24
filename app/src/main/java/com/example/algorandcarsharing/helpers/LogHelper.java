package com.example.algorandcarsharing.helpers;

import android.util.Log;

import com.example.algorandcarsharing.constants.Constants;

public class LogHelper {
    public enum LogType{
        DEBUG,
        ERROR,
        WARNING
    }

    public static void log(String title, String message, LogType type) {
        if(!Constants.development) {
            return;
        }

        switch (type) {
            case DEBUG:
                Log.d(title, message);
                break;
            case ERROR:
                Log.e(title, message);
                break;
            case WARNING:
                Log.w(title, message);
                break;
            default:
                Log.v(title, message);
        }
    }

    public static void log(String title, String message) {
        if(!Constants.development) {
            return;
        }

        Log.d(title, message);
    }

    public static void error(String title, Throwable e, boolean printStackTrace) {
        if(!Constants.development) {
            return;
        }

        Log.e(title, e.getMessage());
        if(printStackTrace) {
            e.printStackTrace();
        }
    }

    public static void error(String title, Throwable e) {
        if(!Constants.development) {
            return;
        }

        Log.e(title, e.getMessage());
        e.printStackTrace();
    }
}

package com.example.algorandcarsharing.helpers;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

public class UtilsHelper {

    protected static final Long fees = 1000L;
    protected static final Long escrowMinBalance = 1000000L;

    public UtilsHelper() {
    }


    public static String readAssetFile(Context context, String filepath) throws IOException {
        InputStream stream = context.getAssets().open(filepath);

        int size = stream.available();
        byte[] buffer = new byte[size];
        stream.read(buffer);
        stream.close();

        return new String(buffer);
    }
}

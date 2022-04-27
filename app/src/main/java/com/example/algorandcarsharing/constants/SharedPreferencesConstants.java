package com.example.algorandcarsharing.constants;

public class SharedPreferencesConstants {

    public enum AccountPreferences {
        Mnemonic("account_mnemonic"),
        Balance("account_balance");

        private final String key;

        AccountPreferences(String key) {
            this.key = key;
        }

        public static String getPreference() {
            return "account_preferences";
        }

        public String getKey() {
            return key;
        }
    }

    public enum IntentExtra {
        AppId("appid");

        private final String key;

        IntentExtra(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}

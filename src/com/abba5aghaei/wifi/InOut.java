package com.abba5aghaei.wifi;

public interface InOut {

    void error(String message);
    void log(String message);
    void warn(String message);
    String getPassword();
}

package com.abba5aghaei.wifi;

import java.util.ArrayList;

class MacintoshWifiManager extends  BaseWifiManager {
    MacintoshWifiManager(InOut inout) {
        super(inout);
    }

    @Override
    void initialize() {

    }

    @Override
    boolean connect(String ssid, String profile, int auth) {
        return false;
    }

    @Override
    boolean tryToConnect(String ssid, String profile) {
        return false;
    }

    @Override
    boolean isConnected() {
        return false;
    }

    @Override
    boolean disconnect() {
        return false;
    }

    @Override
    boolean isHotspotSupport() {
        return false;
    }

    @Override
    boolean turnOnHotspot() {
        return false;
    }

    @Override
    boolean turnOffHotspot() {
        return false;
    }

    @Override
    boolean setHotspot(String ssid, String key) {
        return false;
    }

    @Override
    boolean profileExist(String profile) {
        return false;
    }

    @Override
    boolean createProfile(String name) {
        return false;
    }

    @Override
    boolean createProfile(String name, String key, int auth) {
        return false;
    }

    @Override
    boolean deleteProfile(String name) {
        return false;
    }

    @Override
    ArrayList<ArrayList<String>> list() {
        return null;
    }

    @Override
    ArrayList<String> getInterfaces() {
        return null;
    }

    @Override
    boolean isSupport() {
        return false;
    }
}

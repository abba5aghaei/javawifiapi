//INOG
package com.abba5aghaei.wifi;

import java.util.ArrayList;
import java.util.Locale;

//author @abba5aghaei

public class WifiManager {

    public static int OPEN = 0;
    public static int WEP = 1;
    public static int WPA2 = 2;
    private BaseWifiManager baseWifiManager;

    public WifiManager(InOut inout) throws WifiException {
        String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((OS.contains("mac")) || (OS.contains("darwin"))) {
            baseWifiManager = new MacintoshWifiManager(inout);
        }
        else if (OS.contains("win")) {
            baseWifiManager = new WindowsWifiManager(inout);
        }
        else if (OS.contains("nux")) {
            baseWifiManager = new LinuxWifiManager(inout);
        }
        else if(OS.contains("laris")) {
            baseWifiManager = new SolarisWifiManager(inout);
        }
        else {
            throw new WifiException("Unknown operating system");
        }
        if(!baseWifiManager.isSupport()) {
            throw new WifiException("System does not support wifi");
        }
    }

    public boolean connect(String ssid, String profile, int auth) {
        return baseWifiManager.connect(ssid, profile, auth);
    }

    public boolean tryToConnect(String ssid, String profile) {
        return baseWifiManager.tryToConnect(ssid, profile);
    }

    public boolean isConnected() {
        return baseWifiManager.isConnected();
    }

    public boolean disconnect() {
        return baseWifiManager.disconnect();
    }

    public boolean isHotspotSupport() {
        return baseWifiManager.hotspotSupport;
    }

    public boolean turnOnHotspot() {
        return baseWifiManager.turnOnHotspot();
    }

    public boolean turnOffHotspot() {
        return baseWifiManager.turnOffHotspot();
    }

    public boolean setHotspotProfile(String ssid, String key) {
        return baseWifiManager.setHotspot(ssid, key);
    }

    public boolean profileExist(String profile) {
        return baseWifiManager.profileExist(profile);
    }

    public boolean createProfile(String name) {
        return baseWifiManager.createProfile(name);
    }

    public boolean createProfile(String name, String key, int auth) {
        return baseWifiManager.createProfile(name, key, auth);
    }

    public boolean deleteProfile(String name) {
        return baseWifiManager.deleteProfile(name);
    }

    public boolean isSupport() {
        return baseWifiManager.isSupport();
    }

    public ArrayList<ArrayList<String>> list() {
        return baseWifiManager.list();
    }

    public ArrayList<String> getInterfaces() {
        return baseWifiManager.getInterfaces();
    }

    public String getSSID() {
        return baseWifiManager.SSID;
    }

    public String getKey() {
        return baseWifiManager.Key;
    }

    public String getMode() {
        return baseWifiManager.Mode;
    }

    public String getMaxClients() {
        return baseWifiManager.Max_clients;
    }

    public String getAuthentication() {
        return baseWifiManager.Authentication;
    }

    public String getCipher() {
        return baseWifiManager.Cipher;
    }

    public String getStatus() {
        return baseWifiManager.Status;
    }

    public String getInterface() {
        return baseWifiManager.Interface;
    }

    public boolean isWifiConnected() {
        return baseWifiManager.wifiConnected;
    }

    public boolean isHotspotActive() {
        return baseWifiManager.hotspotActive;
    }

    public void setInterface(String interfaceName) {
        baseWifiManager.Interface = interfaceName;
    }

    public void setSSID(String SSID) {
        baseWifiManager.SSID = SSID;
    }

    public void setKey(String Key) {
        baseWifiManager.Key = Key;
    }
}

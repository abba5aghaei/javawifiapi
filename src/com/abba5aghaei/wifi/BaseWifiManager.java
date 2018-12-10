package com.abba5aghaei.wifi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

abstract class BaseWifiManager {

    String SSID;
    String Key;
    String Mode;
    String Max_clients;
    String Authentication;
    String Cipher;
    String Status;
    String Interface;
    boolean wifiConnected;
    boolean hotspotActive;
    boolean hotspotSupport;
    protected InOut inout;

    BaseWifiManager(InOut inout) {
        this.inout = inout;
        initialize();
        new Thread(()->{
            hotspotSupport = isHotspotSupport();
        }).start();
    }

    abstract void initialize();

    abstract boolean connect(String ssid, String profile, int auth);

    abstract boolean tryToConnect(String ssid, String profile);

    abstract boolean isConnected();

    abstract boolean disconnect();

    abstract boolean isHotspotSupport();

    abstract boolean turnOnHotspot();

    abstract boolean turnOffHotspot();

    abstract boolean setHotspot(String ssid, String key);

    abstract boolean profileExist(String profile);

    abstract boolean createProfile(String name);

    abstract boolean createProfile(String name, String key, int auth);

    abstract boolean deleteProfile(String name);

    abstract ArrayList<ArrayList<String>> list();

    abstract ArrayList<String> getInterfaces();

    abstract boolean isSupport();

    protected String getOutput(Process process) {
        String line = "";
        StringBuffer output = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
        }
        catch (IOException e) {
            inout.error("Error in WifiManager.getOutput: " + e.getMessage());
        }
        return output.toString();
    }

    String encode(String normal) {
        try {
            byte[] buf = normal.getBytes();
            StringBuffer sb = new StringBuffer();
            for (byte b : buf) {
                sb.append(String.format("%x", b));
            }
            return sb.toString();
        }
        catch (Exception e) {
            inout.error("Error in WifiManager.encode: " + e.getMessage());
            return "";
        }
    }

    String decode(String encoded) {
        try {
            StringBuffer sb = new StringBuffer();
            for (int i=0;i<encoded.length()-1;i+=2) {
                String output = encoded.substring(i, (i+2));
                int decimal = Integer.parseInt(output, 16);
                sb.append((char)decimal);
            }
            return sb.toString();
        }
        catch (Exception e) {
            inout.error("Error in WifiManager.decode: " + e.getMessage());
            return "";
        }
    }
}
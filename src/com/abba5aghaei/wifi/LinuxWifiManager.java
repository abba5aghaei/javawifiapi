package com.abba5aghaei.wifi;

import java.io.*;
import java.util.*;

class LinuxWifiManager extends BaseWifiManager {

    LinuxWifiManager(InOut inout) {
        super(inout);
    }

    @Override
    void initialize() {
        try {
            File config = new File(System.getProperty("user.home") + "/.jwa/config.abs");
            if(config.exists()) {
                Scanner scanner = new Scanner(new FileInputStream(config));
                scanner.useDelimiter("%");
                if (scanner.hasNext()) {
                    SSID = decode(scanner.next());
                    Key = decode(scanner.next());
                }
                scanner.close();
            }
            else {
                SSID = "Stream-file-sharing";
                Key = "1234567890";
            }
            Mode = "Allowed";
            Max_clients = "100";
            Cipher = "CCMP";
            Authentication = "WPA2-Personal";
            Status = "Ready";
        } catch (FileNotFoundException e) {
            inout.error("Error in LinuxWifiManager.initialize: " + e.getMessage());
        }
    }

    @Override
    boolean connect(String ssid, String profile, int auth) {
        if(profileExist(profile)) {
            inout.log("Trying with profile "+profile);
            if(tryWithProfile(ssid, profile)) {
                wifiConnected = true;
                return true;
            }
            else {
                inout.log("Deleting profile "+profile);
                if(!deleteProfile(profile))
                    inout.warn("Can't delete profile");
                inout.warn("Password is wrong");
                return retryToConnect(ssid, profile, auth);
            }
        }
        else {
            return retryToConnect(ssid, profile, auth);
        }
    }

    private boolean retryToConnect(String ssid, String profile, int auth) {
        if(auth==0) {
            if(createProfile(ssid)) {
                return connect(ssid, profile, auth);
            }
            else {
                inout.warn("Can't create open profile");
                return false;
            }
        }
        else {
            String key = inout.getPassword();
            if(key==null) return false;
            if(createProfile(ssid, key, auth)) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(isConnected()) {
                    wifiConnected = true;
                    return true;
                }
                else  {
                    if(!deleteProfile(profile))
                        inout.warn("Can't delete profile");
                    inout.warn("Password is wrong");
                    return retryToConnect(ssid, profile, auth);
                }
            }
            else {
                inout.warn("Can't create WPA2 profile");
                return false;
            }
        }
    }

    private boolean tryWithProfile(String ssid, String profile) {
        if(tryToConnect(ssid, profile)) {
            inout.log("Try to connect success");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(isConnected()) {
                inout.log("Connected to "+profile);
                return true;
            }
            else  {
                inout.warn("refused on connection check");
                return false;
            }
        }
        else {
            inout.warn("refused on connection trying");
            return false;
        }
    }

    @Override
    boolean tryToConnect(String ssid, String profile) {
        try {
            List<String> list = Arrays.asList("nmcli", "con", "up", ssid);
            ProcessBuilder builder = new ProcessBuilder();
            String response = getOutput(builder.command(list).start());
            inout.log("Response for nmcli con up "+ssid+": \n"+response);
            return true;
        }
        catch (Exception e) {
            inout.error("Error in LinuxWifiManager.tryToConnect: " + e.getMessage());
            return false;
        }
    }

    @Override
    boolean isConnected() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("nmcli device status");
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            String[] lines = output.split("\n");
            for(short i=1;i<lines.length;i++) {
                StringTokenizer tokenizer = new StringTokenizer(lines[i]);
                if(tokenizer.nextToken().equals(Interface)) {
                    tokenizer.nextToken();
                    if(tokenizer.nextToken().equals("connected"))
                        return true;
                }
            }
        }
        catch (Exception e) {
            inout.error("Error in LinuxWifiManager.isConnected: " + e.getMessage());
        }
        return false;
    }

    @Override
    boolean disconnect() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("nmcli device disconnect "+Interface);
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            wifiConnected = false;
            return true;
        }
        catch (Exception e) {
            inout.error("Error in LinuxWifiManager.disconnect: " + e.getMessage());
            return false;
        }
    }

    @Override
    boolean isHotspotSupport() {
        return true; // :D
    }

    @Override
    boolean turnOnHotspot() {
        try {
            if(!profileExist(SSID)) {
                Runtime runtime = Runtime.getRuntime();
                Process process = runtime.exec(String.format("nmcli connection add type wifi ifname %s con-name %s autoconnect no ssid %s", Interface, SSID, SSID));
                process.waitFor();
                String output = getOutput(process);
                inout.log(output);
                setHotspot(SSID, Key);
            }
            Runtime runtime2 = Runtime.getRuntime();
            Process process2 = runtime2.exec(String.format("nmcli connection up %s", SSID));
            process2.waitFor();
            String output2 = getOutput(process2);
            inout.log(output2);
            if(output2.contains("successfully")) {
                hotspotActive = true;
                inout.log("Hotspot started");
                return true;
            }
        }
        catch (Exception e) {
            inout.error("Error in LinuxWifiManager.turnOnHotspot: " + e.getMessage());
        }
        return false;
    }

    @Override
    boolean turnOffHotspot() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(String.format("nmcli connection down %s", SSID));
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            if(output.contains("successfully")) {
                hotspotActive = false;
                inout.log("Hotspot Stopped");
                return true;
            }
        }
        catch (Exception e) {
            inout.error("Error in LinuxWifiManager.turnOffHotspot: " + e.getMessage());
        }
        return false;
    }

    @Override
    boolean setHotspot(String ssid, String key) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(String.format("nmcli connection modify %s 802-11-wireless.mode ap 802-11-wireless.band bg ipv4.method shared", SSID));
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            process = runtime.exec(String.format("nmcli connection modify %s wifi-sec.key-mgmt wpa-psk", ssid));
            process.waitFor();
            output = getOutput(process);
            inout.log(output);
            process = runtime.exec(String.format("nmcli connection modify %s wifi-sec.psk %s", ssid, key));
            process.waitFor();
            output = getOutput(process);
            inout.log(output);
            File config = new File(System.getProperty("user.home") + "/.jwa/config.abs");
            if (!config.getParentFile().exists()) {
                if (!config.getParentFile().createNewFile()) {
                    throw new Exception("Can't create log file");
                }
            }
            if (!config.exists() || !config.isFile()) {
                if (!config.createNewFile()) {
                    throw new Exception("Can't create log file");
                }
            }
            PrintWriter writer = new PrintWriter(config);
            writer.write(encode(SSID));
            writer.write("%");
            writer.write(encode(Key));
            writer.flush();
            writer.close();
            return true;
        }
        catch (Exception e) {
            inout.error("Error in LinuxWifiManager.setHotspot: " + e.getMessage());
            return false;
        }
    }

    @Override
    boolean profileExist(String profile) {
        try {
            String output = getOutput(Runtime.getRuntime().exec("nmcli connection"));
            inout.log(output);
            output = output.substring(output.indexOf("\n"));
            StringTokenizer tokenizer = new StringTokenizer(output);
            while(tokenizer.hasMoreTokens()) {
                if(tokenizer.nextToken().equals(profile))
                    return true;
            }
        }
        catch (Exception e) {
            inout.error("Error in LinuxWifiManager.profileExist: " + e.getMessage());
        }
        return false;
    }

    @Override
    boolean createProfile(String name) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(String.format("nmcli connection add type wifi ifname %s con-name %s autoconnect no ssid %s", Interface, name, name));
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            return true;
        }
        catch (Exception e) {
            inout.error("Error in LinuxWifiManager.createProfile: " + e.getMessage());
            return false;
        }
    }

    @Override
    boolean createProfile(String name, String key, int auth) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(String.format("nmcli device wifi connect %s password %s", name, key));
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            return true;
        }
        catch (Exception e) {
            inout.error("Error in LinuxWifiManager.createProfile(2): " + e.getMessage());
            return false;
        }
    }

    @Override
    boolean deleteProfile(String name) {
        try {
            String output = getOutput(Runtime.getRuntime().exec(String.format("nmcli connection delete %s", name)));
            inout.log(output);
            return output.contains("successfully");
        }
        catch (Exception e) {
            inout.error("Error in LinuxWifiManager.deleteProfile: " + e.getMessage());
            return false;
        }
    }

    @Override
    ArrayList<ArrayList<String>> list() {
        refreshInterface();
        ArrayList<ArrayList<String>> list = new ArrayList<>();
        ArrayList<String> ssid = new ArrayList<>();
        ArrayList<String> signal = new ArrayList<>();
        ArrayList<String> auth = new ArrayList<>();
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("nmcli dev wifi");
            process.waitFor();
            String response = getOutput(process);
            inout.log(response);
            String[] lines = response.split("\n");
            for(short i=1;i<lines.length;i++) {
                StringTokenizer tokenizer = new StringTokenizer(lines[i]);
                String first = tokenizer.nextToken();
                if(first.trim().equals("*")) {
                    ssid.add(tokenizer.nextToken());
                }
                else {
                    ssid.add(first);
                }
                tokenizer.nextToken();
                tokenizer.nextToken();
                tokenizer.nextToken();
                tokenizer.nextToken();
                signal.add(tokenizer.nextToken());
                tokenizer.nextToken();
                auth.add(tokenizer.nextToken());
            }
            for(int i=0 ; i<ssid.size() ; i++) {
                ArrayList<String> wifi = new ArrayList<>();
                wifi.add(ssid.get(i));
                wifi.add(signal.get(i));
                wifi.add(auth.get(i));
                list.add(wifi);
            }
        } catch (IOException |InterruptedException e) {
            inout.error("Error in LinuxWifiManager.list: " + e.getMessage());
        }
        return list;
    }

    private void refreshInterface() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("nmcli device wifi rescan");
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
        } catch (IOException | InterruptedException e) {
            inout.error("Error in LinuxWifiManager.refreshInterface: " + e.getMessage());
        }
    }

    @Override
    ArrayList<String> getInterfaces() {
        ArrayList<String> interfaces = new ArrayList<>();
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("nmcli device status");
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            String[] lines = output.split("\n");
            for(short i=1;i<lines.length;i++) {
                StringTokenizer tokenizer = new StringTokenizer(lines[i]);
                String iface = tokenizer.nextToken();
                if (!(iface.contains("eth")) || !(iface.equals("lo"))) {
                    interfaces.add(iface);
                }
            }
        }
        catch (Exception e) {
            inout.error("Error in LinuxWifiManager.getInterfaces: " + e.getMessage());
        }
        return interfaces;
    }

    @Override
    boolean isSupport() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("nmcli device wifi rescan");
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            if(output.contains("not found")) {
                return false;
            }
            else if(output.contains("rror")) {
                return false;
            }
            else return true;
        } catch (IOException |InterruptedException e) {
            inout.error("Error in WifiManager.isSupport: " + e.getMessage());
            return false;
        }
    }
}

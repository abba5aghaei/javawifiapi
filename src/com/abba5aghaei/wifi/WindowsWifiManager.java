package com.abba5aghaei.wifi;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WindowsWifiManager extends BaseWifiManager {

    WindowsWifiManager(InOut inout) {
        super(inout);
    }

    @Override
    void initialize() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("netsh wlan show hostedNetwork");
            Scanner scanner = new Scanner(process.getInputStream());
            scanner.useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";
            inout.log(response);
            scanner.close();
            String[] lines = response.split("\r\n\r\n");
            Pattern patternSS = Pattern.compile(" +SSID name +: (.+)");
            Pattern patternM = Pattern.compile(" +Mode +: (.+)");
            Pattern patternMC = Pattern.compile(" +Max number of clients +: (.+)");
            Pattern patternA = Pattern.compile(" +Authentication +: (.+)");
            Pattern patternC = Pattern.compile(" +Cipher +: (.+)");
            Pattern patternS = Pattern.compile(" +Status +: (.+)");
            for (String line : lines) {
                Matcher matcherSS = patternSS.matcher(line);
                Matcher matcherM = patternM.matcher(line);
                Matcher matcherMC = patternMC.matcher(line);
                Matcher matcherA = patternA.matcher(line);
                Matcher matcherC = patternC.matcher(line);
                Matcher matcherS = patternS.matcher(line);
                if (matcherSS.find()) {
                    SSID = matcherSS.group(1);
                    SSID = SSID.replaceAll("\"", "");
                }
                if (matcherM.find())
                    Mode = matcherM.group(1);
                if (matcherMC.find())
                    Max_clients = matcherMC.group(1);
                if (matcherA.find())
                    Authentication = matcherA.group(1);
                if (matcherC.find())
                    Cipher = matcherC.group(1);
                if (matcherS.find())
                    Status = matcherS.group(1);
            }
            process = runtime.exec("netsh wlan show hostedNetwork setting=security");
            scanner = new Scanner(process.getInputStream());
            scanner.useDelimiter("\\A");
            response = scanner.hasNext() ? scanner.next() : "";
            inout.log(response);
            scanner.close();
            lines = response.split("\r\n\r\n");
            Pattern pattern = Pattern.compile(" +User security key +: (.+)");
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    Key = matcher.group(1);
                }
            }
        } catch (Exception e) {
            inout.error("Error in WindowsWifiManager.initialize: " + e.getMessage());
        }
        if(SSID.length()==0) {
            SSID = "Not found";
        }if(Key.length()==0) {
            Key = "Not found";
        }if(Mode.length()==0) {
            Mode = "Not found";
        }if(Max_clients.length()==0) {
            Max_clients = "Not found";
        }if(Authentication.length()==0) {
            Authentication = "Not found";
        }if(Cipher.length()==0) {
            Cipher = "Not found";
        }if(Status.length()==0) {
            Status = "Not found";
        }
    }

    @Override
    boolean connect(String ssid, String profile, int auth) {
        if(profileExist(profile)) {
            if(tryToConnect(ssid, profile)) {
                try {Thread.sleep(1000);} catch (InterruptedException e) {}
                if(isConnected()) {
                    wifiConnected = true;
                    return true;
                }
                else {
                    if(!deleteProfile(profile))
                        inout.warn("Can't delete profile");
                    inout.warn("Password is wrong");
                    return retryToConnect(ssid, profile, auth);
                }
            }
            else {
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
                return connect(ssid, profile, auth);
            }
            else {
                inout.warn("Can't create WPA2 profile");
                return false;
            }
        }
    }

    @Override
    boolean tryToConnect(String ssid, String profile) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("netsh wlan connect ssid="+ssid+" name="+profile+" interface="+Interface);
            process.waitFor();
            inout.log(getOutput(process));
            return true;
        }
        catch (Exception e) {
            inout.error("Error in WindowsWifiManager.tryToConnect: " + e.getMessage());
            return false;
        }
    }

    @Override
    boolean isConnected() {
        try {
            String output = getOutput(Runtime.getRuntime().exec("netsh wlan show interfaces"));
            inout.log(output);
            String[] lines = output.split("\r\n\r\n");
            Pattern pattern = Pattern.compile(" +Name +: (.+)");
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    if ((matcher.group(1)).equals(Interface)) {
                        pattern = Pattern.compile(" +State +: (.+)");
                        matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            String result = matcher.group(1);
                            if (result.equals("connected")) {
                                return true;
                            }
                            else if (result.equals("authenticating")) {
                                Thread.sleep(2000);
                                return isConnected();
                            }
                            else if (result.equals("associating")) {
                                Thread.sleep(2000);
                                return isConnected();
                            }
                            else {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            inout.error("Error in WindowsWifiManager.isConnected: " + e.getMessage());
        }
        return false;
    }

    @Override
    boolean disconnect() {
        try {
            inout.log(getOutput(Runtime.getRuntime().exec("netsh wlan disconnect")));
            wifiConnected = false;
            return true;
        }
        catch (Exception e) {
            inout.error("Error in WindowsWifiManager.disconnect: " + e.getMessage());
            return false;
        }
    }

    @Override
    boolean isHotspotSupport() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("netsh wlan show drivers");
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            String deviceAvailability = "Hosted network supported  : Yes";
            if (output.contains(deviceAvailability)) {
                return true;
            } else
                return false;
        } catch (IOException | InterruptedException e) {
            inout.error("Error in WindowsWifiManager.isSupport: " + e.getMessage());
            return false;
        }
    }

    @Override
    boolean turnOnHotspot() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("netsh wlan start hostednetwork");
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            if (output.contains("The hosted network couldn't be started.")){
                return false;
            }
            else if (output.contains("You must run")){
                return false;
            }
            else {
                hotspotActive = true;
                inout.log("Hotspot started");
                return true;
            }
        }
        catch (Exception e) {
            inout.error("Error in WindowsWifiManager.turnOnHotspot: " + e.getMessage());
            return false;
        }
    }

    @Override
    boolean turnOffHotspot() {
        try {
            inout.log(getOutput(Runtime.getRuntime().exec("netsh wlan stop hostednetwork")));
            hotspotActive = false;
            inout.log("Hotspot stopped");
            return true;
        }
        catch (Exception e) {
            inout.error("Error in WindowsWifiManager.turnOffHotspot: " + e.getMessage());
            return false;
        }
    }

    @Override
    boolean setHotspot(String ssid, String key) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("netsh wlan set hostednetwork mode=allow ssid=" + ssid + " key=" + key);
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            return true;
        } catch (Exception e) {
            inout.error("Error in WindowsWifiManager.setHotspot: " + e.getMessage());
            return false;
        }
    }

    @Override
    boolean profileExist(String profile) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("netsh wlan show profile");
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            StringTokenizer tokenizer = new StringTokenizer(output);
            while(tokenizer.hasMoreElements())
                if(tokenizer.nextElement().equals(profile))
                    return true;
        } catch (IOException | InterruptedException e) {
            inout.error("Error in WindowsWifiManager.profileExist: " + e.getMessage());
        }
        return false;
    }

    @Override
    boolean createProfile(String name) {
        File profile = null;
        try {
            profile = new File(name+".xml");
            PrintWriter writer = new PrintWriter(profile);
            String open_profile = "<?xml version=\"1.0\"?>\n" +
                    "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n" +
                    "\t<name>%s</name>\n" +
                    "\t<SSIDConfig>\n" +
                    "\t\t<SSID>\n" +
                    "\t\t\t<hex>%s</hex>\n" +
                    "\t\t\t<name>%s</name>\n" +
                    "\t\t</SSID>\n" +
                    "\t</SSIDConfig>\n" +
                    "\t<connectionType>ESS</connectionType>\n" +
                    "\t<connectionMode>auto</connectionMode>\n" +
                    "\t<MSM>\n" +
                    "\t\t<security>\n" +
                    "\t\t\t<authEncryption>\n" +
                    "\t\t\t\t<authentication>open</authentication>\n" +
                    "\t\t\t\t<encryption>none</encryption>\n" +
                    "\t\t\t\t<useOneX>false</useOneX>\n" +
                    "\t\t\t</authEncryption>\n" +
                    "\t\t</security>\n" +
                    "\t</MSM>\n" +
                    "\t<MacRandomization xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v3\">\n" +
                    "\t\t<enableRandomization>false</enableRandomization>\n" +
                    "\t</MacRandomization>\n" +
                    "</WLANProfile>\n";
            writer.print(String.format(open_profile,name,encode(name),name));
            writer.flush();
            writer.close();
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(String.format("netsh wlan add profile filename=%s interface=%s user=all", profile.getAbsolutePath(), Interface));
            process.waitFor();
            inout.log(getOutput(process));
            try {profile.delete();} catch (Exception ex) {}
            return true;
        } catch (Exception e) {
            inout.error("Error in WindowsWifiManager.createProfile: " + e.getMessage());
            try {profile.delete();} catch (Exception ex) {}
            return false;
        }
    }

    @Override
    public boolean createProfile(String name, String key, int auth) {
        File profile = null;
        try {
            profile = new File(name+".xml");
            PrintWriter writer = new PrintWriter(profile);
            if(auth==1) {
                String WPA_profile = "<?xml version=\"1.0\"?>\n" +
                        "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n" +
                        "\t<name>%s</name>\n" +
                        "\t<SSIDConfig>\n" +
                        "\t\t<SSID>\n" +
                        "\t\t\t<hex>496C614E6165696D69</hex>\n" +
                        "\t\t\t<name>IlaNaeimi</name>\n" +
                        "\t\t</SSID>\n" +
                        "\t</SSIDConfig>\n" +
                        "\t<connectionType>ESS</connectionType>\n" +
                        "\t<connectionMode>auto</connectionMode>\n" +
                        "\t<MSM>\n" +
                        "\t\t<security>\n" +
                        "\t\t\t<authEncryption>\n" +
                        "\t\t\t\t<authentication>WPAPSK</authentication>\n" +
                        "\t\t\t\t<encryption>AES</encryption>\n" +
                        "\t\t\t\t<useOneX>false</useOneX>\n" +
                        "\t\t\t</authEncryption>\n" +
                        "\t\t\t<sharedKey>\n" +
                        "\t\t\t\t<keyType>passPhrase</keyType>\n" +
                        "\t\t\t\t<protected>true</protected>\n" +
                        "\t\t\t\t<keyMaterial>01000000D08C9DDF0115D1118C7A00C04FC297EB01000000D6691F9EF7A0CF4B862E6CFE638F148500000000020000000000106600000001000020000000C7602F59CCD554AC42E0A286645D590374B5854088F69CCEC68A909B2D8DD514000000000E80000000020000200000004192D901FB14E18D9A09C6E26C9CFE84F637344D1F31BA17028D6795857C6EE9100000002D3989D4C2F3F52901C1F63C47AE9042400000003402E52465882D8C34F44401F16D37130A195558CCB3248E2D28BEBBB2BBEBE466E888C782873C3D4114A18DC5399C9C1D9CC64ABEF66AD4EC4765E957F2DD3C</keyMaterial>\n" +
                        "\t\t\t</sharedKey>\n" +
                        "\t\t</security>\n" +
                        "\t</MSM>\n" +
                        "\t<MacRandomization xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v3\">\n" +
                        "\t\t<enableRandomization>false</enableRandomization>\n" +
                        "\t</MacRandomization>\n" +
                        "</WLANProfile>\n";
                writer.print(String.format(WPA_profile,name));
            }
            else {
                String WPA2_profile = "<?xml version=\"1.0\"?>\n" +
                        "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n" +
                        "\t<name>%s</name>\n" +
                        "\t<SSIDConfig>\n" +
                        "\t\t<SSID>\n" +
                        "\t\t\t<hex>6162626135616768616569</hex>\n" +
                        "\t\t\t<name>abba5aghaei</name>\n" +
                        "\t\t</SSID>\n" +
                        "\t</SSIDConfig>\n" +
                        "\t<connectionType>ESS</connectionType>\n" +
                        "\t<connectionMode>auto</connectionMode>\n" +
                        "\t<MSM>\n" +
                        "\t\t<security>\n" +
                        "\t\t\t<authEncryption>\n" +
                        "\t\t\t\t<authentication>WPA2PSK</authentication>\n" +
                        "\t\t\t\t<encryption>AES</encryption>\n" +
                        "\t\t\t\t<useOneX>false</useOneX>\n" +
                        "\t\t\t</authEncryption>\n" +
                        "\t\t\t<sharedKey>\n" +
                        "\t\t\t\t<keyType>passPhrase</keyType>\n" +
                        "\t\t\t\t<protected>true</protected>\n" +
                        "\t\t\t\t<keyMaterial>01000000D08C9DDF0115D1118C7A00C04FC297EB01000000D6691F9EF7A0CF4B862E6CFE638F148500000000020000000000106600000001000020000000D6AF651BABDCCD6D23B0140AAA342DEE6E32DE5AB63CE492446DA75BB875611A000000000E8000000002000020000000781A6B99D58E558BC0115B7F6322B9525EF61347427B7ECE0225CE2DF243773710000000723CA521BDBCD2F8135393CD71F5436B40000000F0F942A9DB0E7CFAF646407ADD37F6FF7B541505D6BCE9ECB3A51B1F9E9747DB338C1439E76395A421C4E47D53389EDE3FE488C1E7CA588394F28765176C5D52</keyMaterial>\n" +
                        "\t\t\t</sharedKey>\n" +
                        "\t\t</security>\n" +
                        "\t</MSM>\n" +
                        "\t<MacRandomization xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v3\">\n" +
                        "\t\t<enableRandomization>false</enableRandomization>\n" +
                        "\t</MacRandomization>\n" +
                        "</WLANProfile>\n";
                writer.print(String.format(WPA2_profile,name));
            }
            writer.flush();
            writer.close();
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(String.format("netsh wlan add profile filename=%s interface=%s user=all", profile.getAbsolutePath(), Interface));
            process.waitFor();
            process = runtime.exec(String.format("netsh wlan set profileparameter name=%s SSIDname=%s keyMaterial=%s", name, name, key));
            process.waitFor();
            inout.log(getOutput(process));
            try {profile.delete();} catch (Exception ex) {}
            return true;
        } catch (Exception e) {
            inout.error("Error in WindowsWifiManager.createProfile(2): " + e.getMessage());
            try {profile.delete();} catch (Exception ex) {}
            return false;
        }
    }

    @Override
    boolean deleteProfile(String name) {
        try {
            inout.log(getOutput(Runtime.getRuntime().exec("netsh wlan delete profile "+ name)));
        } catch (Exception e) {
            inout.error("Error in WindowsWifiManager.deleteProfile: " + e.getMessage());
            return false;
        }
        return true;
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
            Process process = runtime.exec("netsh wlan show networks mode=bssid");
            process.waitFor();
            Thread.sleep(1000);
            process = runtime.exec("netsh wlan show networks mode=bssid");
            process.waitFor();
            Scanner scanner = new Scanner(process.getInputStream());
            scanner.useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";
            inout.log(response);
            scanner.close();
            String[] lines = response.split("\r\n\r\n");
            Pattern pattern = Pattern.compile("^SSID [0-9]+ : (.+)");
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    ssid.add(matcher.group(1));
                }
            }
            pattern = Pattern.compile(" +Signal +: (\\d+)\\W");
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    signal.add(matcher.group(1));
                }
            }
            pattern = Pattern.compile(" +Authentication +: (.+)");
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    auth.add(matcher.group(1));
                }
            }
            for(int i=0 ; i<ssid.size() ; i++) {
                ArrayList<String> wifi = new ArrayList<>();
                wifi.add(ssid.get(i));
                wifi.add(signal.get(i));
                wifi.add(auth.get(i));
                list.add(wifi);
            }
        } catch (Exception e) {
            inout.error("Error in WindowsWifiManager.list: " + e.getMessage());
        }
        return list;
    }

    private void refreshInterface() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(String.format("netsh interface set interface name=%s admin=DISABLED", Interface));
            process.waitFor();
            process = runtime.exec(String.format("netsh interface set interface name=%s admin=ENABLED", Interface));
            process.waitFor();
            inout.log(getOutput(process));
        } catch (Exception e) {
            inout.error("Error in WindowsWifiManager.refreshInterface: " + e.getMessage());
        }
    }

    @Override
    ArrayList<String> getInterfaces() {
        ArrayList<String> names = new ArrayList<>();
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("netsh wlan show drivers");
            process.waitFor();
            String response = getOutput(process);
            inout.log(response);
            String[] lines = response.split("\r\n\r\n");
            Pattern pattern = Pattern.compile("Interface name: (.+)");
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    names.add(matcher.group(1));
                }
            }
        } catch (InterruptedException | IOException e) {
            inout.error("Error in WindowsWifiManager.getInterfaces: " + e.getMessage());
        }
        return names;
    }

    @Override
    boolean isSupport() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("netsh wlan show interfaces");
            process.waitFor();
            String output = getOutput(process);
            inout.log(output);
            if(output.trim().length()==0)
                return false;
            return !output.contains("There is no");
        } catch (IOException | InterruptedException e) {
            inout.error("Error in WifiManager.isSupport: " + e.getMessage());
            return false;
        }
    }
}

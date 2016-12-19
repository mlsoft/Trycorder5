package net.ddns.mlsoftlaberge.trycorder.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by mlsoft on 16-06-25.
 */
public class Fetcher {

    private Context context;

    public Fetcher(Context cx) {
        context=cx;
    }

    // =================================================================================
    // run a command line program with args, return the printed output
    // invoque with run(new String[] { "ls", "-la" },"/data/data");
    public synchronized String run(String[] cmd, String workdirectory)
            throws IOException {
        String result = "";

        try {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            // set working directory
            if (workdirectory != null)
                builder.directory(new File(workdirectory));
            builder.redirectErrorStream(true);
            Process process = builder.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[1024];
            while (in.read(re) != -1) {
                System.out.println(new String(re));
                result = result + new String(re);
            }
            in.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    // =================================================================================
    // functions to fetch info from the system

    // =================== fetch telephone status =======================

    public String fetch_tel_status() {
        Context cx = context;
        String result = null;
        TelephonyManager tm = (TelephonyManager) cx
                .getSystemService(Context.TELEPHONY_SERVICE);//
        String str = "";
        str += "DeviceId(IMEI) = " + tm.getDeviceId() + "\n";
        str += "DeviceSoftwareVersion = " + tm.getDeviceSoftwareVersion()
                + "\n";
        str += "Line1Number = " + tm.getLine1Number() + "\n";
        str += "NetworkCountryIso = " + tm.getNetworkCountryIso() + "\n";
        str += "NetworkOperator = " + tm.getNetworkOperator() + "\n";
        str += "NetworkOperatorName = " + tm.getNetworkOperatorName() + "\n";
        str += "NetworkType = " + tm.getNetworkType() + "\n";
        str += "PhoneType = " + tm.getPhoneType() + "\n";
        str += "SimCountryIso = " + tm.getSimCountryIso() + "\n";
        str += "SimOperator = " + tm.getSimOperator() + "\n";
        str += "SimOperatorName = " + tm.getSimOperatorName() + "\n";
        str += "SimSerialNumber = " + tm.getSimSerialNumber() + "\n";
        str += "SimState = " + tm.getSimState() + "\n";
        str += "SubscriberId(IMSI) = " + tm.getSubscriberId() + "\n";
        str += "VoiceMailNumber = " + tm.getVoiceMailNumber() + "\n";

        int mcc = cx.getResources().getConfiguration().mcc;
        int mnc = cx.getResources().getConfiguration().mnc;
        str += "IMSI MCC (Mobile Country Code):" + String.valueOf(mcc) + "\n";
        str += "IMSI MNC (Mobile Network Code):" + String.valueOf(mnc) + "\n";
        result = str;
        return result;
    }

    // ================ fetch process info ===================

    public String fetch_process_info() {
        String result = "";
        try {
            String[] args = {"/system/bin/top", "-n", "1"};
            result = run(args, "/system/bin/");
        } catch (IOException ex) {
            say("fetch_process_info ex=" + ex.toString());
        }
        return result;
    }

    // ================= fetch network info ===================

    public String fetch_dmesg_info() {
        String result = "";
        try {
            String[] args = {"/system/bin/dmesg"};
            result = run(args, "/system/bin/");
        } catch (IOException ex) {
            say("fetch_dmesg_info ex=" + ex.toString());
        }
        return result;
    }

    // ============ fetch system info ===========
    private StringBuffer buffer;

    public String fetch_system_info() {
        buffer = new StringBuffer();
        initProperty("java.vendor.url", "java.vendor.url");
        initProperty("java.class.path", "java.class.path");
        initProperty("user.home", "user.home");
        initProperty("java.class.version", "java.class.version");
        initProperty("os.version", "os.version");
        initProperty("java.vendor", "java.vendor");
        initProperty("user.dir", "user.dir");
        initProperty("user.timezone", "user.timezone");
        initProperty("path.separator", "path.separator");
        initProperty(" os.name", " os.name");
        initProperty("os.arch", "os.arch");
        initProperty("line.separator", "line.separator");
        initProperty("file.separator", "file.separator");
        initProperty("user.name", "user.name");
        initProperty("java.version", "java.version");
        initProperty("java.home", "java.home");
        return buffer.toString();
    }

    private void initProperty(String description, String propertyStr) {
        buffer.append(description).append(":");
        buffer.append(System.getProperty(propertyStr)).append("\n");
    }

    // ================= fetch os information ===================

    public String fetch_os_info() {
        StringBuffer sInfo = new StringBuffer();
        final ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(100);
        Iterator<ActivityManager.RunningTaskInfo> l = tasks.iterator();
        while (l.hasNext()) {
            ActivityManager.RunningTaskInfo ti = (ActivityManager.RunningTaskInfo) l.next();
            sInfo.append("id: ").append(ti.id);
            sInfo.append("\nbaseActivity: ").append(
                    ti.baseActivity.flattenToString());
            sInfo.append("\nnumActivities: ").append(ti.numActivities);
            sInfo.append("\nnumRunning: ").append(ti.numRunning);
            sInfo.append("\ndescription: ").append(ti.description);
            sInfo.append("\n\n");
        }
        return sInfo.toString();
    }

    // ================= fetch network info ===================

    public String fetch_network_info() {
        String result = "";
        NetInfo netinfo = new NetInfo(context);
        result += String.format("Network type : %d\n", netinfo.getCurrentNetworkType());
        result += String.format("Wifi IP Addr : %s\n", netinfo.getWifiIpAddress());
        result += String.format("Wifi MAC Addr : %s\n", netinfo.getWiFiMACAddress());
        result += String.format("Wifi SSID : %s\n", netinfo.getWiFiSSID());
        result += String.format("IP Address : %s\n", netinfo.getIPAddress());
        return result;
    }

    public String fetch_ip_address() {
        NetInfo netinfo = new NetInfo(context);
        return netinfo.getWifiIpAddress();
    }

    // ===================================================================================
    // public functions to obtain different infos from network interface

    public class NetInfo {
        private ConnectivityManager connManager = null;
        private WifiManager wifiManager = null;
        private WifiInfo wifiInfo = null;

        public NetInfo(Context context) {
            connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            wifiInfo = wifiManager.getConnectionInfo();
        }

        public int getCurrentNetworkType() {
            if (null == connManager)
                return 0;

            NetworkInfo netinfo = connManager.getActiveNetworkInfo();

            return netinfo.getType();
        }

        public String getWifiIpAddress() {
            if (null == wifiManager || null == wifiInfo)
                return "";

            int ipAddress = wifiInfo.getIpAddress();

            return String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));
        }

        public String getWiFiMACAddress() {
            if (null == wifiManager || null == wifiInfo)
                return "";

            return wifiInfo.getMacAddress();
        }

        public String getWiFiSSID() {
            if (null == wifiManager || null == wifiInfo)
                return "";

            return wifiInfo.getSSID();
        }

        public String getIPAddress() {
            String ipaddress = "";

            try {
                Enumeration<NetworkInterface> enumnet = NetworkInterface.getNetworkInterfaces();
                NetworkInterface netinterface = null;

                while (enumnet.hasMoreElements()) {
                    netinterface = enumnet.nextElement();

                    for (Enumeration<InetAddress> enumip = netinterface.getInetAddresses();
                         enumip.hasMoreElements(); ) {
                        InetAddress inetAddress = enumip.nextElement();

                        if (!inetAddress.isLoopbackAddress()) {
                            ipaddress = inetAddress.getHostAddress();

                            break;
                        }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }

            return ipaddress;
        }
    }

    // =================================================================================
    // fetch the internet connectivity

    public String fetch_connectivity() {
        if (checkInternet()) {
            return "INTERNET is Active\n";
        }
        return "INTERNET is Off\n";
    }

    // check internet connectivity (Hi! Elvis)
    public boolean checkInternet() {
        ConnectivityManager connect = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connect.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED
                || connect.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        return false;
    }

    // ================= fetch cpu info ===================

    public String fetch_cpu_info() {
        String result = "";
        try {
            String[] args = {"/system/bin/cat", "/proc/cpuinfo"};
            result = run(args, "/system/bin/");
        } catch (IOException ex) {
            say("fetch_cpu_info ex=" + ex.toString());
        }
        return result;
    }

    // ================= fetch memory info ===================

    public String fetch_memory_info() {
        StringBuffer memoryInfo = new StringBuffer();
        final ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(outInfo);
        memoryInfo.append("\nTotal Available Memory :")
                .append(outInfo.availMem >> 10).append("k");
        memoryInfo.append("\nTotal Available Memory :")
                .append(outInfo.availMem >> 20).append("M");
        memoryInfo.append("\nIn low memory situation:").append(
                outInfo.lowMemory);

        String result = null;
        try {
            String[] args = { "/system/bin/cat", "/proc/meminfo" };
            result = run(args, "/system/bin/");
        } catch (IOException ex) {
            Log.i("fetch_memory_info", "ex=" + ex.toString());
        }

        return memoryInfo.toString() + "\n\n" + result;
    }

    public String fetch_sensors_list() {
        StringBuffer buffer=new StringBuffer("");
        SensorManager mSensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);

        List<Sensor> mList= mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (int i = 0; i < mList.size(); i++) {
            buffer.append(mList.get(i).getName() + " - " + mList.get(i).getVendor() + " - " + mList.get(i).getType() + "\n");
        }
        return buffer.toString();
    }

    private void say(String texte) {
        // dont say anything
    }

    // ==================================================================================
    //  get system info module

    private StatFs getStatFs() {
        File path = Environment.getDataDirectory();
        return new StatFs(path.getPath());
    }

    private long getAvailableInternalMemorySize(StatFs stat) {
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    private long getTotalInternalMemorySize(StatFs stat) {
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    public String fetch_packinfo() {
        StringBuilder message=new StringBuilder();
        message.append("Locale: ").append(Locale.getDefault()).append('\n');
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi;
            pi = pm.getPackageInfo(context.getPackageName(), 0);
            message.append("Version: ").append(pi.versionName).append('\n');
            message.append("Package: ").append(pi.packageName).append('\n');
        } catch (Exception e) {
            Log.e("CustomExceptionHandler", "Error", e);
            message.append("Could not get Version information for ").append(
                    context.getPackageName());
        }
        message.append("Phone Model: ").append(android.os.Build.MODEL).append('\n');
        message.append("Android Version: ").append(android.os.Build.VERSION.RELEASE).append('\n');
        message.append("Board: ").append(android.os.Build.BOARD).append('\n');
        message.append("Brand: ").append(android.os.Build.BRAND).append('\n');
        message.append("Device: ").append(android.os.Build.DEVICE).append('\n');
        message.append("Host: ").append(android.os.Build.HOST).append('\n');
        message.append("ID: ").append(android.os.Build.ID).append('\n');
        message.append("Model: ").append(android.os.Build.MODEL).append('\n');
        message.append("Product: ").append(android.os.Build.PRODUCT).append(
                '\n');
        message.append("Type: ").append(android.os.Build.TYPE).append('\n');
        StatFs stat = getStatFs();
        message.append("Total Internal memory: ").append(
                getTotalInternalMemorySize(stat)).append('\n');
        message.append("Available Internal memory: ").append(
                getAvailableInternalMemorySize(stat)).append('\n');
        return(message.toString());
    }

    public String fetch_device_name() {
        return(android.os.Build.MODEL);
    }

    public String fetch_dhcpinfo() {
        DhcpInfo d;
        WifiManager wifii;

        wifii = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        d = wifii.getDhcpInfo();

        StringBuilder message=new StringBuilder();
        message.append("Locale: ").append(Locale.getDefault()).append('\n');
        message.append("DNS 1: " + addrtostring(d.dns1)+ "\n");
        message.append("DNS 2: " + addrtostring(d.dns2)+ "\n");
        message.append("Default Gateway: " + addrtostring(d.gateway)+ "\n");
        message.append("IP Address: " + addrtostring(d.ipAddress)+ "\n");
        message.append("Lease Time: " + String.valueOf(d.leaseDuration)+ "\n");
        message.append("Subnet Mask: " + addrtostring(d.netmask)+ "\n");
        message.append("Server IP: " + addrtostring(d.serverAddress)+ "\n");

        return(message.toString());
    }

    private String addrtostring(int addr) {
        String addrtext=(String.format("%d.%d.%d.%d",addr&0xff,(addr>>8)&0xff,(addr>>16)&0xff,(addr>>24)&0xff));
        return(addrtext);
    }
}

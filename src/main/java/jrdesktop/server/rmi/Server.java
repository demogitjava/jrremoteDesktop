package jrdesktop.server.rmi;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import javax.swing.JOptionPane;

import jrdesktop.ConnectionInfos;
import jrdesktop.HostProperties;
import jrdesktop.SysTray;
import jrdesktop.main;
import jrdesktop.server.Config;
import jrdesktop.viewer.ViewerData;
import jrdesktop.server.robot;
import jrdesktop.utilities.ClipbrdUtility;
import jrdesktop.utilities.FileUtility;
import jrdesktop.utilities.InetAdrUtility;
import jrdesktop.utilities.ZipUtility;

/**
 * Server.java
 * @author benbac
 */

public class Server extends Thread {
    
    private static boolean idle = true;
    private static boolean running = false;
    
    private static Registry registry;
    private static ServerImpl serverImpl;
    
    private static robot rt = new robot();
    public static ClipbrdUtility clipbrdUtility;
    
    private static ArrayList<Object> Objects = new ArrayList<Object>();            
    private static Hashtable<Integer, ViewerData> viewers = 
            new Hashtable<Integer, ViewerData>();
    private static Hashtable<Integer, ConnectionInfos> connectionsInfos = 
            new Hashtable<Integer, ConnectionInfos>();     
        
    private static String uploadingFolder;   
        
    public static void Start() { 
        idle = false;
        running = false;                 
        Config.loadConfiguration();
        if (Config.ssl_enabled) {        
            FileUtility.checkFile(main.KEY_STORE, "keystore");
            FileUtility.checkFile(main.TRUST_STORE, "truststore");        
            main.setStoreProperties();            
        }
        else
            main.clearStoreProperties();        
        
        if (Config.default_address)
            System.setProperty("java.rmi.server.hostname", 
                    Config.server_address);
        else
            System.getProperties().remove("java.rmi.server.hostname");        
         
        try{
            
            if (Config.ssl_enabled && Config.multihomed_enabled)
                serverImpl = new ServerImpl(
                        new MultihomeRMIClientSocketFactory(
                            new SslRMIClientSocketFactory(),
                            InetAdrUtility.getLocalIPAdresses()),
                        new SslRMIServerSocketFactory(null, null, true));                         
            else if (Config.ssl_enabled && !Config.multihomed_enabled)
                serverImpl = new ServerImpl(
                        new SslRMIClientSocketFactory(), 
                        new SslRMIServerSocketFactory(null, null, true));                    
            else if (!Config.ssl_enabled && Config.multihomed_enabled)
                serverImpl = new ServerImpl(
                        new MultihomeRMIClientSocketFactory(null,
                        InetAdrUtility.getLocalIPAdresses()), null);                       
            else if (!Config.ssl_enabled && !Config.multihomed_enabled)
                serverImpl = new ServerImpl();                        
            
            if (Config.ssl_enabled)
                registry = LocateRegistry.createRegistry(Config.server_port, 
                        new SslRMIClientSocketFactory(),
                        new SslRMIServerSocketFactory(null, null, true)); 
            else
                registry = LocateRegistry.createRegistry(Config.server_port); 
            
            registry.rebind("ServerImpl", serverImpl); 
            
        } catch (Exception e) {                  
            e.getStackTrace();
            Stop();          
            
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error !!",
                    JOptionPane.ERROR_MESSAGE); 
            return;
        }   

        System.out.println(getStatus());
        running = true;
        clipbrdUtility = new ClipbrdUtility();         
        uploadingFolder = FileUtility.getCurrentDirectory(); 
        SysTray.updateServerStatus(SysTray.SERVER_STARTED);        
    }              
    
    public static void Stop() {
        if (running) {
            running = false;
            disconnectAllViewers();
            SysTray.updateServerStatus(SysTray.SERVER_STOPPED);            
        }
        else
            SysTray.updateServerStatus(SysTray.CONNECTION_FAILED);
        try {            
            if (registry != null)
                UnicastRemoteObject.unexportObject(registry, true);            
        } catch (Exception e) {
            e.getStackTrace();
        }  
        registry = null;
        serverImpl = null;
    }
    
    public static boolean isRunning() {
        return running;
    }
    
    public static boolean isIdle() {
        return idle;
    }
    
    public static void updateOptions(Object data, int index) {
        ArrayList Options = (ArrayList) data;         
        
        viewers.get(index).setScreenScale((Float) Options.get(0));  
        viewers.get(index).setScreenRect(
                rt.getCustomScreenRect((Rectangle) Options.get(1)));  
        viewers.get(index).setCompressionLevel((Integer) Options.get(2)); 
        viewers.get(index).setDataCompression((Boolean) Options.get(3));            
        viewers.get(index).setImageQuality((Float) Options.get(4));
        viewers.get(index).setColorQuality((Integer) Options.get(5)); 
        viewers.get(index).setClipboardTransfer((Boolean) Options.get(6));  
        viewers.get(index).setInetAddress((InetAddress) Options.get(7));
    }
        
    public static void updateData(byte[] data, int index) {    
        Object object;
        try {
            if (viewers.get(index).isDataCompressionEnabled())        
                object = ZipUtility.decompressObject(data);
            else
                object = ZipUtility.byteArraytoObject(data);
            
            connectionsInfos.get(index).incReceivedData(data.length);
            rt.updateData(object, viewers.get(index));               
        }
        catch (Exception e) {
            e.getStackTrace();
        }       
    }

    public static byte[] updateData(int index) {  
        byte[] data = null;            
        ArrayList<byte[]> allData = new ArrayList<byte[]>(2);
        allData.add(rt.CaptureScreenByteArray(viewers.get(index)));
        //Objects.add(rt.CaptureScreenByteArray(viewers.get(index)));        
        Objects.add(viewers.get(index).getScreenRect());             
        if (viewers.get(index).isClipboardTransferEnabled())
            if (!clipbrdUtility.isEmpty())
                Objects.add(clipbrdUtility.getContent());

        synchronized(Objects) {
            try {
                if (viewers.get(index).isDataCompressionEnabled())
                    data = ZipUtility.compressObject(Objects,
                        viewers.get(index).getCompressionLevel());
                else
                    data = ZipUtility.objecttoByteArray(Objects);  
            }
            catch (IOException e) {
                e.getStackTrace();
            }
            allData.add(data);
            try {
                data = ZipUtility.objecttoByteArray(allData);
            } catch (IOException ioe) {
                ioe.getStackTrace();
            }
            Objects = new ArrayList<Object>();            
        }
        
        connectionsInfos.get(index).incSentData(data.length);
        return data;   
    }  

    public static void AddObject(Object object) {
        Objects.add(object);
    } 
    
    public static synchronized int addViewer(InetAddress inetAddress,
            String username, String password) {
        if (!Config.username.equals(username) || 
                !Config.password.equals(password))
            return -1;

        int index = viewers.size();        
        viewers.put(index, new ViewerData());            
        connectionsInfos.put(index, new ConnectionInfos(true));

        SysTray.displayViewer(inetAddress.toString(), index, true);        
        return index;
    }
         
    public static synchronized int removeViewer(int index) {
        String viewer = viewers.get(index).getInetAddress().toString();
        
        viewers.remove(index);
        connectionsInfos.remove(index);
        
        SysTray.displayViewer(viewer, viewers.size(), false);        
        return index;
    } 
    
    public static void disconnectAllViewers() {
        Enumeration<Integer> viewerEnum = viewers.keys();
        while (viewerEnum.hasMoreElements())
            removeViewer(viewerEnum.nextElement());
    }
    
   public static byte[] ReceiveFile(String fileName, int index){
      try {
         File file = new File(fileName);
         byte buffer[] = new byte[(int)file.length()];
         BufferedInputStream input = new
            BufferedInputStream(new FileInputStream(file));
         input.read(buffer, 0, buffer.length);
         input.close();
               
         connectionsInfos.get(index).incSentData(buffer.length);
         return(buffer);
      } catch(Exception e){
            e.printStackTrace();
         return(null);
      }
   }
      
    public static void SendFile(byte[] filedata, String fileName, int index) {
        try {             
            fileName = uploadingFolder + fileName;
            new File(new File(fileName).getParent()).mkdirs();
            File file = new File(fileName);

            BufferedOutputStream output = new
                BufferedOutputStream(new FileOutputStream(file));
            output.write(filedata, 0, filedata.length);
            output.flush();
            output.close();  
                      
            connectionsInfos.get(index).incReceivedData(filedata.length);
        } catch (Exception e) {
            e.getStackTrace();
        }                 
   }   
    
    public static int SendClipboardFileList() {
        File[] files = clipbrdUtility.getFiles();
        if (files.length == 0) return 0;
        ArrayList<Object> FileSysInfos = new ArrayList<Object>();
        FileSysInfos.add(files[0].getParent() + File.separatorChar);
        FileSysInfos.add(FileUtility.getAllFiles(files)); 
        Objects.add(FileSysInfos);       
        return files.length;
    }
    
    public static void setUploadingFolder () {
       uploadingFolder = FileUtility.getCurrentDirectory();
       Objects.add(new File("."));        
    }     
    
    public static ArrayList<InetAddress> getViewersAds () {
        ArrayList<InetAddress> viewersAds = new ArrayList<InetAddress>(); 
        for (int i=0; i<viewers.size(); i++)
            viewersAds.add(viewers.get(i).getInetAddress());
        return viewersAds;
    }
    
    public static int getViewersCount () {
        return viewers.size();
    }
    
    public static void displayViewerProperties (int index) {
        HostProperties.displayRemoteProperties(
            viewers.get(index).getProperties());
    }
    
    public static void setViewerProperties (InetAddress inetAddress, 
            Hashtable props) {
        int index = getViewersAds().indexOf(inetAddress);
        viewers.get(index).setProperties(props);
    }
    
    public static void displayConnectionInfos(int index) {
        connectionsInfos.get(index).display();
    }
    
    public static String getStatus() {
        boolean auth = (Config.username.length() != 0) || 
            (Config.password.length() != 0);
        String status = "Running ..." + 
            "\nat: " + Config.server_address + ":" + Config.server_port + 
            "\nauthentication: " + (auth == true ? "enabled" : "disabled") +
            "\nencryption: " + 
            (Config.ssl_enabled == true ? "enabled" : "disabled") +
            "\nmultihomed: " + 
            (Config.multihomed_enabled == true ? "enabled" : "disabled")+ "\n";
        return status;
    }
}

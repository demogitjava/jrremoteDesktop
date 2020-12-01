package jrdesktop;

import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import jrdesktop.server.rmi.Server;
import jrdesktop.viewer.rmi.Viewer;
import jrdesktop.utilities.FileUtility;



public class main {
    
    public static final URL IDLE_ICON = main.class.getResource("images/idle.png");
    public static final URL ALIVE_ICON = main.class.getResource("images/background.png");        
    public static final URL WAIT_ICON = main.class.getResource("images/display.png");
    public static final URL START_ICON = main.class.getResource("images/player_play.png");
    public static final URL STOP_ICON = main.class.getResource("images/player_stop.png");
    public static final URL PAUSE_ICON = main.class.getResource("images/player_pause.png");
    public static final URL INPUTS_ICON = main.class.getResource("images/input_devices.png");
    public static final URL LOCKED_INPUTS_ICON = main.class.getResource("images/locked_inputs.png");
    public static final URL FULL_SCREEN_ICON = main.class.getResource("images/view_fullscreen.png");
    public static final URL NORMAL_SCREEN_ICON = main.class.getResource("images/view_nofullscreen.png");
    public static final URL DEFAULT_SCREEN_ICON = main.class.getResource("images/default_screen.png");
    public static final URL CUSTOM_SCREEN_ICON = main.class.getResource("images/custom_screen.png");
    
    public static String CONFIG_FILE;
    public static String SERVER_CONFIG_FILE;
    public static String VIEWER_CONFIG_FILE;
    
    public static String KEY_STORE;   
    public static String TRUST_STORE;
        
    public static void main (String args[]) {          
        if (System.getSecurityManager() == null)
	    System.setSecurityManager(new SecurityMng());       

        CONFIG_FILE = FileUtility.getCurrentDirectory() + "config";
        SERVER_CONFIG_FILE = FileUtility.getCurrentDirectory() + "server.config";
        VIEWER_CONFIG_FILE = FileUtility.getCurrentDirectory() + "viewer.config";    
    
        KEY_STORE = FileUtility.getCurrentDirectory() + "keystore";   
        TRUST_STORE = FileUtility.getCurrentDirectory() + "truststore";
    
        System.getProperties().remove("java.rmi.server.hostname");        
                          
        if (args.length > 0) {                    
            String arg;
            boolean serverSide = true;
            String server = "127.0.0.1";
            int port = 6666;
            String username = "";
            String password = "";
            boolean ssl = false;
            boolean multihome = false;
            
            arg = args[0];
            if (arg.equals("-help") || arg.equals("-?")) // display usage information
                displayHelp();     
            else if (arg.equals("-version")) // display version information
                System.out.println("\t" + getVersionInfos());               
            else if (arg.equals("server")) // start server with default paramaters
                startServer(6666, "", "", false, false);
            else if (arg.equals("viewer")) // start viewer with default paramaters
                startViewer("127.0.0.1", 6666, "", "", false);            
            else if (arg.equals("display")) // display jrdesktop's main window
                mainFrame.main(null);            
            else {
                for (int i=0; i<args.length; i++) {
                    arg = args[i];                  
                    
                    if (arg.startsWith("-a:")) {
                        server = arg.substring(3);   
                        serverSide = false;
                    }
                    else if (arg.startsWith("-p:")) {
                        try {
                            port = Integer.parseInt(arg.substring(3));
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid port number, using default.");
                        }
                        if( port < 1 || port > 65535) {
                            port = 6666;
                            System.err.println("Invalid port number, using default.");
                        }
                    }
                    else if (arg.startsWith("-u:"))
                        username = arg.substring(3);
                    else if (arg.startsWith("-d:"))
                        password = arg.substring(3);
                    else if (arg.startsWith("-s"))
                        ssl = true;
                    else if (arg.startsWith("-m"))
                        multihome = true;
                }
                    
                if (serverSide)
                    startServer(port, username, password, ssl , multihome);
                else
                    startViewer(server, port, username, password, ssl);         
            }
        }
        else
            displayHelp();           
        
        Config.loadConfiguration();
        if (!Config.Systray_disabled)
            SysTray.Show();
        if (!Config.GUI_disabled)
            mainFrame.main(null);         
    }      
       
    public static void displayHelp() {
        System.out.println(                            
            "jrdesktop - Java Remote Desktop.\n" + 
            "http://jrdesktop.sourceforge.net/\n\n" + 
            
            "Usage: java -jar jrdesktop.jar <command> [options]\n\n" + 
               
            "   display     display main window.\n" +            
            "   server      start server using default parameters.\n" +
            "   viewer      start viewer using default parameters.\n" +            
            "   (default parameters : local machine with port 6666).\n\n" +
                
            "Options:\n" + 
            "   -a:address      server's address.\n" +
            "   -p:port         server's port.\n" +
            "   -u:username     user's name.\n" +
            "   -d:password     user's password.\n" +
            "   -s              secured connection using SSL.\n" +
            "   -m              multihomed server.\n" +
            "   -version        display version information.\n" +
            "   -help or -?     display usage information.\n"
        );
    }
    
    public static void startServer(int port, 
            String username, String password, 
            boolean ssl_enabled, boolean multihomed_enabled) {
        
        jrdesktop.server.Config.SetConfiguration(port, username, password, 
                ssl_enabled, multihomed_enabled);
        
        Server.Start();
    }    
    
    public static void startViewer(String server, int port, 
            String username, String password, boolean ssl_enabled) {
        
        jrdesktop.viewer.Config.SetConfiguration(server, port, 
                username, password, ssl_enabled);
        
        new Viewer().Start();     
    }            
    
   public static void setStoreProperties() {     
        System.setProperty("javax.net.ssl.trustStore", TRUST_STORE); 
        System.setProperty("javax.net.ssl.trustStorePassword", "trustword"); 
        System.setProperty("javax.net.ssl.keyStore", KEY_STORE); 
        System.setProperty("javax.net.ssl.keyStorePassword", "password");   
   }
    
   public static void clearStoreProperties() {
        System.getProperties().remove("javax.net.ssl.trustStore"); 
        System.getProperties().remove("javax.net.ssl.trustStorePassword");         
        System.getProperties().remove("javax.net.ssl.keyStore"); 
        System.getProperties().remove("javax.net.ssl.keyStorePassword");               
    }      
    
    public static String getVersionInfos() {
        try {
            String classContainer = main.class.getProtectionDomain().
                getCodeSource().getLocation().toString();           
            URL manifestUrl = new URL("jar:" + classContainer + 
                    "!/META-INF/MANIFEST.MF");            
            Manifest manifest = new Manifest(manifestUrl.openStream());  
            Attributes attr = manifest.getMainAttributes();
            final String version = attr.getValue("Implementation-Version");
            final String built_date = attr.getValue("Built-Date");
            return "jrdesktop " + version + " \tBuilt date: " + built_date;
        } catch (Exception e){
            e.getStackTrace();
            return null;
        } 
    }
   
    public static void exit() {
        if (Server.isRunning())       
            Server.Stop();
        clearStoreProperties();
        System.setSecurityManager(null);
        System.exit(0);
    }
}

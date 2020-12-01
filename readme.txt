Java Remote Desktop 

jrdesktop 0.2.0030 - june 23th, 2008

Overview
	Java Remote Desktop (jrdesktop) is an open source software for viewing and/or controlling a distance PC.

	Besides then screenshots, keyboard and mouse events transfer, jrdesktop includes many additional features (like: file transfer, data compression, color conversion, ...).

	jrdesktop uses RMI (Remote Method Invocation) with SSL/TLS to establish a secured connection between the viewer and the server.

	Java Remote Desktop (jrdesktop) is intended to run across different platforms (based on JVM).

Main features

    * Screenshots, keyboard and mouse events transfer
    * Control functions : Start, Stop, Pause and Resume, view only, full control
    * Screen functions : full screen, custom size, scale, ...
    * Data compression (with level selection)
    * JPEG quality compression (with level selection)
    * Color quality (full colors, 16 bits, 256 colors, gray color)
    * Clipboard transfer (texts and images only)
    * File transfer (only small files)
    * Connection infos : duration, transferred data size, speed
    * Authentication & encryption
    * Multi-sessions
    * ....

Requirements: JDK 6 (JRE 6) or above
	
Execution
	•	Make sure that you already added java bin directory to the environment variable : PATH
			(example : PATH = 	...; c:\Program Files\Java\jdk1.6.0_03\bin)
	
	•	Simply double click on jrdesktop.jar to start the application
	
	•	Or manually from the command line:
			
            Usage: java -jar jrdesktop.jar <command> [options]
               
				display     display main window.
				server      start server using default parameters.
				viewer      start viewer using default parameters.           
               (default parameters : local machine with port 6666).
               
            Options:
               -a:address      server's address.
               -p:port         server's port.
               -u:username     user's name.
               -d:password     user's password.
               -s              secured connection using SSL.
               -m              multihomed server (server with many network interfaces (IP addresses)).
               -version        display version information.
               -help or -?     display usage information.
			   
	•	Examples	

			Display jrdesktop main windows	
				java -jar jrdesktop.jar display
				
			Start jrdesktop server using default parameters (local machine with port 6666)
				java -jar jrdesktop.jar server
				
			Connect to (view) local machine
				java -jar jrdesktop.jar viewer
				
			Start multihomed server using port 8888, admin as username and pwd as password with secured connection (ssl)
				java -jar jrdesktop.jar -p:8888 -u:admin -d:pwd -s -m
			
			Connect to (view) 192.168.1.2 using port 8888, admin as username and  pwd as password with secured connection (ssl)
				java -jar jrdesktop.jar -a:192.168.1.2 -p:8888 -u:admin -d:pwd -s

			
Any comments, bugs, suggestions, questions; please contact us.

http://jrdesktop.sourceforge.net/

pfe062008@gmail.com

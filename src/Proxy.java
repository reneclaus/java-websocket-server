import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.java_websocket.WebSocketImpl;


public class Proxy {
	List<WebSocketServerProxy> webSocketServers = new ArrayList<WebSocketServerProxy>() ;
	List<SocketServerProxy> socketServers = new ArrayList<SocketServerProxy>() ;
	
	public void notifyMessage(Object src, String msg) {
		for (WebSocketServerProxy s : webSocketServers) {
			s.send(src, msg) ;
		}
		for (SocketServerProxy s : socketServers) {
			s.send(src, msg) ;
		}
	}
	
	public void send (String msg) {
		notifyMessage(null, msg) ;
	}
	
	public boolean startWebSocketServer (int port) {
		try {
			WebSocketServerProxy s = new WebSocketServerProxy(this, port);
			webSocketServers.add(s) ;
			s.start();
			return true ;
		} catch (IOException e) {
			return false ;
		}
	}
	public boolean startSocketServer (int port) {
		try {
			SocketServerProxy s = new SocketServerProxy(this, port);
			socketServers.add(s) ;
			s.start();
			return true ;
		} catch (IOException e) {
			return false ;
		}
	}
	public void exit () {
		for (WebSocketServerProxy s : webSocketServers) {
			try {
				s.stop() ;
			} catch (IOException | InterruptedException e) {}
		}
		for (SocketServerProxy s : socketServers) {
			s.stop() ;
		}
	}
	public void restart () {
		for (WebSocketServerProxy s : webSocketServers) {
			try {
				s.stop() ;
				s.start() ;
			} catch (IOException | InterruptedException e) {}
		}
		for (SocketServerProxy s : socketServers) {
			s.stop() ;
			try {
				s.start() ;
			} catch (IOException e) {}
		}
	}
	
	public static InetAddress getLocalIpAddress() {
		InetAddress inetAddress_final = null ;
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                /*if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress ;
	                }*/
	               // return inetAddress ;
	                if (inetAddress_final != null && inetAddress.isLoopbackAddress())
	                	continue ;
	                if (inetAddress_final != null && !(inetAddress instanceof Inet4Address))
	                	continue ;
	                inetAddress_final = inetAddress ;
	            }
	        }
	    } catch (SocketException ex) {}
	    return inetAddress_final;
	}
	
	public static Pattern webSocketServer_arg = Pattern.compile("-WS([0-9]+)") ;
	public static Pattern socketServer_arg = Pattern.compile("-S([0-9]+)") ;
	public static Pattern webServer_arg_port = Pattern.compile("-P([0-9]+)") ;
	public static Pattern webServer_arg_htdocs = Pattern.compile("-W(.*)") ;
	public static void main( String[] args ) throws InterruptedException , IOException {
		Proxy proxy = new Proxy() ;
		if (args.length == 0) {
			args = new String[]{"-WS3037", "-WS3039", "-S3038", "-P8080", "-W", "."} ;
			System.out.println("Specify the ports as command line options:") ;
			System.out.println("  -WS3037   --> websocket server on port 3037") ;
			System.out.println("  -S3038    --> standard socket on port 3038") ;
			System.out.println("  -P8080    --> web server on port 3037") ;
			System.out.println("  -W folder --> web server in subfolder files") ;
			System.out.print("Default options:") ;
			for (int i = 0 ; i < args.length ; i++)
				System.out.print(" " + args[i]) ;
			System.out.println() ;
		}
		
		InetAddress ip = getLocalIpAddress() ;
		WebSocketImpl.DEBUG = false;
		
		int www_port = 8888 ;
		File www_htdocs = new File(System.getProperty("user.dir")) ;
		
		for (int i = 0 ; i < args.length ; i++) {
			Matcher webSocketServer_matcher = webSocketServer_arg.matcher(args[i]) ;
			if (webSocketServer_matcher.matches()) {
				int port = Integer.parseInt(webSocketServer_matcher.group(1)) ;
				if(proxy.startWebSocketServer(port)) {
					System.out.println("Websocket opened at: ws:/" + ip.toString() + ":" + port );
				}
				else {
					System.out.println("Unable to open websocket on port " + port) ;
				}
				continue ;
			}
			Matcher socketServer_matcher = socketServer_arg.matcher(args[i]) ;
			if (socketServer_matcher.matches()) {
				int port = Integer.parseInt(socketServer_matcher.group(1)) ;
				if(proxy.startSocketServer(port)) {
					System.out.println("Socket opened at: " + ip.toString().substring(1) + ":" + port );
				}
				else {
					System.out.println("Unable to open socket on port " + port) ;
				}
				continue ;
			}
			Matcher webServer_matcher = webServer_arg_port.matcher(args[i]) ;
			if (webServer_matcher.matches()) {
				www_port = Integer.parseInt(webServer_matcher.group(1)) ;
				continue ;
			}
			if(args[i].equals("-W") && i + 1 < args.length) {
				www_htdocs = new File(args[i+1]) ;
				i++ ;
				continue ;
			}
		}
		NanoHTTPD http_server = null ;
		if (www_port > 0) {
			try {
				http_server = new NanoHTTPD(ip, www_port, www_htdocs) ;
				System.out.println("HTTP Server started at http:/" + ip.toString() + ":" + www_port) ;
				System.out.println("HTTP Server points to: " + www_htdocs.getAbsolutePath()) ;
			}
			catch (BindException e) {
				System.out.println("Unable to start webserver. Port " + www_port + " in use.") ;
			}
		}

		BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
		while ( true ) {
			String in = sysin.readLine();
			if( in.equals( "exit" ) ) {
				proxy.exit() ;
				break;
			} else if( in.equals( "restart" ) ) {
				proxy.restart() ;
				break;
			}
			else {
				proxy.send( in );
			}
		}
		if (http_server != null)	
			http_server.stop() ;
	}
}

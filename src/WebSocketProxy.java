import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;


public class WebSocketProxy extends WebSocketServer {

	public WebSocketProxy( int port ) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
	}
	
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected" );
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " disconnected" );
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		Collection<WebSocket> con = connections();
		synchronized ( con ) {
			for( WebSocket c : con ) {
				if (c != conn)
					c.send( message );
			}
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {}

	/**
	 * Sends <var>text</var> to all currently connected WebSocket clients.
	 * 
	 * @param text
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	public void sendToAll( String text ) {
		Collection<WebSocket> con = connections();
		synchronized ( con ) {
			for( WebSocket c : con ) {
				c.send( text );
			}
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
	

	public static void main( String[] args ) throws InterruptedException , IOException {
		InetAddress ip = getLocalIpAddress() ;
		
		WebSocketImpl.DEBUG = false;
		int port = 3037; // 843 flash policy port
		try {
			port = Integer.parseInt( args[ 0 ] );
		} catch ( Exception ex ) {}
		WebSocketProxy s = new WebSocketProxy( port );
		s.start();
		System.out.println("Websocket started at: ws:/" + ip.toString() + ":" + s.getPort() );
		

		int http_port = 8888 ;
		try {
			http_port = Integer.parseInt( args[ 1 ] );
		} catch ( Exception ex ) {}
		File wwwroot = new File(System.getProperty("user.dir")) ;
		NanoHTTPD http_server = new NanoHTTPD(ip, http_port, wwwroot) ;
		System.out.println("HTTP Server started at http:/" + ip.toString() + ":" + http_port) ;
		System.out.println("HTTP Server points to: " + wwwroot.toString()) ;

		BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
		while ( true ) {
			String in = sysin.readLine();
			if( in.equals( "exit" ) ) {
				s.stop();
				break;
			} else if( in.equals( "restart" ) ) {
				s.stop();
				s.start();
				break;
			}
			else {
				s.sendToAll( in );
			}
		}
		http_server.stop() ;
	}
}

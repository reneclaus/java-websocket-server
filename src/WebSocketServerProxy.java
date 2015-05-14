import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;


public class WebSocketServerProxy extends org.java_websocket.server.WebSocketServer {
	
	private Proxy proxy ;
	private int port ;

	public WebSocketServerProxy( Proxy p, int port ) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
		proxy = p ;
		this.port = port ;
	}
	
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected to websocket port " + port);
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " disconnected from websocket port " + port);
	}
	
	@Override
	public void onMessage(WebSocket conn, String message) {
		proxy.notifyMessage(conn, message) ;
	}
	
	public void send(Object src, String text ) {
		Collection<WebSocket> con = connections();
		synchronized ( this ) {
			for( WebSocket c : con ) {
				if (c != src)
					c.send( text );
			}
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {}
}

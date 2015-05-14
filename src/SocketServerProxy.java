import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SocketServerProxy {
	
	private Proxy proxy ;
	
	List<SocketProxy> sockets = Collections.synchronizedList(new ArrayList<SocketProxy>()) ;
	ServerSocket ss = null ;
	int port ;
	List<Proxy> webSocketServers = new ArrayList<Proxy>();

	public SocketServerProxy (Proxy p, int port) {
		this.port = port ;
		this.proxy = p ;
	}
	
	public void close () {
		
	}
	
	public void start() throws IOException {
		ss = new ServerSocket(port) ;
		new Thread("ServerSocket[port=" + port + "]") {
			public void run () {
				try {
					while (true) {
						Socket s = ss.accept() ;
						sockets.add(new SocketProxy(s, proxy, SocketServerProxy.this)) ;
					}
				}
				catch (IOException e) {}
			}
		}.start() ;
	}
	
	public void send (Object src, String msg) {
		for (SocketProxy s : sockets) {
			if (src != s)
				s.send(msg) ;
		}
	}
	public void stop() {
		try {
			ss.close() ;
		} catch (IOException e) {}
		while (sockets.size() > 0) {
			sockets.get(0).close() ;
		}
	}

	public void onSocketClose(SocketProxy socketToWebSocket) {
		sockets.remove(socketToWebSocket) ;
	}
}

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;


public class SocketProxy {
	Socket socket ;
	Proxy proxy;
	SocketServerProxy socketServer ;
	
	public SocketProxy (Socket s, Proxy p, SocketServerProxy ss) {
		socket = s ;
		proxy = p ;
		socketServer = ss ;
		new Thread("Socket[port=" + s.getPort() + "]") {
			public void run(){
				int n; byte[] buffer = new byte[1024];
				try {
					InputStream is = socket.getInputStream();
					while((n = is.read(buffer)) > -1) {
						byte[] tmp = Arrays.copyOfRange(buffer, 0, n) ;
						String str = new String(tmp) ;
						proxy.notifyMessage(SocketProxy.this, str) ;
					}
				}
				catch (IOException e) {}
				close() ;
			}
		}.start() ;
		System.out.println(socket.getInetAddress().getHostAddress() + " connect on socket port " + socket.getLocalPort()) ;
	}
	
	public void send (String msg) {
		try {
			socket.getOutputStream().write(msg.getBytes()) ;
		} catch (IOException e) {
			close() ;
		}
	}
	
	public void close () {
		try {
			socket.close();
		} catch (IOException e) {}
		System.out.println(socket.getInetAddress().getHostAddress() + " disconnect from socket port " + socket.getLocalPort()) ;
		socketServer.onSocketClose(this) ;
	}
}

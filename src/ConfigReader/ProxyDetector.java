package ConfigReader;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

public class ProxyDetector {
	
	final static String PROXY_ENABLED = "proxy.enabled";
	final static String PROXY_AUTO_DETECT = "proxy.auto.detect";
	final static String PROXY_HOST = "proxy.host";
	final static String PROXY_PORT = "proxy.port";
	final static String PROXY_MAVEN_PROTOCAL = "proxy.maven.protocol";

	public static boolean isAbleToConnect(URL source, Proxy proxy) {
		 try {
			 HttpURLConnection con = null;
		        HttpURLConnection.setFollowRedirects(false);
		        if(proxy != null)
		        	con = (HttpURLConnection) source.openConnection(proxy);
		        else
		        	con = (HttpURLConnection) source.openConnection();
		        con.setConnectTimeout(1000);
		        con.setReadTimeout(1000);
		        con.setRequestMethod("HEAD");
		       if (con.getResponseCode() == HttpURLConnection.HTTP_OK)
		    	   return false;
		     	 else
		     		return true;
		    } catch (Exception e) {
		        return true;
		    }
	}
	
	/**
	 * checks if proxy is required
	 * first attempt without proxy, second with proxy, checking the validity of the proxy connection
	 * @param source
	 * @return
	 */
	public static void setProxyAutoDetection(URL source) {

		Proxy proxy = null;

		String host = Config.getValue(PROXY_HOST);
		int port = Config.getIntValue(PROXY_PORT);
		String username = Config.getValue("proxy.username");
		String password = Config.getValue("proxy.password");
		boolean isProxyAutoDetect = Config.getBooleanValue(PROXY_AUTO_DETECT);
		
		// return if auto detect proxy is disabled or already set
		if(!isProxyAutoDetect) return;
		
		// return false if connection can be established without proxy
		boolean isValidConnection = isAbleToConnect(source, null);
		 // if connection was established using proxy, then return true
		if(isValidConnection)
		{
			Config.putValue(PROXY_ENABLED, false);
			return;
		}


		// set username/password for proxy authenticator
		if (!username.isEmpty() && !password.isEmpty()) {
			Authenticator.setDefault(new Authenticator() {
				@Override
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password.toCharArray());
				}
			});
		}

		// set and download through proxy if enabled
		if (!host.isEmpty() && port != -1)
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
		
		 isValidConnection = isAbleToConnect(source, proxy);
		 
		 // if connection was established using proxy, then return true
		 if(isValidConnection) {
			 System.out.println("proxy detected, switching proxy on");
			 Config.putValue(PROXY_ENABLED, true);
		 }
		 else 
			 Config.putValue(PROXY_ENABLED, false);
	}
}
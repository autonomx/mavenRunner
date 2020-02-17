package ConfigReader;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

public class ProxyDetector {

	public static boolean isProxy(String source) {
		try {
			System.setProperty("java.net.useSystemProxies", "true");
			List<Proxy> l = ProxySelector.getDefault().select(new URI(source));

			for (Iterator<Proxy> iter = l.iterator(); iter.hasNext();) {

				Proxy proxy = iter.next();

				System.out.println("proxy hostname : " + proxy.type());

				InetSocketAddress addr = (InetSocketAddress) proxy.address();

				if (addr == null) {

					return false;

				} else {
					System.out.println("proxy hostname : " + addr.getHostName());
					System.out.println("proxy port : " + addr.getPort());
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}

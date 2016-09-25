/**
 * 
 */
package mybt;

import java.net.InetAddress;

/**
 * @author wxy
 *
 */
public class InfoHash {
	private InetAddress address;
	private String infohash;
	private String type;
	
	public InfoHash(){}
	
	
	public InfoHash(InetAddress address, String infohash, String type) {
		super();
		this.address = address;
		this.infohash = infohash;
		this.type = type;
	}

	public InetAddress getAddress() {
		return address;
	}
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	public String getInfohash() {
		return infohash;
	}
	public void setInfohash(String infohash) {
		this.infohash = infohash;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	
}

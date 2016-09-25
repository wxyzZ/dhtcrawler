/**
 * 
 */
package mybt;

/**
 * @author wxy
 *
 */
public class App {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DHTServer server = new DHTServer("0.0.0.0", 6881, 100);
		server.start();
		server.auto_send_find_node();
	}

}

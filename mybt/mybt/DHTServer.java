/**
 * 
 */
package mybt;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wxy
 *
 */
public class DHTServer extends DHTClient {

	private String ip;
	private int port;
	private DatagramSocket dataSocket;
	private DatagramPacket dataPacket;
	private byte receiveByte[];

	/**
	 * @param max_node_qsize
	 */
	public DHTServer(String ip, int port, int max_node_qsize) {
		super(max_node_qsize);
		this.ip = ip;
		this.port = port;
		try {
			dataSocket = new DatagramSocket(new InetSocketAddress(this.ip, this.port));
			System.out.println("server is ok!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.setSocket(dataSocket);
	}

	@Override
	public void run() {
		super.re_join_DHT();
		receiveByte = new byte[65536];
		dataPacket = new DatagramPacket(receiveByte, receiveByte.length);
		int i = 0;
		while (i == 0)// 无数据，则循环
		{
			try {
				dataSocket.receive(dataPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			i = dataPacket.getLength();
			if (i > 0) {
				byte[] validData = Utils.getByteArray(receiveByte, 0, dataPacket.getLength() - 1);

				Map<String, Object> map = null;
				try {
					map = Utils.deBencode(validData);
				} catch (IOException e) {
					e.printStackTrace();
				}
				Node sourceNode = new Node(dataPacket.getAddress().toString(), dataPacket.getPort(), null);
				try {
					onMessage(map, sourceNode);
				} catch (IOException e) {
					e.printStackTrace();
				}
				i = 0;
			}
		}
	}

	public void onMessage(Map<String, Object> map, Node sourceNode) throws IOException {
		if (map.get("y") == null)
			return;
		String y = new String((byte[]) map.get("y"));
		// handle find_nodes response
		if (("r").equals(y)) {
			Map<String, Object> subMap = (Map<String, Object>) map.get("r");
			if (subMap.containsKey("nodes")) {
				super.process_find_node_response(subMap);
			}

		} else if (("q").equals(y)) {
			String q = new String((byte[]) map.get("q"));
			switch (q) {
			case "get_peers":
				on_get_peers_request(map, sourceNode);
				break;
			case "announce_peer":
				on_announce_peer_request(map, sourceNode);
				System.out.println("Announce");
				break;
			default:
				play_dead(map, sourceNode);
				break;
			}
		}
	}

	private void sendMessage(Map<String, Object> map, Node targetNode) {
		try {
			byte[] sendData = Utils.enBencode(map);

			InetAddress destinationIp = InetAddress.getByAddress(targetNode.getIp().getBytes());
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, destinationIp,
					targetNode.getPort());
			dataSocket.send(sendPacket);
		} catch (Exception e) {

		}

	}

	private void on_get_peers_request(Map<String, Object> map, Node sourceNode) {
		if (map.get("q") != null && "get_peers".equals(new String((byte[]) map.get("q")))) {
			Map<String, Object> subMap = (Map<String, Object>) map.get("a");
			byte[] infoHash = (byte[]) subMap.get("info_hash");
			System.out.println("infohash:\t" + Utils.byteArrayToHex(infoHash));

			// response
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("t", map.get("t"));
			responseMap.put("y", "r");
			Map<String, String> subMap1 = new HashMap<>();
			subMap1.put("id", new String(Utils.getNeighbor(super.nodeid, infoHash)));
			subMap1.put("token", new String(Utils.getByteArray(infoHash, 0, 1)));
			subMap1.put("nodes", "");
			responseMap.put("r", subMap1);

			sendMessage(responseMap, sourceNode);
		}
	}

	private void on_announce_peer_request(Map<String, Object> map, Node sourceNode) {
		if (map.get("q") != null && map.get("q").equals("announce_peer")) {
			Map<String, String> subMap = (Map<String, String>) map.get("a");
			String infoHash = subMap.get("info_hash");
			String token = subMap.get("token");
			String nid = subMap.get("id");
			System.out.println(infoHash);

			if (new String(Utils.getByteArray(infoHash.getBytes(), 0, 1)).equals(token)) {

				if (subMap.containsKey("implied_port") && Integer.valueOf(subMap.get("implied_port")) != 0) {
					System.out.println("implied_port" + sourceNode.getPort());
				} else {
					System.out.println("no  implied_port" + subMap.get("port"));
				}
			}
			// response
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("t", map.get("t"));
			responseMap.put("y", "r");
			Map<String, String> subMap1 = new HashMap<>();
			subMap1.put("id", new String(Utils.getNeighbor(super.nodeid, nid.getBytes())));
			responseMap.put("r", subMap1);
			sendMessage(responseMap, sourceNode);
		}
	}

	private void play_dead(Map<String, Object> map, Node sourceNode) {
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("t", map.get("t"));
		responseMap.put("y", "r");
		List<Object> ls = new ArrayList<Object>();
		ls.add(202);
		ls.add("Server Error");
		responseMap.put("e", ls);
		sendMessage(responseMap, sourceNode);
	}
}

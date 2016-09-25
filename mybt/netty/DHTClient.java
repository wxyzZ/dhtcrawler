package netty;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.DatagramPacket;
import mybt.Node;
import mybt.Queue;
import mybt.Utils;

public class DHTClient extends Thread {

	private int max_node_qsize;
	protected byte[] nodeid;
	private ChannelFuture channel;
	private Queue<Node> queue = new Queue<>();
	private final List<InetSocketAddress> BOOTSTRAP_NODES = new ArrayList<>(Arrays.asList(
			new InetSocketAddress("router.bittorrent.com", 6881), new InetSocketAddress("dht.transmissionbt.com", 6881),
			new InetSocketAddress("router.utorrent.com", 6881)));

	public DHTClient(int max_node_qsize) {
		this.nodeid = Utils.randomId();
		this.max_node_qsize = max_node_qsize;
	}

	public void setChannel(ChannelFuture channel) {
		this.channel = channel;
	}

	public void findNode(InetSocketAddress address, byte[] nid) {
		nid = nid == null ? this.nodeid : Utils.getNeighbor(this.nodeid, Utils.randomId());
		Map<String, Object> map = new HashMap<>();
		map.put("t", Utils.getRandomString(2));
		map.put("y", "q");
		map.put("q", "find_node");

		Map<String, Object> subMap = new HashMap<>();
		subMap.put("id", nid);
		subMap.put("target", Utils.randomId());
		map.put("a", subMap);

		try {
			byte[] sendData = Utils.enBencode(map);
//			System.out.println(new String(sendData));
			DatagramPacket sendPacket = new DatagramPacket(Unpooled.copiedBuffer(sendData), new InetSocketAddress(address.getAddress(),address.getPort()));
			channel.channel().writeAndFlush(sendPacket);
		} catch (Exception e) {

		}
	}

	private void joinDHT() {
		for (InetSocketAddress address : BOOTSTRAP_NODES) {
			findNode(address, null);
		}
	}

	public void re_join_DHT() {
		Timer autoRejoinDHTTimer = new Timer();
		autoRejoinDHTTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (queue.size() <= 0) {
					joinDHT();
				}
				// System.out.println("node count:" + queue.size());
			}
		}, 1000, 5000);
	}

	public void auto_send_find_node() {
		double wait = 1.0 / this.max_node_qsize;
		while (true) {
			try {
				if (queue.size() <= 0)
					continue;
				Node node = queue.remove();
				findNode(new InetSocketAddress(node.getIp(), node.getPort()), node.getNid());
				sleep((long) wait * 1000);
			} catch (Exception e) {
				continue;
			}
		}
	}

	public void process_find_node_response(Map<String, Object> map) throws UnknownHostException {
		List<Node> ls = Utils.decodeNodes((byte[]) map.get("nodes"));
		for (Node n : ls) {
			if (n.getNid().length != 20)
				continue;
			// if(n.getIp()==this.id) continue;
			if (n.getPort() < 1 || n.getPort() > 65535)
				continue;
			if (queue.size() < max_node_qsize)
				queue.insert(n);
		}
	}

	@Override
	public void run() {
		re_join_DHT();
	}
}

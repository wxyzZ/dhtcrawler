package netty;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.proxy.Socks5ProxyHandler;
import mybt.Node;
import mybt.Utils;

public class DHTServer extends DHTClient {
	/**
	 * 记录日志
	 */
	private Logger logger = Logger.getLogger(DHTServer.class);

	/**
	 * 最大节点数
	 */
	public int maxGoodNodeCount;

	/**
	 * 自动重新加入DHT网络 timer
	 * 
	 */

	/**
	 * Netty Channel
	 */
	private ChannelFuture channel;


	public DHTServer(String hostname, int port, int maxGoodNodeCount) {
		super(maxGoodNodeCount);
		EventLoopGroup group = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		final DHTServerHandler packetHandler = new DHTServerHandler();
		b.channel(NioDatagramChannel.class).option(ChannelOption.SO_RCVBUF, 65536).option(ChannelOption.SO_SNDBUF,
				268435456);
		b.group(group);
		b.handler(new ChannelInitializer<DatagramChannel>() {
			@Override
			protected void initChannel(DatagramChannel ch) throws Exception {
//				ch.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress("127.0.0.1", 8889)));
				ch.pipeline().addLast(packetHandler);
			}

		});

		try {
			channel = b.bind(6881).sync();
			System.out.println("server is ok!");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		super.setChannel(channel);
	}

	public class DHTServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
			ByteBuf buf = (ByteBuf) msg.copy().content();
			byte[] req = new byte[buf.readableBytes()];
			buf.readBytes(req);
			Map<String, Object> map = Utils.deBencode(req);
			Node sourceNode = new Node(msg.sender().getAddress().getHostAddress(), msg.sender().getPort(), null);
			onMessage(map, sourceNode);
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
			DatagramPacket sendPacket = new DatagramPacket(Unpooled.copiedBuffer(sendData), new InetSocketAddress(destinationIp,targetNode.getPort()));
			channel.channel().writeAndFlush(sendPacket);
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

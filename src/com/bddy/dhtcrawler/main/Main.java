package com.bddy.dhtcrawler.main;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.bddy.dhtcrawler.db.ConnectionPool;
import com.bddy.dhtcrawler.db.RedisPool;
import com.bddy.dhtcrawler.listener.OnAnnouncePeerListener;
import com.bddy.dhtcrawler.listener.OnGetPeersListener;
import com.bddy.dhtcrawler.server.DHTServer;
import com.bddy.dhtcrawler.structure.DownloadPeer;
import com.bddy.dhtcrawler.structure.Queue;
import com.bddy.dhtcrawler.task.WireMetadataDownloadTask;
import com.bddy.dhtcrawler.util.ByteUtil;

import redis.clients.jedis.Jedis;

public class Main {
	
	public static long count = 0;
	
	public static void main(String[] args) throws Exception {
		
//		Jedis jedis = RedisPool.getJedis();
//		if (jedis == null) {
//			System.out.println("get jedis failed.");
//			return;
//		}
//		jedis.flushDB();
//		jedis.flushAll();
//		ConnectionPool connPool = new ConnectionPool("com.mysql.jdbc.Driver"
//				 ,"jdbc:mysql://xxx.xxx.xxx.xxx:3306/dht?useUnicode=true&characterEncoding=UTF-8" ,"root" ,"123456");
//		connPool .createPool();
//		
		final BlockingQueue<DownloadPeer> dps = new LinkedBlockingQueue<>();

		for (int i = 0; i < 50; i++) {
			Thread t = new WireMetadataDownloadTask( dps);
			t.start();
		}

		
		DHTServer server = new DHTServer("0.0.0.0", 6882, 88800);
		server.setOnGetPeersListener(new OnGetPeersListener() {
			
			@Override
			public void onGetPeers(InetSocketAddress address, byte[] info_hash) {
				//System.out.println("get_peers request, address:" + address.getHostString() + ", info_hash:" + ByteUtil.byteArrayToHex(info_hash));
			}
		});
		server.setOnAnnouncePeerListener(new OnAnnouncePeerListener() {
			
			@Override
			public void onAnnouncePeer(InetSocketAddress address, byte[] info_hash, int port) {
				//System.out.println("announce_peer request, address:" + address.getHostString() + ":" + port + ", info_hash:" + ByteUtil.byteArrayToHex(info_hash) + "dps size:" + dps.size());
//				if (jedis.getSet(ByteUtil.byteArrayToHex(info_hash), "1") == null) {
					if (dps.size() > 10000)
						return;
					try {
						dps.put(new DownloadPeer(address.getHostString(), port, info_hash));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
//				}
			}
		});
		//server.setDaemon(true);
		server.start();
	}
	
	private static boolean existInfoHash(ConnectionPool connPool, String infoHash) {
		int count = 1;
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try {
			conn = connPool.getConnection();
			statement = conn.prepareStatement("select count(*) as count from tb_file where info_hash=?");
			statement.setString(1, infoHash);
			rs = statement.executeQuery();
			count = rs.getInt("count");
			rs.close();
			statement.close();
		} catch (SQLException e) {
			//e.printStackTrace();
		} finally {
			if (conn != null)
				connPool.returnConnection(conn);
		}
		if (count > 0)
			return true;
		return false;
	}
}

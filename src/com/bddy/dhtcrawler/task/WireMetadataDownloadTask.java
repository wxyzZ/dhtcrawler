package com.bddy.dhtcrawler.task;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;

import com.bddy.dhtcrawler.db.ConnectionPool;
import com.bddy.dhtcrawler.handler.AnnouncePeerInfoHashWireHandler;
import com.bddy.dhtcrawler.listener.OnMetadataListener;
import com.bddy.dhtcrawler.structure.DownloadPeer;
import com.bddy.dhtcrawler.structure.Torrent;

public class WireMetadataDownloadTask extends Thread{
	
	private AnnouncePeerInfoHashWireHandler handler = new AnnouncePeerInfoHashWireHandler();
	
	private ConnectionPool connPool;
	private BlockingQueue<DownloadPeer> dps;
	
	public WireMetadataDownloadTask( BlockingQueue<DownloadPeer> dps) {
		super();
//		this.connPool = connPool;
		this.dps = dps;
		initHandler();
	}

	@Override
	public void run() {
		
		while (true) {
			//System.out.println("current work thread: " + tid + ", dps size:" + dps.size());
			try {
				DownloadPeer peer = dps.take();
				handler.handler(new InetSocketAddress(peer.getIp(), peer.getPort()), peer.getInfo_hash());
				Thread.sleep(500);
			} catch (InterruptedException e) {
				//ignore
			}
		}
		
	}
	
	private void initHandler() {
		handler.setOnMetadataListener(new OnMetadataListener() {
			@Override
			public void onMetadata(Torrent torrent) {
				//System.out.println("finished,dps size:" + dps.size());
				if (torrent == null || torrent.getInfo() == null)
					return;
				//入库操作
				/*Connection conn = null;
				PreparedStatement stament = null;
				try {
					conn = connPool.getConnection();
					stament = conn.prepareStatement("insert into tb_file(info_hash,name,files) values(?,?,?)");
					stament.setString(1, torrent.getInfo_hash());
					stament.setString(2, torrent.getInfo().getName());
					stament.setBytes(3, BZipUtil.bZip2(JSON.toJSONBytes(torrent.getInfo().getFiles())));
					int i = stament.executeUpdate();
					if (i != 1)
						//System.out.println("insert info_hash[" + torrent.getInfo_hash() +"] failed.");
					stament.close();
				} catch (SQLException e) {
					//e.printStackTrace();
					if (stament != null)
						try {
							stament.close();
							stament = null;
						} catch (SQLException e1) {
							//e1.printStackTrace();
						}
				} finally {
					if (conn != null) {
						connPool.returnConnection(conn);
						conn = null;
					}
				}*/
				
			}
		});
	}
	
}

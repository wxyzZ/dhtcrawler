package netty;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.bddy.dhtcrawler.structure.DownloadPeer;
import com.bddy.dhtcrawler.task.WireMetadataDownloadTask;

public class AppMain {

	public static void main(String[] args) {

		final BlockingQueue<DownloadPeer> dps = new LinkedBlockingQueue<>();

		for (int i = 0; i < 50; i++) {
			Thread t = new WireMetadataDownloadTask(dps);
			t.start();
		}

		DHTServer server = new DHTServer("0.0.0.0", 6881, 200);
		server.start();
		server.auto_send_find_node();
	}

}

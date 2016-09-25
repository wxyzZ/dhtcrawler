/**
 * 
 */
package mybt;

import java.net.InetAddress;
import java.util.HashSet;


/**
 * @author wxy
 *
 */
public class Master extends Thread {
	
	private int MAX_QUEUE_LT=30;
	private int MAX_QUEUE_PT=30;
	private int n_downloading_lt=0;
	private int n_downloading_pt=0;
	private Queue<InfoHash> metadata_queue = new Queue<>();
	private Queue<InfoHash> queue = new Queue<>();
	private HashSet visited = new HashSet();
	public void log_hass(String infohash,InetAddress address){
		if(n_downloading_lt<MAX_QUEUE_LT)
		{
			InfoHash hash = new InfoHash(address,infohash,"lt");
			queue.insert(hash);
		}
	}
	
	public void log_announce(String infohash,InetAddress address){
		if(n_downloading_pt<MAX_QUEUE_PT)
		{
			InfoHash hash = new InfoHash(address,infohash,"pt");
			queue.insert(hash);
		}
	}
	
	@Override
	public void run(){
		while(true){
			while (metadata_queue.size()>0)
				get_torrent();
			InfoHash infohash = queue.remove();
			if(visited.contains(infohash.getInfohash()))
				continue;
			if(visited.size()>10000)
				visited=new HashSet();
			visited.add(infohash.getInfohash());
			//TODO 此处判断数据库中是否有该infohash，如果有，则更新时间。
			
			if("pt".equals(infohash.getType())){
				
			}else if("lt".equals(infohash.getType()))
				continue;
		}
	}
	
	private void get_torrent(){
		
	}
}

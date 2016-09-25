package proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

/**
 * Created with IntelliJ IDEA. User: ASUS Date: 14-6-24 Time: 下午3:33 To change
 * this template use File | Settings | File Templates.
 */
public class Proxy {

	private final int localPort;
	private final String remoteHost;
	private final int remotePort;

	/**
	 * @param localPort
	 * @param remoteHost
	 * @param remotePort
	 */
	public Proxy(int localPort, String remoteHost, int remotePort) {
		this.localPort = localPort;
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
	}

	public void run() throws Exception {
		System.err.println("Proxying *:" + localPort + " to " + remoteHost + ':' + remotePort + " ...");

		// Configure the bootstrap.
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							// 注册handler
							ch.pipeline().addLast(new ObjectEncoder(),
									new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
									new ProxyFrontendHandler(remoteHost, remotePort));
						}
					}).childOption(ChannelOption.AUTO_READ, false);

			ChannelFuture f = b.bind(localPort).sync();

			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {
		new Proxy(12359, "127.0.0.1", 12358).run();
	}
}
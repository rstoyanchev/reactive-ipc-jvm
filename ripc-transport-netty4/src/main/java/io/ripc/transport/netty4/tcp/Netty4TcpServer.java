package io.ripc.transport.netty4.tcp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.ripc.protocol.tcp.AbstractTcpServer;


public class Netty4TcpServer extends AbstractTcpServer<ByteBuf, ByteBuf> {

	private final int port;

	private ServerBootstrap bootstrap;

	private ChannelFuture bindFuture;


	public Netty4TcpServer(int port) {
		this.port = port;
		this.bootstrap = new ServerBootstrap().group(new NioEventLoopGroup())
				.channel(NioServerSocketChannel.class);
	}


	@Override
	public void start() {
		this.bootstrap.childHandler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel channel) throws Exception {
				channel.pipeline().addLast("last", new Netty4TcpChannelHandler(getHandler()));
			}
		});
		try {
			this.bindFuture = this.bootstrap.bind(this.port).sync();
			if (!bindFuture.isSuccess()) {
				throw new IllegalStateException(this.bindFuture.cause());
			}
			SocketAddress localAddress = this.bindFuture.channel().localAddress();
			if (localAddress instanceof InetSocketAddress) {
				logger.info("Started server at port: " + ((InetSocketAddress) localAddress).getPort());
			}

		} catch (InterruptedException ex) {
			logger.error("Startup interrupted", ex);
		}
	}

	@Override
	public void shutdown() {
		try {
			this.bindFuture.channel().close().sync();
		}
		catch (Throwable ex) {
			logger.error("Error while shutting down server", ex);
		}
	}


}

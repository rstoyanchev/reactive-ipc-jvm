package io.ripc.transport.netty4.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.ripc.protocol.tcp.TcpHandler;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Netty4TcpChannelHandler extends ChannelDuplexHandler {

	private static final Logger logger = LoggerFactory.getLogger(Netty4TcpChannelHandler.class);


	private final TcpHandler<ByteBuf, ByteBuf> tcpHandler;

	private Netty4TcpConnection tcpConnection;


	public Netty4TcpChannelHandler(TcpHandler<ByteBuf, ByteBuf> tcpHandler) {
		this.tcpHandler = tcpHandler;
	}


	@Override
	public void channelActive(ChannelHandlerContext context) throws Exception {
		super.channelActive(context);

		Channel channel = context.channel();
		this.tcpConnection = new Netty4TcpConnection(channel);

		logger.debug("New connection: {}", channel);

		this.tcpHandler.handle(this.tcpConnection)
				.subscribe(new VoidSubscriber() {
					@Override
					public void onError(Throwable ex) {
						logger.error("Connection handling failed", ex);
						channel.close();
					}

					@Override
					public void onComplete() {
						logger.debug("Connection handling completed: {}", channel.remoteAddress());
						channel.close();
					}
				});
	}

	@Override
	public void channelInactive(ChannelHandlerContext context) throws Exception {
		logger.debug("Connection closed: ", context.channel());
		this.tcpConnection.reader().handleComplete();
		super.channelInactive(context);
	}

	@Override
	public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
		if (!this.tcpConnection.reader().handleData(message)) {
			super.channelRead(context, message);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		this.tcpConnection.reader().handleError(cause);
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		this.tcpConnection.handleWriteEnabled();

	}


	private static abstract class VoidSubscriber implements Subscriber<Void> {

		@Override
		public void onSubscribe(Subscription subscription) {
			// do nothing
		}

		@Override
		public void onNext(Void aVoid) {
			// do nothing
		}
	}

}

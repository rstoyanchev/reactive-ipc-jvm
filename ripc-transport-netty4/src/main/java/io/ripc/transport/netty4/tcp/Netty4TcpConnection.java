package io.ripc.transport.netty4.tcp;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.ripc.core.Flushable;
import io.ripc.protocol.tcp.TcpConnection;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;


public class Netty4TcpConnection implements TcpConnection<ByteBuf, ByteBuf> {

	private final Channel channel;

	private final Netty4ReadPublisher reader;

	private final Queue<WriteHandler> writeHandlers = new ConcurrentLinkedQueue<>();

	private final DemandHandler demandHandler = new DemandHandler();


	public Netty4TcpConnection(Channel channel) {
		this.channel = channel;
		this.reader = new Netty4ReadPublisher(channel);
	}


	@Override
	public Netty4ReadPublisher reader() {
		return this.reader;
	}

	@Override
	public Publisher<Void> writer(Publisher<ByteBuf> writePublisher) {
		WriteHandler handler = new WriteHandler(1);
		this.writeHandlers.add(handler);
		writePublisher.subscribe(handler);
		return handler;
	}

	private ChannelFuture writeToChannel(ByteBuf data) {
		ChannelFuture future = this.channel.write(data);
		if (this.channel.isWritable()) {
			this.demandHandler.request(1);
		}
		return future;
	}

	void handleWriteEnabled() {
		for (WriteHandler writeHandler : this.writeHandlers) {
			writeHandler.offerDemand(1);
		}
	}


	/**
	 * Each write Publisher is assigned a WriteHandler that implements
	 * {@code Subscriber} to track how many items have been written through its
	 * Publisher and implements {@code Publisher<Void>} to signal to subscribers
	 * the completion of all writes for its Publisher.
	 */
	private class WriteHandler implements Subscriber<ByteBuf>, Flushable, Publisher<Void> {

		private Subscription subscription;

		private final AtomicLong requested = new AtomicLong();

		private volatile ChannelFuture lastWriteFuture; // TODO: aggregate all write futures

		private final ChannelPromise promise = channel.newPromise();


		public WriteHandler(long initialDemand) {
			this.requested.addAndGet(initialDemand);
		}


		public boolean offerDemand(long n) {
			if (this.requested.get() > 0) {
				return false;
			}
			else {
				this.requested.addAndGet(n);
				this.subscription.request(n);
				return true;
			}
		}

		// WriteSubscriber<ByteBuf> methods

		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
			this.subscription.request(this.requested.getAndSet(0));
		}

		@Override
		public void onNext(ByteBuf data) {
			this.requested.decrementAndGet();
			this.lastWriteFuture = writeToChannel(data);
		}

		@Override
		public void onError(Throwable error) {
			this.promise.setFailure(error);
		}

		@Override
		public void onComplete() {
			this.lastWriteFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						promise.setSuccess();
					}
					else {
						promise.setFailure(future.cause());
					}
				}
			});
			Netty4TcpConnection.this.channel.flush();
			Netty4TcpConnection.this.writeHandlers.remove(this);
		}

		@Override
		public void flush() {
			Netty4TcpConnection.this.channel.flush();
		}


		// Publisher<Void> methods

		@Override
		public void subscribe(Subscriber<? super Void> subscriber) {
			this.promise.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) {
					if (future.isSuccess()) {
						subscriber.onComplete();
					}
					else {
						subscriber.onError(future.cause());
					}
				}
			});
		}
	}


	private class DemandHandler {

		private final Map<WriteHandler, Boolean> cache = new ConcurrentHashMap<>();

		/**
		 * Request (n) from the next write Publisher in round-robin fashion.
		 */
		public void request(long n) {
			for (WriteHandler writeHandler : Netty4TcpConnection.this.writeHandlers) {
				if (this.cache.put(writeHandler, true) == null) {
					if (writeHandler.offerDemand(n)) {
						return;
					}
				}
			}
			this.cache.clear();
			if (!Netty4TcpConnection.this.writeHandlers.isEmpty()) {
				request(n);
			}
		}
	}

}

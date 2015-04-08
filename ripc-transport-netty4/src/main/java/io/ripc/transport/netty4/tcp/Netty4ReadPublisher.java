package io.ripc.transport.netty4.tcp;


import java.util.concurrent.atomic.AtomicLong;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;


class Netty4ReadPublisher implements Publisher<ByteBuf> {

	private final Channel channel;

	private Subscriber<? super ByteBuf> subscriber;

	private final AtomicLong requested = new AtomicLong();


	public Netty4ReadPublisher(Channel channel) {
		this.channel = channel;
		channel.config().setAutoRead(false);
	}


	@Override
	public void subscribe(Subscriber<? super ByteBuf> subscriber) {
		if (this.subscriber != null) {
			throw new IllegalArgumentException("One read subscriber only allowed.");
		}
		this.subscriber = subscriber;
		this.subscriber.onSubscribe(new ReadSubscription());
	}

	boolean handleData(Object message) {
		if (message instanceof ByteBuf) {
			this.subscriber.onNext((ByteBuf) message);
			if (this.requested.decrementAndGet() > 0) {
				this.channel.read();
			}
			return true;
		}
		else {
			return false;
		}
	}

	void handleError(Throwable error) {
		if (this.subscriber != null) {
			this.subscriber.onError(error);
		}
	}

	void handleComplete() {
		if (this.subscriber != null) {
			this.subscriber.onComplete();
		}
	}


	private class ReadSubscription implements Subscription {

		@Override
		public void request(long n) {
			if (n < 0) {
				String text = "Spec 3.9: Request signals must be a positive number.";
				subscriber.onError(new IllegalArgumentException(text));
				return;
			}
			if (n == Long.MAX_VALUE) {
				requested.set(Long.MAX_VALUE);
			}
			else {
				requested.addAndGet(n);
			}
			channel.read();
		}

		@Override
		public void cancel() {
			requested.set(-1);
			channel.close();
		}
	}

}

package io.ripc.transport.netty4.tcp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.ripc.core.Flushable;
import io.ripc.protocol.tcp.SimpleTcpInterceptor;
import io.ripc.protocol.tcp.TcpConnection;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TcpServerSample {

	private static final Logger logger = LoggerFactory.getLogger(TcpServerSample.class);


	public static void main(String[] args) {

		Netty4TcpServer server = new Netty4TcpServer(0);

		server.interceptor(new LoggingInterceptor()).next(new ShortcircuitInterceptor())
				.last(connection -> {
					CompletionPublisher completionPublisher = new CompletionPublisher();
					EchoProcessor echoProcessor = new EchoProcessor(completionPublisher);
					connection.reader().subscribe(echoProcessor);
					connection.writer(echoProcessor);
					return completionPublisher;
				});

		server.start();

		try {
			logger.info("Press any key to exit");
			System.in.read();
		}
		catch (IOException e) {
			// ignore
		}

		server.shutdown();
		System.exit(0);
	}

	private static class LoggingInterceptor extends SimpleTcpInterceptor<ByteBuf, ByteBuf> {

		@Override
		public Publisher<Void> handle(TcpConnection<ByteBuf, ByteBuf> connection) {
			logger.debug("Intercepted connection opening");
			return getNext().handle(connection);
		}
	}

	private static class ShortcircuitInterceptor extends SimpleTcpInterceptor<ByteBuf, ByteBuf> {

		private AtomicLong counter = new AtomicLong();

		@Override
		public Publisher<Void> handle(TcpConnection<ByteBuf, ByteBuf> connection) {
			if (counter.incrementAndGet() % 2 == 0) {
				logger.debug("Short-circuiting further processing.");
				return new CompletionPublisher(true);
			}
			return getNext().handle(connection);
		}
	}

	private static class EchoProcessor implements Processor<ByteBuf, ByteBuf> {

		private final CompletionPublisher completionPublisher;

		private Subscription readSubscription;

		private Subscriber<? super ByteBuf> writeSubscriber;

		private Flushable flushable;


		private EchoProcessor(CompletionPublisher publisher) {
			this.completionPublisher = publisher;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.readSubscription = subscription;
		}

		@Override
		public void onNext(ByteBuf byteBuf) {
			Charset charset = Charset.forName("UTF-8");
			String message = byteBuf.toString(charset);
			if ("TheEnd\n".equals(message)) {
				logger.debug("That's all Folks!");
				this.completionPublisher.setComplete();
			}
			else {
				logger.debug("Echoing message: " + message);
				this.writeSubscriber.onNext(Unpooled.buffer().writeBytes(("Hello " + message).getBytes(charset)));
				this.flushable.flush();
			}
		}

		@Override
		public void onError(Throwable t) {
		}

		@Override
		public void onComplete() {
		}

		@Override
		public void subscribe(Subscriber<? super ByteBuf> subscriber) {
			this.writeSubscriber = subscriber;
			this.flushable = (subscriber instanceof Flushable ? (Flushable) subscriber : new NoOpFlushable());
			this.writeSubscriber.onSubscribe(new Subscription() {
				@Override
				public void request(long n) {
					// When requested to write, request to read
					readSubscription.request(n);
				}
				@Override
				public void cancel() {
					readSubscription.cancel();
				}
			});

		}
	}

	private static class NoOpFlushable implements Flushable {

		@Override
		public void flush() {
			// no-op
		}
	}

	/**
	 * Naive implementation
	 */
	private static class CompletionPublisher implements Publisher<Void> {

		private final List<Subscriber> subscribers = new ArrayList<>();

		private AtomicReference<Object> result = new AtomicReference<>();


		public CompletionPublisher() {
		}

		public CompletionPublisher(Object result) {
			this.result.set(result);
		}

		@Override
		public void subscribe(Subscriber<? super Void> subscriber) {
			if (this.result.get() == null) {
				this.subscribers.add(subscriber);
			}
			else if (this.result.get() instanceof Throwable) {
				subscriber.onError((Throwable) this.result.get());
			}
			else {
				subscriber.onComplete();
			}
		}

		public void setComplete() {
			if (this.result.compareAndSet(null, true)) {
				this.subscribers.forEach(Subscriber::onComplete);
			}
		}

		public void setError(Throwable ex) {
			if (this.result.compareAndSet(null, ex)) {
				for (Subscriber<?> subscriber : this.subscribers) {
					subscriber.onError(ex);
				}
			}
		}

	}

}

package io.ripc.transport.netty4.tcp;


import static rx.RxReactiveStreams.*;

import java.util.concurrent.atomic.AtomicLong;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.ripc.internal.Publishers;
import io.ripc.protocol.tcp.Connection;
import io.ripc.protocol.tcp.TcpInterceptor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TcpServerSample {

    private static final Logger logger = LoggerFactory.getLogger(TcpServerSample.class);

    public static TcpInterceptor<ByteBuf, ByteBuf, ByteBuf, ByteBuf> log() {
        return handler -> input -> {
            logger.error("Received a new connection.");
            return handler.handle(input);
        };
    }

    public static TcpInterceptor<ByteBuf, ByteBuf, ByteBuf, ByteBuf> shortCircuitAltConnection() {
        return handler -> {
            final AtomicLong connCounter = new AtomicLong();
            return input -> {
                if (connCounter.incrementAndGet() % 2 == 0) {
                    logger.error("Short-circuiting further processing.");
                    return input.write(Publishers.just(Unpooled.buffer().writeBytes("Go Away!!! \n".getBytes())));
                }
                return handler.handle(input);
            };
        };
    }

    public static TcpInterceptor<ByteBuf, ByteBuf, String, String> stringConnectionAdapter() {
        return handler ->
                stringConnection ->
                        handler.handle(new ByteBufToStringConnection(stringConnection));
    }

    public static void main(String[] args) {
        Netty4TcpServer.<ByteBuf, ByteBuf>create(0)
                .intercept(log())
                .intercept(stringConnectionAdapter())
                .start(connection ->
                                toPublisher(toObservable(connection)
                                        .flatMap(data -> toObservable(connection.write(Publishers.just("Hello " + data)))))
                );
    }


    private static class ByteBufToStringConnection implements Connection<ByteBuf, ByteBuf> {

        private final Connection<String, String> delegate;


        public ByteBufToStringConnection(Connection<String, String> stringConnection) {
            this.delegate = stringConnection;
        }

        @Override
        public Publisher<Void> write(Publisher<ByteBuf> data) {
            return delegate.write(new StringToByteBufPublisher(data));
        }

        @Override
        public Publisher<Void> write(Publisher<ByteBuf> data, FlushSelector<ByteBuf> flushSelector) {
            return null;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuf> subscriber) {
            delegate.subscribe(new StringToByteBufSubscriber(subscriber));
        }
    }

    private static class StringToByteBufPublisher implements Publisher<String> {

        private final Publisher<ByteBuf> delegate;


        public StringToByteBufPublisher(Publisher<ByteBuf> byteBufPublisher) {
            this.delegate = byteBufPublisher;
        }

        @Override
        public void subscribe(Subscriber<? super String> subscriber) {
            this.delegate.subscribe(new ByteBufToStringSubscriber(subscriber));
        }
    }

    private static class ByteBufToStringSubscriber implements Subscriber<ByteBuf> {

        private final Subscriber<? super String> delegate;


        public ByteBufToStringSubscriber(Subscriber<? super String> subscriber) {
            this.delegate = subscriber;
        }

        @Override
		public void onSubscribe(Subscription s) {
			this.delegate.onSubscribe(s);
		}

        @Override
		public void onNext(ByteBuf byteBuf) {
            this.delegate.onNext(byteBuf.toString());
		}

        @Override
		public void onError(Throwable t) {
            this.delegate.onError(t);
		}

        @Override
		public void onComplete() {
            this.delegate.onComplete();
		}
    }

    private static class StringToByteBufSubscriber implements Subscriber<String> {

        private final Subscriber<? super ByteBuf> delegate;


        public StringToByteBufSubscriber(Subscriber<? super ByteBuf> subscriber) {
            this.delegate = subscriber;
        }

        @Override
		public void onSubscribe(Subscription subscription) {
            this.delegate.onSubscribe(subscription);
		}

        @Override
		public void onNext(String s) {
            this.delegate.onNext(Unpooled.wrappedBuffer(s.getBytes()));
		}

        @Override
		public void onError(Throwable t) {
            this.delegate.onError(t);
		}

        @Override
		public void onComplete() {
            this.delegate.onComplete();
		}
    }

}
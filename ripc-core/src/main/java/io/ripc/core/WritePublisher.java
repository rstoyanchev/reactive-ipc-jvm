package io.ripc.core;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A {@code Publisher} that adapts another for write I/O, for example applying
 * requests to flush based on some strategy.
 */
public class WritePublisher<T> implements Publisher<T> {

	private final Publisher<T> adaptee;

	private int flushCount;


	private WritePublisher(Publisher<T> adaptee) {
		this.adaptee = adaptee;
	}

	/**
	 * Create a WritePublisher by wrapping the actual Publisher.
	 */
	public static <S> WritePublisher<S> adapt(Publisher<S> publisher) {
		return new WritePublisher<>(publisher);
	}

	/**
	 * Configure how many items should be written before requesting flush.
	 */
	public WritePublisher<T> withFlushCount(int count) {
		this.flushCount = count;
		return this;
	}

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {

		if (this.flushCount > 0 && subscriber instanceof Flushable) {
			Flushable flushable = (Flushable) subscriber;

			this.adaptee.subscribe(new Subscriber<T>() {

				private volatile int count;

				@Override
				public void onSubscribe(Subscription s) {
					subscriber.onSubscribe(s);
				}

				@Override
				public void onNext(T t) {
					subscriber.onNext(t);
					if (++this.count == WritePublisher.this.flushCount) {
						flushable.flush();
						WritePublisher.this.flushCount = 0;
					}
				}

				@Override
				public void onError(Throwable t) {
					subscriber.onError(t);
				}

				@Override
				public void onComplete() {
					subscriber.onComplete();
				}
			});
		}
		else {
			this.adaptee.subscribe(subscriber);
		}
	}

}

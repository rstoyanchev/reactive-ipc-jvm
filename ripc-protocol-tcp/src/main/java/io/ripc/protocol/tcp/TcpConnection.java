package io.ripc.protocol.tcp;

import org.reactivestreams.Publisher;


public interface TcpConnection<I, O> {

	Publisher<I> reader();

	/**
	 *
	 * @see io.ripc.core.Flushable
	 * @see io.ripc.core.WritePublisher
	 */
	Publisher<Void> writer(Publisher<O> publisher);

}

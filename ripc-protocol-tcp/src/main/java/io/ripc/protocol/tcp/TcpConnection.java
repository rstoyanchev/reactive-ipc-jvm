package io.ripc.protocol.tcp;

import io.ripc.core.ConnectionEventListener;
import org.reactivestreams.Publisher;

/**
 * A {@code Connection} provides a getReader for inbound data and a setWriter for outbound.
 */
public interface TcpConnection {

	Publisher<?> getReader();

	void setWriter(Publisher<?> writer);

	TcpConnection addListener(ConnectionEventListener listener);

}

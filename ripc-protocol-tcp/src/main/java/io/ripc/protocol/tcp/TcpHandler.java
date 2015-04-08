package io.ripc.protocol.tcp;


import org.reactivestreams.Publisher;

public interface TcpHandler<I, O> {

	Publisher<Void> handle(TcpConnection<I, O> connection);

}
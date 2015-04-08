package io.ripc.protocol.tcp;


/**
 * A {@link TcpInterceptor} that does not change the connection input and output.
 */
public abstract class SimpleTcpInterceptor<I, O> extends AbstractTcpInterceptor<I, O, I, O> {

}

package io.ripc.protocol.tcp;


/**
 * A {@code TcpHandler} that can be chained to another handler to form an
 * interception chain and may transform the connection input and output.
 *
 * @param <I> Input (this handler)
 * @param <O> Output (this handler)
 * @param <II> Input (next handler)
 * @param <OO> Output (next handler)
 */
public interface TcpInterceptor<I, O, II, OO> extends TcpHandler<I, O> {

	/**
	 * Configure the next interceptor.
	 */
	<III, OOO> TcpInterceptor<II, OO, III, OOO> next(TcpInterceptor<II, OO, III, OOO> handler);

	/**
	 * Configure the last handler.
	 */
	void last(TcpHandler<II, OO> handler);

}

package io.ripc.protocol.tcp;

/**
 * Base class for TcpInterceptor implementations.
 *
 * <p>Implementations that do not transform the connection input or output should
 * consider extending {@link io.ripc.protocol.tcp.SimpleTcpInterceptor}.
 */
public abstract class AbstractTcpInterceptor<I, O, II, OO> implements TcpInterceptor<I, O, II, OO> {

	private TcpHandler<II, OO> nextHandler;


	public TcpHandler<II, OO> getNext() {
		return this.nextHandler;
	}

	@Override
	public <III, OOO> TcpInterceptor<II, OO, III, OOO> next(TcpInterceptor<II, OO, III, OOO> handler) {
		this.nextHandler = handler;
		return handler;
	}

	@Override
	public void last(TcpHandler<II, OO> handler) {
		this.nextHandler = handler;
	}

}

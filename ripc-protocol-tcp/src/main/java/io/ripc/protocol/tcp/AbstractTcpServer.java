package io.ripc.protocol.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTcpServer<I, O> implements TcpServer {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private TcpHandler<I, O> handler;


	/**
	 * Set up an interception chain of handlers vs a single {@link #handler}.
	 */
	public <II, OO> TcpInterceptor<I, O, II, OO> interceptor(TcpInterceptor<I, O, II, OO> handler) {
		this.handler = handler;
		return handler;
	}

	/**
	 * Set a single handler vs an interception chain of {@link #interceptor}.
	 */
	public void handler(TcpHandler<I, O> handler) {
		this.handler = handler;
	}

	/**
	 * Return the configured TcpHandler.
	 */
	protected TcpHandler<I, O> getHandler() {
		return this.handler;
	}

}

package io.ripc.core;

/**
 *
 * @author Rossen Stoyanchev
 */
public interface WriteCompletedListener extends ConnectionEventListener {


	/**
	 * @return whether to flush following this write
	 */
	boolean writeCompleted(long count, Object lastItemWritten);

}

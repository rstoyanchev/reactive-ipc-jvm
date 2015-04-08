package io.ripc.core;


/**
 * A contract to request flush operations.
 *
 * <p>Intended to be implemented by a {@link org.reactivestreams.Subscriber}
 * performing write I/O and to be used within the context of the
 * {@link org.reactivestreams.Publisher} producing items to be written.
 * A {@link #flush()} may be invoked in between calls to
 * {@link org.reactivestreams.Subscriber#onNext(Object) onNext} to request
 * flushing all items that may have been buffered so far.
 */
public interface Flushable {

	/**
	 * Request a flush operation.
	 */
	public void flush();

}

/**
 * Created by drogoul, 10 mars 2014
 * 
 */
package msi.gama.common.util;

import java.util.concurrent.*;
import msi.gama.common.interfaces.IUpdaterTarget;
import msi.gama.common.util.ThreadedUpdater.IUpdaterMessage;

/**
 * Class ThreadedUpdater.
 * 
 * @author drogoul
 * @since 10 mars 2014
 * 
 */
public abstract class ThreadedUpdater<Message extends IUpdaterMessage> implements Runnable, IUpdaterTarget<Message> {

	private Thread runThread;
	private final BlockingQueue<Message> messages = new LinkedBlockingQueue(4);
	private IUpdaterTarget<Message> control;

	@Override
	public boolean isDisposed() {
		return control.isDisposed();
	}

	@Override
	public void updateWith(final Message m) {
		messages.offer(m);
	}

	public static interface IUpdaterMessage {

		public boolean isEmpty();
	}

	public void setTarget(final IUpdaterTarget<Message> l) {
		control = l;
		if ( runThread == null ) {
			runThread = new Thread(this, control.getClass().getSimpleName() + " updater thread");
			runThread.start();
		}
	}

	public void flush() {

	}

	@Override
	public void run() {
		while (true) {

			try {
				final Message m = messages.take();
				if ( m == null || m.isEmpty() ) { return; }
				GuiUtils.run(new Runnable() {

					@Override
					public void run() {
						if ( !control.isDisposed() ) {
							control.updateWith(m);
						}
					}
				});
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}

		}
	}
}
/*
 * Copyright 2008-2011, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

/**
 * Utility class for parallel operations.
 * @author akarnokd
 *
 */
public final class Parallels {
	/** Private constructor. */
	private Parallels() {
		// utility class
	}
	/**
	 * Schedule to run a task after the given amount of time delay
	 * in the event dispatch thread.
	 * @param delay the delay in milliseconds
	 * @param task the task to run.
	 */
	public static void runDelayedInEDT(final long delay, final Runnable task) {
		if (task != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						TimeUnit.MILLISECONDS.sleep(delay);
						SwingUtilities.invokeLater(task);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}, "Delayed EDT run with" + delay).start();
		}
	}
	/**
	 * Invoke a set of runnable objects in parallel and wait for all of them to finish.
	 * Exceptions are printed to the error output. The runnables are executed
	 * on the number of available processor sized executor
	 * @param run the list of runnables
	 */
	public static void invokeAndWait(Runnable... run) {
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		try {
			List<Future<?>> futures = new LinkedList<Future<?>>();
			for (Runnable r : run) {
				futures.add(exec.submit(r));
			}
			for (Future<?> f : futures) {
				try {
					f.get();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				} catch (ExecutionException e1) {
					e1.printStackTrace();
				}
			}
			
		} finally {
			exec.shutdown();
		}
	}
	/**
	 * Wait for the given collection of futures to complete and invoke the runnable
	 * in the event dispatch thread.
	 * @param futures the collection of futures
	 * @param runInEDT the task to run in the EDT
	 */
	public static void waitForFutures(final Collection<Future<?>> futures, final Runnable runInEDT) {
		Thread waiter = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					for (Future<?> f : futures) {
						f.get();
					}
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				} catch (ExecutionException ex) {
					ex.printStackTrace();
				}
				if (!Thread.currentThread().isInterrupted()) {
					SwingUtilities.invokeLater(runInEDT);
				}
			}
		}, "Waiter for " + futures.size() + " futures to finish");
		waiter.start();
	}
	/**
	 * Wait for all futures to complete and silently drop the exceptions.
	 * @param futures the iterable of futures
	 */
	public static void waitForAll(final Iterable<Future<?>> futures) {
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}
}

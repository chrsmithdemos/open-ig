/*
 * Copyright 2008-2013, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.model;

import java.io.IOException;

import hu.openig.net.MessageSerializable;


/**
 * An abstract class which collects the result or exception from the execution
 * of its <code>invoke()</code> method executed on a different thread.
 * @author akarnokd, 2013.04.26.
 */
public abstract class DeferredCall extends DeferredTransform<MessageObjectIO> {
	@Override
	protected final MessageSerializable transform(MessageObjectIO intermediate) throws IOException {
		return intermediate.toMessage();
	}
}
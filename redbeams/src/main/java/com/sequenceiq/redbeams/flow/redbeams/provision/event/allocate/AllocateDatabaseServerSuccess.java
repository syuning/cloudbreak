package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

import com.sequenceiq.redbeams.flow.stack.RedbeamsEvent;

/**
 * The event that occurs when a database has been allocated.
 */
public class AllocateDatabaseServerSuccess extends RedbeamsEvent {
    public AllocateDatabaseServerSuccess(Long stackId) {
        super(stackId);
    }
}
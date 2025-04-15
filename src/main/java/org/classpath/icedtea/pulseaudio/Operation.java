/* Operation.java
   Copyright (C) 2008 Red Hat, Inc.

This file is part of IcedTea-Sound.

IcedTea-Sound is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License as published by
the Free Software Foundation, version 2.

IcedTea-Sound is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with IcedTea-Sound; see the file COPYING.  If not, write to
the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version.
 */

package org.classpath.icedtea.pulseaudio;

import java.util.Arrays;
import com.sun.jna.ptr.PointerByReference;

import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary;

import static vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_operation_state.PA_OPERATION_CANCELLED;
import static vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_operation_state.PA_OPERATION_DONE;
import static vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_operation_state.PA_OPERATION_RUNNING;


/**
 * Encapsulates a pa_operation object
 * <p>
 * This is really needed only so that we can deallocate the reference counted
 * object. Any time a function returns an Operation object, the reference has
 * been incremented. The object wont be freed unless a releaseReference() is
 * done.
 * <p>
 * Please see the pulseaudio api docs for more information on a pa_opreation
 * object
 */
public class Operation {

    private PointerByReference operationPointer;
    private final EventLoop eventLoop;

    // These should never be written to in java. They will be initialized
    // properly in native code.
    public static final long RUNNING = PA_OPERATION_RUNNING;
    public static final long DONE = PA_OPERATION_DONE;
    public static final long CANCELLED = PA_OPERATION_CANCELLED;

    // If value is not one of RUNNING, DONE, CANCELLED, throw an
    // IllegalStateException. Otherwise return the input.
    private static long checkNativeOperationState(long value) {
        if (!Arrays.asList(RUNNING, DONE, CANCELLED).contains(value)) {
            throw new IllegalStateException("Unknown operation state: " + value);
        }
        return value;
    }

    private void native_ref() {
        PulseaudioLibrary.INSTANCE.pa_operation_ref(operationPointer);
    }

    private void native_unref() {
        PulseaudioLibrary.INSTANCE.pa_operation_unref(operationPointer);
    }

    private long native_get_state() {
        long state = PulseaudioLibrary.INSTANCE.pa_operation_get_state(operationPointer);
        return state;
    }

    Operation(PointerByReference operationPointer) {
        assert (operationPointer != null);
        this.operationPointer = operationPointer;
        this.eventLoop = EventLoop.getEventLoop();
    }

    /**
     * Increase reference count by 1
     */
    void addReference() {
        assert (operationPointer != null);
        synchronized (eventLoop.threadLock) {
            native_ref();
        }
    }

    /**
     * Decrease reference count by 1. If the count reaches 0, object will be freed
     */
    void releaseReference() {
        assert (operationPointer != null);
        synchronized (eventLoop.threadLock) {
            native_unref();
        }
        operationPointer = null;
    }

    // FIXME broken function
    boolean isNull() {
        if (operationPointer == null) {
            return true;
        }
        return false;
    }

    long getState() {
        assert (operationPointer != null);
        synchronized (eventLoop.threadLock) {
            return checkNativeOperationState(native_get_state());
        }
    }

    /**
     * Block until the operation has completed
     */
    void waitForCompletion() {
        assert (operationPointer != null);

        boolean interrupted = false;
        do {
            synchronized (eventLoop.threadLock) {
                if (getState() == DONE) {
                    return;
                }
                try {
                    eventLoop.threadLock.wait();
                } catch (InterruptedException e) {
                    // ingore the interrupt for now
                    interrupted = true;
                }
            }
        } while (getState() != DONE);

        // let the caller know about the interrupt
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}

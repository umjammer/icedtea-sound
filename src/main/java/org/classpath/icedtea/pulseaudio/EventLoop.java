/* EventLoop.java
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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import com.sun.jna.ptr.PointerByReference;

import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_context_notify_cb_t;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_sink_info_cb_t;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_source_info_cb_t;
import vavi.sound.pulseaudio.jna.pulseaudio.pa_mainloop_api;

import static java.lang.System.getLogger;


/**
 * This class wraps pulseaudio's event loop. It also holds the lock used in the
 * rest of pulse-java
 */
public final class EventLoop implements Runnable {

    private static final Logger logger = getLogger(EventLoop.class.getName());

    /*
     * any methods that can obstruct the behaviour of pa_mainloop should run
     * synchronized
     */

    /*
     * the threadLock object is the object used for synchronizing the
     * non-thread-safe operations of pulseaudio's c api
     */
    final Object threadLock = new Object();

    private static EventLoop instance = null;

    private final List<ContextListener> contextListeners;
    // private List<SourceDataLine> lines;
    private String appName;
    private String serverString;

    private long status;
    // private boolean eventLoopIsRunning = false;

    private List<String> targetPortNameList = new ArrayList<>();
    private List<String> sourcePortNameList = new ArrayList<>();

    /*
     * JNI stuff
     *
     * Do not synchronize the individual functions, synchronize
     * block/method/lines around the call
     */

    static final int PA_ITERATE_BLOCK = 1;
    static final int PA_ITERATE_NOBLOCK = 0;

    final pa_sink_info_cb_t sink_list_success_cb = (context, i, eol, userdata) -> {

        if (eol == 0) {
            sink_callback(i.name.getString(0));
        } else {
            synchronized (EventLoop.getEventLoop().threadLock) {
                EventLoop.getEventLoop().threadLock.notifyAll();
            }
        }
    };

    final pa_source_info_cb_t source_list_success_cb = (context, i, eol, userdata) -> {

        if (eol == 0) {
            source_callback(i.name.getString(0));
        } else {
            synchronized (EventLoop.getEventLoop().threadLock) {
                EventLoop.getEventLoop().threadLock.notifyAll();
            }
        }
    };

    private final pa_context_notify_cb_t context_change_callback = (/* pa_context */ context, userdata) -> {
        assert context != null;

        logger.log(Level.TRACE, "context state changed to %d".formatted(PulseaudioLibrary.INSTANCE.pa_context_get_state(context)));

        // Call the EventLoop.update method in java
        // to handle all java-side events

        update(PulseaudioLibrary.INSTANCE.pa_context_get_state(context));
    };

    private void native_setup(String appName, String server) {

        assert (appName != null);

        logger.log(Level.TRACE, "native_setup() called");
        PointerByReference /* pa_mainloop */ mainloop = PulseaudioLibrary.INSTANCE.pa_mainloop_new();
        assert (mainloop != null);
        pa_mainloop_api mainloop_api = PulseaudioLibrary.INSTANCE.pa_mainloop_get_api(mainloop);

        PointerByReference /* pa_context */ context = null;

        final String string_appName;
        logger.log(Level.TRACE, "using appName : %s".formatted(appName));
        context = PulseaudioLibrary.INSTANCE.pa_context_new(mainloop_api, appName);

        PulseaudioLibrary.INSTANCE.pa_context_set_state_callback(context, context_change_callback, null);

        if (server != null) {
            /* obtain the server from the caller */
            logger.log(Level.TRACE, "About to connect to server: %s".formatted(server));
            PulseaudioLibrary.INSTANCE.pa_context_connect(context, server, 0, null);
        } else {
            logger.log(Level.TRACE, "using default server");
            PulseaudioLibrary.INSTANCE.pa_context_connect(context, (String) null, 0, null);
        }

        this.mainloopPointer = mainloop;
        this.contextPointer = context;
        logger.log(Level.TRACE, "native_setup() returning");
    }

    /**
     * the original jni poll function released the threadLock while polling,
     * so that other threads can call the pulseaudio api meanwhile. a java
     * monitor can not be exited inside a synchronized block, so prepare and
     * dispatch are done holding the threadLock and poll is done without it.
     */
    private int native_iterate(int timeout) {

        int returnval;

        synchronized (threadLock) {
            returnval = PulseaudioLibrary.INSTANCE.pa_mainloop_prepare(mainloopPointer, timeout);
        }
        if (returnval < 0) {
            return -1;
        }
        returnval = PulseaudioLibrary.INSTANCE.pa_mainloop_poll(mainloopPointer);
        if (returnval < 0) {
            return -1;
        }
        synchronized (threadLock) {
            returnval = PulseaudioLibrary.INSTANCE.pa_mainloop_dispatch(mainloopPointer);
        }
        if (returnval < 0) {
            return -1;
        }
        return returnval;
    }

    private pa_context_notify_cb_t context_drain_complete_callback = (/* pa_context */ context, userdata) -> {
        PulseaudioLibrary.INSTANCE.pa_context_disconnect(context);
    };

    private void native_shutdown() {

        logger.log(Level.TRACE, "native_shutdown() starting");

        PointerByReference /* pa_operation */ o = PulseaudioLibrary.INSTANCE.pa_context_drain(contextPointer, context_drain_complete_callback, null);
        if (o == null) {
            PulseaudioLibrary.INSTANCE.pa_context_disconnect(contextPointer);
            PulseaudioLibrary.INSTANCE.pa_mainloop_free(mainloopPointer);
        } else {
            PulseaudioLibrary.INSTANCE.pa_operation_unref(o);
        }

        PulseaudioLibrary.INSTANCE.pa_context_unref(contextPointer);

        this.mainloopPointer = null;
        this.contextPointer = null;

        logger.log(Level.TRACE, "native_shutdown() returning");
    }

    /*
     * These fields hold pointers
     */
    private PointerByReference contextPointer;
    private PointerByReference mainloopPointer;

    private EventLoop() {
        contextListeners = new ArrayList<>();
    }

    public synchronized static EventLoop getEventLoop() {
        if (instance == null) {
            instance = new EventLoop();
        }
        return instance;
    }

    void setAppName(String appName) {
        this.appName = appName;
    }

    void setServer(String serverString) {
        this.serverString = serverString;
    }

    @Override
    public void run() {
        native_setup(this.appName, this.serverString);

        logger.log(Level.INFO, "Eventloop.run(): eventloop starting");

        /*
         * Perhaps this loop should be written in C doing a Java to C call on
         * every iteration of the loop might be slow
         */
        while (true) {
            // timeout is in milliseconds
            // timout = 0 means dont block
            native_iterate(100);

            synchronized (threadLock) {
                if (Thread.interrupted()) {
                    native_shutdown();

                    // clean up the listeners
                    synchronized (contextListeners) {
                        contextListeners.clear();
                    }

                    logger.log(Level.INFO, "EventLoop.run(): event loop terminated");

                    return;
                }
            }
        }
    }

    void addContextListener(ContextListener contextListener) {
        synchronized (contextListeners) {
            contextListeners.add(contextListener);
        }
    }

    void removeContextListener(ContextListener contextListener) {
        synchronized (contextListeners) {
            contextListeners.remove(contextListener);
        }
    }

    long getStatus() {
        return this.status;
    }

    public void update(long status) {
        synchronized (threadLock) {
            logger.log(Level.TRACE, "update() called! status = " + status);
            this.status = status;
            fireEvent(new ContextEvent(status));
        }

        if (status == ContextEvent.FAILED) {
            logger.log(Level.WARNING,
                    "EventLoop.update(): Context failed");
        }
    }

    private void fireEvent(ContextEvent e) {
        logger.log(Level.TRACE, "firing event: " + e.getType());

        synchronized (contextListeners) {
            logger.log(Level.TRACE, contextListeners.size());
            for (ContextListener listener : contextListeners) {
                listener.update(e);
            }
        }
    }

    PointerByReference getContextPointer() {
        return contextPointer;
    }

    PointerByReference getMainLoopPointer() {
        return mainloopPointer;
    }

    PointerByReference nativeUpdateTargetPortNameList() {
        PointerByReference /* pa_operation */ o = PulseaudioLibrary.INSTANCE.pa_context_get_sink_info_list(contextPointer, sink_list_success_cb, null);
        assert o != null;
        return o;
    }

    PointerByReference nativeUpdateSourcePortNameList() {
        PointerByReference /* pa_operation */ o = PulseaudioLibrary.INSTANCE.pa_context_get_source_info_list(contextPointer, source_list_success_cb, null);
        assert o != null;
        return o;
    }

    synchronized List<String> updateTargetPortNameList() {
        targetPortNameList = new ArrayList<>();
        Operation op;
        synchronized (this.threadLock) {
            op = new Operation(nativeUpdateTargetPortNameList());
        }

        op.waitForCompletion();

        assert (op.getState() == Operation.DONE);

        op.releaseReference();
        return targetPortNameList;
    }

    synchronized List<String> updateSourcePortNameList() {
        sourcePortNameList = new ArrayList<>();
        Operation op;
        synchronized (this.threadLock) {
            op = new Operation(nativeUpdateSourcePortNameList());
        }

        op.waitForCompletion();

        assert (op.getState() == Operation.DONE);

        op.releaseReference();
        return sourcePortNameList;
    }

    public void source_callback(String name) {
        sourcePortNameList.add(name);
        synchronized (EventLoop.getEventLoop().threadLock) {
            EventLoop.getEventLoop().threadLock.notifyAll();
        }
    }

    public void sink_callback(String name) {
        targetPortNameList.add(name);
    }
}

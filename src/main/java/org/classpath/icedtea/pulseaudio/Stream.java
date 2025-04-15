/* PulseAudioClip.java
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.sound.sampled.LineUnavailableException;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_context_success_cb_t;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_sample_format;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_sink_input_info_cb_t;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_stream_flags;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_stream_notify_cb_t;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_stream_request_cb_t;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_stream_state;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_stream_success_cb_t;
import vavi.sound.pulseaudio.jna.pulseaudio.pa_buffer_attr;
import vavi.sound.pulseaudio.jna.pulseaudio.pa_cvolume;
import vavi.sound.pulseaudio.jna.pulseaudio.pa_sample_spec;
import vavi.sound.pulseaudio.jna.pulseaudio.pa_timing_info;

import static java.lang.System.getLogger;
import static vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_seek_mode.PA_SEEK_RELATIVE;


/**
 * This class encapsulates a pa_stream object and provides easier access to the
 * native functions
 * <p>
 * <p>
 * for more details on this see the pa_stream_* functions in the pulseaudio api
 * docs
 */
final class Stream {

    private static final Logger logger = getLogger(Stream.class.getName());

    public interface StateListener {

        void update();
    }

    public interface CorkListener {

        void update();
    }

    public interface WriteListener {

        void update();
    }

    public interface ReadListener {

        void update();
    }

    public interface OverflowListener {

        void update();
    }

    public interface UnderflowListener {

        void update();
    }

    public interface PlaybackStartedListener {

        void update();
    }

    public interface LatencyUpdateListener {

        void update();
    }

    public interface MovedListener {

        void update();
    }

    public interface UpdateTimingInfoListener {

        void update();
    }

    public interface SuspendedListener {

        void update();
    }

    // see comments in ContextEvent.java and Operation.java
    // These are the possible stream states.
    public static final long STATE_UNCONNECTED = pa_stream_state.PA_STREAM_UNCONNECTED;
    public static final long STATE_CREATING = pa_stream_state.PA_STREAM_CREATING;
    public static final long STATE_READY = pa_stream_state.PA_STREAM_READY;
    public static final long STATE_FAILED = pa_stream_state.PA_STREAM_FAILED;
    public static final long STATE_TERMINATED = pa_stream_state.PA_STREAM_TERMINATED;

    // Throw an IllegalStateException if value is not one of the possible
    // states. Otherwise return the input.
    public static long checkNativeStreamState(long value) {
        if (!Arrays.asList(STATE_UNCONNECTED, STATE_CREATING, STATE_READY, STATE_FAILED, STATE_TERMINATED).contains(value)) {
            throw new IllegalStateException("Illegal constant for ContextEvent: " + value);
        }
        return value;
    }

    // These are stream flags.
    public static long FLAG_NOFLAGS = pa_stream_flags.PA_STREAM_NOFLAGS;
    public static final long FLAG_START_CORKED = pa_stream_flags.PA_STREAM_START_CORKED;
    public static long FLAG_INTERPOLATE_TIMING = pa_stream_flags.PA_STREAM_INTERPOLATE_TIMING;
    public static long FLAG_NOT_MONOTONIC = pa_stream_flags.PA_STREAM_NOT_MONOTONIC;
    public static long FLAG_AUTO_TIMING_UPDATE = pa_stream_flags.PA_STREAM_AUTO_TIMING_UPDATE;
    public static long FLAG_NO_REMAP_CHANNELS = pa_stream_flags.PA_STREAM_NO_REMAP_CHANNELS;
    public static long FLAG_NO_REMIX_CHANNELS = pa_stream_flags.PA_STREAM_FIX_FORMAT;
    public static long FLAG_FIX_FORMAT = pa_stream_flags.PA_STREAM_FIX_FORMAT;
    public static long FLAG_FIX_RATE = pa_stream_flags.PA_STREAM_FIX_RATE;
    public static long FLAG_FIX_CHANNELS = pa_stream_flags.PA_STREAM_FIX_CHANNELS;
    public static long FLAG_DONT_MOVE = pa_stream_flags.PA_STREAM_DONT_MOVE;
    public static long FLAG_VARIABLE_RATE = pa_stream_flags.PA_STREAM_VARIABLE_RATE;
    public static long FLAG_PEAK_DETECT = pa_stream_flags.PA_STREAM_PEAK_DETECT;
    public static long FLAG_START_MUTED = pa_stream_flags.PA_STREAM_START_MUTED;
    public static final long FLAG_ADJUST_LATENCY = pa_stream_flags.PA_STREAM_ADJUST_LATENCY;
    public static long FLAG_EARLY_REQUESTS = pa_stream_flags.PA_STREAM_EARLY_REQUESTS;
    public static long FLAG_DONT_INHIBIT_AUTO_SUSPEND = pa_stream_flags.PA_STREAM_DONT_INHIBIT_AUTO_SUSPEND;
    public static long FLAG_START_UNMUTED = pa_stream_flags.PA_STREAM_START_UNMUTED;
    public static long FLAG_FAIL_ON_SUSPEND = pa_stream_flags.PA_STREAM_FAIL_ON_SUSPEND;

    // We don't change this to static longs like we did with all other pulse
    // audio enums mirrored in java because we never use the pulse audio
    // integer value of formats on the java side. In java, the handling of
    // formats is strictly symbolic and string based. For that, enums are
    // better.
    public enum Format {
        PA_SAMPLE_U8,
        PA_SAMPLE_ULAW,
        PA_SAMPLE_ALAW,
        PA_SAMPLE_S16LE,
        PA_SAMPLE_S16BE,
        PA_SAMPLE_FLOAT32LE,
        PA_SAMPLE_FLOAT32BE,
        PA_SAMPLE_S32LE,
        PA_SAMPLE_S32BE
    }

    public static final String DEFAULT_DEVICE = null;

    // stores a pointer to pa_stream
    private PointerByReference streamPointer;
    // stores a pointer to the java_context/this object for callbacks
    private PointerByReference contextPointer;

    private final Format format;
    private float cachedVolume;

    private StreamBufferAttributes bufAttr = new StreamBufferAttributes(0, 0, 0, 0, 0);
    private static final Object bufAttrMutex = new Object();

    private final List<StateListener> stateListeners;
    private final List<WriteListener> writeListeners;
    private final List<ReadListener> readListeners;
    private final List<OverflowListener> overflowListeners;
    private final List<UnderflowListener> underflowListeners;
    private final List<PlaybackStartedListener> playbackStartedListeners;
    private final List<LatencyUpdateListener> latencyUpdateListeners;
    private final List<MovedListener> movedListeners;
    private final List<SuspendedListener> suspendedListeners;
    private final List<CorkListener> corkListeners;

    private pa_context_success_cb_t set_sink_input_volume_callback = (context, success, userdata) -> {
        synchronized (EventLoop.getEventLoop().threadLock) {
            EventLoop.getEventLoop().threadLock.notifyAll();
        }
    };

    static String getStringFromFormat(int format) {

        final String value;

        if (format == pa_sample_format.PA_SAMPLE_U8) {
            value = "PA_SAMPLE_U8";
        } else if (format == pa_sample_format.PA_SAMPLE_ALAW) {
            value = "PA_SAMPLE_ALAW";
        } else if (format == pa_sample_format.PA_SAMPLE_ULAW) {
            value = "PA_SAMPLE_ULAW";
        } else if (format == pa_sample_format.PA_SAMPLE_S16BE) {
            value = "PA_SAMPLE_S16BE";
        } else if (format == pa_sample_format.PA_SAMPLE_S16LE) {
            value = "PA_SAMPLE_S16LE";
        } else if (format == pa_sample_format.PA_SAMPLE_S32BE) {
            value = "PA_SAMPLE_S32BE";
        } else if (format == pa_sample_format.PA_SAMPLE_S32LE) {
            value = "PA_SAMPLE_S32LE";
        } else {
            value = "PA_SAMPLE_INVALID";
        }

        return value;
    }

    static int getFormatFromString(final String encoding) {

        int format;

        if ("PA_SAMPLE_U8".equals(encoding)) {
            format = pa_sample_format.PA_SAMPLE_U8;
        } else if ("PA_SAMPLE_ALAW".equals(encoding)) {
            format = pa_sample_format.PA_SAMPLE_ALAW;
        } else if ("PA_SAMPLE_ULAW".equals(encoding)) {
            format = pa_sample_format.PA_SAMPLE_ULAW;
        } else if ("PA_SAMPLE_S16BE".equals(encoding)) {
            format = pa_sample_format.PA_SAMPLE_S16BE;
        } else if ("PA_SAMPLE_S16LE".equals(encoding)) {
            format = pa_sample_format.PA_SAMPLE_S16LE;
        } else if ("PA_SAMPLE_S32BE".equals(encoding)) {
            format = pa_sample_format.PA_SAMPLE_S32BE;
        } else if ("PA_SAMPLE_S32LE".equals(encoding)) {
            format = pa_sample_format.PA_SAMPLE_S32LE;
        } else {
            format = pa_sample_format.PA_SAMPLE_INVALID;
        }

        return format;
    }

    private pa_stream_notify_cb_t stream_state_callback = (/* pa_stream */ stream, userdata) -> {
        logger.log(Level.TRACE, "stream_state_callback called");

        if (PulseaudioLibrary.INSTANCE.pa_stream_get_state(new PointerByReference(stream)) == pa_stream_state.PA_STREAM_CREATING) {
            this.stateCallback();
        } else {
            this.stateCallback();
        }
    };

    private pa_stream_request_cb_t stream_write_callback = (/* pa_stream */ stream, length, userdata) -> {
        logger.log(Level.TRACE, "stream_write_callback called");

        if (PulseaudioLibrary.INSTANCE.pa_stream_get_state(new PointerByReference(stream)) == pa_stream_state.PA_STREAM_CREATING) {
            this.writeCallback();
        } else {
            this.writeCallback();
        }
    };

    private pa_stream_request_cb_t stream_read_callback = (/* pa_stream */ stream, length, userdata) -> {
        logger.log(Level.TRACE, "stream_read_callback called");

        if (PulseaudioLibrary.INSTANCE.pa_stream_get_state(new PointerByReference(stream)) == pa_stream_state.PA_STREAM_CREATING) {
            this.readCallback();
        } else {
            this.readCallback();
        }
    };

    private pa_stream_notify_cb_t stream_overflow_callback = (/* pa_stream */ stream, userdata) -> {
        logger.log(Level.TRACE, "stream_overflow_callback called");

        if (PulseaudioLibrary.INSTANCE.pa_stream_get_state(new PointerByReference(stream)) == pa_stream_state.PA_STREAM_CREATING) {
            this.overflowCallback();
        } else {
            this.overflowCallback();
        }
    };

    private pa_stream_notify_cb_t stream_underflow_callback = (/* pa_stream */ stream, userdata) -> {
        logger.log(Level.TRACE, "stream_underflow_callback called");

        if (PulseaudioLibrary.INSTANCE.pa_stream_get_state(new PointerByReference(stream)) == pa_stream_state.PA_STREAM_CREATING) {
            this.underflowCallback();
        } else {
            this.underflowCallback();
        }
    };

    private pa_stream_success_cb_t update_timing_info_callback = (/* pa_stream */ stream, success, userdata) -> {
        synchronized (EventLoop.getEventLoop().threadLock) {
            EventLoop.getEventLoop().threadLock.notifyAll();
        }

        if (success == 0) {
            throw new IllegalStateException("drain failed");
        }
    };

    // requires pulseaudio 0.9.11 :(
    private pa_stream_notify_cb_t stream_started_callback = (/* pa_stream */ stream, userdata) -> {
        logger.log(Level.TRACE, "stream_started_callback called");

        if (PulseaudioLibrary.INSTANCE.pa_stream_get_state(new PointerByReference(stream)) == pa_stream_state.PA_STREAM_CREATING) {
            this.playbackStartedCallback();
        } else {
            this.playbackStartedCallback();
        }
    };

    private pa_stream_notify_cb_t stream_latency_update_callback = (/* pa_stream */ stream, userdata) -> {
        logger.log(Level.TRACE, "stream_latency_update_callback called");

        if (PulseaudioLibrary.INSTANCE.pa_stream_get_state(new PointerByReference(stream)) == pa_stream_state.PA_STREAM_CREATING) {
            this.latencyUpdateCallback();
        } else {
            this.latencyUpdateCallback();
        }
    };

    private pa_stream_notify_cb_t stream_moved_callback = (/* pa_stream */ stream, userdata) -> {
        logger.log(Level.TRACE, "stream_moved_callback called");

        if (PulseaudioLibrary.INSTANCE.pa_stream_get_state(new PointerByReference(stream)) == pa_stream_state.PA_STREAM_CREATING) {
            this.movedCallback();
        } else {
            this.movedCallback();
        }
    };

    private pa_stream_notify_cb_t stream_suspended_callback = (/* pa_stream */ stream, userdata) -> {
        logger.log(Level.TRACE, "stream_suspended_callback called");

        if (PulseaudioLibrary.INSTANCE.pa_stream_get_state(new PointerByReference(stream)) == pa_stream_state.PA_STREAM_CREATING) {
            this.suspendedCallback();
        } else {
            this.suspendedCallback();
        }
    };

    private pa_stream_notify_cb_t buf_attr_changed_callback = (/* pa_stream */ stream, userdata) -> {

        if (PulseaudioLibrary.INSTANCE.pa_stream_get_state(new PointerByReference(stream)) == pa_stream_state.PA_STREAM_CREATING) {
            this.bufferAttrCallback();
        } else {
            this.bufferAttrCallback();
        }
    };

    // used to set stream flags and states.
    // The names in Stream.java have a {STATE_,FLAG_} prefix, but we don't want to
    // add the underscore in every line in init_constants, so we add it here. If
    // constants with no prefix are ever introduced (i.e. java_prefix is "",
    // it's important to remove the ##_ )

    private void native_pa_stream_new(PointerByReference contextPointer,
                                      String name, String encoding, int sampleRate, int channels) {

        logger.log(Level.TRACE, "creating a new PulseAudio stream");

        this.contextPointer = contextPointer;

        PointerByReference /* pa_context */ context = contextPointer;
        assert context != null;

        pa_sample_spec sample_spec = new pa_sample_spec();

        sample_spec.format = getFormatFromString(encoding);
        sample_spec.rate = sampleRate;
        sample_spec.channels = (byte) channels;

        if (PulseaudioLibrary.INSTANCE.pa_sample_spec_valid(sample_spec) == 0) {
            throw new IllegalArgumentException("Invalid format");
        }

        PointerByReference /* pa_stream */ stream = PulseaudioLibrary.INSTANCE.pa_stream_new(context, name, sample_spec, null);
        assert stream != null;

        this.streamPointer = stream;

        /*
         *
         * The stream has been created; now setup the callbacks
         * so we can do somethig about them
         *
         */

        PulseaudioLibrary.INSTANCE.pa_stream_set_state_callback(stream, stream_state_callback, context.getPointer());
        PulseaudioLibrary.INSTANCE.pa_stream_set_write_callback(stream, stream_write_callback, context.getPointer());
        PulseaudioLibrary.INSTANCE.pa_stream_set_read_callback(stream, stream_read_callback, context.getPointer());
        PulseaudioLibrary.INSTANCE.pa_stream_set_overflow_callback(stream, stream_overflow_callback, context.getPointer());
        PulseaudioLibrary.INSTANCE.pa_stream_set_underflow_callback(stream, stream_underflow_callback, context.getPointer());
        PulseaudioLibrary.INSTANCE.pa_stream_set_started_callback(stream, stream_started_callback, context.getPointer());
        PulseaudioLibrary.INSTANCE.pa_stream_set_latency_update_callback(stream, stream_latency_update_callback, context.getPointer());
        PulseaudioLibrary.INSTANCE.pa_stream_set_moved_callback(stream, stream_moved_callback, context.getPointer());
        PulseaudioLibrary.INSTANCE.pa_stream_set_suspended_callback(stream, stream_suspended_callback, context.getPointer());
        PulseaudioLibrary.INSTANCE.pa_stream_set_buffer_attr_callback(stream, buf_attr_changed_callback, context.getPointer());
    }

    private void native_pa_stream_unref() {

        PointerByReference /* java_context */ j_context = this.contextPointer;
        assert j_context != null;
        this.contextPointer = null;

        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        PulseaudioLibrary.INSTANCE.pa_stream_unref(stream);
        this.streamPointer = null;
    }

    private long native_pa_stream_get_state() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        return PulseaudioLibrary.INSTANCE.pa_stream_get_state(stream);
    }

    private PointerByReference native_pa_stream_get_context() {

        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        PointerByReference /* pa_context */ context = PulseaudioLibrary.INSTANCE.pa_stream_get_context(stream);
        assert context != null;
        return context;
    }

    private int native_pa_stream_get_index() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        return PulseaudioLibrary.INSTANCE.pa_stream_get_index(stream);
    }

    private int native_pa_stream_get_device_index() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        return PulseaudioLibrary.INSTANCE.pa_stream_get_device_index(stream);
    }

    private String native_pa_stream_get_device_name() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        final String name = PulseaudioLibrary.INSTANCE.pa_stream_get_device_name(stream);
        assert name != null;
        return name;
    }

    private int native_pa_stream_is_suspended() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        return PulseaudioLibrary.INSTANCE.pa_stream_is_suspended(stream);
    }

    private int native_pa_stream_connect_playback(String device,
                                                  int bufferMaxLength, int bufferTargetLength,
                                                  int bufferPreBuffering, int bufferMinimumRequest,
                                                  int bufferFragmentSize, long flags, byte[] volumePointer,
                                                  PointerByReference sync_streamPointer) {
        PointerByReference /* pa_stream */ sync_stream;
        if (sync_streamPointer != null) {
            sync_stream = sync_streamPointer;
            logger.log(Level.DEBUG, "Master stream is %s".formatted(sync_stream));
        } else {
            sync_stream = null;
        }

        PointerByReference /* pa_stream */ stream = this.streamPointer;

        pa_buffer_attr buffer_attr = new pa_buffer_attr();

        buffer_attr.maxlength = bufferMaxLength;
        buffer_attr.tlength = bufferTargetLength;
        buffer_attr.prebuf = bufferPreBuffering;
        buffer_attr.minreq = bufferMinimumRequest;

        int value = PulseaudioLibrary.INSTANCE.pa_stream_connect_playback(stream, device, buffer_attr,
                (int) flags, null, sync_stream);
        return value;
    }

    private int native_pa_stream_connect_record(String device,
                                                int bufferMaxLength, int bufferTargetLength,
                                                int bufferPreBuffering, int bufferMinimumRequest,
                                                int bufferFragmentSize, long flags, byte[] volumePointer,
                                                byte[] sync_streamPointer) {

        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;

        pa_buffer_attr buffer_attr = new pa_buffer_attr();
        buffer_attr.maxlength = bufferMaxLength;
        buffer_attr.fragsize = bufferFragmentSize;

        int value = PulseaudioLibrary.INSTANCE.pa_stream_connect_record(stream, device, buffer_attr, (int) flags);
        return value;
    }

    private int native_pa_stream_disconnect() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        int return_value = PulseaudioLibrary.INSTANCE.pa_stream_disconnect(stream);

        return return_value;
    }

    private int native_pa_stream_write(byte[] data, int offset, int length) {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        Memory data_buffer = new Memory(length);
        data_buffer.read(0L, data, offset, length);
        int value = PulseaudioLibrary.INSTANCE.pa_stream_write(stream, data_buffer.getPointer(0), new NativeLong(length), null, 0, PA_SEEK_RELATIVE);
        return value;
    }

    private byte[] native_pa_stream_peek() {

        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        PointerByReference startLocation = new PointerByReference();
        NativeLongByReference count = new NativeLongByReference();

        if (PulseaudioLibrary.INSTANCE.pa_stream_peek(stream, startLocation, count) < 0) {
            return null;
        }

        /* no data available */
        if (startLocation == null) {
            return null;
        }
        int length = count.getValue().intValue();
        byte[] data = new byte[length];
        return data;
    }

    private int native_pa_stream_drop() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        return PulseaudioLibrary.INSTANCE.pa_stream_drop(stream);
    }

    private int native_pa_stream_writable_size() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        if (stream == null) {
            return 0;
        }
        int size = PulseaudioLibrary.INSTANCE.pa_stream_writable_size(stream).intValue();
        return size;
    }

    private int native_pa_stream_readable_size() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        return PulseaudioLibrary.INSTANCE.pa_stream_readable_size(stream).intValue();
    }

    private pa_stream_success_cb_t drain_callback = (/* pa_stream */ stream, success, userdata) -> {

        assert stream != null;
        synchronized (EventLoop.getEventLoop().threadLock) {
            EventLoop.getEventLoop().threadLock.notifyAll();
        }

        if (success == 0) {
            throw new IllegalStateException("drain failed");
        }
    };

    private PointerByReference native_pa_stream_drain() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        PointerByReference /* pa_operation */ operation = PulseaudioLibrary.INSTANCE.pa_stream_drain(stream, drain_callback, null);
        assert operation != null;
        return operation;
    }

    private PointerByReference native_pa_stream_updateTimingInfo() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        PointerByReference /* pa_operation */ o = PulseaudioLibrary.INSTANCE.pa_stream_update_timing_info(stream, update_timing_info_callback, null);
        assert o != null;
        return o;
    }

    public int bytesInBuffer() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        final pa_timing_info timing_info = PulseaudioLibrary.INSTANCE.pa_stream_get_timing_info(stream);
        int write_index = (int) timing_info.write_index;
        int read_index = (int) timing_info.read_index;
        return write_index - read_index;
    }

    /*
     * pa_operation pa_stream_update_timing_info (pa_streamp,
     * pa_stream_success_cb_t cb, voiduserdata) Request a timing info structure
     * update for a stream.
     */

    private int native_pa_stream_is_corked() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        return PulseaudioLibrary.INSTANCE.pa_stream_is_corked(stream);
    }

    private pa_stream_success_cb_t cork_callback = (/* pa_stream */ stream, success, userdata) -> {

        assert stream != null;
        synchronized (EventLoop.getEventLoop().threadLock) {
            EventLoop.getEventLoop().threadLock.notifyAll();
        }

        if (success == 0) {
            throw new IllegalStateException("cork failed");
        }
    };

    private PointerByReference native_pa_stream_cork(int yes) {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        PointerByReference /* pa_operation */ operation = PulseaudioLibrary.INSTANCE.pa_stream_cork(stream, yes, cork_callback, null);
        assert operation != null;
        return operation;
    }

    private pa_stream_success_cb_t flush_callback = (/* pa_stream */ stream, success, userdata) -> {

        assert stream != null;
        synchronized (EventLoop.getEventLoop().threadLock) {
            EventLoop.getEventLoop().threadLock.notifyAll();
        }

        if (success == 0) {
            throw new IllegalStateException("flush failed");
        }
    };

    private PointerByReference native_pa_stream_flush() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        PointerByReference /* pa_operation */ operation = PulseaudioLibrary.INSTANCE.pa_stream_flush(stream, flush_callback, null);
        assert operation != null;
        return operation;
    }

    private pa_stream_success_cb_t trigger_callback = (/* pa_stream */ stream, success, userdata) -> {

        assert stream != null;
        synchronized (EventLoop.getEventLoop().threadLock) {
            EventLoop.getEventLoop().threadLock.notifyAll();
        }

        if (success == 0) {
            throw new IllegalStateException("trigger failed");
        }
    };

    /*
     * pa_operation pa_stream_prebuf (pa_streams, pa_stream_success_cb_t cb,
     * voiduserdata) Reenable prebuffering as specified in the pa_buffer_attr
     * structure.
     */

    private PointerByReference native_pa_stream_trigger() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        PointerByReference /* pa_operation */ operation = PulseaudioLibrary.INSTANCE.pa_stream_trigger(stream, trigger_callback, null);
        assert operation != null;
        return operation;
    }

    private pa_stream_success_cb_t set_name_callback = (/* pa_stream */ stream, success, userdata) -> {
        synchronized (EventLoop.getEventLoop().threadLock) {
            EventLoop.getEventLoop().threadLock.notifyAll();
        }

        if (success == 0) {
            throw new IllegalStateException("set_name failed");
        }
    };

    /* returns an operationPointer */
    private PointerByReference native_pa_stream_set_name(String name) {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;

        PointerByReference /* pa_operation */ operation = PulseaudioLibrary.INSTANCE.pa_stream_set_name(stream, name, set_name_callback, null);
        assert operation != null;

        return operation;
    }

    /* Return the current playback/recording time */
    private long native_pa_stream_get_time() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;

        LongByReference /* pa_usec_t */ time = new LongByReference();
        int result = PulseaudioLibrary.INSTANCE.pa_stream_get_time(stream, time);
        assert result == 0;

        return time.getValue();
    }

    /* Return the total stream latency */
    private long native_pa_stream_get_latency() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        LongByReference /* pa_usec_t */ returnValue = new LongByReference();
        IntByReference negative = new IntByReference();
        int result = PulseaudioLibrary.INSTANCE.pa_stream_get_latency(stream, returnValue, negative);
        assert result == 0;
        assert negative.getValue() == 0;
        return returnValue.getValue();
    }

    /*
     * const pa_timing_info pa_stream_get_timing_info (pa_streams) Return the
     * latest raw timing data structure.
     */

    private StreamSampleSpecification native_pa_stream_get_sample_spec() {
        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;

        pa_sample_spec sample_spec = PulseaudioLibrary.INSTANCE.pa_stream_get_sample_spec(stream);
        assert sample_spec != null;

        String format = getStringFromFormat(sample_spec.format);
        int rate = sample_spec.rate;
        int channels = sample_spec.channels;

        StreamSampleSpecification return_object = new StreamSampleSpecification(format, rate, channels);

        return return_object;
    }

    /*
     * const pa_channel_map pa_stream_get_channel_map (pa_streams) Return a
     * pointer to the stream's channel map. const
     */
    private StreamBufferAttributes native_pa_stream_get_buffer_attr() {

        logger.log(Level.TRACE, "in native_pa_stream_get_buffer_attributes");

        PointerByReference /* pa_stream */ stream = this.streamPointer;
        pa_buffer_attr buffer = PulseaudioLibrary.INSTANCE.pa_stream_get_buffer_attr(stream);
        assert buffer != null;

        int maxLength = buffer.maxlength;
        int targetLength = buffer.tlength;
        int preBuffering = buffer.prebuf;
        int minimumRequest = buffer.minreq;
        int fragmentSize = buffer.fragsize;

        StreamBufferAttributes return_object = new StreamBufferAttributes(maxLength, targetLength,
                preBuffering, minimumRequest, fragmentSize);

        return return_object;
    }

    private pa_stream_success_cb_t set_buffer_attr_callback = (/* pa_stream */ stream, success, userdata) -> {
        synchronized (EventLoop.getEventLoop().threadLock) {
            EventLoop.getEventLoop().threadLock.notifyAll();
        }

        if (success == 0) {
            throw new IllegalStateException("set_buffer_attr failed");
        }
    };

    private PointerByReference native_pa_stream_set_buffer_attr(StreamBufferAttributes info) {

        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;

        pa_buffer_attr buffer = new pa_buffer_attr();

        buffer.maxlength = info.getMaxLength();
        buffer.tlength = info.getTargetLength();
        buffer.prebuf = info.getPreBuffering();
        buffer.minreq = info.getMinimumRequest();
        buffer.fragsize = info.getFragmentSize();

if (logger.isLoggable(Level.TRACE)) {
 pa_buffer_attr old_buffer = PulseaudioLibrary.INSTANCE.pa_stream_get_buffer_attr(stream);
 logger.log(Level.TRACE, "old buffer values: %d %d %d %d %d".formatted(old_buffer.maxlength, old_buffer.tlength, old_buffer.prebuf, old_buffer.minreq, old_buffer.fragsize));
 logger.log(Level.TRACE, "want these values: %d %d %d %d %d".formatted(buffer.maxlength, buffer.tlength, buffer.prebuf, buffer.minreq, buffer.fragsize));
}
        PointerByReference /* pa_operation */ operation = PulseaudioLibrary.INSTANCE.pa_stream_set_buffer_attr(stream, buffer, set_buffer_attr_callback, null);

        assert operation != null;
        return operation;
    }

    private pa_stream_success_cb_t update_sample_rate_callback = (/* pa_stream */ stream, success, userdata) -> {
        assert stream != null;
        synchronized (EventLoop.getEventLoop().threadLock) {
            EventLoop.getEventLoop().threadLock.notifyAll();
        }

        if (success == 0) {
            throw new IllegalStateException("update_sampl_rate failed");
        }
    };

    private PointerByReference native_pa_stream_update_sample_rate(int rate) {

        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        PointerByReference /* pa_operation */ operation = PulseaudioLibrary.INSTANCE.pa_stream_update_sample_rate(stream, rate, update_sample_rate_callback, null);
        assert operation != null;
        return operation;
    }

    PointerByReference native_set_volume(float new_volume) {

        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;
        PointerByReference /* pa_context */ context = PulseaudioLibrary.INSTANCE.pa_stream_get_context(stream);
        assert context != null;

        int stream_id = PulseaudioLibrary.INSTANCE.pa_stream_get_index(stream);
        int channels = PulseaudioLibrary.INSTANCE.pa_stream_get_sample_spec(stream).channels;
        pa_cvolume cv = new pa_cvolume();

        PointerByReference /* pa_operation */ o = PulseaudioLibrary.INSTANCE.pa_context_set_sink_input_volume(context, stream_id, PulseaudioLibrary.INSTANCE.pa_cvolume_set(cv, channels, (int) new_volume), set_sink_input_volume_callback, null);
        assert o != null;

        return o;
    }

    private pa_sink_input_info_cb_t get_sink_input_volume_callback = (context, i, eol, userdata) -> {
        assert context != null;

        if (eol == 0) {
            update_channels_and_volume(i.volume.channels, (float) i.volume.values[0]);
        } else {
            synchronized (EventLoop.getEventLoop().threadLock) {
                EventLoop.getEventLoop().threadLock.notifyAll();
            }
        }
    };

    PointerByReference native_update_volume() {

        PointerByReference /* pa_stream */ stream = this.streamPointer;
        assert stream != null;

        int sink_input_index = PulseaudioLibrary.INSTANCE.pa_stream_get_index(stream);

        PointerByReference /* pa_context */ context = PulseaudioLibrary.INSTANCE.pa_stream_get_context(stream);
        assert context != null;

        PointerByReference /* pa_operation */ o = PulseaudioLibrary.INSTANCE.pa_context_get_sink_input_info(context, sink_input_index, get_sink_input_volume_callback, null);
        assert o != null;
        return o;
    }

    /*
     * pa_operation pa_stream_proplist_update (pa_streams, pa_update_mode_t
     * mode, pa_proplistp, pa_stream_success_cb_t cb, voiduserdata) Update the
     * property list of the sink input/source output of this stream, adding new
     * entries. pa_operation pa_stream_proplist_remove (pa_streams, const char
     * const keys[], pa_stream_success_cb_t cb, voiduserdata) Update the
     * property list of the sink input/source output of this stream, remove
     * entries. int pa_stream_set_monitor_stream (pa_streams, uint32_t
     * sink_input_idx) For record streams connected to a monitor source: monitor
     * only a very specific sink input of the sink. uint32_t
     * pa_stream_get_monitor_stream (pa_streams) Return what has been set with
     * pa_stream_set_monitor_stream() ebfore.
     */

    Stream(PointerByReference contextPointer, String name, Format format, int sampleRate, int channels) {
        // logger.log(Level.TRACE, "format: " + format.toString());

        stateListeners = new LinkedList<>();
        writeListeners = new LinkedList<>();
        readListeners = new LinkedList<>();
        overflowListeners = new LinkedList<>();
        underflowListeners = new LinkedList<>();
        playbackStartedListeners = new LinkedList<>();
        latencyUpdateListeners = new LinkedList<>();
        movedListeners = new LinkedList<>();
        suspendedListeners = new LinkedList<>();
        corkListeners = new LinkedList<>();
        this.format = format;

        StreamSampleSpecification spec = new StreamSampleSpecification(format,
                sampleRate, channels);

        native_pa_stream_new(contextPointer, name, spec.getFormat().toString(),
                spec.getRate(), spec.getChannels());
    }

    void addStateListener(StateListener listener) {
        synchronized (stateListeners) {
            stateListeners.add(listener);
        }
    }

    void removeStateListener(StateListener listener) {
        synchronized (stateListeners) {
            stateListeners.remove(listener);
        }
    }

    void addWriteListener(WriteListener listener) {
        synchronized (writeListeners) {
            writeListeners.add(listener);
        }
    }

    void removeWriteListener(WriteListener listener) {
        synchronized (writeListeners) {
            writeListeners.remove(listener);
        }
    }

    void addReadListener(ReadListener listener) {
        synchronized (readListeners) {
            readListeners.add(listener);
        }
    }

    void removeReadListener(ReadListener listener) {
        synchronized (readListeners) {
            readListeners.remove(listener);
        }
    }

    void addOverflowListener(OverflowListener listener) {
        synchronized (overflowListeners) {
            overflowListeners.add(listener);
        }
    }

    void removeOverflowListener(OverflowListener listener) {
        synchronized (overflowListeners) {
            overflowListeners.remove(listener);
        }
    }

    void addUnderflowListener(UnderflowListener listener) {
        synchronized (underflowListeners) {
            underflowListeners.add(listener);
        }
    }

    void removeUnderflowListener(UnderflowListener listener) {
        synchronized (underflowListeners) {
            underflowListeners.remove(listener);
        }
    }

    void addCorkListener(CorkListener listener) {
        synchronized (corkListeners) {
            corkListeners.add(listener);
        }
    }

    void removeCorkListener(CorkListener listener) {
        synchronized (corkListeners) {
            corkListeners.remove(listener);
        }
    }

    void addPlaybackStartedListener(PlaybackStartedListener listener) {
        synchronized (playbackStartedListeners) {
            playbackStartedListeners.add(listener);
        }
    }

    void removePlaybackStartedListener(PlaybackStartedListener listener) {
        synchronized (playbackStartedListeners) {
            playbackStartedListeners.remove(listener);
        }
    }

    void addLatencyUpdateListener(LatencyUpdateListener listener) {
        synchronized (latencyUpdateListeners) {
            latencyUpdateListeners.add(listener);
        }
    }

    void removeLatencyUpdateListener(LatencyUpdateListener listener) {
        synchronized (playbackStartedListeners) {
            latencyUpdateListeners.remove(listener);
        }
    }

    void addMovedListener(MovedListener listener) {
        synchronized (movedListeners) {
            movedListeners.add(listener);
        }
    }

    void removeMovedListener(MovedListener listener) {
        synchronized (movedListeners) {
            movedListeners.remove(listener);
        }
    }

    void addSuspendedListener(SuspendedListener listener) {
        synchronized (suspendedListeners) {
            suspendedListeners.add(listener);
        }
    }

    void removeSuspendedListener(SuspendedListener listener) {
        synchronized (suspendedListeners) {
            suspendedListeners.remove(listener);
        }
    }

    long getState() {
        return checkNativeStreamState(native_pa_stream_get_state());
    }

    PointerByReference getContextPointer() {
        return native_pa_stream_get_context();
    }

    int getSinkInputIndex() {
        return native_pa_stream_get_index();
    }

    /**
     * @return the index of the sink or source this stream is connected to in
     * the server
     */
    int getDeviceIndex() {
        return native_pa_stream_get_device_index();
    }

    private void setBufAttr() {
        synchronized (bufAttrMutex) {
            bufAttr = native_pa_stream_get_buffer_attr();
        }
    }

    @SuppressWarnings("unused")
    private void bufferAttrCallback() {
        setBufAttr();
    }

    int getBufferSize() {
        synchronized (bufAttrMutex) {
            return bufAttr.getMaxLength();
        }
    }

    /**
     * @return the name of the sink or source this stream is connected to in the
     * server
     */
    String getDeviceName() {
        return native_pa_stream_get_device_name();
    }

    /**
     * if the sink or source this stream is connected to has been suspended.
     *
     * @return
     */
    boolean isSuspended() {
        return (native_pa_stream_is_suspended() != 0);
    }

    /**
     * Connect the stream to a sink
     *
     * @param deviceName the device to connect to. use
     *                   <code>null</code> for the default device
     * @throws LineUnavailableException
     */
    void connectForPlayback(String deviceName, StreamBufferAttributes bufferAttributes, PointerByReference syncStreamPointer)
            throws LineUnavailableException {

        int returnValue = native_pa_stream_connect_playback(
                deviceName,
                bufferAttributes.getMaxLength(),
                bufferAttributes.getTargetLength(),
                bufferAttributes.getPreBuffering(),
                bufferAttributes.getMinimumRequest(),
                bufferAttributes.getFragmentSize(),
                FLAG_START_CORKED, null, syncStreamPointer
        );
        if (returnValue < 0) {
            throw new LineUnavailableException("Unable To connect a line for playback");
        }
    }

    /**
     * Connect the stream to a source.
     *
     * @throws LineUnavailableException
     */
    void connectForRecording(String deviceName, long flags, StreamBufferAttributes bufferAttributes)
            throws LineUnavailableException {

        int returnValue = native_pa_stream_connect_record(
                deviceName,
                bufferAttributes.getMaxLength(),
                bufferAttributes.getTargetLength(),
                bufferAttributes.getPreBuffering(),
                bufferAttributes.getMinimumRequest(),
                bufferAttributes.getFragmentSize(),
                flags, null, null
        );
        if (returnValue < 0) {
            throw new LineUnavailableException("Unable to connect line for recording");
        }
    }

    /**
     * Disconnect a stream from a source/sink.
     */
    void disconnect() {
        int returnValue = native_pa_stream_disconnect();
        assert (returnValue == 0);
    }

    /**
     * Write data to the server
     *
     * @param data
     * @param length
     * @return
     */
    int write(byte[] data, int offset, int length) {
        return native_pa_stream_write(data, offset, length);
    }

    /**
     * Read the next fragment from the buffer (for recording).
     */
    byte[] peek() {
        return native_pa_stream_peek();
    }

    /**
     * Remove the current fragment on record streams.
     */
    void drop() {
        native_pa_stream_drop();
    }

    /**
     * Return the number of bytes that may be written using write().
     *
     * @return
     */
    int getWritableSize() {
        return native_pa_stream_writable_size();
    }

    /**
     * Return the number of bytes that may be read using peek().
     *
     * @return
     */
    int getReableSize() {
        return native_pa_stream_readable_size();
    }

    /**
     * Drain a playback stream
     *
     * @return
     */
    Operation drain() {
        Operation drainOperation = new Operation(native_pa_stream_drain());
        return drainOperation;
    }

    Operation updateTimingInfo() {
        Operation updateOperation = new Operation(
                native_pa_stream_updateTimingInfo());
        return updateOperation;
    }

    /**
     * this function is called whenever the state changes
     */
    @SuppressWarnings("unused")
    private void stateCallback() {
        synchronized (EventLoop.getEventLoop().threadLock) {
            if (getState() == Stream.STATE_READY) {
                setBufAttr();
            }
        }
        synchronized (stateListeners) {
            for (StateListener listener : stateListeners) {
                listener.update();
            }
        }
    }

    @SuppressWarnings("unused")
    private void writeCallback() {
        synchronized (writeListeners) {
            for (WriteListener listener : writeListeners) {
                listener.update();
            }
        }
    }

    @SuppressWarnings("unused")
    private void readCallback() {
        synchronized (readListeners) {
            for (ReadListener listener : readListeners) {
                listener.update();
            }
        }
    }

    @SuppressWarnings("unused")
    private void overflowCallback() {
        logger.log(Level.TRACE, "overflowCallback called");
        synchronized (overflowListeners) {
            for (OverflowListener listener : overflowListeners) {
                listener.update();
            }
        }
    }

    @SuppressWarnings("unused")
    private void underflowCallback() {
        synchronized (underflowListeners) {
            for (UnderflowListener listener : underflowListeners) {
                listener.update();
            }
        }
    }

    /**
     * callback function that is called when a the server starts playback after
     * an underrun or on initial startup
     */
    @SuppressWarnings("unused")
    private void playbackStartedCallback() {
        synchronized (playbackStartedListeners) {
            for (PlaybackStartedListener listener : playbackStartedListeners) {
                listener.update();
            }
        }
    }

    /**
     * called whenever a latency information update happens
     */
    @SuppressWarnings("unused")
    private void latencyUpdateCallback() {
        synchronized (latencyUpdateListeners) {
            for (LatencyUpdateListener listener : latencyUpdateListeners) {
                listener.update();
            }
        }
    }

    /**
     * whenever the stream is moved to a different sink/source
     */
    @SuppressWarnings("unused")
    private void movedCallback() {
        synchronized (movedListeners) {
            for (MovedListener listener : movedListeners) {
                listener.update();
            }
        }
    }

    @SuppressWarnings("unused")
    private void corkCallback() {
        synchronized (corkListeners) {
            for (CorkListener listener : corkListeners) {
                listener.update();
            }
        }
    }

    /**
     * whenever the sink/source this stream is connected to is suspended or
     * resumed
     */
    @SuppressWarnings("unused")
    private void suspendedCallback() {
        synchronized (suspendedListeners) {
            for (SuspendedListener listener : suspendedListeners) {
                listener.update();
            }
        }
    }

    boolean isCorked() {
        int corked = native_pa_stream_is_corked();
        if (corked < 0) {
            throw new IllegalStateException("Unable to determine state");
        }
        return corked != 0;
    }

    /**
     * Pause (or resume) playback of this stream temporarily.
     *
     * @param cork
     * @return
     */
    Operation cork(boolean cork) {
        int yes = cork ? 1 : 0;
        Operation corkOperation = new Operation(native_pa_stream_cork(yes));
        return corkOperation;
    }

    Operation cork() {
        return cork(true);
    }

    Operation unCork() {
        return cork(false);
    }

    /**
     * Flush the playback buffer of this stream.
     *
     * @return
     */
    Operation flush() {
        Operation flushOperation = new Operation(native_pa_stream_flush());
        return flushOperation;
    }

    /*
     * Operation pa_stream_prebuf (pa_streams, pa_stream_success_cb_t cb, void
     * userdata)
     *
     * Reenable prebuffering as specified in the pa_buffer_attr structure.
     */

    /**
     * Request immediate start of playback on this stream.
     */
    Operation triggerStart() {
        Operation triggerOperation = new Operation(native_pa_stream_trigger());
        return triggerOperation;
    }

    /**
     * set the stream's name
     *
     * @param name
     * @return
     */
    Operation setName(String name) {
        Operation setNameOperation = new Operation(native_pa_stream_set_name(name));
        return setNameOperation;
    }

    /**
     * @return a time between ? and ?
     */
    long getTime() {
        return native_pa_stream_get_time();
    }

    /**
     * @return the total stream latency in microseconds
     */
    long getLatency() {
        return native_pa_stream_get_latency();
    }

    /*
     * const pa_timing_info pa_stream_get_timing_info (pa_streams) Return the
     * latest raw timing data structure.
     */

    Format getFormat() {
        return format;
    }

    /*
     * const pa_channel_map pa_stream_get_channel_map (pa_streams) Return a
     * pointer to the stream's channel map.
     */

    StreamBufferAttributes getBufferAttributes() {
        return native_pa_stream_get_buffer_attr();
    }

    Operation setBufferAtrributes(StreamBufferAttributes attr) {
        return new Operation(native_pa_stream_set_buffer_attr(attr));
    }

    /**
     * Change the stream sampling rate during playback.
     */
    Operation updateSampleRate(int rate) {
        return new Operation(native_pa_stream_update_sample_rate(rate));
    }

    PointerByReference getStreamPointer() {
        return streamPointer;
    }

    void free() {
        native_pa_stream_unref();
    }

    float getCachedVolume() {
        return this.cachedVolume;
    }

    void setCachedVolume(float volume) {
        this.cachedVolume = volume;
    }

    void update_channels_and_volume(int channels, float volume) {
        this.cachedVolume = volume;
    }
}

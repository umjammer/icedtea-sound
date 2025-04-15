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
import javax.sound.sampled.Line;
import javax.sound.sampled.Port;
import com.sun.jna.ptr.PointerByReference;

import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_context_success_cb_t;
import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary.pa_sink_info_cb_t;
import vavi.sound.pulseaudio.jna.pulseaudio.pa_cvolume;

import static java.lang.System.getLogger;


final class PulseAudioTargetPort extends PulseAudioPort {

    private static final Logger logger = getLogger(PulseAudioTargetPort.class.getName());

    /** aka speaker */
    PulseAudioTargetPort(String name) {
        super(name);
    }

    @Override
    public void open() {

        super.open();

        PulseAudioMixer parent = PulseAudioMixer.getInstance();
        parent.addTargetLine(this);
    }

    @Override
    public void close() {

        if (!isOpen) {
            throw new IllegalStateException("not open, so cant close Port");
        }

        PulseAudioMixer parent = PulseAudioMixer.getInstance();
        parent.removeTargetLine(this);

        super.close();
    }

    private pa_context_success_cb_t sink_callback = (context, success, userdata) -> {
        synchronized (EventLoop.getEventLoop().threadLock) {
            EventLoop.getEventLoop().threadLock.notifyAll();
        }
    };

    private pa_sink_info_cb_t get_sink_volume_callback = (context, i, eol, userdata) -> {
        assert context != null;

        if (eol == 0) {
            logger.log(Level.TRACE, i.name);
            update_channels_and_volume(i.volume.channels, (float) i.volume.values[0]);
        } else {
            synchronized (EventLoop.getEventLoop().threadLock) {
                EventLoop.getEventLoop().threadLock.notifyAll();
            }
        }
    };

    // FIXME
    @Override
    public PointerByReference native_set_volume(float newValue) {
        if (name == null) {
            return null;    // return oome
        }

        PointerByReference /* pa_context */ context = this.contextPointer;
        assert context != null;

        pa_cvolume cv = new pa_cvolume();
        PointerByReference /* pa_operation */ o = PulseaudioLibrary.INSTANCE.pa_context_set_sink_volume_by_name(context, name, PulseaudioLibrary.INSTANCE.pa_cvolume_set(cv, channels, (int) newValue), sink_callback, null);

        return o;
    }

    // FIXME
    @Override
    public PointerByReference native_update_volume() {
        if (name == null) {
            return null;    // oome
        }

        PointerByReference /* pa_context */ context = this.contextPointer;
        assert context != null;

        PointerByReference /* pa_operation */ o = PulseaudioLibrary.INSTANCE.pa_context_get_sink_info_by_name(context, name, get_sink_volume_callback, null);
        assert o != null;

        return o;
    }

    @Override
    public Line.Info getLineInfo() {
        return new Port.Info(Port.class, getName(), false);
    }
}

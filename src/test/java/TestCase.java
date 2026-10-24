/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import com.sun.jna.ptr.PointerByReference;

import vavi.sound.pulseaudio.jna.pulseaudio.PulseaudioLibrary;
import vavi.sound.pulseaudio.jna.pulseaudio.pa_mainloop_api;

import org.junit.jupiter.api.Test;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-04-19 nsano initial version <br>
 */
public class TestCase {

    @Test
    void test1() throws Exception {
        PointerByReference mainloop = PulseaudioLibrary.INSTANCE.pa_mainloop_new();
        pa_mainloop_api mainloop_api = PulseaudioLibrary.INSTANCE.pa_mainloop_get_api(mainloop);
        PointerByReference context = PulseaudioLibrary.INSTANCE.pa_context_new(mainloop_api, "Test");
    }
}

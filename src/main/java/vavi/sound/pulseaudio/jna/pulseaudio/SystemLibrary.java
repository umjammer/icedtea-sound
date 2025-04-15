/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pulseaudio.jna.pulseaudio;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.PointerType;


/**
 * SystemLibrary.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-04-15 nsano initial version <br>
 */
public interface SystemLibrary extends Library {

    SystemLibrary INSTANCE = Native.load("c", SystemLibrary.class);

    int poll(PointerType ufds, NativeLong nfds, int timeout);
}

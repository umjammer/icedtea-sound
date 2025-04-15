/* OtherSoundProvidersAvailableTest.java
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
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.Test;

import static java.lang.System.getLogger;


public class OtherSoundProvidersAvailableTest {

    private static final Logger logger = getLogger(OtherSoundProvidersAvailableTest.class.getName());

    @Test
    public void testOtherSoundProviders() {
        logger.log(Level.TRACE, "This tests if alsa mixers are still available");

        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        Mixer.Info selectedMixerInfo = null;
        Mixer mixer;

        boolean selected = false;
        int i = 0;
        logger.log(Level.TRACE, "Available Mixers:");
        // use 0,0 or the default
        for (Mixer.Info info : mixerInfos) {
            logger.log(Level.TRACE, "Mixer Line " + i++ + ": " + info.getName()
                    + " " + info.getDescription());
            if (info.getName().contains("0,0") && !selected) {
                logger.log(Level.TRACE, "^ selecting as the mixer to use");
                selectedMixerInfo = info;
                selected = true;
            }
        }

        if (selectedMixerInfo != null) {
            logger.log(Level.TRACE, selectedMixerInfo.toString());
        }

        logger.log(Level.TRACE, "Selected mixer is of class: ");

        mixer = AudioSystem.getMixer(selectedMixerInfo);
        logger.log(Level.TRACE, mixer.getClass().toString());
        try {
            Line.Info sourceDataLineInfo = null;

            mixer.open(); // initialize the mixer

            Line.Info[] allLineInfo = mixer.getSourceLineInfo();
            logger.log(Level.TRACE, "Source lines supported by mixer: ");
            int j = 0;
            for (Line.Info lineInfo : allLineInfo) {
                logger.log(Level.TRACE, "Source Line " + j++ + ": " + lineInfo.getLineClass());
                if (lineInfo.toString().contains("SourceDataLine")) {
                    sourceDataLineInfo = lineInfo;
                }
            }

            if (sourceDataLineInfo == null) {
                logger.log(Level.TRACE, "Mixer supports no SourceDataLines");
            } else {
                SourceDataLine sourceDataLine = (SourceDataLine) mixer.getLine(sourceDataLineInfo);

                sourceDataLine.open();
                // sourceDataLine.write('a', 0, 2);
                sourceDataLine.close();
            }
        } catch (LineUnavailableException e) {
            logger.log(Level.TRACE, "Line unavailable");
        } finally {
            mixer.close();
        }
    }
}

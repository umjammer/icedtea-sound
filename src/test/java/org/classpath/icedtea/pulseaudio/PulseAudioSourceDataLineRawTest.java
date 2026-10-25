/* PulseAudioSourceDataLineRawTest.java
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

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static java.lang.System.getLogger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
public class PulseAudioSourceDataLineRawTest {

    private static final Logger logger = getLogger(PulseAudioSourceDataLineRawTest.class.getName());

    PulseAudioMixer mixer = null;

    volatile int started = 0;
    volatile int stopped = 0;

    final AudioFormat aSupportedFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_UNSIGNED, 44100f, 8, 1, 1, 44100f, true);

    static class ThreadWriter extends Thread {

        final PulseAudioSourceDataLine line;
        final AudioInputStream stream;

        public ThreadWriter(AudioInputStream stream, PulseAudioSourceDataLine line) throws LineUnavailableException {

            this.line = line;
            this.stream = stream;

            if (line.isOpen()) {
                line.close();
            }
        }

        @Override
        public void run() {
            try {
                AudioFormat audioFormat = stream.getFormat();

                line.open(audioFormat);

                byte[] abData = new byte[1000];
                int bytesRead = 0;

                line.start();

                while (bytesRead >= 0) {
                    bytesRead = stream.read(abData, 0, abData.length);
                    // logger.log(Level.TRACE, "read data");
                    if (bytesRead > 0) {
                        // logger.log(Level.TRACE, "about to write data");
                        line.write(abData, 0, bytesRead);
                        // logger.log(Level.TRACE, "wrote data");
                    }
                }

                line.drain();
                line.close();

            } catch (LineUnavailableException | IOException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }
    }

    @BeforeEach
    public void setUp() throws LineUnavailableException {

        mixer = PulseAudioMixer.getInstance();
        if (mixer.isOpen()) {
            mixer.close();
        }

        mixer.open();

        started = 0;
        stopped = 0;
    }

    @Test
    public void testStartAndStopEventsOnCork() throws UnsupportedAudioFileException, IOException,
            LineUnavailableException, InterruptedException {

        System.out.println("This test checks if START and STOP notifications appear on corking");

        File soundFile = new File("src/test/resources/testsounds/startup.wav");
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        AudioFormat audioFormat = audioInputStream.getFormat();

        PulseAudioSourceDataLine line;
        line = (PulseAudioSourceDataLine) mixer.getLine(new DataLine.Info(SourceDataLine.class, audioFormat));
        assertNotNull(line);

        LineListener startStopListener = event -> {
            if (event.getType() == LineEvent.Type.START) {
                logger.log(Level.TRACE, "START");
                started++;
            }

            if (event.getType() == LineEvent.Type.STOP) {
                logger.log(Level.TRACE, "STOP");
                stopped++;
            }
        };

        line.addLineListener(startStopListener);
        logger.log(Level.TRACE, "Launching threadWriter");
        ThreadWriter writer = new ThreadWriter(audioInputStream, line);
        writer.start();
        //logger.log(Level.TRACE, "started");

        // playback may start late (e.g. while a suspended sink resumes)
        for (int i = 0; i < 50 && started == 0; i++) {
            Thread.sleep(100);
        }
        Thread.sleep(1000);

        // CORK
        line.stop();

        Thread.sleep(1000);

        // UNCORK
        line.start();

        Thread.sleep(1000);

        //logger.log(Level.TRACE, "waiting for thread to finish");
        writer.join();

        assertEquals(2, started);
        assertEquals(2, stopped);
    }

    @Test
    public void testVolume() throws Exception {

        Mixer selectedMixer = mixer;
        SourceDataLine line = (SourceDataLine) selectedMixer.getLine(new Line.Info(SourceDataLine.class));

        File soundFile = new File(new java.io.File(".").getCanonicalPath()
                + "/src/test/resources/testsounds/logout.wav");
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        AudioFormat audioFormat = audioInputStream.getFormat();

        line.open(audioFormat);
        line.start();
        PulseAudioVolumeControl volume = (PulseAudioVolumeControl) line.getControl(FloatControl.Type.VOLUME);

        volume.setValue(PulseAudioVolumeControl.MAX_VOLUME);

        byte[] abData = new byte[1000];
        int bytesRead = 0;

        while (bytesRead >= 0) {
            bytesRead = audioInputStream.read(abData, 0, abData.length);
            if (bytesRead > 0) {
                line.write(abData, 0, bytesRead);
            }
        }

        line.drain();
        line.close();
        selectedMixer.close();

    }

    @Test
    public void testSettingStreamName() throws LineUnavailableException,
            UnsupportedAudioFileException, IOException {
        File soundFile = new File("src/test/resources/testsounds/logout.wav");
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        AudioFormat audioFormat = audioInputStream.getFormat();

        PulseAudioSourceDataLine line;
        line = (PulseAudioSourceDataLine) mixer.getLine(new DataLine.Info(SourceDataLine.class, audioFormat));

        String name = "Knights Who Say ... Oh my god, i am so sorry, i didnt mean it...";
        line.setName(name);

        line.open(audioFormat);
        line.start();

        byte[] abData = new byte[1000];
        int bytesRead = 0;

        while (bytesRead >= 0) {
            bytesRead = audioInputStream.read(abData, 0, abData.length);
            if (bytesRead > 0) {
                line.write(abData, 0, bytesRead);
            }
        }

        assertEquals(name, line.getName());
        //
        // FIXME test that PulseAudio also knows this correctly using
        // introspection
        //

        line.drain();
        line.stop();
        line.close();
    }

    @Test
    public void messWithStreams() throws LineUnavailableException {
        System.out.println("This test tries to unCork a stream which hasnt been corked");

        PulseAudioSourceDataLine line = (PulseAudioSourceDataLine) mixer
                .getLine(new DataLine.Info(SourceDataLine.class, aSupportedFormat, 1000));

        line.open();
        line.start();
        Stream s = line.getStream();
        Operation o;
        synchronized (EventLoop.getEventLoop().threadLock) {
            o = s.unCork();
        }
        o.waitForCompletion();
        o.releaseReference();
        line.stop();
        line.close();
    }

    @AfterEach
    public void tearDown() {
    }
}

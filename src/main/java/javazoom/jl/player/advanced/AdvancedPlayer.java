/*
 * 11/19/04		1.0 moved to LGPL.
 *-----------------------------------------------------------------------
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */

package javazoom.jl.player.advanced;

import java.io.InputStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import javax.sound.sampled.FloatControl; // Import FloatControl

/**
 * a hybrid of javazoom.jl.player.Player tweeked to include <code>play(startFrame, endFrame)</code>
 * hopefully this will be included in the api
 */
public class AdvancedPlayer
{
    /** The MPEG audio bitstream.*/
    private Bitstream bitstream;
    /** The MPEG audio decoder. */
    private Decoder decoder;
    /** The AudioDevice the audio samples are written to. */
    private AudioDevice audio;
    /** Has the player been closed? */
    private boolean closed = false;
    /** Has the player played back all frames from the stream? */
    private boolean complete = false;
    private int lastPosition = 0;
    /** Listener for the playback process */
    private PlaybackListener listener;

    /**
     * Creates a new <code>Player</code> instance.
     */
    public AdvancedPlayer(InputStream stream) throws JavaLayerException
    {
        this(stream, null);
    }

    public AdvancedPlayer(InputStream stream, AudioDevice device) throws JavaLayerException
    {
        bitstream = new Bitstream(stream);
        //decoder = new Decoder(); // Decoder is typically initialized when audio device is opened

        if (device!=null) {
            audio = device;
        } else {
            audio = FactoryRegistry.systemRegistry().createAudioDevice();
        }
        // It's common for the AudioDevice's open method to take the Decoder
        // or for the Decoder to be configured with parameters obtained from the AudioDevice
        // For JLayer, the Decoder is typically passed to audio.open()
        decoder = new Decoder();
        audio.open(decoder);
    }

    public void play() throws JavaLayerException
    {
        play(Integer.MAX_VALUE);
    }

    /**
     * Plays a number of MPEG audio frames.
     *
     * @param frames	The number of frames to play.
     * @return	true if the last frame was played, or false if there are
     *			more frames.
     */
    public boolean play(int frames) throws JavaLayerException
    {
        boolean ret = true;

        // report to listener
        if(listener != null) listener.playbackStarted(createEvent(PlaybackEvent.STARTED));

        while (frames-- > 0 && ret)
        {
            ret = decodeFrame();
        }
        {
            AudioDevice out = audio;
            if (out != null)
            {
                out.flush();
                synchronized (this)
                {
                    complete = (!closed && !ret);
                }
                if(listener != null && complete) {
                    listener.playbackFinished(createEvent(out, PlaybackEvent.STOPPED));
                }
            }
        }
        return ret;
    }

    /**
     * Closes this player. Any audio currently playing is stopped
     * immediately.
     */
    public synchronized void close()
    {
        AudioDevice out = audio;
        if (out != null)
        {
            closed = true;
            out.close();
            lastPosition = out.getPosition();
            audio = null;
            decoder = null;
            try
            {
                if (bitstream != null) {
                    bitstream.close();
                }
            }
            catch (BitstreamException ex)
            {
                ex.printStackTrace();
            }
            bitstream = null;
        }
    }

    /**
     * Decodes a single frame.
     *
     * @return true if there are no more frames to decode, false otherwise.
     */
    protected boolean decodeFrame() throws JavaLayerException
    {
        try
        {
            AudioDevice out = audio;
            if (out == null || closed) return false;

            Header h = bitstream.readFrame();
            if (h == null) return false;

            if (decoder == null) {
                throw new JavaLayerException("Decoder not initialized");
            }
            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);

            synchronized (this)
            {
                out = audio;
                if(out != null && !closed)
                {
                    out.write(output.getBuffer(), 0, output.getBufferLength());
                } else {
                    return false;
                }
            }

            bitstream.closeFrame();
        }
        catch (BitstreamException e) {
            if (e.getMessage() != null && e.getMessage().contains("END OF STREAM")) {
                return false;
            }
            throw new JavaLayerException("Exception decoding audio frame", e);
        }
        catch (RuntimeException ex)
        {
            throw new JavaLayerException("Exception decoding audio frame", ex);
        }
        return true;
    }

    /**
     * skips over a single frame
     * @return false	if there are no more frames to decode, true otherwise.
     */
    protected boolean skipFrame() throws JavaLayerException
    {
        if (bitstream == null || closed) return false;
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        return true;
    }

    /**
     * Plays a range of MPEG audio frames
     * @param start	The first frame to play
     * @param end		The last frame to play
     * @return true if the last frame was played, or false if there are more frames.
     */
    public boolean play(final int start, final int end) throws JavaLayerException
    {
        if (start < 0 || end < 0 || end < start) {
            throw new JavaLayerException("Invalid start/end frame parameters");
        }
        boolean ret = true;
        int framesToSkip = start;
        while (framesToSkip-- > 0 && ret) {
            ret = skipFrame();
        }
        if (!ret) {
            return false;
        }
        return play(end - start + 1);
    }

    /**
     * Constructs a <code>PlaybackEvent</code>
     */
    private PlaybackEvent createEvent(int id)
    {
        return createEvent(audio, id);
    }

    /**
     * Constructs a <code>PlaybackEvent</code>
     */
    private PlaybackEvent createEvent(AudioDevice dev, int id)
    {
        // Ensure dev is not null before calling getPosition
        int position = (dev != null) ? dev.getPosition() : lastPosition;
        return new PlaybackEvent(this, id, position);
    }

    /**
     * sets the <code>OggPlaybackListener</code>
     */
    public void setPlayBackListener(PlaybackListener listener)
    {
        this.listener = listener;
    }

    /**
     * gets the <code>OggPlaybackListener</code>
     */
    public PlaybackListener getPlayBackListener()
    {
        return listener;
    }

    /**
     * Stops the player and notifies the <code>OggPlaybackListener</code>.
     * This method stops playback and prepares for potential closing,
     * but doesn't release all resources like close() does.
     * The original `stop()` method called `close()`, which might be too aggressive
     * if a resume functionality is desired.
     */
    public synchronized void stop()
    {
        if (!closed) {
            if (listener != null) {
                listener.playbackFinished(createEvent(PlaybackEvent.STOPPED));
            }
            close();
        }
    }

    /**
     * Attempts to set the volume of the audio output.
     * Note: This functionality depends on the underlying AudioDevice supporting
     * volume control via FloatControl.Type.MASTER_GAIN or FloatControl.Type.VOLUME.
     * Not all AudioDevice implementations may support this.
     *
     * @param volumeDB A value typically between 0.0f (silent) and 1.0f (full volume)
     *               if using FloatControl.Type.VOLUME.
     *               Or a dB value if using FloatControl.Type.MASTER_GAIN.
     *               This method provides a basic example for MASTER_GAIN.
     */
    public synchronized void setVolume(float volumeDB) {
        AudioDevice out = audio;
        if (!closed && out instanceof javax.sound.sampled.SourceDataLine sdl) {
            try {
                if (sdl.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) sdl.getControl(FloatControl.Type.MASTER_GAIN);
                    float min = gainControl.getMinimum();
                    float max = gainControl.getMaximum();
                    if (volumeDB < min) volumeDB = min;
                    if (volumeDB > max) volumeDB = max;
                    gainControl.setValue(volumeDB);
                } else if (sdl.isControlSupported(FloatControl.Type.VOLUME)) {
                    FloatControl volCtrl = (FloatControl) sdl.getControl(FloatControl.Type.VOLUME);
                    float minVol = volCtrl.getMinimum();
                    float maxVol = volCtrl.getMaximum();
                    float valueToSet = Math.max(minVol, Math.min(volumeDB, maxVol));
                    volCtrl.setValue(valueToSet);
                } else {
                    System.out.println("Volume control not supported by this AudioDevice.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Checks if the player has finished playing all frames.
     * @return true if playback is complete and the player is not closed, false otherwise.
     */
    public synchronized boolean isComplete() {
        return complete;
    }

    /**
     * Checks if the player is closed.
     * @return true if the player is closed, false otherwise.
     */
    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Gets the last known position of the audio device in frames.
     * @return The last known position or 0 if not available.
     */
    public int getLastPosition() {
        return lastPosition;
    }
}
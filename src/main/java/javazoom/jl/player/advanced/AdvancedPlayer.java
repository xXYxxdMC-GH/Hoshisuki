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

//		if (!ret) // This condition seems to be always true if loop finishes due to frames--
        // It should rather check if playback was stopped for other reasons or completed
        // The original logic seems to always flush and close if play(frames) is called.
        {
            // last frame, ensure all data flushed to the audio device.
            AudioDevice out = audio;
            if (out != null)
            {
//				System.out.println(audio.getPosition());
                out.flush();
//				System.out.println(audio.getPosition());
                synchronized (this)
                {
                    // If ret is false, it means decodeFrame returned false (end of stream)
                    complete = (!closed && !ret); // Mark complete if not closed and end of stream reached
                    // close(); // Closing here means it cannot be resumed or played further.
                    // This might be intended, but for a player, often 'stop' is different from 'close permanently'
                }

                // report to listener only if playback actually finished due to end of stream or explicit stop
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
            // Stop playback before closing the device
            // This ensures that any ongoing audio processing is halted.
            // However, the original 'stop()' method also calls 'close()'.
            // If 'close()' is meant to be a full resource release, this is okay.
            out.close(); // Close the audio device first
            lastPosition = out.getPosition(); // Get position before nullifying audio
            audio = null; // Nullify after use
            decoder = null; // Also nullify decoder
            try
            {
                if (bitstream != null) { // Check if bitstream is not null before closing
                    bitstream.close();
                }
            }
            catch (BitstreamException ex)
            {
                // Log or handle exception
                ex.printStackTrace();
            }
            bitstream = null; // Nullify after closing
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
            if (out == null || closed) return false; // if closed or audio is null, cannot decode

            Header h = bitstream.readFrame();
            if (h == null) return false; // End of stream

            // Ensure decoder is available
            if (decoder == null) {
                // This case should ideally not happen if player is properly initialized
                throw new JavaLayerException("Decoder not initialized");
            }
            // sample buffer set when decoder constructed
            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);

            synchronized (this)
            {
                out = audio; // Re-fetch in case it was closed by another thread
                if(out != null && !closed) // Check again before writing
                {
                    out.write(output.getBuffer(), 0, output.getBufferLength());
                } else {
                    return false; // Audio device closed or player closed during decode
                }
            }

            bitstream.closeFrame();
        }
        catch (BitstreamException e) {
            // If it's an end-of-stream related BitstreamException, it might be normal
            // For other BitstreamExceptions, rethrow as JavaLayerException
            if (e.getMessage() != null && e.getMessage().contains("END OF STREAM")) { // Heuristic
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
        if (!ret) { // Could not skip to start frame
            return false;
        }
        return play(end - start + 1); // Play 'frameCount' frames, so (end - start + 1)
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
     * sets the <code>PlaybackListener</code>
     */
    public void setPlayBackListener(PlaybackListener listener)
    {
        this.listener = listener;
    }

    /**
     * gets the <code>PlaybackListener</code>
     */
    public PlaybackListener getPlayBackListener()
    {
        return listener;
    }

    /**
     * Stops the player and notifies the <code>PlaybackListener</code>.
     * This method stops playback and prepares for potential closing,
     * but doesn't release all resources like close() does.
     * The original `stop()` method called `close()`, which might be too aggressive
     * if a resume functionality is desired.
     */
    public synchronized void stop()
    {
        if (!closed) { // Only stop if not already closed
            // Notify listener that playback is stopping.
            // The original code created event with PlaybackEvent.STOPPED.
            // This is fine.
            if (listener != null) {
                listener.playbackFinished(createEvent(PlaybackEvent.STOPPED));
            }

            // The original `stop()` called `close()`.
            // If `stop()` is meant to be a "pause and can resume later" or "stop current track",
            // then `close()` might be too much as it releases all resources.
            // For now, let's keep the original behavior of calling close().
            // If a more sophisticated pause/resume is needed, this part needs rethinking.
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
        // 检查 audio device 是否有效、播放器是否关闭，以及 audio device 是否为 SourceDataLine 类型
        if (!closed && out instanceof javax.sound.sampled.SourceDataLine) {
            try {
                javax.sound.sampled.SourceDataLine sdl = (javax.sound.sampled.SourceDataLine) out;
                // 优先尝试 MASTER_GAIN 控制
                if (sdl.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) sdl.getControl(FloatControl.Type.MASTER_GAIN);
                    float min = gainControl.getMinimum(); // 获取最小dB值
                    float max = gainControl.getMaximum(); // 获取最大dB值
                    // 确保音量值在允许范围内
                    if (volumeDB < min) volumeDB = min;
                    if (volumeDB > max) volumeDB = max;
                    gainControl.setValue(volumeDB); // 设置音量 (dB)
                } else if (sdl.isControlSupported(FloatControl.Type.VOLUME)) {
                    // 如果 MASTER_GAIN 不支持，尝试 VOLUME 控制
                    // 注意：这里的 volumeDB 参数可能需要针对 0.0-1.0 的范围进行重新解释
                    FloatControl volCtrl = (FloatControl) sdl.getControl(FloatControl.Type.VOLUME);
                    // ... (此处代码块中有注释，说明需要进行dB到线性值的转换)
                    System.out.println("MASTER_GAIN not supported, VOLUME control might be available but requires different scaling for the 'volumeDB' parameter.");
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
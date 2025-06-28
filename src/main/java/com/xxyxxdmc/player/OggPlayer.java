package com.xxyxxdmc.player;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class OggPlayer {

    private static final int JOGG_BUFFER_SIZE = 4096;
    private volatile boolean stopRequested = false;

    private final List<OggPlaybackListener> listeners = new CopyOnWriteArrayList<>();

    private long lastProgressUpdateTime = 0;
    private static final long PROGRESS_UPDATE_INTERVAL_MS = 1000;
    private String currentFilePath;

    public void addPlaybackListener(OggPlaybackListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    public void removePlaybackListener() {
        this.listeners.clear();
    }

    public void removePlaybackListener(OggPlaybackListener listener) {
        this.listeners.remove(listener);
    }

    private void firePlaybackStopped(boolean dueToError) {
        for (OggPlaybackListener listener : listeners) {
            listener.onPlaybackStopped(this.currentFilePath, dueToError);
        }
    }

    private void firePlaybackFinished() {
        for (OggPlaybackListener listener : listeners) {
            listener.onPlaybackFinished(this.currentFilePath);
        }
    }

    private void firePlaybackError(OggPlayerException e) {
        for (OggPlaybackListener listener : listeners) {
            listener.onPlaybackError(this.currentFilePath, e);
        }
    }

    public void play(String filePath) throws OggPlayerException {
        this.stopRequested = false;
        this.currentFilePath = filePath;
        this.lastProgressUpdateTime = 0;

        SyncState syncState = new SyncState();
        StreamState streamState = new StreamState();
        Info info = new Info();
        Comment comment = new Comment();
        DspState dspState = new DspState();
        SourceDataLine outputLine = null;

        try (InputStream inputStream = new FileInputStream(filePath);
             BufferedInputStream bitStream = new BufferedInputStream(inputStream)) {

            syncState.init();
            info.init();
            comment.init();
            readOggHeaders(bitStream, syncState, streamState, info, comment);
            dspState.synthesis_init(info);
            Block block = new Block(dspState);
            block.init(dspState);
            AudioFormat audioFormat = new AudioFormat((float) info.rate, 16, info.channels, true, false);
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

            outputLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            outputLine.open(audioFormat);
            outputLine.start();

            decodeAndPlayStream(bitStream, syncState, streamState, info, dspState, block, outputLine);

            if (!stopRequested) {
                outputLine.drain();
                firePlaybackFinished();
            }

        } catch (IOException | LineUnavailableException | SecurityException e) {
            OggPlayerException oggEx = new OggPlayerException("Error during playback setup or IO: " + e.getMessage());
            firePlaybackError(oggEx);
            throw oggEx;
        } catch (OggPlayerException e) {
             firePlaybackError(e);
             throw e;
        } finally {
            if (outputLine != null) {
                outputLine.stop();
                outputLine.flush();
                outputLine.close();
            }
            streamState.clear();
            dspState.clear();
            syncState.clear();
            info.clear();
            comment.clear();
        }
    }

    /**
     * Call this method from another thread to stop the playback.
     */
    public void stop() {
        this.stopRequested = true;
    }

    private void readOggHeaders(BufferedInputStream bitStream, SyncState syncState, StreamState streamState,
                                Info info, Comment comment) throws IOException, OggPlayerException {
        Page page = new Page();
        Packet packet = new Packet();
        byte[] buffer;
        int bytesRead;

        boolean identificationHeaderRead = false;
        while (!identificationHeaderRead && !stopRequested) {
            int bufferIndex = syncState.buffer(JOGG_BUFFER_SIZE);
            buffer = syncState.data;
            bytesRead = bitStream.read(buffer, bufferIndex, JOGG_BUFFER_SIZE);
            syncState.wrote(bytesRead);

            if (syncState.pageout(page) != 1) {
                if (bytesRead < JOGG_BUFFER_SIZE && !stopRequested) throw new OggPlayerException("Could not read initial Ogg page or premature EOF.");
                if (!stopRequested) throw new OggPlayerException("Not enough data for an Ogg page in initial read.");
                return;
            }

            streamState.init(page.serialno());

            if (streamState.pagein(page) < 0 && !stopRequested) throw new OggPlayerException("Error reading first Ogg page (streamState.pagein).");
            if (streamState.packetout(packet) != 1 && !stopRequested) throw new OggPlayerException("Error reading first Ogg packet (streamState.packetout).");
            if (info.synthesis_headerin(comment, packet) < 0 && !stopRequested) throw new OggPlayerException("Error parsing Ogg identification header.");
            identificationHeaderRead = true;
        }
        if (stopRequested) return;

        int headersProcessed = 0;
        while (headersProcessed < 2 && !stopRequested) {
            int pageoutResult = syncState.pageout(page);
            if (pageoutResult == 0) {
                int bufferIndex = syncState.buffer(JOGG_BUFFER_SIZE);
                buffer = syncState.data;
                bytesRead = bitStream.read(buffer, bufferIndex, JOGG_BUFFER_SIZE);
                syncState.wrote(bytesRead);
                if (bytesRead == 0 && !stopRequested) throw new OggPlayerException("Premature EOF while reading Ogg headers.");
                if (stopRequested) return;
                continue;
            }
            if (pageoutResult < 0 && !stopRequested) throw new OggPlayerException("Corrupt Ogg page during header reading.");
            if (stopRequested) return;

            if (streamState.pagein(page) < 0 && !stopRequested) throw new OggPlayerException("Error in streamState.pagein() during header reading.");
            if (stopRequested) return;

            while (headersProcessed < 2 && !stopRequested) {
                int packetOutResult = streamState.packetout(packet);
                if (packetOutResult == 0) break;
                if (packetOutResult < 0) {
                    System.err.println("Warning: Corrupt Ogg packet during header reading. Attempting to skip.");
                    continue;
                }
                if (info.synthesis_headerin(comment, packet) < 0 && !stopRequested) throw new OggPlayerException("Error parsing Ogg comment or setup header.");
                if (stopRequested) return;
                headersProcessed++;
            }
        }
    }

    private void decodeAndPlayStream(BufferedInputStream bitStream, SyncState syncState, StreamState streamState,
                                     Info info, DspState dspState, Block block, SourceDataLine outputLine)
            throws IOException {
        Page page = new Page();
        Packet packet = new Packet();
        byte[] buffer;
        int bytesRead;

        boolean endOfStream = false;
        float[][][] pcmWorkspace = new float[1][][];
        int[] pcmChannelIndex = new int[info.channels];

        while (!endOfStream && !stopRequested) {
            long currentTimeMs = System.currentTimeMillis();
            if (outputLine.isOpen() && outputLine.isRunning() && (currentTimeMs - lastProgressUpdateTime > PROGRESS_UPDATE_INTERVAL_MS)) {
                lastProgressUpdateTime = currentTimeMs;
            }

            int pageoutResult = syncState.pageout(page);
            if (pageoutResult == 0) {
                int bufferIndex = syncState.buffer(JOGG_BUFFER_SIZE);
                buffer = syncState.data;
                bytesRead = bitStream.read(buffer, bufferIndex, JOGG_BUFFER_SIZE);
                syncState.wrote(bytesRead);
                if (bytesRead == 0) {
                    endOfStream = true;
                }
                continue;
            }

            if (pageoutResult < 0) {
                System.err.println("Warning: Corrupt Ogg page or stream hole detected. Skipping.");
                continue;
            }

            streamState.pagein(page);

            while (!stopRequested) {
                int packetoutResult = streamState.packetout(packet);
                if (packetoutResult == 0) break;
                if (packetoutResult < 0) {
                    System.err.println("Warning: Corrupt Ogg packet. Skipping.");
                    continue;
                }

                if (block.synthesis(packet) == 0) {
                    dspState.synthesis_blockin(block);
                } else {
                    System.err.println("Warning: Vorbis synthesis error for a packet.");
                }

                int samplesAvailable;
                while ((samplesAvailable = dspState.synthesis_pcmout(pcmWorkspace, pcmChannelIndex)) > 0 && !stopRequested) {
                    writePcmToOutputLine(pcmWorkspace[0], pcmChannelIndex, samplesAvailable, info, outputLine);
                    dspState.synthesis_read(samplesAvailable);
                }
                if (stopRequested) break;
            }

            if (page.eos() != 0) {
                endOfStream = true;
            }
        }

        if (stopRequested) {
            outputLine.stop();
            outputLine.flush();
            firePlaybackStopped(false);
        }
    }

    private void writePcmToOutputLine(float[][] pcmData, int[] pcmChannelIndex, int samples,
                                      Info info, SourceDataLine outputLine) {
        if (stopRequested) return;

        byte[] outBuffer = new byte[samples * info.channels * 2];
        int bufferWriteIndex = 0;

        for (int i = 0; i < samples; i++) {
            for (int j = 0; j < info.channels; j++) {
                float sampleFloat = pcmData[j][pcmChannelIndex[j] + i];
                int sampleInt = (int) (sampleFloat * 32767.0f);

                sampleInt = Math.max(-32768, Math.min(32767, sampleInt));
                outBuffer[bufferWriteIndex++] = (byte) sampleInt;
                outBuffer[bufferWriteIndex++] = (byte) (sampleInt >>> 8);
            }
        }
        if (!stopRequested) {
            outputLine.write(outBuffer, 0, bufferWriteIndex);
        }
    }
}
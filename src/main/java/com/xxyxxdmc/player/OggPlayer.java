package com.xxyxxdmc.player;

import com.jcraft.jogg.*;
import com.jcraft.jorbis.*;
import javax.sound.sampled.*;
import java.io.*;
import java.util.List; // Added
import java.util.concurrent.CopyOnWriteArrayList; // Added

public class OggPlayer {

    private static final int JOGG_BUFFER_SIZE = 4096;
    private volatile boolean stopRequested = false;

    private final List<PlaybackListener> listeners = new CopyOnWriteArrayList<>();

    private long lastProgressUpdateTime = 0;
    private static final long PROGRESS_UPDATE_INTERVAL_MS = 1000;
    private String currentFilePath;

    public void addPlaybackListener(PlaybackListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    public void removePlaybackListener() {
        this.listeners.clear();
    }

    public void removePlaybackListener(PlaybackListener listener) {
        this.listeners.remove(listener);
    }

    private void firePlaybackStarted() {
        for (PlaybackListener listener : listeners) {
            listener.onPlaybackStarted(this.currentFilePath);
        }
    }

    private void firePlaybackStopped(boolean dueToError) {
        for (PlaybackListener listener : listeners) {
            listener.onPlaybackStopped(this.currentFilePath, dueToError);
        }
    }

    private void firePlaybackFinished() {
        for (PlaybackListener listener : listeners) {
            listener.onPlaybackFinished(this.currentFilePath);
        }
    }

    private void firePlaybackError(OggPlayerException e) {
        for (PlaybackListener listener : listeners) {
            listener.onPlaybackError(this.currentFilePath, e);
        }
    }

    private void fireProgressUpdate(long currentMicroseconds, long totalMicroseconds) {
        for (PlaybackListener listener : listeners) {
            listener.onProgressUpdate(this.currentFilePath, currentMicroseconds, totalMicroseconds);
        }
    }


    public void play(String filePath) throws OggPlayerException { // Simplified exception signature
        this.stopRequested = false;
        this.currentFilePath = filePath; // Store for listeners
        this.lastProgressUpdateTime = 0; // Reset for new playback

        SyncState syncState = new SyncState();
        StreamState streamState = new StreamState();
        Info info = new Info();
        Comment comment = new Comment();
        DspState dspState = new DspState();
        SourceDataLine outputLine = null; // Declare here for access in finally and for progress

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

            outputLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo); // Assign to class-accessible variable
            outputLine.open(audioFormat);
            outputLine.start();

            firePlaybackStarted(); // Notify listeners: playback has started

            decodeAndPlayStream(bitStream, syncState, streamState, info, dspState, block, outputLine);

            if (!stopRequested) { // If not stopped by user or error
                outputLine.drain(); // Wait for buffer to empty only if playback completed naturally
                firePlaybackFinished(); // Notify listeners: playback finished naturally
            }

        } catch (IOException | LineUnavailableException | SecurityException e) {
            // Wrap standard exceptions into OggPlayerException
            OggPlayerException oggEx = new OggPlayerException("Error during playback setup or IO: " + e.getMessage());
            firePlaybackError(oggEx); // Notify listeners of the error
            throw oggEx; // Rethrow
        } catch (OggPlayerException e) { // Catch exceptions from readOggHeaders or other specific logic
             firePlaybackError(e); // Notify listeners of the error
             throw e; // Rethrow
        } finally {
            if (outputLine != null) {
                outputLine.stop(); // Already handled in decodeAndPlayStream if stopRequested
                outputLine.flush(); // Already handled
                outputLine.close(); // Ensure it's closed
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

        long totalMicroseconds = -1;

        while (!endOfStream && !stopRequested) {
            long currentTimeMs = System.currentTimeMillis();
            if (outputLine.isOpen() && outputLine.isRunning() && (currentTimeMs - lastProgressUpdateTime > PROGRESS_UPDATE_INTERVAL_MS)) {
                long currentPositionMicro = outputLine.getMicrosecondPosition();
                fireProgressUpdate(currentPositionMicro, totalMicroseconds);
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
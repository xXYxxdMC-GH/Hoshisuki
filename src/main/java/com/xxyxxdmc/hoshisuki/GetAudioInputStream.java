package com.xxyxxdmc.hoshisuki;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class GetAudioInputStream {
    public AudioInputStream getAudioInputStream(AudioFormat decodedFormat, AudioInputStream audioInputStream) {
        return AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);
    }
}

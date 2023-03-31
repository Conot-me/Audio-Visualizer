import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

/**
*
* Alot of the wav code could be simplified using an inputstream. However, i wanted to learn how to work with
* the raw bytes of a file and how to interpret it. This allowed me to gain new knowledge in areas like endianness.
* Below describes the file format that i am working with.
* http://soundfile.sapp.org/doc/WaveFormat/
*
* */

public class Wav {
    double numberOfSamples;
    double sampleRate;
    ArrayList<Double> leftChannelSamples = new ArrayList<>();
    ArrayList<Double> rightChannelSamples = new ArrayList<>();
    public Clip clip;

    byte[] riff;
    byte[] bytes;
    byte[] chunkID;
    byte[] chunkSizeBytes;
    byte[] formatBytes;
    byte[] subChunk1ID;
    byte[] subchunk1SizeBytes;
    byte[] audioFormatBytes;
    byte[] numChannelsBytes;
    byte[] sampleRateBytes;
    byte[] byteRateBytes;
    byte[] blockAlignBytes;
    byte[] bitsPerSampleBytes;

    byte[] byteSamples;
    int bitsPerSample;
    int numChannels;

    public Wav() throws IOException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }

        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("WAV songs", "wav");
        chooser.setFileFilter(filter);
        if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            riff = new byte[]{82, 73, 70, 70}; // represents the RIFF ID
            bytes = Files.readAllBytes(file.toPath());
            chunkID = new byte[]{bytes[0], bytes[1], bytes[2], bytes[3]};
            System.out.println(chunkID[0]);
            if (!Arrays.equals(chunkID, riff)) {
                System.out.println("not riff");
                return;
            }
            System.out.println("riff file");

            chunkSizeBytes = new byte[]{bytes[4], bytes[5], bytes[6], bytes[7]};
            int chunkSize = ByteBuffer.wrap(chunkSizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
            System.out.println("chunksize: " + chunkSize);

            formatBytes = new byte[]{bytes[8], bytes[9], bytes[10], bytes[11]};
            String format = new String(formatBytes);

            if (!format.equals("WAVE")) {
                System.out.println("not wave file.");
                return;
            }
            System.out.println("WAVE file");

            //Collecting all the fields in the RIFF chunk
            subChunk1ID = new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]};
            subchunk1SizeBytes = new byte[]{bytes[16], bytes[17], bytes[18], bytes[19]};
            audioFormatBytes = new byte[]{bytes[20], bytes[21]};

            //Collecting all the fields in the fmt sub-chunk
            numChannelsBytes = new byte[]{bytes[22], bytes[23]};
            sampleRateBytes = new byte[]{bytes[24], bytes[25], bytes[26], bytes[27]};
            byteRateBytes = new byte[]{bytes[28], bytes[29], bytes[30], bytes[31]};
            blockAlignBytes = new byte[]{bytes[32], bytes[33]};
            bitsPerSampleBytes = new byte[]{bytes[34], bytes[35]};

            System.out.println(new String(subChunk1ID));
            System.out.println("SubChunk1Size: " + littleEndianToInt(subchunk1SizeBytes));
            System.out.println("AudioFormat: " + littleEndian2ByteToInt(audioFormatBytes));
            System.out.println("NumberOfChannels: " + littleEndian2ByteToInt(numChannelsBytes));
            System.out.println("SampleRate: " + littleEndianToInt(sampleRateBytes));
            System.out.println("ByteRate: " + littleEndianToInt(byteRateBytes));
            System.out.println("BlockAlign: " + littleEndian2ByteToInt(blockAlignBytes));
            System.out.println("BitsPerSample: " + littleEndian2ByteToInt(bitsPerSampleBytes));

            sampleRate = littleEndianToInt(sampleRateBytes);

            byte[] data = {100, 97, 116, 97}; //these bytes represent the data sub-chunk ID
            byte[] dataBytes = new byte[4];
            int offset = 36; //if no offset then the next chunk starts at 36.
            //find the offset between the fmt and data subchunks
            for (int i = 36; i < bytes.length; i++) {
                dataBytes[0] = bytes[i];
                dataBytes[1] = bytes[i + 1];
                dataBytes[2] = bytes[i + 2];
                dataBytes[3] = bytes[i + 3];
                if (Arrays.equals(data, dataBytes)) {
                    offset = i - 36;
                    System.out.println("offset: " + offset);
                    break;
                }
            }

            System.out.println(new String(dataBytes) + " Chunk starts at index: " + (offset + 36));
            byte[] subChunk2SizeBytes = {bytes[40 + offset], bytes[41 + offset], bytes[42 + offset], bytes[43 + offset]};
            if (littleEndian2ByteToInt(bitsPerSampleBytes) == 4) {
                System.out.println("NumberOfSamples: " + ((bytes.length - (43 + offset)) / 4)); //divide by 4 when 2 channels. divide by 2 when 1 channel
                System.out.println("Two channel channelSubChunk2Size should be: " + ((bytes.length - (43 + offset)) / 4) * littleEndian2ByteToInt(numChannelsBytes) * (littleEndian2ByteToInt(bitsPerSampleBytes) / 8));
                numberOfSamples = ((bytes.length - (43 + offset)) / 4);
            } else {
                System.out.println("NumberOfSamples: " + ((bytes.length - (43 + offset)) / 2)); //divide by 2 when 1 channel
                System.out.println("Single channel SubChunk2Size should be: " + ((bytes.length - (43 + offset)) / 2) * littleEndian2ByteToInt(numChannelsBytes) * (littleEndian2ByteToInt(bitsPerSampleBytes) / 8));
                numberOfSamples = ((bytes.length - (43 + offset)) / 2);

            }
            System.out.println("SubChunk2Size: " + littleEndianToInt(subChunk2SizeBytes));

            //multi channel
            if (littleEndian2ByteToInt(numChannelsBytes) == 2) {
                byteSamples = new byte[littleEndianToInt(subChunk2SizeBytes)];
                int j = 0;

                System.out.println("data size is: " + (bytes.length - (44 + offset)));
                for (int i = 44 + offset; i < bytes.length - 4; i += 4) {
                    try {
                        byte[] leftChannelBytes = {bytes[i], bytes[i + 1]};
                        byte[] rightChannelBytes = {bytes[i + 2], bytes[i + 3]};
                        leftChannelSamples.add((double) littleEndian2ByteToInt(leftChannelBytes));
                        rightChannelSamples.add((double) littleEndian2ByteToInt(rightChannelBytes));

                    } catch (IndexOutOfBoundsException e) {
                        System.out.println("error at index: " + i);
                        return;
                    }

                    byteSamples[j++] = bytes[i];
                    byteSamples[j++] = bytes[i + 1];
                    byteSamples[j++] = bytes[i + 2];
                    byteSamples[j++] = bytes[i + 3];
                } //end of channel 2
            } else {
                //single channel
                byteSamples = new byte[littleEndianToInt(subChunk2SizeBytes)];
                int j = 0;
                for (int i = 44 + offset; i < bytes.length; i++) {
                    byteSamples[j++] = bytes[i];
                }
            }
            System.out.println("sample size: " + byteSamples.length / 2);
            System.out.println("sample example: " + (byteSamples[byteSamples.length / 2] & 0xff));
            System.out.println("left channel size: " + leftChannelSamples.size());

            sampleRate = littleEndianToInt(sampleRateBytes);
            bitsPerSample = littleEndian2ByteToInt(bitsPerSampleBytes);
            numChannels = littleEndian2ByteToInt(numChannelsBytes);
        }
    }

    public void playAudio(){
        try{
            //create AudioInputStream to be used to play audio
            InputStream input = new ByteArrayInputStream(byteSamples);
            AudioFormat formatter = new AudioFormat((float)sampleRate, bitsPerSample, numChannels,true,false);
            AudioInputStream audioInputStream = new AudioInputStream(input, formatter, byteSamples.length / formatter.getFrameSize());

            // create a clip to play the audio
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();

        }catch (Exception e){
            System.out.println("error playing audio");
            e.printStackTrace();
        }
    }

    public static int littleEndianToInt(byte[] bytes){
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static int littleEndianToSigned16BitInt(byte[] bytes){
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static int littleEndian2ByteToInt(byte[] bytes) {
        //return (bytes[1] & 0xff) << 8 | (bytes[0] & 0xff);
        return ByteBuffer.wrap(bytes, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static int bigEndianToInt(byte[] bytes){
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public static int[] singleChannelBytesConversion(byte[]samples){
        int[] arrayBuffer = new int[samples.length/2];
        System.out.println(arrayBuffer.length);
        int counter = 0;
        for (int i = 0; i < samples.length; i+=2) {
            try{
                byte[] buffer = {samples[i], samples[i+1]};
                arrayBuffer[counter++] = littleEndianToSigned16BitInt(buffer);
            }catch (Exception e){
                System.out.println("error at index: " + i);
            }
        }
        return arrayBuffer;
    }

    /*
    legacy code. not used anymore as it is not efficient due to the large amount of samples in audio files.
    performs the discrete fourier transform on the audio samples.
    Currently only works for single channel wav files.
 */
    public static void dft(byte[] byteSamples){
        //int N = samples.length;
        int[] samples = singleChannelBytesConversion(byteSamples);
        System.out.println("samples new length: " + samples.length);
        int N = samples.length;

        double[] real = new double[N];  // real part of DFT
        double[] imaginary = new double[N];  // imaginary part of DFT

        // Perform DFT
        for (int k = 0; k < N; k++) {
            for (int n = 0; n < N; n++) {
                real[k] += samples[n] * Math.cos(2 * Math.PI * k * n / N);
                imaginary[k] -= samples[n] * Math.sin(2 * Math.PI * k * n / N);
            }
        }
    }
}

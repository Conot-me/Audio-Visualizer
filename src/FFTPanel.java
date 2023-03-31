import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

public class FFTPanel extends JPanel {
    double numberOfSamples;
    double sampleRate;
    ArrayList<Double> samples;
    int audioPosition = 0;
    final String text;
    double graphWidth;
    double graphHeight;
    double[] audioSamples;
    double[][] magnitude;
    double[] window;
    double[][] fftData;
    double largestMagnitude = 0;
    public final int windowSize = 512; //works best when a power of 2
    int hopSize = windowSize / 2;
    int numWindows;
    int frequencyBandsLength = 52;
    double[][] frequencyBandWindows;
    int sampleBuffer[] = new int[frequencyBandsLength];
    private double[] bufferDecrease = new double[frequencyBandsLength];

    public FFTPanel(String text, double numberOfSamples, double sampleRate, ArrayList<Double> samples) throws IOException {
        setPreferredSize(new Dimension(1425,200));
        setSize(1269,500);
        setBackground(new Color(60, 63, 65));
        setBorder(BorderFactory.createEtchedBorder());
        setOpaque(true);
        this.numberOfSamples = numberOfSamples;
        this.sampleRate = sampleRate;
        this.text = text;
        graphWidth = (getWidth() - 20);
        graphHeight =  (getHeight() - 20);

        for (int i = 0; i < frequencyBandsLength; i++) {
            sampleBuffer[i] = 0; //zero pad the buffer cells for later
        }

        audioSamples = new double[samples.size()];
        for (int i = 0; i < Math.floor(samples.size()); i++) {
            audioSamples[i] = samples.get(i);
        }

        numWindows = (audioSamples.length - windowSize) / hopSize + 1; // Total number of windows
        window = new double[windowSize * 2];
        DoubleFFT_1D fft = new DoubleFFT_1D(window.length/2);
        fftData = new double[numWindows][windowSize * 2];
        frequencyBandWindows = new double[numWindows][frequencyBandsLength];

        //Define the Blackman window function
        double[] blackmanWindow = new double[windowSize];
        for (int i = 0; i < windowSize; i++) {
            blackmanWindow[i] = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (windowSize - 1)) + 0.08 * Math.cos(4 * Math.PI * i / (windowSize - 1));
        }

        //Loop over each window, apply blackman function and compute the FFT
        for (int i = 0; i < numWindows; i++) {
            for (int j = 0; j < windowSize; j++) {
                window[j] = audioSamples[i * hopSize + j];
            }

            //Apply the Blackman window function to the data
            for (int j = 0; j < windowSize; j++) {
                window[j] *= blackmanWindow[j];
            }

            fft.realForwardFull(window);
            System.arraycopy(window, 0, fftData[i], 0, windowSize);
        }

        // Calculate magnitude of each frequency bin
        magnitude = new double[numWindows][windowSize];
        for (int j = 0; j < fftData.length; j++) {
            for (int i = 0; i < windowSize; i++) {
                double real = fftData[j][2 * i];
                double imag = fftData[j][2 * i + 1];
                //magnitude[j][i] = (float) (Math.log10(real * real + imag * imag)); //log10 does not work well.
                magnitude[j][i] = (float) (Math.sqrt(real * real + imag * imag)); //sqrt gives more weight to values closer to 0.
            }
        }

        int counter;
        int sampleCount;
        int power;
        for (int i = 0; i < numWindows; i++) {
            counter = 0;
            sampleCount = 1;
            power = 0;
            //calculate frequency band average for the window.
            for (int j = 0; j < frequencyBandsLength; j++) {
                float average = 0;
                //each frequency band gets bigger by a factor of 2
                if (j == 16 || j == 32 ||j == 40 ||  j == 48 || j == 56) {
                    sampleCount = (int)Math.pow(2,power) * 2;
                    power++;
                    if(power == 3){
                        sampleCount -= 2;
                    }
                }
                for (int k = 0; k < sampleCount; k++){
                    average += magnitude[i][counter] * (counter + 1); //the higher the frequency the bigger the multiplier
                    counter++;
                }
                average /= counter;
                if(average > largestMagnitude && j <= 2) largestMagnitude = average;
                frequencyBandWindows[i][j] = average;
            }
        }
    }

    /*********************************************************************************
     *                                                                               *
     *                             GRAPH VIS RENDERING                               *
     *                                                                               *
     *********************************************************************************/

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        //draw graph boundaries
        g.setColor(new Color(21, 20, 20));
        g.fillRect(10,10, (int)graphWidth, (int)graphHeight);

        g.setColor(Color.LIGHT_GRAY);
        g.drawString("FFT: " + frequencyBandsLength + "Bands",30, (int)(0.05 * graphHeight + 10));
        g.drawString("Window Size: " + windowSize,30, (int)(0.05 * graphHeight + 25));

        int width = (int)Math.floor(graphWidth / (frequencyBandsLength*2));
        int gap = (int)graphWidth - (width * (frequencyBandsLength*2));

        float hue = 0.0f;
        float colorShift = 360.0f / frequencyBandsLength;
        for (int i = 0; i < frequencyBandsLength; i++) {
            int height = (int)(Math.min(((frequencyBandWindows[audioPosition][i]/(largestMagnitude / 2))) * graphHeight, 480)); //480 ensures it stays within bounds
            Color barColor = Color.getHSBColor(((hue / 360.0f) % 360.0f), 1f, 0.8f);
            g.setColor(barColor);

            //when true, make the buffer == height and reset the buffer decrease value.
            if (height > sampleBuffer[i]) {
                sampleBuffer[i] = height;
                bufferDecrease [i] = 3;
            }
            //when true, decrease the sample buffer and increase the buffer decrease value.
            if (height < sampleBuffer[i]){
                sampleBuffer[i] -= bufferDecrease[i];
                if (height != 0)bufferDecrease[i] += 1;
                height = sampleBuffer[i];
            }

            //draw the bins where y is the height.
            g.fillRect(10+((i*width)),(int)(graphHeight + 10), width - gap, Math.min(-height,  0));
            g.fillRect((int)(graphWidth + 10)-((i*width)),(int)(graphHeight + 10), gap - width, Math.min(-height,  0));

            hue += colorShift; //Shift hue for next bar
        }
    }

}

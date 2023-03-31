import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class AudioController {

    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame("Audio Domain Visualiser");
        Wav wav = new Wav();
        FFTPanel fftController = new FFTPanel("FFT", wav.numberOfSamples, wav.sampleRate, wav.leftChannelSamples);
        WaveformPanel leftChannelController = new WaveformPanel("Left Channel", wav.numberOfSamples, wav.sampleRate, wav.leftChannelSamples);
        WaveformPanel rightChannelController = new WaveformPanel("Right Channel", wav.numberOfSamples, wav.sampleRate, wav.rightChannelSamples);
        fftController.setPreferredSize(new Dimension(1269,500));

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width/3,dim.height/5);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(false);
        frame.setDefaultLookAndFeelDecorated(true);
        frame.getContentPane().setBackground(Color.RED);
        frame.setIconImage(new ImageIcon("src/FFT_ICON.jpg").getImage());
        UIManager.put("InternalFrame.activeTitleBackground", new ColorUIResource(Color.black));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(fftController, BorderLayout.NORTH);
        panel.add(leftChannelController, BorderLayout.CENTER);
        panel.add(rightChannelController, BorderLayout.SOUTH);

        frame.add(panel);
        frame.add(panel);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        frame.setResizable(false);

        Timer timer = new Timer();
        TimerTask waveformTimer;
        TimerTask fftTimer;


        long songDuration =(int)(wav.leftChannelSamples.size()/wav.sampleRate) * 1000000; //duration of the song in microseconds
        System.out.println("duration: "+songDuration);
        int graphWidth = (int)fftController.graphWidth;

        waveformTimer = new TimerTask() {
            long elapsedTime;
            int audioPosition;
            @Override
            public void run() {
                if (wav.clip.getMicrosecondPosition() >= wav.clip.getMicrosecondLength()){
                    System.out.println("timer 1 cancelled");
                    cancel();
                    return;
                }

                // Get the elapsed time in seconds
                elapsedTime = wav.clip.getMicrosecondPosition();

                // Calculate the x position for the current elapsed time on the graph
                audioPosition = (int) ((elapsedTime / (double) songDuration) * graphWidth);

                //run in the event dispatch thread
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        leftChannelController.audioPosition = audioPosition;
                        rightChannelController.audioPosition = audioPosition;
                        leftChannelController.repaint();
                        rightChannelController.repaint();
                    }
                });
            }
        };


        double sampleRate = wav.sampleRate;
        double windowSize = fftController.windowSize;

        // Window overlap factor (0.0 to 1.0)
        double overlap = 0.5;

        // Duration of each window in microseconds || windowSize / microseconds per sample * overlap = duration per window
        double windowDuration = windowSize / sampleRate * 1000000 * (1.0 - overlap);


        FFTPanel finalFftController = fftController;
        fftTimer = new TimerTask() {
            long currentPosition;
            @Override
            public void run() {
                if (wav.clip.getMicrosecondPosition() >= wav.clip.getMicrosecondLength()){
                    System.out.println("Timer task cancelled");
                    cancel();
                    return;
                }

                currentPosition = wav.clip.getMicrosecondPosition();
                int audioPosition = (int) (currentPosition / windowDuration);

                //run in the event dispatch thread
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        finalFftController.audioPosition = audioPosition;
                        finalFftController.repaint();
                    }
                });
            }
        };
        wav.playAudio();

        timer.scheduleAtFixedRate(waveformTimer, 0,50);
        timer.scheduleAtFixedRate(fftTimer, 0, 30);
    }
}


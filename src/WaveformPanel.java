import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

public class WaveformPanel extends JPanel implements MouseMotionListener, MouseListener {
    double numberOfSamples;
    double sampleRate;
    ArrayList<Double> samples;
    int audioPosition = 0;
    String text;
    int graphWidth;
    int graphHeight;
    Graphics2D g2d;
    double[][] amplitudes;
    int mousePosX;
    boolean drawMousePos;

    public WaveformPanel(String text, double numberOfSamples, double sampleRate, ArrayList<Double> samples){
        setPreferredSize(new Dimension(1269,150));// width 1269 works well with 1 pixel between each bar.
        setSize(1269,150);
        setBackground(new Color(60, 63, 65));
        setBorder(BorderFactory.createEtchedBorder());

        this.numberOfSamples = numberOfSamples;
        this.sampleRate = sampleRate;
        this.samples = samples;
        this.text = text;
        graphWidth = (getWidth() - 20);
        graphHeight = (getHeight() - 20);
        addMouseMotionListener(this);
        setFocusable(true);

        amplitudes  = new double[graphWidth][2];
        int negativeCounter;
        int positiveCounter;
        int downsampleFactor = samples.size() / graphWidth;


        // Find the maximum amplitude value in the entire audio data
        double maxAmplitude = 0;
        for (int i = 0; i < samples.size(); i++) {
            double amplitude = Math.abs(samples.get(i) / 32768.0); //normalise between 0-1. 32768 is the highest amplitude for PCM_SIGNED wav files.
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude;
            }
        }

        //normalise between 0-1. 32768 is the highest amplitude for PCM_SIGNED wav files.
        //collect the average negative and positive range of each downSampleFactor
        for (int i = 0; i < graphWidth; i++) {
            positiveCounter = 0;
            negativeCounter = 0;
            double positiveSum = 0.0;
            double negativeSum = 0.0;
            for (int j = 0; j < downsampleFactor; j++) {
                int sampleIndex = i * downsampleFactor + j;
                if (sampleIndex >= samples.size()) {
                    break;
                }
                if (samples.get(sampleIndex) < 0) {
                    negativeSum += Math.abs(samples.get(sampleIndex) / 32768.0);
                    negativeCounter++;
                    continue;
                }
                positiveSum += samples.get(sampleIndex) / 32768.0;
                positiveCounter++;
            }

            double positiveAverage = positiveCounter > 0 ? positiveSum / positiveCounter : 0.0;
            double negativeAverage = negativeCounter > 0 ? negativeSum / negativeCounter : 0.0;
            amplitudes[i][0] = positiveAverage;
            amplitudes[i][1] = negativeAverage;
        }
    }

    /*********************************************************************************
     *                                                                               *
     *                                WAVEFORM RENDERING                             *
     *                                                                               *
     *********************************************************************************/

    Color red = new Color(255, 255, 255, 163);
    Color green = new Color(41, 255, 0);
    Color redA = new Color(255, 0, 0, 35);
    Color greenA = new Color(51, 255, 0, 35);

    GradientPaint lineGradient = new GradientPaint(10, 10, red, graphWidth+10, graphHeight+10, green);

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(1));

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(10,10, graphWidth, graphHeight);

        g2d.setColor(Color.BLACK);
        g2d.drawLine(10,graphHeight/2 + 10, graphWidth + 10,graphHeight/2 + 10);

        g2d.setColor(new Color(2, 0, 0, 174)); //darker intellij
        g2d.fillRect(10,10, graphWidth, graphHeight);

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString(text,30, 25);

        g2d.setPaint(lineGradient);
        for (int i = 0; i < amplitudes.length; i++) {
            //draw a line from the positive average to negative average
            g2d.drawLine(10+i, (int)(graphHeight/2 + 10 + (graphHeight/2 * amplitudes[i][0])),10+i, (int)(graphHeight/2 + 10 - (graphHeight/2 * amplitudes[i][1])));
        }

        g2d.setColor(new Color(79, 91, 95, 64)); //white-ish
        g2d.fillRect(10,10, audioPosition, graphHeight); //song microsecond position.

        //draw y axis line on mouse position.
        if (drawMousePos){
            g2d.setColor(Color.black);
            g.fillRect(mousePosX,10, 1, graphHeight);
        }
    }

    /*********************************************************************************
     *                                                                               *
     *                                MOUSE EVENTS                                   *
     *                                                                               *
     *********************************************************************************/

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (e.getX() > 10 && e.getX() <= graphWidth + 10 && e.getY() > 10 && e.getY() <= graphHeight){
            drawMousePos = true;
            mousePosX = e.getX();
            return;
        }
        drawMousePos = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (e.getX() > 10 && e.getX() <= graphWidth + 10 && e.getY() > 10 && e.getY() <= graphHeight){
            drawMousePos = true;
            return;
        }
        drawMousePos = false;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        drawMousePos = false;
    }
}

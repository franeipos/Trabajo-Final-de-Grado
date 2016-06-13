package com.example.francisco.eardrive;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import org.jtransforms.fft.DoubleFFT_1D;
import org.jtransforms.fft.FloatFFT_1D;


/**
 * Created by Francisco on 04/06/2016.
 */
public class SoundMeter {
    private static final int sampleRate = 8000;
    private AudioRecord audio;
    private int bufferSize;
    private double lastLevel = 0;
    private Complex audioBuffer[];
    private int tiempoAmbienteBuffer = 0;
    private double ambienteAmplitud = 0;
    private int numBuffersAmbiente = 0;


    public void start() {
        try {

//            bufferSize = AudioRecord
//                    .getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
//                            AudioFormat.ENCODING_PCM_16BIT);
//            Log.i("Buffer" , "BUFFER SIZE : " + bufferSize);
            bufferSize = 1024;
            audioBuffer =  new Complex[bufferSize];
        } catch (Exception e) {
            android.util.Log.e("TrackingFlow", "Exception", e);
        }

        audio = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);


        audio.startRecording();
    }

    /**
     * Functionality that gets the sound level out of the sample
     */
    public double getAmplitud() {

        try {
            short[] buffer = new short[bufferSize];

            int bufferReadResult = 1;

            if (audio != null) {

                // Sense the voice...
                bufferReadResult = audio.read(buffer, 0, bufferSize);
                convertShortToDouble(buffer);
                calculateFFT();
                double sumLevel = 0;
                for (int i = 0; i < bufferReadResult; i++) {
                    sumLevel += buffer[i];

                }
                lastLevel = Math.abs((sumLevel / bufferReadResult));
               // lastLevel  = 20*Math.log10(Math.abs(lastLevel)/32767);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lastLevel;
    }

    private void convertShortToDouble(short[] buffer) {
        for (int i =0;i<buffer.length;i++){
            audioBuffer[i] = new Complex(buffer[i]/32768.0,0);
        }
    }

    public void calculateFFT() {

        //it is assumed that a float array audioBuffer exists with even length = to
        //the capture size of your audio buffer

        //The size of the FFT will be the size of your audioBuffer / 2
        //int FFT_SIZE = bufferSize / 2;
        int FFT_SIZE = bufferSize;


        //Take the FFT
       double array [] = new double [4];
        array[0] = -0.03480425839330703;
        array[1] = 0.07910192950176387;
        array[2] = 0.7233322451735928;
        array[3]= 0.1659819820667019;
        FFT fft =  new FFT();
        double [] abs = fft.fftABS(audioBuffer);

        //The first 1/2 of audioBuffer now contains bins that represent the frequency
        //of your wave, in a way.  To get the actual frequency from the bin:
        //frequency_of_bin = bin_index * sample_rate / FFT_SIZE

        //assuming the length of audioBuffer is even, the real and imaginary parts will be
        //stored as follows
        //audioBuffer[2*k] = Re[k], 0<=k<n/2
        //audioBuffer[2*k+1] = Im[k], 0<k<n/2

        //Define the frequencies of interest
        float freqMin = 14400;
        float freqMax = 16200;

        double mMaxFFTSample = 0.0;
        int mPeak = 0;

        //Loop through the fft bins and filter frequencies
        for(int fftBin = 0; fftBin < audioBuffer.length / 2; fftBin++){
//            //Calculate the frequency of this bin assuming a sampling rate of 44,100 Hz
//            float frequency = (float)fftBin * (float)sampleRate / (float)FFT_SIZE;
//
//            //Calculate the index where the real and imaginary parts are stored
//            double real =  2*fftBin;
//            int imaginary =  2*fftBin + 1;
//
//            double abs = Math.sqrt((array[(int)real]* array[(int)real]) + Math.pow(array[imaginary], 2));
//            Log.i("Array" , "Muestra " + fftBin + " : " + array[(int)real] + " , " + array[imaginary]);

            if(abs[fftBin] > mMaxFFTSample){
                mMaxFFTSample = abs[fftBin];
                mPeak = fftBin;
            }
        }

        float frecuncia = (sampleRate / FFT_SIZE) * mPeak;
        compararSonidos(frecuncia,mMaxFFTSample);
    }

    public void compararSonidos(float frec , double abs){


        if(frec > 100 && frec < 800){
            Log.e("SOUND","Frecuencia : " + frec);

            Log.e("SOUND", "Magnitud : " + abs);
        }

    }


    public void stop() {
        try {
            if (audio != null) {
                audio.stop();
                audio.release();
                audio = null;
            }
        }
        catch (Exception e) {e.printStackTrace();}
    }

}

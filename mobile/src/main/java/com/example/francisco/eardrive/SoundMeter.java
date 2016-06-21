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
    private static final int sampleRate = 44100;
    private AudioRecord audio;
    private int bufferSize;
    private double lastLevel = 0;
    private Complex audioBuffer[];
    private double[] window;
    private int [] alertas;
    private int contadorLlamadas = 0 ;
    public void start() {
        try {

//            bufferSize = AudioRecord
//                    .getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
//                            AudioFormat.ENCODING_PCM_16BIT);
//            Log.i("Buffer" , "BUFFER SIZE : " + bufferSize);
            bufferSize = 1024;
            audioBuffer =  new Complex[bufferSize];
            alertas = new int[8];
            limpiarAlertas();
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

                    convertShortToDouble(applyWindow(buffer));
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

    private void convertShortToDouble(double[] buffer) {
        for (int i =0;i<buffer.length;i++){
            audioBuffer[i] = new Complex(buffer[i]/32768.0,0);
        }
    }

    public void calculateFFT() {

        int FFT_SIZE = bufferSize;

        FFT fft =  new FFT();
        double [] abs = fft.fftABS(audioBuffer);

        compararSonidos(abs);
    }

    public void compararSonidos(double[] abs){

        double potenciaArmonico1 = 0.0;
        double potenciaArmonico2 = 0.0;
        int posicionArmonico1 = 0;
        int posicionArmonico2 = 0;

        //Loop through the fft bins and filter frequencies
        for(int fftBin = 0; fftBin < audioBuffer.length / 2; fftBin++){
//
            if(abs[fftBin] > potenciaArmonico1){
                potenciaArmonico2 = potenciaArmonico1;
                posicionArmonico2 = posicionArmonico1;
                posicionArmonico1 = fftBin;
                potenciaArmonico1 = abs[fftBin];
            }
            else if(abs[fftBin] > potenciaArmonico2){
                posicionArmonico2 = fftBin;
                potenciaArmonico2 = abs[fftBin];
            }
        }

        float frecuencia1 = (sampleRate / bufferSize) * posicionArmonico1;
        float frecuencia2 = (sampleRate / bufferSize) * posicionArmonico2;
        //Log.e("FREC" , "Armonico 1 : " + frecuencia1 + "Energía : " +  abs[9] + "  "+ abs[10] + "  " + abs[11] + abs[12] ) ;

        if(frecuencia1 > 1200 && frecuencia1 < 1400){
            if(abs[9] > 0.1 || abs[10] > 0.1 || abs[11] > 0.1 || abs[12] > 0.1 || abs[13]> 0.1){
                meterAlerta(1);
            }

            //Log.e("FREC", "Energy abs : " + abs + " Fecuencia : " + frec + "cont : " + contadorLlamadas) ;
        }
        else{
            meterAlerta(0);
        }

    }


    /** build a Hamming window filter for samples of a given size
     * See http://www.labbookpages.co.uk/audio/firWindowing.html#windows
     * @param size the sample size for which the filter will be created
     */
    private void buildHammWindow(int size) {
        if(window != null && window.length == size) {
            return;
        }
        window = new double[size];
        for(int i = 0; i < size; ++i) {
            window[i] = .54 - .46 * Math.cos(2 * Math.PI * i / (size - 1.0));
        }
    }

    private double[] applyWindow(short[] input) {
        double[] res = new double[input.length];

        buildHammWindow(input.length);
        for(int i = 0; i < input.length; ++i) {
            res[i] = (double)input[i] * window[i];
        }
        return res;
    }

    public void meterAlerta(int tipo) {
        boolean metido = false;

        alertas[contadorLlamadas] = tipo;
        contadorLlamadas++;
    }

    public int comprobarAlerta() {
        int tipoAlerta = -1;
        int  [] vecesAlerta =  new int[3];

        for (int i = 1; i < alertas.length; i++) {
            vecesAlerta[alertas[i]]++;
        }


        for (int i=1; i < vecesAlerta.length; i++){
            if(vecesAlerta[i]>3){
                tipoAlerta = vecesAlerta[i];
                limpiarAlertas();
                contadorLlamadas =0;
            }
        }

        if(contadorLlamadas == 8){
            if(alertas[alertas.length-1]!=0){ //si acaba de sonar una alerta nos la guardamos para la siguiente
                int aux = alertas[alertas.length-1];
                limpiarAlertas();
                alertas[0] = aux;
                contadorLlamadas = 1;
            }
            else{
                limpiarAlertas();
                contadorLlamadas=0;
            }

        }

        return tipoAlerta;
    }
    public void limpiarAlertas() {
        for(int i=0;i < alertas.length;i++) {
            alertas[i] = 0;
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

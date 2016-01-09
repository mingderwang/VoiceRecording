package com.log4analytics.mobile.voiderecording;

import android.app.Activity;
import java.util.Arrays;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.media.AudioRecord.OnRecordPositionUpdateListener;

import java.net.DatagramSocket;

import java.io.IOException;
//import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Send extends Activity implements OnClickListener, OnRecordPositionUpdateListener  {

    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    private static final int DEFAULT_SAMPLE_RATE = 16000;

    private static int RESOLUTION = AudioFormat.ENCODING_PCM_16BIT;
    private static final short RESOLUTION_IN_BYTES = 2;

    // Number of channels (MONO = 1, STEREO = 2)
    final short CHANNELS = 1;
    private int mFramePeriod;
    private int mBufferSize;

    private Button startButton,stopButton;

    private short[][] buffers;
    private int ix;

    public static DatagramSocket socket;
    private int port=50005;

    AudioRecord recorder;

    private int sampleRate = DEFAULT_SAMPLE_RATE ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_IN_DEFAULT;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = 0;
    private boolean status = true;

    private short[] buffer;

    @Override
    public void onMarkerReached(AudioRecord arg0) {
        // TODO Auto-generated method stub
    }

    public void stopRecording() {
        if (null != recorder) {
            status = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            streamThread = null;
        }
        Log.w("VS", "Audio recording stopped");
    }

    @Override
    public void onPeriodicNotification(AudioRecord arg0) {
        float norm;
        short[] buf = buffers[ix % buffers.length];
        int vol = 0;
        for (int i = 0; i < buf.length; i++)
            vol = Math.max(vol, Math.abs(buf[i]));
        norm = (float) vol / (float) Short.MAX_VALUE;
     //   glSurface.setLightness(norm);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_button:
       //         startRecording();
      //          setRecordMode();
                break;
            case R.id.stop_button:
      //          stopRecording();
      //          setRecordMode();
                break;
            default:
                break;
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);

        startButton = (Button) findViewById (R.id.start_button);
        stopButton = (Button) findViewById (R.id.stop_button);

        startButton.setOnClickListener (startListener);
        stopButton.setOnClickListener(stopListener);

    }

    private final View.OnClickListener stopListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            status = false;
            stopRecording();
            Log.d("VS", "Recorder released");
        }

    };

    private final View.OnClickListener startListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            status = true;

            startRecording();
        }

    };

    private AudioRecord findAudioRecord() {
        // TODO Auto-generated method stub
        int[] mSampleRates = new int[]{8000};

        for (int rate : mSampleRates) {

            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_16BIT }) {

                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO}) {

                    try {

                        Log.w("AudiopRecording", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "

                                + channelConfig);

                        minBufSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (minBufSize != AudioRecord.ERROR_BAD_VALUE) {

                            // check if we can instantiate and have a success
                            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelConfig, audioFormat, minBufSize);
                            int result = recorder.getState();
                            if (result == AudioRecord.STATE_INITIALIZED) {
                                System.out.println(result);
                                buffers = new short[256][minBufSize];
                                return recorder;
                            }
                        } else {
                            System.out.println("ERROR BAD VALUE");
                        }

                    } catch (Exception e) {

                        Log.e("AudiopRecording", rate + "Exception, keep trying.",e);

                    }

                }

            }

        }

        Log.d("VS","no audioRecorder");
        return null;
    }

    private Thread streamThread;

    // convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    public void startRecording() {



        streamThread = new Thread(new Runnable() {

            @Override
            public void run() {

                //DatagramSocket socket = new DatagramSocket();
                Log.d("VS", "Socket Created");



                //final InetAddress destination = InetAddress.getByName("192.168.1.5");
                Log.w("VS", "Address retrieved");

                AudioRecord recorder = findAudioRecord();

                Log.w("VS", "Buffer created of size " + minBufSize);
                //DatagramPacket packet;

                Log.w("VS", "Recorder initialized");

                if (recorder != null) {
                    recorder.startRecording();

                    ix = 0;
                    int read = 0;

                    while (status == true) {


                        //reading data from MIC into buffer
                       // minBufSize = recorder.read(buffer, 0, buffer.length);

                        recorder.setPositionNotificationPeriod(minBufSize);

                            //long t1 = System.currentTimeMillis();
                        int index = ix++ % buffers.length;
                            buffer = buffers[index];
                            read = recorder.read(buffer, 0, buffer.length);
                            //time after reading
                            //read_time = System.currentTimeMillis();
                            //Log.d(LOG_TAG, "read bytes: " + read);
                            //Log.d(LOG_TAG, "read_time=" + (read_time - t1));

                            //int vol = 0;
                            //for (int i=0;i<buffer.length;i++)
                            //  vol = Math.max(vol, Math.abs(buffer[i]));
                            //float norm = (float)vol/(float)Short.MAX_VALUE;
                            //glSurface.setLightness(norm);

                        //putting buffer in the packet
                        //packet = new DatagramPacket (buffer,buffer.length,destination,port);

                        //socket.send(packet);
                        System.out.println("MinBufferSize: " + minBufSize);
                        System.out.println("index: " + index);
                        byte[] b = short2byte(buffer);
                        System.out.println(Arrays.toString(b));

                    }
                } else {
                    System.out.println("null");
                }

            }

        });
        streamThread.start();
    }
}
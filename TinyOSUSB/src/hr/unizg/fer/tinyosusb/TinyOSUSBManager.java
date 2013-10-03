package hr.unizg.fer.tinyosusb;

import android.hardware.usb.UsbManager;
import android.util.Log;

import hr.unizg.fer.tinyosusb.packet.Packetizer;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class TinyOSUSBManager implements TinyOSCommunicationManager {

	private final String TAG = TinyOSUSBManager.class.getSimpleName();
	
	private static byte DELIMITER = (byte) 0x7e;

	private static byte ESCAPE = (byte) 0x7d;

	private static byte ESCAPE_DELIMITER = (byte) 0x5e;

	private static byte ESCAPE_ESCAPE = (byte) 0x5d;

	private static int BUFFER_SIZE = 1024;

    private static int PACKET_TAIL_SIZE = 5;

    private static int PACKET_HEAD_SIZE = 2;

    /**
     * The device currently in use, or {@code null}.
     */
    private UsbSerialDriver mSerialDevice;

    /**
     * The system's USB service.
     */
    private UsbManager mUsbManager;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private byte[] data;
    private byte[] dataBuffer;
    
    /**
     * Current amount of bytes in dataBuffer
     */
    private int dataBufferLength;
    
    /**
     * Number of escape bytes in the packet
     */
    private int escapeCount;
    
    
    /**
     * Indicates the start and end of the serial delimiter 0x7e
     */
    private boolean startDelimiter;
    private boolean endDelimiter;
    
    private Packetizer packetizer;
    
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
        	TinyOSUSBManager.this.buildPacket(data);
        }
    };
    
    public TinyOSUSBManager(UsbManager m) {
    	mUsbManager = m;
        startDelimiter = false;
        endDelimiter = false;
        dataBuffer = new byte[BUFFER_SIZE];
        dataBufferLength = 0;
        escapeCount = 0;
        data = null;
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (mSerialDevice != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(mSerialDevice, mListener);
            mExecutor.submit(mSerialIoManager);
            packetizer = new Packetizer("packetizer", mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }
    
    /**
     * Trims serial headers and footers
     * @param data
     * @return returns the payload
     */
	private byte[] trim(byte[] data) {
		
    	byte[] payload = new byte[data.length - PACKET_TAIL_SIZE];
    	for (int i = 0; i < payload.length; i++) {
			payload[i] = data[i + PACKET_HEAD_SIZE];
		}
    	
    	return payload;
    }

    /**
     * Checks if the packet is complete and forwards it to the method that takes care of escaping
     * @param start
     * @param end
     */
    private void checkData(boolean start, boolean end) {
        if (startDelimiter == true && endDelimiter == true) {

            //packet is complete, send it to host
            buildPacket(dataBuffer, dataBufferLength - escapeCount);

            //reset delimiter flags and the data buffer
            escapeCount = 0;
            dataBufferLength = 0;
            startDelimiter = false;
            endDelimiter = false;
        }
    }
	
	/**
	 * This method ensures the entire packet is sent instead of packet fragments
	 * @param data
	 */
	private void buildPacket(byte[] data) {

        for (byte aData : data) {

            checkData(startDelimiter, endDelimiter);

            if (aData == DELIMITER) {
                if (startDelimiter == false) {
                    startDelimiter = true;
                } else if (startDelimiter == true) {
                    endDelimiter = true;
                }
            }

            if (aData == ESCAPE)
                escapeCount += 1;

            dataBuffer[dataBufferLength] = aData;
            dataBufferLength += 1;
        }

        checkData(startDelimiter, endDelimiter);
	}
	
	private void buildPacket(byte[] dataBuffer, int size) {
		data = new byte[size];

        boolean escape = false;
        int idx = 0;
        int bufferIdx = 0;

        //remove escape sequence 0x7D 0x5D and 0x7D 0x5E
        while (idx < size) {

            if( dataBuffer[bufferIdx] == ESCAPE ) {
                escape = true;
                bufferIdx++;
            } else {
                escape = false;
            }

            if(escape == true) {
                if( dataBuffer[bufferIdx] == ESCAPE_DELIMITER )
                    data[idx] = DELIMITER;

                if( dataBuffer[bufferIdx] == ESCAPE_ESCAPE )
                    data[idx] = ESCAPE;

            } else {
                data[idx] = dataBuffer[bufferIdx];
            }

            idx++;
            bufferIdx++;
        }
		
		//trim the headers and call the callback
		data = trim(data);
		updateNewData(data);
	}
    
    public abstract void updateNewData(byte[] data);
    
//------------Interface implementation-----------------//
	
	@Override
	public void open() {
        mSerialDevice = UsbSerialProber.acquire(mUsbManager);
        Log.d(TAG, "Resumed, mSerialDevice=" + mSerialDevice);
        if (mSerialDevice == null) {
        	Log.d(TAG, "No serial device");
        } else {
            try {
                mSerialDevice.open();
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                try {
                    mSerialDevice.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                mSerialDevice = null;
                return;
            }
            Log.d(TAG, "Serial device: " + mSerialDevice);
        }

        onDeviceStateChange();
	}

	@Override
	public void close() {
        stopIoManager();
        if (mSerialDevice != null) {
            try {
                mSerialDevice.close();
            } catch (IOException e) {
                // Ignore.
            }
            mSerialDevice = null;
        }
	}

	@Override
	public byte[] read() {
		return data;
	}

	@Override
	public void write(byte[] data) {
        try {
            packetizer.writeSourcePacket(data);
        } catch (Exception e) {
            //TinyOSActivity.mDumpTextView.append("FAIL\n");
        }
	}
}

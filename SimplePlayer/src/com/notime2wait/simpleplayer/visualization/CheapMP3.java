package com.notime2wait.simpleplayer.visualization;

/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * CheapMP3 represents an MP3 file by doing a "cheap" scan of the file,
 * parsing the frame headers only and getting an extremely rough estimate
 * of the volume level of each frame.
 * 
 * TODO: Useful unit tests might be to look for sync in various places:
 * FF FA
 * FF FB
 * 00 FF FA
 * FF FF FA
 * ([ 00 ] * 12) FF FA
 * ([ 00 ] * 13) FF FA
 */
public class CheapMP3 extends CheapSoundFile {
    public static Factory getFactory() {
        return new Factory() {
            public CheapSoundFile create() {
                return new CheapMP3();
            }
            public String[] getSupportedExtensions() {
                return new String[] { "mp3" };
            }
        };
    }

    // Member variables representing frame data
    private int mNumFrames;
    //the gain spread in 0..255 range
    private int[] mHistogram = new int[256];
    //private int[] mFrameOffsets;
    //private int[] mFrameLens;
    public int[] mFrameGains;
    private int mFileSize;
    private int mAvgBitRate;
    private int mGlobalSampleRate;
    private int mGlobalChannels;

    // Member variables used during initialization
    private int mMaxFrames;
    private int mBitrateSum;
    //private int mMinGain;
   // private int mMaxGain;
    
    private final static int BUFFER_LEN = 16384;//16384;//8192;//16384;//32768;//262144; //65536;//131072;
    
    private String soundfilePath;

    public CheapMP3() {
    }

    public int getNumFrames() {
        return mNumFrames;
    }
    
    public int getLowerBound(float percent) {
    			int minGain = 0;
    	        int sum = 0;
    	        int goal = (int) (mNumFrames*percent);
    	        while (minGain < 255 && sum < goal) {
    	            sum += mHistogram[minGain];
    	            minGain++;
    	        }
    	        return minGain;
    }
    
    public int getUpperBound(float percent) {
    	
    	int sum = 0;
    	int maxGain = 255;
    	int goal = (int) (mNumFrames*percent);
        while (maxGain > 2 && sum < goal) {
            sum += mHistogram[maxGain];
            maxGain--;
        }
        return maxGain;
    }
/*
    public int[] getFrameOffsets() {
        return mFrameOffsets;
    }
*/
    public int getSamplesPerFrame() {
        return 1152;
    }
/*
    public int[] getFrameLens() {
        return mFrameLens;
    }
*/
    public int[] getFrameGains() {
        return mFrameGains;
    }

    public int getFileSizeBytes() {
        return mFileSize;        
    }

    public int getAvgBitrateKbps() {
        return mAvgBitRate;
    }

    public int getSampleRate() {
        return mGlobalSampleRate;
    }

    public int getChannels() {
        return mGlobalChannels;
    }

    public String getFiletype() {
        return "MP3";
    }

    /**
     * MP3 supports seeking into the middle of the file, no header needed,
     * so this method is supported to hear exactly what a "cut" of the file
     * sounds like without needing to actually save a file to disk first.
     */
    /*public int getSeekableFrameOffset(int frame) {
        if (frame <= 0) {
            return 0;
        } else if (frame >= mNumFrames) {
            return mFileSize;
        } else {
            return mFrameOffsets[frame];
        }
    }*/
    

    public void ReadFile(String path)
            throws java.io.FileNotFoundException,
                   java.io.IOException {
            filepath = path;
            ReadFile(new File(path));
        }

    public void ReadFile(File inputFile)
            throws java.io.FileNotFoundException,
            java.io.IOException {
        super.ReadFile(inputFile);
        soundfilePath = inputFile.getPath();
        mNumFrames = 0;
        mMaxFrames = 64;  // This will grow as needed
        //mFrameOffsets = new int[mMaxFrames];
        //mFrameLens = new int[mMaxFrames];
        mFrameGains = new int[mMaxFrames];
        mBitrateSum = 0;
        //mMinGain = 255;
        //mMaxGain = 0;

        // No need to handle filesizes larger than can fit in a 32-bit int
        mFileSize = (int)mInputFile.length();
        int samples = (int)mFileSize/BUFFER_LEN;
        int remainder = mFileSize%BUFFER_LEN;
        if (remainder!=0) samples++;
        //InputStream stream = new BufferedInputStream(new FileInputStream(mInputFile), BUFFER_LEN);
        FileInputStream stream = new FileInputStream(mInputFile);
        /*FileChannel stream =  new FileInputStream(mInputFile).getChannel();
        ByteBuffer buff = stream.map(FileChannel.MapMode.READ_ONLY, 0, stream.size());
        buff.position(0);*/
        int pos = 0;
    	byte[] buffer = new byte[BUFFER_LEN];
    	int len;
        for (int i=0; i<samples; i++) {
        	len = (i==samples-1 && remainder!=0)? remainder : BUFFER_LEN;
        	//pos=0;
        	if (pos>BUFFER_LEN) pos = pos%BUFFER_LEN;
        	else pos = 0;
        	stream.read(buffer, 0, len);
        	//buff.get(buffer, 0, len);
        	while (pos < len) {
        		
        		while (pos < len-12 && buffer[pos] != -1)
        			pos++;
        		if (pos>len-12) 
        			break;
            

            
            

            // Check for MPEG 1 Layer III or MPEG 2 Layer III codes
            int mpgVersion = 0;
            if (buffer[pos+1] == -6 || buffer[pos+1] == -5) {
                mpgVersion = 1;
            } else if (buffer[pos+1] == -14 || buffer[pos+1] == -13) {
                mpgVersion = 2;
            } else {
                pos++;
                continue;
            }

            // The third byte has the bitrate and samplerate
            int bitRate;
            int sampleRate;
            if (mpgVersion == 1) {
                // MPEG 1 Layer III
                bitRate = BITRATES_MPEG1_L3[(buffer[pos+2] & 0xF0) >> 4];
                sampleRate = SAMPLERATES_MPEG1_L3[(buffer[pos+2] & 0x0C) >> 2];
            } else {
                // MPEG 2 Layer III
                bitRate = BITRATES_MPEG2_L3[(buffer[pos+2] & 0xF0) >> 4];
                sampleRate = SAMPLERATES_MPEG2_L3[(buffer[pos+2] & 0x0C) >> 2];
            }

            if (bitRate == 0 || sampleRate == 0) {
                pos += 2;
                continue;
            }

            // From here on we assume the frame is good
            
            /*
            if (mProgressListener != null) {
                boolean keepGoing = mProgressListener.reportProgress(
                    pos * 1.0 / mFileSize);
                if (!keepGoing) {
                    break;
                }
            }*/
            
            mGlobalSampleRate = sampleRate;
            int padding = (buffer[pos+2] & 2) >> 1;
            int frameLen = 144 * bitRate * 1000 / sampleRate + padding;

            int gain;
            if ((buffer[pos+3] & 0xC0) == 0xC0) {
                // 1 channel
                mGlobalChannels = 1;
                if (mpgVersion == 1) {
                    gain = ((buffer[pos+10] & 0x01) << 7) +
                        ((buffer[pos+11] & 0xFE) >> 1);
                } else {
                    gain = ((buffer[pos+9] & 0x03) << 6) +
                    ((buffer[pos+10] & 0xFC) >> 2);
                }
            } else {
                // 2 channels
                mGlobalChannels = 2;
                if (mpgVersion == 1) {
                    gain = ((buffer[pos+9]  & 0x7F) << 1) +
                        ((buffer[pos+10] & 0x80) >> 7);
                } else {
                    gain = 0;  // ???
                }
            }

            mBitrateSum += bitRate;

            //mFrameOffsets[mNumFrames] = pos;
            //mFrameLens[mNumFrames] = frameLen;
            mFrameGains[mNumFrames] = gain;
            mHistogram[gain]++;
            //if (gain < mMinGain)
            //    mMinGain = gain;
            //if (gain > mMaxGain)
            //    mMaxGain = gain;

            mNumFrames++;
            	if (mNumFrames == mMaxFrames) {
            		// We need to grow our arrays.  Rather than naively
            		// doubling the array each time, we estimate the exact
            		// number of frames we need and add 10% padding.  In
            		// practice this seems to work quite well, only one
            		// resize is ever needed, however to avoid pathological
            		// cases we make sure to always double the size at a minimum.

            		mAvgBitRate = mBitrateSum / mNumFrames;
            		int totalFramesGuess =
            				((mFileSize / mAvgBitRate) * sampleRate) / 144000;
            		int newMaxFrames = totalFramesGuess * 11 / 10;
            		if (newMaxFrames < mMaxFrames * 2)
            			newMaxFrames = mMaxFrames * 2;

            		//int[] newOffsets = new int[newMaxFrames];
            		//int[] newLens = new int[newMaxFrames];
            		int[] newGains = new int[newMaxFrames];
            		System.arraycopy(mFrameGains, 0, newGains, 0, mNumFrames);
            		//for (int j = 0; j < mNumFrames; j++) 
            		//	newGains[j] = mFrameGains[j];
                
            		//mFrameOffsets = newOffsets;
            		//mFrameLens = newLens;
            		mFrameGains = newGains;
            		mMaxFrames = newMaxFrames;
            	}
            pos += frameLen-12;
        	}
        	if (mProgressListener != null && (i+1)%5==0) {
                boolean keepGoing = mProgressListener.reportProgress(
                    (i*BUFFER_LEN+pos) * 1.0 / mFileSize);
                if (!keepGoing) {
                    break;
                }
            }
        }
        stream.close();
        // We're done reading the file, do some postprocessing
        if (mNumFrames > 0)
            mAvgBitRate = mBitrateSum / mNumFrames;
        else
            mAvgBitRate = 0;
    }
/*
    public void WriteFile(File outputFile, int startFrame, int numFrames)
            throws java.io.IOException {
        outputFile.createNewFile();
        FileInputStream in = new FileInputStream(mInputFile);
        FileOutputStream out = new FileOutputStream(outputFile);
        int maxFrameLen = 0;
        for (int i = 0; i < numFrames; i++) {
            if (mFrameLens[startFrame + i] > maxFrameLen)
                maxFrameLen = mFrameLens[startFrame + i];
        }
        byte[] buffer = new byte[maxFrameLen];
        int pos = 0;
        for (int i = 0; i < numFrames; i++) {
            int skip = mFrameOffsets[startFrame + i] - pos;
            int len = mFrameLens[startFrame + i];
            if (skip > 0) {
                in.skip(skip);
                pos += skip;
            }
            in.read(buffer, 0, len);
            out.write(buffer, 0, len);
            pos += len;
        }
        in.close();
        out.close();
    }
*/
    static private int BITRATES_MPEG1_L3[] = {
        0,  32,  40,  48,  56,  64,  80,  96,
        112, 128, 160, 192, 224, 256, 320,  0 };
    static private int BITRATES_MPEG2_L3[] = {
        0,   8,  16,  24,  32,  40,  48,  56,
        64,  80,  96, 112, 128, 144, 160, 0 };
    static private int SAMPLERATES_MPEG1_L3[] = {
        44100, 48000, 32000, 0 };
    static private int SAMPLERATES_MPEG2_L3[] = {
        22050, 24000, 16000, 0 };
};

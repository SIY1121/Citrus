package mod;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avdevice;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.swresample;
import org.bytedeco.javacpp.swscale;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVInputFormat;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avformat.Read_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.avformat.Seek_Pointer_long_int;
import org.bytedeco.javacpp.avformat.Write_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVDictionaryEntry;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.bytedeco.javacpp.avutil.AVRational;
import org.bytedeco.javacpp.swresample.SwrContext;
import org.bytedeco.javacpp.swscale.SwsContext;
import org.bytedeco.javacpp.swscale.SwsFilter;
import org.bytedeco.javacv.FFmpegLockCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

public class FFmpegFrameGrabberMod extends FrameGrabber {
    private static Exception loadingException = null;
    static Map<Pointer, InputStream> inputStreams;
    static ReadCallback readCallback;
    static SeekCallback seekCallback;
    private InputStream inputStream;
    private AVIOContext avio;
    private String filename;
    private AVFormatContext oc;
    private AVStream video_st;
    private AVStream audio_st;
    private AVCodecContext video_c;
    private AVCodecContext audio_c;
    private AVFrame picture;
    private AVFrame picture_rgb;
    private BytePointer[] image_ptr;
    private Buffer[] image_buf;
    private AVFrame samples_frame;
    private BytePointer[] samples_ptr;
    private Buffer[] samples_buf;
    private BytePointer[] samples_ptr_out;
    private Buffer[] samples_buf_out;
    private AVPacket pkt;
    private AVPacket pkt2;
    private int sizeof_pkt;
    private int[] got_frame;
    private SwsContext img_convert_ctx;
    private SwrContext samples_convert_ctx;
    private int samples_channels;
    private int samples_format;
    private int samples_rate;
    private boolean frameGrabbed;
    private Frame frame;

    public static String[] getDeviceDescriptions() throws Exception {
        tryLoad();
        throw new UnsupportedOperationException("Device enumeration not support by FFmpeg.");
    }

    public static org.bytedeco.javacv.FFmpegFrameGrabber createDefault(File deviceFile) throws Exception {
        return new org.bytedeco.javacv.FFmpegFrameGrabber(deviceFile);
    }

    public static org.bytedeco.javacv.FFmpegFrameGrabber createDefault(String devicePath) throws Exception {
        return new org.bytedeco.javacv.FFmpegFrameGrabber(devicePath);
    }

    public static org.bytedeco.javacv.FFmpegFrameGrabber createDefault(int deviceNumber) throws Exception {
        throw new Exception(org.bytedeco.javacv.FFmpegFrameGrabber.class + " does not support device numbers.");
    }

    public static void tryLoad() throws Exception {
        if (loadingException != null) {
            throw loadingException;
        } else {
            try {
                Loader.load(avutil.class);
                Loader.load(swresample.class);
                Loader.load(avcodec.class);
                Loader.load(avformat.class);
                Loader.load(swscale.class);
                avcodec.avcodec_register_all();
                avformat.av_register_all();
                avformat.avformat_network_init();
                Loader.load(avdevice.class);
                avdevice.avdevice_register_all();
            } catch (Throwable var1) {
                if (var1 instanceof Exception) {
                    throw loadingException = (Exception) var1;
                } else {
                    throw loadingException = new Exception("Failed to load " + org.bytedeco.javacv.FFmpegFrameGrabber.class, var1);
                }
            }
        }
    }

    public FFmpegFrameGrabberMod(File file) {
        this(file.getAbsolutePath());
    }

    public FFmpegFrameGrabberMod(String filename) {
        this.filename = filename;
        this.pixelFormat = -1;
        this.sampleFormat = -1;
    }

    public FFmpegFrameGrabberMod(InputStream inputStream) {
        this.inputStream = inputStream;
        this.pixelFormat = -1;
        this.sampleFormat = -1;
    }

    public void release() throws Exception {
        this.releaseUnsafe();
    }

    void releaseUnsafe() throws Exception {
        if (this.pkt != null && this.pkt2 != null) {
            if (this.pkt2.size() > 0) {
                avcodec.av_packet_unref(this.pkt);
            }

            this.pkt = this.pkt2 = null;
        }

        int i;
        if (this.image_ptr != null) {
            for (i = 0; i < this.image_ptr.length; ++i) {
                avutil.av_free(this.image_ptr[i]);
            }

            this.image_ptr = null;
        }

        if (this.picture_rgb != null) {
            avutil.av_frame_free(this.picture_rgb);
            this.picture_rgb = null;
        }

        if (this.picture != null) {
            avutil.av_frame_free(this.picture);
            this.picture = null;
        }

        if (this.video_c != null) {
            avcodec.avcodec_free_context(this.video_c);
            this.video_c = null;
        }

        if (this.samples_frame != null) {
            avutil.av_frame_free(this.samples_frame);
            this.samples_frame = null;
        }

        if (this.audio_c != null) {
            avcodec.avcodec_free_context(this.audio_c);
            this.audio_c = null;
        }

        if (this.inputStream == null && this.oc != null && !this.oc.isNull()) {
            avformat.avformat_close_input(this.oc);
            this.oc = null;
        }

        if (this.img_convert_ctx != null) {
            swscale.sws_freeContext(this.img_convert_ctx);
            this.img_convert_ctx = null;
        }

        if (this.samples_ptr_out != null) {
            for (i = 0; i < this.samples_ptr_out.length; ++i) {
                avutil.av_free(this.samples_ptr_out[i].position(0L));
            }

            this.samples_ptr_out = null;
            this.samples_buf_out = null;
        }

        if (this.samples_convert_ctx != null) {
            swresample.swr_free(this.samples_convert_ctx);
            this.samples_convert_ctx = null;
        }

        this.got_frame = null;
        this.frameGrabbed = false;
        this.frame = null;
        this.timestamp = 0L;
        this.frameNumber = 0;
        if (this.inputStream != null) {
            try {
                if (this.oc == null) {
                    this.inputStream.close();
                } else {
                    this.inputStream.reset();
                }
            } catch (IOException var5) {
                throw new Exception("Error on InputStream.close(): ", var5);
            } finally {
                inputStreams.remove(this.oc);
                if (this.avio != null) {
                    if (this.avio.buffer() != null) {
                        avutil.av_free(this.avio.buffer());
                        this.avio.buffer((BytePointer) null);
                    }

                    avutil.av_free(this.avio);
                    this.avio = null;
                }

                if (this.oc != null) {
                    avformat.avformat_free_context(this.oc);
                    this.oc = null;
                }

            }
        }

    }

    protected void finalize() throws Throwable {
        super.finalize();
        this.release();
    }

    public double getGamma() {
        return this.gamma == 0.0D ? 2.2D : this.gamma;
    }

    public String getFormat() {
        return this.oc == null ? super.getFormat() : this.oc.iformat().name().getString();
    }

    public int getImageWidth() {
        return this.imageWidth <= 0 && this.video_c != null ? this.video_c.width() : super.getImageWidth();
    }

    public int getImageHeight() {
        return this.imageHeight <= 0 && this.video_c != null ? this.video_c.height() : super.getImageHeight();
    }

    public int getAudioChannels() {
        return this.audioChannels <= 0 && this.audio_c != null ? this.audio_c.channels() : super.getAudioChannels();
    }

    public int getPixelFormat() {
        if (this.imageMode != ImageMode.COLOR && this.imageMode != ImageMode.GRAY) {
            return this.video_c != null ? this.video_c.pix_fmt() : super.getPixelFormat();
        } else if (this.pixelFormat == -1) {
            return this.imageMode == ImageMode.COLOR ? 3 : 8;
        } else {
            return this.pixelFormat;
        }
    }

    public int getVideoCodec() {
        return this.video_c == null ? super.getVideoCodec() : this.video_c.codec_id();
    }

    public int getVideoBitrate() {
        return this.video_c == null ? super.getVideoBitrate() : (int) this.video_c.bit_rate();
    }

    public double getAspectRatio() {
        if (this.video_st == null) {
            return super.getAspectRatio();
        } else {
            AVRational r = avformat.av_guess_sample_aspect_ratio(this.oc, this.video_st, this.picture);
            double a = (double) r.num() / (double) r.den();
            return a == 0.0D ? 1.0D : a;
        }
    }

    public double getFrameRate() {
        if (this.video_st == null) {
            return super.getFrameRate();
        } else {
            AVRational r = this.video_st.avg_frame_rate();
            if (r.num() == 0 && r.den() == 0) {
                r = this.video_st.r_frame_rate();
            }

            return (double) r.num() / (double) r.den();
        }
    }

    public int getAudioCodec() {
        return this.audio_c == null ? super.getAudioCodec() : this.audio_c.codec_id();
    }

    public int getAudioBitrate() {
        return this.audio_c == null ? super.getAudioBitrate() : (int) this.audio_c.bit_rate();
    }

    public int getSampleFormat() {
        if (this.sampleMode != SampleMode.SHORT && this.sampleMode != SampleMode.FLOAT) {
            return this.audio_c != null ? this.audio_c.sample_fmt() : super.getSampleFormat();
        } else if (this.sampleFormat == -1) {
            return this.sampleMode == SampleMode.SHORT ? 1 : 3;
        } else {
            return this.sampleFormat;
        }
    }

    public int getSampleRate() {
        return this.sampleRate <= 0 && this.audio_c != null ? this.audio_c.sample_rate() : super.getSampleRate();
    }

    public String getMetadata(String key) {
        if (this.oc == null) {
            return super.getMetadata(key);
        } else {
            AVDictionaryEntry entry = avutil.av_dict_get(this.oc.metadata(), key, (AVDictionaryEntry) null, 0);
            return entry != null && entry.value() != null ? entry.value().getString() : null;
        }
    }

    public String getVideoMetadata(String key) {
        if (this.video_st == null) {
            return super.getVideoMetadata(key);
        } else {
            AVDictionaryEntry entry = avutil.av_dict_get(this.video_st.metadata(), key, (AVDictionaryEntry) null, 0);
            return entry != null && entry.value() != null ? entry.value().getString() : null;
        }
    }

    public String getAudioMetadata(String key) {
        if (this.audio_st == null) {
            return super.getAudioMetadata(key);
        } else {
            AVDictionaryEntry entry = avutil.av_dict_get(this.audio_st.metadata(), key, (AVDictionaryEntry) null, 0);
            return entry != null && entry.value() != null ? entry.value().getString() : null;
        }
    }

    public void setFrameNumber(int frameNumber) throws Exception {
        this.setTimestamp(Math.round((double) (1000000L * (long) frameNumber) / this.getFrameRate()));
    }

    public void setTimestamp(long timestamp) throws Exception {
        if (this.oc == null) {
            super.setTimestamp(timestamp);
        } else {
            timestamp = timestamp * 1000000L / 1000000L;
            if (this.oc.start_time() != avutil.AV_NOPTS_VALUE) {
                timestamp += this.oc.start_time();
            }

            int ret;
            if ((ret = avformat.avformat_seek_file(this.oc, -1, -9223372036854775808L, timestamp, 9223372036854775807L, 1)) < 0) {
                throw new Exception("avformat_seek_file() error " + ret + ": Could not seek file to timestamp " + timestamp + ".");
            }

            if (this.video_c != null) {
                avcodec.avcodec_flush_buffers(this.video_c);
            }

            if (this.audio_c != null) {
                avcodec.avcodec_flush_buffers(this.audio_c);
            }

            if (this.pkt2.size() > 0) {
                this.pkt2.size(0);
                avcodec.av_packet_unref(this.pkt);
            }

            int var4 = 0;

            while (this.timestamp > timestamp + 1L && this.grabFrame(true, true, false, false) != null && var4++ < 1000) {
                ;
            }

            var4 = 0;

            while (this.timestamp < timestamp - 1L && this.grabFrame(true, true, false, false) != null && var4++ < 1000) {
                ;
            }

            if (this.video_c != null) {
                this.frameGrabbed = true;
            }
        }

    }

    public int getLengthInFrames() {
        return (int) ((double) this.getLengthInTime() * this.getFrameRate() / 1000000.0D);
    }

    public long getLengthInTime() {
        return this.oc.duration() * 1000000L / 1000000L;
    }

    public AVFormatContext getFormatContext() {
        return this.oc;
    }

    public void start() throws Exception {
        this.startUnsafe();
    }

    void startUnsafe() throws Exception {
        this.img_convert_ctx = null;
        this.oc = new AVFormatContext((Pointer) null);
        this.video_c = null;
        this.audio_c = null;
        this.pkt = new AVPacket();
        this.pkt2 = new AVPacket();
        this.sizeof_pkt = this.pkt.sizeof();
        this.got_frame = new int[1];
        this.frameGrabbed = false;
        this.frame = new Frame();
        this.timestamp = 0L;
        this.frameNumber = 0;
        this.pkt2.size(0);
        AVInputFormat f = null;
        if (this.format != null && this.format.length() > 0 && (f = avformat.av_find_input_format(this.format)) == null) {
            throw new Exception("av_find_input_format() error: Could not find input format \"" + this.format + "\".");
        } else {
            AVDictionary options = new AVDictionary((Pointer) null);
            if (this.frameRate > 0.0D) {
                AVRational r = avutil.av_d2q(this.frameRate, 1001000);
                avutil.av_dict_set(options, "framerate", r.num() + "/" + r.den(), 0);
            }

            if (this.pixelFormat >= 0) {
                avutil.av_dict_set(options, "pixel_format", avutil.av_get_pix_fmt_name(this.pixelFormat).getString(), 0);
            } else if (this.imageMode != ImageMode.RAW) {
                avutil.av_dict_set(options, "pixel_format", this.imageMode == ImageMode.COLOR ? "bgr24" : "gray8", 0);
            }

            if (this.imageWidth > 0 && this.imageHeight > 0) {
                avutil.av_dict_set(options, "video_size", this.imageWidth + "x" + this.imageHeight, 0);
            }

            if (this.sampleRate > 0) {
                avutil.av_dict_set(options, "sample_rate", "" + this.sampleRate, 0);
            }

            if (this.audioChannels > 0) {
                avutil.av_dict_set(options, "channels", "" + this.audioChannels, 0);
            }

            Iterator var10 = this.options.entrySet().iterator();

            while (var10.hasNext()) {
                Entry<String, String> e = (Entry) var10.next();
                avutil.av_dict_set(options, (String) e.getKey(), (String) e.getValue(), 0);
            }

            if (this.inputStream != null) {
                if (!this.inputStream.markSupported()) {
                    this.inputStream = new BufferedInputStream(this.inputStream);
                }

                this.inputStream.mark(2147483639);
                this.oc = avformat.avformat_alloc_context();
                this.avio = avformat.avio_alloc_context(new BytePointer(avutil.av_malloc(4096L)), 4096, 0, this.oc, readCallback, (Write_packet_Pointer_BytePointer_int) null, seekCallback);
                this.oc.pb(this.avio);
                this.filename = this.inputStream.toString();
                inputStreams.put(this.oc, this.inputStream);
            }

            int ret;
            if (avformat.avformat_open_input(this.oc, this.filename, f, options) < 0) {
                avutil.av_dict_set(options, "pixel_format", (String) null, 0);
                if ((ret = avformat.avformat_open_input(this.oc, this.filename, f, options)) < 0) {
                    throw new Exception("avformat_open_input() error " + ret + ": Could not open input \"" + this.filename + "\". (Has setFormat() been called?)");
                }
            }

            avutil.av_dict_free(options);
            if ((ret = avformat.avformat_find_stream_info(this.oc, (PointerPointer) null)) < 0) {
                throw new Exception("avformat_find_stream_info() error " + ret + ": Could not find stream information.");
            } else {
                if (avutil.av_log_get_level() >= 32) {
                    avformat.av_dump_format(this.oc, 0, this.filename, 0);
                }

                this.video_st = this.audio_st = null;
                AVCodecParameters video_par = null;
                AVCodecParameters audio_par = null;
                int nb_streams = this.oc.nb_streams();

                for (int i = 0; i < nb_streams; ++i) {
                    AVStream st = this.oc.streams(i);
                    AVCodecParameters par = st.codecpar();
                    if (this.video_st == null && par.codec_type() == 0 && (this.videoStream < 0 || this.videoStream == i)) {
                        this.video_st = st;
                        video_par = par;
                    } else if (this.audio_st == null && par.codec_type() == 1 && (this.audioStream < 0 || this.audioStream == i)) {
                        this.audio_st = st;
                        audio_par = par;
                    }
                }

                if (this.video_st == null && this.audio_st == null) {
                    throw new Exception("Did not find a video or audio stream inside \"" + this.filename + "\" for videoStream == " + this.videoStream + " and audioStream == " + this.audioStream + ".");
                } else {
                    AVCodec codec;
                    Iterator var14;
                    Entry e;
                    if (this.video_st != null) {
//                        if (video_par.codec_id() == avcodec.AV_CODEC_ID_H264)
//                            codec = avcodec.avcodec_find_decoder_by_name("h264_qsv");
//                        else
                            codec = avcodec.avcodec_find_decoder(video_par.codec_id());

                        if (codec == null) {
                            throw new Exception("avcodec_find_decoder() error: Unsupported video format or codec not found: " + video_par.codec_id() + ".");
                        }

                        if ((this.video_c = avcodec.avcodec_alloc_context3(codec)) == null) {
                            throw new Exception("avcodec_alloc_context3() error: Could not allocate video decoding context.");
                        }

                        if (avcodec.avcodec_parameters_to_context(this.video_c, this.video_st.codecpar()) < 0) {
                            this.release();
                            throw new Exception("avcodec_parameters_to_context() error: Could not copy the video stream parameters.");
                        }

                        options = new AVDictionary((Pointer) null);
                        var14 = this.videoOptions.entrySet().iterator();

                        while (var14.hasNext()) {
                            e = (Entry) var14.next();
                            avutil.av_dict_set(options, (String) e.getKey(), (String) e.getValue(), 0);
                        }

                        if ((ret = avcodec.avcodec_open2(this.video_c, codec, options)) < 0) {
                            throw new Exception("avcodec_open2() error " + ret + ": Could not open video codec.");
                        }

                        avutil.av_dict_free(options);
                        if (this.video_c.time_base().num() > 1000 && this.video_c.time_base().den() == 1) {
                            this.video_c.time_base().den(1000);
                        }

                        if ((this.picture = avutil.av_frame_alloc()) == null) {
                            throw new Exception("av_frame_alloc() error: Could not allocate raw picture frame.");
                        }

                        if ((this.picture_rgb = avutil.av_frame_alloc()) == null) {
                            throw new Exception("av_frame_alloc() error: Could not allocate RGB picture frame.");
                        }

                        this.initPictureRGB();
                    }

                    if (this.audio_st != null) {
                        codec = avcodec.avcodec_find_decoder(audio_par.codec_id());
                        if (codec == null) {
                            throw new Exception("avcodec_find_decoder() error: Unsupported audio format or codec not found: " + audio_par.codec_id() + ".");
                        }

                        if ((this.audio_c = avcodec.avcodec_alloc_context3(codec)) == null) {
                            throw new Exception("avcodec_alloc_context3() error: Could not allocate audio decoding context.");
                        }

                        if (avcodec.avcodec_parameters_to_context(this.audio_c, this.audio_st.codecpar()) < 0) {
                            this.release();
                            throw new Exception("avcodec_parameters_to_context() error: Could not copy the audio stream parameters.");
                        }

                        options = new AVDictionary((Pointer) null);
                        var14 = this.audioOptions.entrySet().iterator();

                        while (var14.hasNext()) {
                            e = (Entry) var14.next();
                            avutil.av_dict_set(options, (String) e.getKey(), (String) e.getValue(), 0);
                        }

                        if ((ret = avcodec.avcodec_open2(this.audio_c, codec, options)) < 0) {
                            throw new Exception("avcodec_open2() error " + ret + ": Could not open audio codec.");
                        }

                        avutil.av_dict_free(options);
                        if ((this.samples_frame = avutil.av_frame_alloc()) == null) {
                            throw new Exception("av_frame_alloc() error: Could not allocate audio frame.");
                        }
                    }

                }
            }
        }
    }

    private void initPictureRGB() {
        int width = this.imageWidth > 0 ? this.imageWidth : this.video_c.width();
        int height = this.imageHeight > 0 ? this.imageHeight : this.video_c.height();
        switch (this.imageMode) {
            case COLOR:
            case GRAY:
                if (this.image_ptr != null) {
                    this.image_buf = null;
                    BytePointer[] temp = this.image_ptr;
                    this.image_ptr = null;
                    avutil.av_free(temp[0]);
                }

                int fmt = this.getPixelFormat();
                int size = avutil.av_image_get_buffer_size(fmt, width, height, 1);
                this.image_ptr = new BytePointer[]{(new BytePointer(avutil.av_malloc((long) size))).capacity((long) size)};
                this.image_buf = new Buffer[]{this.image_ptr[0].asBuffer()};
                avutil.av_image_fill_arrays(new PointerPointer(this.picture_rgb), this.picture_rgb.linesize(), this.image_ptr[0], fmt, width, height, 1);
                this.picture_rgb.format(fmt);
                this.picture_rgb.width(width);
                this.picture_rgb.height(height);
                break;
            case RAW:
                this.image_ptr = new BytePointer[]{null};
                this.image_buf = new Buffer[]{null};
                break;
            default:
                assert false;
        }

    }

    public void stop() throws Exception {
        this.release();
    }

    public void trigger() throws Exception {
        if (this.oc != null && !this.oc.isNull()) {
            if (this.pkt2.size() > 0) {
                this.pkt2.size(0);
                avcodec.av_packet_unref(this.pkt);
            }

            for (int i = 0; i < this.numBuffers + 1; ++i) {
                if (avformat.av_read_frame(this.oc, this.pkt) < 0) {
                    return;
                }

                avcodec.av_packet_unref(this.pkt);
            }

        } else {
            throw new Exception("Could not trigger: No AVFormatContext. (Has start() been called?)");
        }
    }

    private void processImage() throws Exception {
        this.frame.imageWidth = this.imageWidth > 0 ? this.imageWidth : this.video_c.width();
        this.frame.imageHeight = this.imageHeight > 0 ? this.imageHeight : this.video_c.height();
        this.frame.imageDepth = 8;
        switch (this.imageMode) {
            case COLOR:
            case GRAY:
                if (this.deinterlace) {
                    throw new Exception("Cannot deinterlace: Functionality moved to FFmpegFrameFilter.");
                }

                if (this.frame.imageWidth != this.picture_rgb.width() || this.frame.imageHeight != this.picture_rgb.height()) {
                    this.initPictureRGB();
                }

                this.img_convert_ctx = swscale.sws_getCachedContext(this.img_convert_ctx, this.video_c.width(), this.video_c.height(), this.video_c.pix_fmt(), this.frame.imageWidth, this.frame.imageHeight, this.getPixelFormat(), 2, (SwsFilter) null, (SwsFilter) null, (DoublePointer) null);
                if (this.img_convert_ctx == null) {
                    throw new Exception("sws_getCachedContext() error: Cannot initialize the conversion context.");
                }

                swscale.sws_scale(this.img_convert_ctx, new PointerPointer(this.picture), this.picture.linesize(), 0, this.video_c.height(), new PointerPointer(this.picture_rgb), this.picture_rgb.linesize());
                this.frame.imageStride = this.picture_rgb.linesize(0);
                this.frame.image = this.image_buf;
                break;
            case RAW:
                this.frame.imageStride = this.picture.linesize(0);
                BytePointer ptr = this.picture.data(0);
                if (ptr != null && !ptr.equals(this.image_ptr[0])) {
                    this.image_ptr[0] = ptr.capacity((long) (this.frame.imageHeight * this.frame.imageStride));
                    this.image_buf[0] = ptr.asBuffer();
                }

                this.frame.image = this.image_buf;
                break;
            default:
                assert false;
        }

        this.frame.image[0].limit(this.frame.imageHeight * this.frame.imageStride);
        this.frame.imageChannels = this.frame.imageStride / this.frame.imageWidth;
    }

    private void processSamples() throws Exception {
        int sample_format = this.samples_frame.format();
        int planes = avutil.av_sample_fmt_is_planar(sample_format) != 0 ? this.samples_frame.channels() : 1;
        int data_size = avutil.av_samples_get_buffer_size((IntPointer) null, this.audio_c.channels(), this.samples_frame.nb_samples(), this.audio_c.sample_fmt(), 1) / planes;
        if (this.samples_buf == null || this.samples_buf.length != planes) {
            this.samples_ptr = new BytePointer[planes];
            this.samples_buf = new Buffer[planes];
        }

        this.frame.sampleRate = this.audio_c.sample_rate();
        this.frame.audioChannels = this.audio_c.channels();
        this.frame.samples = this.samples_buf;
        int sample_size = data_size / avutil.av_get_bytes_per_sample(sample_format);

        int sample_size_in;
        for (sample_size_in = 0; sample_size_in < planes; ++sample_size_in) {
            BytePointer p = this.samples_frame.data(sample_size_in);
            if (!p.equals(this.samples_ptr[sample_size_in]) || this.samples_ptr[sample_size_in].capacity() < (long) data_size) {
                this.samples_ptr[sample_size_in] = p.capacity((long) data_size);
                ByteBuffer b = p.asBuffer();
                switch (sample_format) {
                    case 0:
                    case 5:
                        this.samples_buf[sample_size_in] = b;
                        break;
                    case 1:
                    case 6:
                        this.samples_buf[sample_size_in] = b.asShortBuffer();
                        break;
                    case 2:
                    case 7:
                        this.samples_buf[sample_size_in] = b.asIntBuffer();
                        break;
                    case 3:
                    case 8:
                        this.samples_buf[sample_size_in] = b.asFloatBuffer();
                        break;
                    case 4:
                    case 9:
                        this.samples_buf[sample_size_in] = b.asDoubleBuffer();
                        break;
                    default:
                        assert false;
                }
            }

            this.samples_buf[sample_size_in].position(0).limit(sample_size);
        }

        if (this.audio_c.channels() != this.getAudioChannels() || this.audio_c.sample_fmt() != this.getSampleFormat() || this.audio_c.sample_rate() != this.getSampleRate()) {
            int ret;
            if (this.samples_convert_ctx == null || this.samples_channels != this.getAudioChannels() || this.samples_format != this.getSampleFormat() || this.samples_rate != this.getSampleRate()) {
                this.samples_convert_ctx = swresample.swr_alloc_set_opts(this.samples_convert_ctx, avutil.av_get_default_channel_layout(this.getAudioChannels()), this.getSampleFormat(), this.getSampleRate(), avutil.av_get_default_channel_layout(this.audio_c.channels()), this.audio_c.sample_fmt(), this.audio_c.sample_rate(), 0, (Pointer) null);
                if (this.samples_convert_ctx == null) {
                    throw new Exception("swr_alloc_set_opts() error: Cannot allocate the conversion context.");
                }

                if ((ret = swresample.swr_init(this.samples_convert_ctx)) < 0) {
                    throw new Exception("swr_init() error " + ret + ": Cannot initialize the conversion context.");
                }

                this.samples_channels = this.getAudioChannels();
                this.samples_format = this.getSampleFormat();
                this.samples_rate = this.getSampleRate();
            }

            sample_size_in = this.samples_frame.nb_samples();
            int planes_out = avutil.av_sample_fmt_is_planar(this.samples_format) != 0 ? this.samples_frame.channels() : 1;
            int sample_size_out = swresample.swr_get_out_samples(this.samples_convert_ctx, sample_size_in);
            int sample_bytes_out = avutil.av_get_bytes_per_sample(this.samples_format);
            int buffer_size_out = sample_size_out * sample_bytes_out * (planes_out > 1 ? 1 : this.samples_channels);
            int i;
            if (this.samples_buf_out == null || this.samples_buf.length != planes_out || this.samples_ptr_out[0].capacity() < (long) buffer_size_out) {
                for (i = 0; this.samples_ptr_out != null && i < this.samples_ptr_out.length; ++i) {
                    avutil.av_free(this.samples_ptr_out[i].position(0L));
                }

                this.samples_ptr_out = new BytePointer[planes_out];
                this.samples_buf_out = new Buffer[planes_out];

                for (i = 0; i < planes_out; ++i) {
                    this.samples_ptr_out[i] = (new BytePointer(avutil.av_malloc((long) buffer_size_out))).capacity((long) buffer_size_out);
                    ByteBuffer b = this.samples_ptr_out[i].asBuffer();
                    switch (this.samples_format) {
                        case 0:
                        case 5:
                            this.samples_buf_out[i] = b;
                            break;
                        case 1:
                        case 6:
                            this.samples_buf_out[i] = b.asShortBuffer();
                            break;
                        case 2:
                        case 7:
                            this.samples_buf_out[i] = b.asIntBuffer();
                            break;
                        case 3:
                        case 8:
                            this.samples_buf_out[i] = b.asFloatBuffer();
                            break;
                        case 4:
                        case 9:
                            this.samples_buf_out[i] = b.asDoubleBuffer();
                            break;
                        default:
                            assert false;
                    }
                }
            }

            this.frame.sampleRate = this.samples_rate;
            this.frame.audioChannels = this.samples_channels;
            this.frame.samples = this.samples_buf_out;
            if ((ret = swresample.swr_convert(this.samples_convert_ctx, new PointerPointer(this.samples_ptr_out), sample_size_out, new PointerPointer(this.samples_ptr), sample_size_in)) < 0) {
                throw new Exception("swr_convert() error " + ret + ": Cannot convert audio samples.");
            }

            for (i = 0; i < planes_out; ++i) {
                this.samples_ptr_out[i].position(0L).limit((long) (ret * (planes_out > 1 ? 1 : this.samples_channels)));
                this.samples_buf_out[i].position(0).limit(ret * (planes_out > 1 ? 1 : this.samples_channels));
            }
        }

    }

    public Frame grab() throws Exception {
        return this.grabFrame(true, true, true, false);
    }

    public Frame grabImage() throws Exception {
        return this.grabFrame(false, true, true, false);
    }

    public Frame grabSamples() throws Exception {
        return this.grabFrame(true, false, true, false);
    }

    public Frame grabKeyFrame() throws Exception {
        return this.grabFrame(false, true, true, true);
    }

    public Frame grabFrame(boolean doAudio, boolean doVideo, boolean processImage, boolean keyFrames) throws Exception {
        if (this.oc != null && !this.oc.isNull()) {
            if ((!doVideo || this.video_st == null) && (!doAudio || this.audio_st == null)) {
                return null;
            } else {
                this.frame.keyFrame = false;
                this.frame.imageWidth = 0;
                this.frame.imageHeight = 0;
                this.frame.imageDepth = 0;
                this.frame.imageChannels = 0;
                this.frame.imageStride = 0;
                this.frame.image = null;
                this.frame.sampleRate = 0;
                this.frame.audioChannels = 0;
                this.frame.samples = null;
                this.frame.opaque = null;
                if (doVideo && this.frameGrabbed) {
                    this.frameGrabbed = false;
                    if (processImage) {
                        this.processImage();
                    }

                    this.frame.keyFrame = this.picture.key_frame() != 0;
                    this.frame.image = this.image_buf;
                    this.frame.opaque = this.picture;
                    return this.frame;
                } else {
                    boolean done = false;

                    while (!done) {
                        if (this.pkt2.size() <= 0 && avformat.av_read_frame(this.oc, this.pkt) < 0) {
                            if (!doVideo || this.video_st == null) {
                                return null;
                            }

                            this.pkt.stream_index(this.video_st.index());
                            this.pkt.flags(1);
                            this.pkt.data((BytePointer) null);
                            this.pkt.size(0);
                        }

                        int len;
                        long pts;
                        AVRational time_base;
                        if (!doVideo || this.video_st == null || this.pkt.stream_index() != this.video_st.index() || keyFrames && this.pkt.flags() != 1) {
                            if (doAudio && this.audio_st != null && this.pkt.stream_index() == this.audio_st.index()) {
                                if (this.pkt2.size() <= 0) {
                                    BytePointer.memcpy(this.pkt2, this.pkt, (long) this.sizeof_pkt);
                                }

                                avutil.av_frame_unref(this.samples_frame);
                                len = avcodec.avcodec_decode_audio4(this.audio_c, this.samples_frame, this.got_frame, this.pkt2);
                                if (len <= 0) {
                                    this.pkt2.size(0);
                                } else {
                                    this.pkt2.data(this.pkt2.data().position((long) len));
                                    this.pkt2.size(this.pkt2.size() - len);
                                    if (this.got_frame[0] != 0) {
                                        pts = avutil.av_frame_get_best_effort_timestamp(this.samples_frame);
                                        time_base = this.audio_st.time_base();
                                        this.timestamp = 1000000L * pts * (long) time_base.num() / (long) time_base.den();
                                        this.processSamples();
                                        done = true;
                                        this.frame.timestamp = this.timestamp;
                                        this.frame.keyFrame = this.samples_frame.key_frame() != 0;
                                        this.frame.opaque = this.samples_frame;
                                    }
                                }
                            }
                        } else {
                            len = avcodec.avcodec_decode_video2(this.video_c, this.picture, this.got_frame, this.pkt);
                            if (len < 0 || this.got_frame[0] == 0 || keyFrames && this.picture.pict_type() != 1) {
                                if (this.pkt.data() == null && this.pkt.size() == 0) {
                                    return null;
                                }
                            } else {
                                pts = avutil.av_frame_get_best_effort_timestamp(this.picture);
                                time_base = this.video_st.time_base();
                                this.timestamp = 1000000L * pts * (long) time_base.num() / (long) time_base.den();
                                this.frameNumber = (int) ((double) this.timestamp * this.getFrameRate() / 1000000.0D);
                                if (processImage) {
                                    this.processImage();
                                }

                                done = true;
                                this.frame.timestamp = this.timestamp;
                                this.frame.keyFrame = this.picture.key_frame() != 0;
                                this.frame.image = this.image_buf;
                                this.frame.opaque = this.picture;
                            }
                        }

                        if (this.pkt2.size() <= 0) {
                            avcodec.av_packet_unref(this.pkt);
                        }
                    }

                    return this.frame;
                }
            }
        } else {
            throw new Exception("Could not grab: No AVFormatContext. (Has start() been called?)");
        }
    }

    public AVPacket grabPacket() throws Exception {
        if (this.oc != null && !this.oc.isNull()) {
            return avformat.av_read_frame(this.oc, this.pkt) < 0 ? null : this.pkt;
        } else {
            throw new Exception("Could not trigger: No AVFormatContext. (Has start() been called?)");
        }
    }

    static {
        try {
            tryLoad();
            FFmpegLockCallback.init();
        } catch (Exception var1) {
            ;
        }

        inputStreams = Collections.synchronizedMap(new HashMap());
        readCallback = new ReadCallback();
        seekCallback = new SeekCallback();
    }

    static class SeekCallback extends Seek_Pointer_long_int {
        SeekCallback() {
        }

        public long call(Pointer opaque, long offset, int whence) {
            try {
                InputStream is = (InputStream) FFmpegFrameGrabberMod.inputStreams.get(opaque);
                switch (whence) {
                    case 0:
                        is.reset();
                    case 1:
                        long skipped;
                        for (long remaining = offset; remaining > 0L; remaining -= skipped) {
                            skipped = is.skip(remaining);
                            if (skipped == 0L) {
                                break;
                            }
                        }

                        return 0L;
                    default:
                        return -1L;
                }
            } catch (Throwable var10) {
                System.err.println("Error on InputStream.reset() or skip(): " + var10);
                return -1L;
            }
        }
    }

    static class ReadCallback extends Read_packet_Pointer_BytePointer_int {
        ReadCallback() {
        }

        public int call(Pointer opaque, BytePointer buf, int buf_size) {
            try {
                byte[] b = new byte[buf_size];
                InputStream is = (InputStream) FFmpegFrameGrabberMod.inputStreams.get(opaque);
                int size = is.read(b, 0, buf_size);
                if (size < 0) {
                    return 0;
                } else {
                    buf.put(b, 0, size);
                    return size;
                }
            } catch (Throwable var7) {
                System.err.println("Error on InputStream.read(): " + var7);
                return -1;
            }
        }
    }
}


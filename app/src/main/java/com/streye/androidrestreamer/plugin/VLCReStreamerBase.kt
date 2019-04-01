package com.streye.androidrestreamer.plugin

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import com.pedro.encoder.audio.AudioEncoder
import com.pedro.encoder.audio.GetAacData
import com.pedro.encoder.input.audio.GetMicrophoneData
import com.pedro.encoder.input.audio.MicrophoneManager
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.utils.CodecUtil
import com.pedro.encoder.video.FormatVideoEncoder
import com.pedro.encoder.video.GetVideoData
import com.pedro.encoder.video.VideoEncoder
import com.pedro.rtplibrary.base.RecordController
import com.pedro.rtplibrary.view.GlInterface
import com.pedro.rtplibrary.view.LightOpenGlView
import com.pedro.rtplibrary.view.OffScreenGlThread
import com.pedro.rtplibrary.view.OpenGlView
import com.pedro.vlc.VlcListener
import com.pedro.vlc.VlcVideoLibrary
import java.io.IOException
import java.nio.ByteBuffer


/**
 * Created by pedro on 29/03/19.
 */

abstract class VLCReStreamerBase : GetAacData, GetVideoData, GetMicrophoneData, VlcListener {

  private val TAG = "VLCReStreamerBase"

  private var context: Context? = null
  private var vlcVideoLibrary: VlcVideoLibrary? = null
  protected lateinit var videoEncoder: VideoEncoder
  private lateinit var microphoneManager: MicrophoneManager
  private lateinit var audioEncoder: AudioEncoder
  private var glInterface: GlInterface? = null
  private var streaming = false
  private var videoEnabled = true
  private var onPreview = false
  private lateinit var recordController: RecordController

  private val options = arrayListOf(":fullscreen")

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  constructor(openGlView: OpenGlView) {
    context = openGlView.context
    this.glInterface = openGlView
    glInterface?.init()
    init()
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  constructor(lightOpenGlView: LightOpenGlView) {
    context = lightOpenGlView.context
    this.glInterface = lightOpenGlView
    this.glInterface?.init()
    init()
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  constructor(context: Context) {
    this.context = context
    glInterface = OffScreenGlThread(context)
    glInterface?.init()
    init()
  }

  private fun init() {
    videoEncoder = VideoEncoder(this)
    microphoneManager = MicrophoneManager(this)
    audioEncoder = AudioEncoder(this)
    recordController = RecordController()
    vlcVideoLibrary = VlcVideoLibrary(context, this, glInterface?.surfaceTexture)
    vlcVideoLibrary?.setOptions(options)
  }

  /**
   * Basic auth developed to work with Wowza. No tested with other server
   *
   * @param user auth.
   * @param password auth.
   */
  abstract fun setAuthorization(user: String, password: String)

  /**
   * Call this method before use @startStream. If not you will do a stream without video. NOTE:
   * Rotation with encoder is silence ignored in some devices.
   *
   * @param width resolution in px.
   * @param height resolution in px.
   * @param fps frames per second of the stream.
   * @param bitrate H264 in kb.
   * @param hardwareRotation true if you want rotate using encoder, false if you want rotate with
   * software if you are using a SurfaceView or TextureView or with OpenGl if you are using
   * OpenGlView.
   * @param rotation could be 90, 180, 270 or 0. You should use CameraHelper.getCameraOrientation
   * with SurfaceView or TextureView and 0 with OpenGlView or LightOpenGlView. NOTE: Rotation with
   * encoder is silence ignored in some devices.
   * @return true if success, false if you get a error (Normally because the encoder selected
   * doesn't support any configuration seated or your device hasn't a H264 encoder).
   */
  fun prepareVideo(width: Int, height: Int, fps: Int, bitrate: Int, hardwareRotation: Boolean,
                   iFrameInterval: Int, rotation: Int): Boolean {
    if (onPreview) {
      stopPreview()
      onPreview = true
    }
    val formatVideoEncoder = FormatVideoEncoder.SURFACE
    return videoEncoder.prepareVideoEncoder(width, height, fps, bitrate, rotation, hardwareRotation,
        iFrameInterval, formatVideoEncoder)
  }

  /**
   * backward compatibility reason
   */
  fun prepareVideo(width: Int, height: Int, fps: Int, bitrate: Int, hardwareRotation: Boolean,
                   rotation: Int): Boolean {
    return prepareVideo(width, height, fps, bitrate, hardwareRotation, 2, rotation)
  }

  protected abstract fun prepareAudioRtp(isStereo: Boolean, sampleRate: Int)

  /**
   * Call this method before use @startStream. If not you will do a stream without audio.
   *
   * @param bitrate AAC in kb.
   * @param sampleRate of audio in hz. Can be 8000, 16000, 22500, 32000, 44100.
   * @param isStereo true if you want Stereo audio (2 audio channels), false if you want Mono audio
   * (1 audio channel).
   * @param echoCanceler true enable echo canceler, false disable.
   * @param noiseSuppressor true enable noise suppressor, false  disable.
   * @return true if success, false if you get a error (Normally because the encoder selected
   * doesn't support any configuration seated or your device hasn't a AAC encoder).
   */
  fun prepareAudio(bitrate: Int, sampleRate: Int, isStereo: Boolean, echoCanceler: Boolean,
                   noiseSuppressor: Boolean): Boolean {
    microphoneManager.createMicrophone(sampleRate, isStereo, echoCanceler, noiseSuppressor)
    prepareAudioRtp(isStereo, sampleRate)
    return audioEncoder.prepareAudioEncoder(bitrate, sampleRate, isStereo)
  }

  /**
   * Same to call: rotation = 0; if (Portrait) rotation = 90; prepareVideo(640, 480, 30, 1200 *
   * 1024, false, rotation);
   *
   * @return true if success, false if you get a error (Normally because the encoder selected
   * doesn't support any configuration seated or your device hasn't a H264 encoder).
   */
  fun prepareVideo(): Boolean {
    val rotation = CameraHelper.getCameraOrientation(context)
    return prepareVideo(640, 480, 30, 1200 * 1024, false, rotation)
  }

  /**
   * Same to call: prepareAudio(64 * 1024, 32000, true, false, false);
   *
   * @return true if success, false if you get a error (Normally because the encoder selected
   * doesn't support any configuration seated or your device hasn't a AAC encoder).
   */
  fun prepareAudio(): Boolean {
    return prepareAudio(64 * 1024, 32000, true, false, false)
  }

  /**
   * @param forceVideo force type codec used. FIRST_COMPATIBLE_FOUND, SOFTWARE, HARDWARE
   * @param forceAudio force type codec used. FIRST_COMPATIBLE_FOUND, SOFTWARE, HARDWARE
   */
  fun setForce(forceVideo: CodecUtil.Force, forceAudio: CodecUtil.Force) {
    videoEncoder.setForce(forceVideo)
    audioEncoder.setForce(forceAudio)
  }

  /**
   * Start record a MP4 video. Need be called while stream.
   *
   * @param path where file will be saved.
   * @throws IOException If you init it before start stream.
   */
  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  @Throws(IOException::class)
  fun startRecord(path: String, listener: RecordController.Listener?) {
    recordController.startRecord(path, listener)
    if (!streaming) {
      startEncoders()
    } else if (videoEncoder.isRunning) {
      resetVideoEncoder()
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  @Throws(IOException::class)
  fun startRecord(path: String) {
    startRecord(path, null)
  }

  /**
   * Stop record MP4 video started with @startRecord. If you don't call it file will be unreadable.
   */
  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  fun stopRecord() {
    recordController.stopRecord()
    if (!streaming) stopStream()
  }

  /**
   * Stop camera preview. Ignored if streaming or already stopped. You need call it after
   *
   * @stopStream to release camera properly if you will close activity.
   */
  fun stopPreview() {
    if (!isStreaming() && onPreview && glInterface !is OffScreenGlThread) {
      glInterface?.stop()
      vlcVideoLibrary?.stop()
      onPreview = false
    } else {
      Log.e(TAG, "Streaming or preview stopped, ignored")
    }
  }

  protected abstract fun startStreamRtp(url: String)

  /**
   * Need be called after @prepareVideo or/and @prepareAudio. This method override resolution of
   *
   * @param url of the stream like: protocol://ip:port/application/streamName
   *
   * RTSP: rtsp://192.168.1.1:1935/live/pedroSG94 RTSPS: rtsps://192.168.1.1:1935/live/pedroSG94
   * RTMP: rtmp://192.168.1.1:1935/live/pedroSG94 RTMPS: rtmps://192.168.1.1:1935/live/pedroSG94
   * @startPreview to resolution seated in @prepareVideo. If you never startPreview this method
   * startPreview for you to resolution seated in @prepareVideo.
   */
  fun startStream(url: String, vlcUrl: String) {
    streaming = true
    if (!recordController.isRecording) {
      startEncoders()
    } else {
      resetVideoEncoder()
    }
    startStreamRtp(url)
    onPreview = true
    vlcVideoLibrary?.play(vlcUrl)
  }

  private fun startEncoders() {
    videoEncoder.start()
    audioEncoder.start()
    prepareGlView()
    microphoneManager.start()
    onPreview = true
  }

  private fun resetVideoEncoder() {
    glInterface?.removeMediaCodecSurface()
    videoEncoder.reset()
    glInterface?.addMediaCodecSurface(videoEncoder.inputSurface)
  }

  private fun prepareGlView() {
    if (glInterface is OffScreenGlThread) {
      glInterface = OffScreenGlThread(context)
      glInterface?.init()
      (glInterface as OffScreenGlThread).setFps(videoEncoder.fps)
    }
    if (videoEncoder.rotation == 90 || videoEncoder.rotation == 270) {
      glInterface?.setEncoderSize(videoEncoder.height, videoEncoder.width)
    } else {
      glInterface?.setEncoderSize(videoEncoder.width, videoEncoder.height)
    }
    glInterface?.setRotation(0)
    glInterface?.start()
    if (videoEncoder.inputSurface != null) {
      glInterface?.addMediaCodecSurface(videoEncoder.inputSurface)
    }
    vlcVideoLibrary = VlcVideoLibrary(context, this, glInterface?.surfaceTexture)
    vlcVideoLibrary?.setOptions(options)
  }

  protected abstract fun stopStreamRtp()

  /**
   * Stop stream started with @startStream.
   */
  fun stopStream() {
    if (streaming) {
      streaming = false
      stopStreamRtp()
    }
    if (!recordController.isRecording) {
      microphoneManager.stop()
      glInterface?.removeMediaCodecSurface()
      if (glInterface is OffScreenGlThread) {
        glInterface?.stop()
        vlcVideoLibrary?.stop()
      }
      videoEncoder.stop()
      audioEncoder.stop()
      recordController.resetFormats()
    }
  }

  //cache control
  @Throws(RuntimeException::class)
  abstract fun resizeCache(newSize: Int)

  abstract fun getCacheSize(): Int

  abstract fun getSentAudioFrames(): Long

  abstract fun getSentVideoFrames(): Long

  abstract fun getDroppedAudioFrames(): Long

  abstract fun getDroppedVideoFrames(): Long

  abstract fun resetSentAudioFrames()

  abstract fun resetSentVideoFrames()

  abstract fun resetDroppedAudioFrames()

  abstract fun resetDroppedVideoFrames()

  /**
   * Mute microphone, can be called before, while and after stream.
   */
  fun disableAudio() {
    microphoneManager.mute()
  }

  /**
   * Enable a muted microphone, can be called before, while and after stream.
   */
  fun enableAudio() {
    microphoneManager.unMute()
  }

  /**
   * Get mute state of microphone.
   *
   * @return true if muted, false if enabled
   */
  fun isAudioMuted(): Boolean {
    return microphoneManager.isMuted
  }

  /**
   * Get video camera state
   *
   * @return true if disabled, false if enabled
   */
  fun isVideoEnabled(): Boolean {
    return videoEnabled
  }

  /**
   * Disable send camera frames and send a black image with low bitrate(to reduce bandwith used)
   * instance it.
   */
  fun disableVideo() {
    videoEncoder.startSendBlackImage()
    videoEnabled = false
  }

  /**
   * Enable send camera frames.
   */
  fun enableVideo() {
    videoEncoder.stopSendBlackImage()
    videoEnabled = true
  }

  fun getBitrate(): Int {
    return videoEncoder.bitRate
  }

  fun getResolutionValue(): Int {
    return videoEncoder.width * videoEncoder.height
  }

  fun getStreamWidth(): Int {
    return videoEncoder.width
  }

  fun getStreamHeight(): Int {
    return videoEncoder.height
  }

  fun getGlInterface(): GlInterface {
    return glInterface!!
  }

  /**
   * Set video bitrate of H264 in kb while stream.
   *
   * @param bitrate H264 in kb.
   */
  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  fun setVideoBitrateOnFly(bitrate: Int) {
    videoEncoder.setVideoBitrateOnFly(bitrate)
  }

  /**
   * Set limit FPS while stream. This will be override when you call to prepareVideo method. This
   * could produce a change in iFrameInterval.
   *
   * @param fps frames per second
   */
  fun setLimitFPSOnFly(fps: Int) {
    videoEncoder.fps = fps
  }

  /**
   * Get stream state.
   *
   * @return true if streaming, false if not streaming.
   */
  fun isStreaming(): Boolean {
    return streaming
  }

  /**
   * Get preview state.
   *
   * @return true if enabled, false if disabled.
   */
  fun isOnPreview(): Boolean {
    return onPreview
  }

  /**
   * Get record state.
   *
   * @return true if recording, false if not recoding.
   */
  fun isRecording(): Boolean {
    return recordController.isRecording
  }

  fun pauseRecord() {
    recordController.pauseRecord()
  }

  fun resumeRecord() {
    recordController.resumeRecord()
  }

  fun getRecordStatus(): RecordController.Status {
    return recordController.status
  }

  override fun onComplete() {
    glInterface?.surfaceTexture?.setDefaultBufferSize(240, 160) //This is original stream size.
  }

  override fun onError() {

  }

  protected abstract fun getAacDataRtp(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo)

  override fun getAacData(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    recordController.recordAudio(aacBuffer, info)
    if (streaming) getAacDataRtp(aacBuffer, info)
  }

  protected abstract fun onSpsPpsVpsRtp(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?)

  override fun onSpsPps(sps: ByteBuffer, pps: ByteBuffer) {
    if (streaming) onSpsPpsVpsRtp(sps, pps, null)
  }

  override fun onSpsPpsVps(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer) {
    if (streaming) onSpsPpsVpsRtp(sps, pps, vps)
  }

  protected abstract fun getH264DataRtp(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo)

  override fun getVideoData(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    recordController.recordVideo(h264Buffer, info)
    if (streaming) getH264DataRtp(h264Buffer, info)
  }

  override fun inputPCMData(buffer: ByteArray, size: Int) {
    audioEncoder.inputPCMData(buffer, size)
  }

  override fun onVideoFormat(mediaFormat: MediaFormat) {
    recordController.setVideoFormat(mediaFormat)
  }

  override fun onAudioFormat(mediaFormat: MediaFormat) {
    recordController.setAudioFormat(mediaFormat)
  }
}
package com.streye.androidrestreamer.plugin

import android.content.Context
import android.media.MediaCodec
import android.os.Build
import android.support.annotation.RequiresApi
import com.pedro.encoder.utils.CodecUtil
import com.pedro.rtplibrary.view.LightOpenGlView
import com.pedro.rtplibrary.view.OpenGlView
import com.pedro.rtsp.rtsp.Protocol
import com.pedro.rtsp.rtsp.RtspClient
import com.pedro.rtsp.rtsp.VideoCodec
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import java.nio.ByteBuffer

/**
 * Created by pedro on 29/03/19.
 */

class VLCReStreamerRtsp: VLCReStreamerBase {

  private val rtspClient: RtspClient

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  constructor(openGlView: OpenGlView, connectChecker: ConnectCheckerRtsp) : super(openGlView) {
    rtspClient = RtspClient(connectChecker)
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  constructor(lightOpenGlView: LightOpenGlView, connectChecker: ConnectCheckerRtsp) :
      super(lightOpenGlView) {
    rtspClient = RtspClient(connectChecker)
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  constructor(context: Context, connectChecker: ConnectCheckerRtsp) : super(context) {
    rtspClient = RtspClient(connectChecker)
  }

  /**
   * Internet protocol used.
   *
   * @param protocol Could be Protocol.TCP or Protocol.UDP.
   */
  fun setProtocol(protocol: Protocol) {
    rtspClient.setProtocol(protocol)
  }

  @Throws(RuntimeException::class)
  override fun resizeCache(newSize: Int) {
    rtspClient.resizeCache(newSize)
  }

  override fun getCacheSize(): Int {
    return rtspClient.cacheSize
  }

  override fun getSentAudioFrames(): Long {
    return rtspClient.sentAudioFrames
  }

  override fun getSentVideoFrames(): Long {
    return rtspClient.sentVideoFrames
  }

  override fun getDroppedAudioFrames(): Long {
    return rtspClient.droppedAudioFrames
  }

  override fun getDroppedVideoFrames(): Long {
    return rtspClient.droppedVideoFrames
  }

  override fun resetSentAudioFrames() {
    rtspClient.resetSentAudioFrames()
  }

  override fun resetSentVideoFrames() {
    rtspClient.resetSentVideoFrames()
  }

  override fun resetDroppedAudioFrames() {
    rtspClient.resetDroppedAudioFrames()
  }

  override fun resetDroppedVideoFrames() {
    rtspClient.resetDroppedVideoFrames()
  }

  fun setVideoCodec(videoCodec: VideoCodec) {
    videoEncoder.type =
        if (videoCodec == VideoCodec.H265) CodecUtil.H265_MIME else CodecUtil.H264_MIME
  }

  override fun setAuthorization(user: String, password: String) {
    rtspClient.setAuthorization(user, password)
  }

  override fun prepareAudioRtp(isStereo: Boolean, sampleRate: Int) {
    rtspClient.setIsStereo(isStereo)
    rtspClient.setSampleRate(sampleRate)
  }

  override fun startStreamRtp(url: String) {
    rtspClient.setUrl(url)
  }

  override fun stopStreamRtp() {
    rtspClient.disconnect()
  }

  override fun getAacDataRtp(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    rtspClient.sendAudio(aacBuffer, info)
  }

  override fun onSpsPpsVpsRtp(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?) {
    rtspClient.setSPSandPPS(sps, pps, vps)
    rtspClient.connect()
  }

  override fun getH264DataRtp(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    rtspClient.sendVideo(h264Buffer, info)
  }
}
package com.streye.androidrestreamer.plugin

import android.content.Context
import android.media.MediaCodec
import com.pedro.encoder.utils.CodecUtil
import com.pedro.rtmp.flv.video.ProfileIop
import com.pedro.rtmp.rtmp.RtmpClient
import com.pedro.rtmp.utils.ConnectCheckerRtmp
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

class VLCReStreamerRtmp : VLCReStreamerBase {

  private val rtmpClient: RtmpClient

  constructor(openGlView: OpenGlView, connectChecker: ConnectCheckerRtmp) : super(openGlView) {
    rtmpClient = RtmpClient(connectChecker)
  }

  constructor(lightOpenGlView: LightOpenGlView, connectChecker: ConnectCheckerRtmp) :
      super(lightOpenGlView) {
    rtmpClient = RtmpClient(connectChecker)
  }

  constructor(context: Context, connectChecker: ConnectCheckerRtmp) : super(context) {
    rtmpClient = RtmpClient(connectChecker)
  }

  /**
   * Internet protocol used.
   *
   * @param protocol Could be Protocol.TCP or Protocol.UDP.
   */
  fun setProfileIop(profileIop: ProfileIop) {
    rtmpClient.setProfileIop(profileIop)
  }

  @Throws(RuntimeException::class)
  override fun resizeCache(newSize: Int) {
    rtmpClient.resizeCache(newSize)
  }

  override fun getCacheSize(): Int {
    return rtmpClient.cacheSize
  }

  override fun getSentAudioFrames(): Long {
    return rtmpClient.sentAudioFrames
  }

  override fun getSentVideoFrames(): Long {
    return rtmpClient.sentVideoFrames
  }

  override fun getDroppedAudioFrames(): Long {
    return rtmpClient.droppedAudioFrames
  }

  override fun getDroppedVideoFrames(): Long {
    return rtmpClient.droppedVideoFrames
  }

  override fun resetSentAudioFrames() {
    rtmpClient.resetSentAudioFrames()
  }

  override fun resetSentVideoFrames() {
    rtmpClient.resetSentVideoFrames()
  }

  override fun resetDroppedAudioFrames() {
    rtmpClient.resetDroppedAudioFrames()
  }

  override fun resetDroppedVideoFrames() {
    rtmpClient.resetDroppedVideoFrames()
  }

  fun setVideoCodec(videoCodec: VideoCodec) {
    videoEncoder.type =
      if (videoCodec == VideoCodec.H265) CodecUtil.H265_MIME else CodecUtil.H264_MIME
  }

  override fun setAuthorization(user: String, password: String) {
    rtmpClient.setAuthorization(user, password)
  }

  override fun prepareAudioRtp(isStereo: Boolean, sampleRate: Int) {
    rtmpClient.setAudioInfo(sampleRate, isStereo)
  }

  override fun startStreamRtp(url: String) {
    rtmpClient.connect(url)
  }

  override fun stopStreamRtp() {
    rtmpClient.disconnect()
  }

  override fun getAacDataRtp(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    rtmpClient.sendAudio(aacBuffer, info)
  }

  override fun onSpsPpsVpsRtp(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?) {
    rtmpClient.setVideoInfo(sps, pps, vps)
  }

  override fun getH264DataRtp(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    rtmpClient.sendVideo(h264Buffer, info)
  }
}
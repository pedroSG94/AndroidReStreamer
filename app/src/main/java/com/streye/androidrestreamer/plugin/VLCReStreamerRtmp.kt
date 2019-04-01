package com.streye.androidrestreamer.plugin

import android.content.Context
import android.media.MediaCodec
import android.os.Build
import android.support.annotation.RequiresApi
import com.pedro.rtplibrary.view.LightOpenGlView
import com.pedro.rtplibrary.view.OpenGlView
import net.ossrs.rtmp.ConnectCheckerRtmp
import net.ossrs.rtmp.SrsFlvMuxer
import java.nio.ByteBuffer


/**
 * Created by pedro on 29/03/19.
 */

class VLCReStreamerRtmp : VLCReStreamerBase {

  private val srsFlvMuxer: SrsFlvMuxer

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  constructor(openGlView: OpenGlView, connectChecker: ConnectCheckerRtmp) : super(openGlView) {
    srsFlvMuxer = SrsFlvMuxer(connectChecker)
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  constructor(lightOpenGlView: LightOpenGlView, connectChecker: ConnectCheckerRtmp) :
      super(lightOpenGlView) {
    srsFlvMuxer = SrsFlvMuxer(connectChecker)
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  constructor(context: Context, connectChecker: ConnectCheckerRtmp) : super(context) {
    srsFlvMuxer = SrsFlvMuxer(connectChecker)
  }

  /**
   * H264 profile.
   *
   * @param profileIop Could be ProfileIop.BASELINE or ProfileIop.CONSTRAINED
   */
  fun setProfileIop(profileIop: Byte) {
    srsFlvMuxer.setProfileIop(profileIop)
  }

  @Throws(RuntimeException::class)
  override fun resizeCache(newSize: Int) {
    srsFlvMuxer.resizeFlvTagCache(newSize)
  }

  override fun getCacheSize(): Int {
    return srsFlvMuxer.flvTagCacheSize
  }

  override fun getSentAudioFrames(): Long {
    return srsFlvMuxer.sentAudioFrames
  }

  override fun getSentVideoFrames(): Long {
    return srsFlvMuxer.sentVideoFrames
  }

  override fun getDroppedAudioFrames(): Long {
    return srsFlvMuxer.droppedAudioFrames
  }

  override fun getDroppedVideoFrames(): Long {
    return srsFlvMuxer.droppedVideoFrames
  }

  override fun resetSentAudioFrames() {
    srsFlvMuxer.resetSentAudioFrames()
  }

  override fun resetSentVideoFrames() {
    srsFlvMuxer.resetSentVideoFrames()
  }

  override fun resetDroppedAudioFrames() {
    srsFlvMuxer.resetDroppedAudioFrames()
  }

  override fun resetDroppedVideoFrames() {
    srsFlvMuxer.resetDroppedVideoFrames()
  }

  override fun setAuthorization(user: String, password: String) {
    srsFlvMuxer.setAuthorization(user, password)
  }

  override fun prepareAudioRtp(isStereo: Boolean, sampleRate: Int) {
    srsFlvMuxer.setIsStereo(isStereo)
    srsFlvMuxer.setSampleRate(sampleRate)
  }

  override fun startStreamRtp(url: String) {
    if (videoEncoder.rotation == 90 || videoEncoder.rotation == 270) {
      srsFlvMuxer.setVideoResolution(videoEncoder.height, videoEncoder.width)
    } else {
      srsFlvMuxer.setVideoResolution(videoEncoder.width, videoEncoder.height)
    }
    srsFlvMuxer.start(url)
  }

  override fun stopStreamRtp() {
    srsFlvMuxer.stop()
  }

  override fun getAacDataRtp(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    srsFlvMuxer.sendAudio(aacBuffer, info)
  }

  override fun onSpsPpsVpsRtp(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?) {
    srsFlvMuxer.setSpsPPs(sps, pps)
  }

  override fun getH264DataRtp(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    srsFlvMuxer.sendVideo(h264Buffer, info)
  }
}
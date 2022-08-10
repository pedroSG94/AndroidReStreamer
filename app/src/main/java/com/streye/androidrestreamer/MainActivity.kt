package com.streye.androidrestreamer

import android.os.Bundle
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.pedro.rtplibrary.view.OpenGlView
import com.streye.androidrestreamer.plugin.VLCReStreamerRtmp

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback, ConnectCheckerRtmp {

  private val streamURL = "rtmp://192.168.0.191/live/pedro"
  private val vlcURL = "rtmp://192.168.0.191/live/test"

  private lateinit var vlcReStreamerRtmp: VLCReStreamerRtmp

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val surfaceView = findViewById<OpenGlView>(R.id.surfaceView)
    vlcReStreamerRtmp = VLCReStreamerRtmp(surfaceView, this)
    surfaceView.holder.addCallback(this)
  }

  override fun onConnectionSuccessRtmp() {
    runOnUiThread {
      Toast.makeText(this@MainActivity, "Connection success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onConnectionFailedRtmp(reason: String) {
    runOnUiThread {
      Toast.makeText(this@MainActivity, "Connection failed. $reason", Toast.LENGTH_SHORT).show()
      vlcReStreamerRtmp.stopStream()
    }
  }

  override fun onConnectionStartedRtmp(rtmpUrl: String) {

  }

  override fun onDisconnectRtmp() {
    runOnUiThread { Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show() }
  }

  override fun onNewBitrateRtmp(bitrate: Long) {

  }

  override fun onAuthErrorRtmp() {
    runOnUiThread { Toast.makeText(this@MainActivity, "Auth error", Toast.LENGTH_SHORT).show() }
  }

  override fun onAuthSuccessRtmp() {
    runOnUiThread { Toast.makeText(this@MainActivity, "Auth success", Toast.LENGTH_SHORT).show() }
  }

  override fun surfaceChanged(holder: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
    if (!vlcReStreamerRtmp.isStreaming()) {
      if (vlcReStreamerRtmp.prepareAudio() && vlcReStreamerRtmp.prepareVideo(1280, 720, 30, 3000 * 1000, CameraHelper.getCameraOrientation(this))) {
        vlcReStreamerRtmp.startStream(streamURL, vlcURL)
      }
    }
  }

  override fun surfaceCreated(holder: SurfaceHolder) {

  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {

  }
}

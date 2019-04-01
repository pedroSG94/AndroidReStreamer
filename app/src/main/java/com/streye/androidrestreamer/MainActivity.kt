package com.streye.androidrestreamer

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import android.widget.Toast
import com.streye.androidrestreamer.plugin.VLCReStreamerRtmp
import kotlinx.android.synthetic.main.activity_main.*
import net.ossrs.rtmp.ConnectCheckerRtmp

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback, ConnectCheckerRtmp {

  private val streamURL = "rtmp://10.7.12.62/live/pedro"
  private val vlcURL = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov"

  private lateinit var vlcReStreamerRtmp: VLCReStreamerRtmp

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
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

  override fun onDisconnectRtmp() {
    runOnUiThread { Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show() }
  }

  override fun onAuthErrorRtmp() {
    runOnUiThread { Toast.makeText(this@MainActivity, "Auth error", Toast.LENGTH_SHORT).show() }
  }

  override fun onAuthSuccessRtmp() {
    runOnUiThread { Toast.makeText(this@MainActivity, "Auth success", Toast.LENGTH_SHORT).show() }
  }

  override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    if (!vlcReStreamerRtmp.isStreaming()) {
      if (vlcReStreamerRtmp.isRecording() || vlcReStreamerRtmp.prepareAudio() && vlcReStreamerRtmp.prepareVideo()) {
        vlcReStreamerRtmp.startStream(streamURL, vlcURL)
      }
    }
  }

  override fun surfaceDestroyed(p0: SurfaceHolder?) {
    if (vlcReStreamerRtmp.isStreaming()) vlcReStreamerRtmp.stopStream()
    if (vlcReStreamerRtmp.isOnPreview()) vlcReStreamerRtmp.stopPreview()
  }

  override fun surfaceCreated(p0: SurfaceHolder?) {

  }
}

package com.hmomeni.marsrover

import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        receiveInstructionsBtn.setOnClickListener {
            updateRover()
        }

    }

    private fun updateRover() {
        val client = OkHttpClient()

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("rover_id", "12856496")
            .build()
        val request = Request.Builder()
            .url("https://roverapi.reev.ca")
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code() == 200) {
                    val body = response.body()!!.string()
                    response.body()!!.close()
                    Log.d(this@MainActivity.javaClass.simpleName, "Data=$body")

                    val jsonObject = JSONObject(body)
                    val weirs = jsonObject.getJSONArray("weirs")

                    roverView.blockedCells = Array(20) {
                        return@Array Array(10) {
                            false
                        }
                    }
                    val weirPoints = mutableListOf<Point>()
                    for (i in 0 until weirs.length()) {
                        val weir = weirs.getJSONObject(i)

                        val x = weir.getInt("x")
                        val y = weir.getInt("y")

                        weirPoints.add(Point(x,y))
                    }

                    val startPoint = jsonObject.getJSONObject("start_point")
                    val sX = startPoint.getInt("x")
                    val sY = startPoint.getInt("y")

                    roverView.reset()
                    roverView.updateLayout(Point(sX, sY), weirPoints)

                    val commands = jsonObject.getString("command")

                    runOnUiThread {
                        roverView.processCommand(commands)
                    }
                }

            }
        })
    }

}

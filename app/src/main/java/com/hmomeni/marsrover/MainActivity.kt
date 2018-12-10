package com.hmomeni.marsrover

import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity(), RoverView.RoverListener {

    private var roverState: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        roverView.roverListener = this

        savedInstanceState?.let {
            if (it.containsKey("rover")) {
                roverState = JSONObject(it.getString("rover"))
                roverView.post {
                    processServerResponse(roverState!!)
                }
            }
        }

        receiveInstructionsBtn.setOnClickListener {
            updateRover()
        }

        lazerBtn.setOnClickListener {
            roverView.useLazer()
        }
    }

    // let's save the instance in case of a configuration change
    // we could have used the ViewModel pattern here
    override fun onSaveInstanceState(outState: Bundle) {
        roverState?.let {
            outState.putString("rover", roverState.toString())
        }
        super.onSaveInstanceState(outState)
    }

    private var inProgress = false
    private fun updateRover() {
        if (inProgress) {
            roverView.showMessage(roverView.roverPosition, "I said hang on! I'm not a multi-tasker...")
            return
        }
        inProgress = true
        roverView.showMessage(roverView.roverPosition, "Hang on! I'm trying to contact HQ...")
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
                inProgress = false
                runOnUiThread {
                    roverView.showMessage(roverView.roverPosition, "Oh! Seems I can't contact HQ.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                inProgress = false
                val body = response.body()!!.string()
                roverView.post {
                    if (response.code() == 200) {
                        response.body()!!.close()
                        Log.d(this@MainActivity.javaClass.simpleName, "Data=$body")

                        val jsonObject = JSONObject(body)
                        roverState = jsonObject
                        processServerResponse(jsonObject)

                    } else {
                        roverView.showMessage(roverView.roverPosition, "Oh! Seems I can't contact HQ.")
                    }
                }

            }
        })
    }

    // we could have used Gson for a much easier json parsing
    private fun processServerResponse(jsonObject: JSONObject) {
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

            weirPoints.add(Point(x, y))
        }

        val startPoint = jsonObject.getJSONObject("start_point")
        val sX = startPoint.getInt("x")
        val sY = startPoint.getInt("y")

        roverView.reset()
        roverView.updateLayout(Point(sX, sY), weirPoints)

        val commands = jsonObject.getString("command")

        roverView.processCommand(commands)
    }

    override fun showLazerButton() {
        lazerBtn.visibility = View.VISIBLE
    }

    override fun hideLazerButton() {
        lazerBtn.visibility = View.GONE
    }

}

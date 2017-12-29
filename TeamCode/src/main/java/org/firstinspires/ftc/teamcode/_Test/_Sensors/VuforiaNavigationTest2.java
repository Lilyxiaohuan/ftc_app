/*
Copyright (c) 2016 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESSFOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.firstinspires.ftc.teamcode._Test._Sensors;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.vuforia.Image;

import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.teamcode._Libs.CameraLib;
import org.firstinspires.ftc.teamcode._Libs.VuforiaLib_FTC2016;
import org.firstinspires.ftc.teamcode._Libs.VuforiaLib_FTC2017;

import java.nio.ByteBuffer;

import static org.firstinspires.ftc.teamcode._Libs.VuforiaLib_FTC2017.formatPosition;

/**
 * This OpMode illustrates the basics of using the VuforiaLib_FTC2016 library to determine
 * positioning and orientation of robot on the FTC field.
 */

@Autonomous(name="Test: Vuforia Navigation Test 2", group ="Test")
//@Disabled
public class VuforiaNavigationTest2 extends OpMode {

    VuforiaLib_FTC2017 mVLib;
    int mLoopCount;

    @Override public void init() {
        /**
         * Start up Vuforia using VuforiaLib_FTC2017
         */
        mVLib = new VuforiaLib_FTC2017();
        mVLib.init(this, null);     // pass it this OpMode (so it can do telemetry output) and use its license key for now
    }

    @Override public void start()
    {
        /** Start tracking the data sets we care about. */
        mVLib.start();

        mLoopCount = 0;
    }

    @Override public void loop()
    {
        mVLib.loop();       // update recognition info

        RelicRecoveryVuMark vuMark = mVLib.getVuMark();

        if (vuMark != RelicRecoveryVuMark.UNKNOWN) {

            /* Found an instance of the template. In the actual game, you will probably
             * loop until this condition occurs, then move on to act accordingly depending
             * on which VuMark was visible. */
            telemetry.addData("VuMark", "%s visible", vuMark);

            /* For fun, we also show the navigational pose data. In the Relic Recovery game,
             * it is unlikely that you will be able to act on this pose information,
             * since there are multiple copies of the same sign in different locations. */
            OpenGLMatrix lastLocation = mVLib.getLastLocation();

            /**
             * Provide feedback as to where the robot was last located (if we know).
             */
            if (mVLib.haveLocation())
                telemetry.addData("Position:", VuforiaLib_FTC2017.formatPosition(lastLocation));
            else
                telemetry.addData("Position:", "Unknown");
            if (mVLib.haveHeading())
                telemetry.addData("Orientation:", VuforiaLib_FTC2017.formatOrientation(lastLocation));
            else
                telemetry.addData("Orientation:", "Unknown");
        }
        else {
            telemetry.addData("VuMark", "not visible");
        }

        // test image access through Vuforia
        // look at the columns of the top half or bottom half of the image alternately
        Bitmap b = mVLib.getBitmap(4);
        if (b != null) {
            CameraLib.CameraImage frame = new CameraLib.CameraImage(b);
            CameraLib.Size camSize = frame.cameraSize();
            telemetry.addData("Size", String.valueOf(camSize.width) + "x" + String.valueOf(camSize.height));

            // sample some pixels in both orientations
            // camera left
            frame.setCameraRight(false);
            telemetry.addData("camera left", "upper left %s right %s",
                    CameraLib.Pixel.colorName(frame.rectHue(new RectF(0.25f, 0.25f, 0.35f, 0.35f))),
                    CameraLib.Pixel.colorName(frame.rectHue(new RectF(0.75f, 0.25f, 0.85f, 0.35f))));
            telemetry.addData("camera left", "lower left %s right %s",
                    CameraLib.Pixel.colorName(frame.rectHue(new RectF(0.25f, 0.75f, 0.35f, 0.85f))),
                    CameraLib.Pixel.colorName(frame.rectHue(new RectF(0.75f, 0.75f, 0.85f, 0.85f))));
            // camera right
            frame.setCameraRight(true);
            telemetry.addData("camera right", "upper left %s right %s",
                    CameraLib.Pixel.colorName(frame.rectHue(new RectF(0.25f, 0.25f, 0.35f, 0.35f))),
                    CameraLib.Pixel.colorName(frame.rectHue(new RectF(0.75f, 0.25f, 0.85f, 0.35f))));
            telemetry.addData("camera right", "lower left %s right %s",
                    CameraLib.Pixel.colorName(frame.rectHue(new RectF(0.25f, 0.75f, 0.35f, 0.85f))),
                    CameraLib.Pixel.colorName(frame.rectHue(new RectF(0.75f, 0.75f, 0.85f, 0.85f))));
        }

    }

    @Override public void stop()
    {
        mVLib.stop();
    }

}

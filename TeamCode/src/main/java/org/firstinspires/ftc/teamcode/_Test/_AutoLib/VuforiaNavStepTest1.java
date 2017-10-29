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
package org.firstinspires.ftc.teamcode._Test._AutoLib;

import android.hardware.Camera;

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.teamcode._Libs.AutoLib;
import org.firstinspires.ftc.teamcode._Libs.CameraLib;
import org.firstinspires.ftc.teamcode._Libs.DistanceSensor;
import org.firstinspires.ftc.teamcode._Libs.HeadingSensor;
import org.firstinspires.ftc.teamcode._Libs.SensorLib;
import org.firstinspires.ftc.teamcode._Libs.VuforiaLib_FTC2017;

import java.util.ArrayList;

/**
 * This OpMode uses a Step that uses the VuforiaLib_FTC2017 library to determine
 * which column of the shelves to fill first, then shuts down Vuforia and
 * moves the robot under gyro control while using the camera to look for the
 * correct Cryptobox to stop at.
 */

class VuforiaGetMarkStep extends AutoLib.Step {

    VuforiaLib_FTC2017 mVLib;
    OpMode mOpMode;
    LookForCryptoBoxStep mMTCBStep;

    public VuforiaGetMarkStep(OpMode opMode, LookForCryptoBoxStep step) {
        mOpMode = opMode;
        mMTCBStep = step;

        // best to do this now, which is called from opmode's init() function
        mVLib = new VuforiaLib_FTC2017();
        mVLib.init(mOpMode, null);
    }

    public boolean loop() {
        super.loop();
        if (firstLoopCall()) {
            mVLib.start();
        }
        mVLib.loop();       // update recognition info
        RelicRecoveryVuMark vuMark = mVLib.getVuMark();
        boolean found = (vuMark != RelicRecoveryVuMark.UNKNOWN);
        if (found) {
            // Found an instance of the template -- tell "MoveTo.. step which one
            mMTCBStep.setVuMarkString(vuMark.toString());
            // shut down Vuforia - we're done with it
            mVLib.stop();
        }
        return found;       // done?
    }
}

// this is a guide step that starts up the Camera and uses it to
// determine when the appropriate CryptoBox is in sight, at which time
// it terminates the GuidedTerminatedDriveStep of which it is part
//
class LookForCryptoBoxStep extends AutoLib.Step {
    String mVuMarkString;
    CameraLib.CameraAcquireFrames mCamAcqFr;
    OpMode mOpMode;

    public LookForCryptoBoxStep(OpMode opMode) {
        mOpMode = opMode;
    }

    public void setVuMarkString(String s) { mVuMarkString = s; }

    public boolean loop() {
        super.loop();
        if (firstLoopCall()) {
            mCamAcqFr = new CameraLib.CameraAcquireFrames();
            if (mCamAcqFr.init(2) == false)     // init camera at 2nd smallest size
                mOpMode.telemetry.addData("error: ", "cannot initialize camera");
        }
        mOpMode.telemetry.addData("VuMark", "%s found", mVuMarkString);

        // get most recent frame from camera (may be same as last time or null)
        CameraLib.CameraImage frame = mCamAcqFr.loop();

        // log debug info ...
        if (frame != null) {
            final int bandSize = 6;
            mOpMode.telemetry.addData("hue columns", frame.columnHue(bandSize));
            mOpMode.telemetry.addData("dom columns", frame.columnDom(bandSize));
        }

        // look for indicated column of Cryptobox and return true when we're there
        // TBD

        return false;  // for now, just kill the mode manually
    }
}


@Autonomous(name="Test: VuforiaNavStepTest1", group ="Test")
//@Disabled
public class VuforiaNavStepTest1 extends OpMode {

    boolean bDone;
    AutoLib.Sequence mSequence;             // the root of the sequence tree
    DcMotor mMotors[];                      // motors, some of which can be null: assumed order is fr, br, fl, bl
    ModernRoboticsI2cGyro mGyro;            // gyro to use for heading information
    SensorLib.CorrectedMRGyro mCorrGyro;    // gyro corrector object

    @Override public void init()
    {
        // get the hardware
        AutoLib.HardwareFactory mf = null;
        final boolean debug = true;
        if (debug)
            mf = new AutoLib.TestHardwareFactory(this);
        else
            mf = new AutoLib.RealHardwareFactory(this);

        // get the motors: depending on the factory we created above, these may be
        // either dummy motors that just log data or real ones that drive the hardware
        // assumed order is fr, br, fl, bl
        mMotors = new DcMotor[4];
        mMotors[0] = mf.getDcMotor("fr");
        mMotors[1] = mf.getDcMotor("br");
        (mMotors[2] = mf.getDcMotor("fl")).setDirection(DcMotor.Direction.REVERSE);
        (mMotors[3] = mf.getDcMotor("bl")).setDirection(DcMotor.Direction.REVERSE);

        // get hardware gyro
        mGyro = (ModernRoboticsI2cGyro) hardwareMap.gyroSensor.get("gyro");

        // wrap gyro in an object that calibrates it and corrects its output
        mCorrGyro = new SensorLib.CorrectedMRGyro(mGyro);
        mCorrGyro.calibrate();

        // create the root Sequence for this autonomous OpMode
        mSequence = new AutoLib.LinearSequence();
        LookForCryptoBoxStep terminatorStep = new LookForCryptoBoxStep(this);
        mSequence.add(new VuforiaGetMarkStep(this, terminatorStep));
        AutoLib.MotorGuideStep guideStep = new AutoLib.SquirrelyGyroGuideStep(this, 90, 0, mCorrGyro, null, null, 0.5f);
        mSequence.add(new AutoLib.GuidedTerminatedDriveStep(this, guideStep, terminatorStep, mMotors));
    }

    @Override public void start()
    {
        // start out not-done
        bDone = false;
    }

    @Override
    public void loop() {

        // until we're done, keep looping through the current Step(s)
        if (!bDone)
            bDone = mSequence.loop();       // returns true when we're done
        else
            telemetry.addData("sequence finished", "");
    }

    @Override
    public void stop() {
        super.stop();
    }

}
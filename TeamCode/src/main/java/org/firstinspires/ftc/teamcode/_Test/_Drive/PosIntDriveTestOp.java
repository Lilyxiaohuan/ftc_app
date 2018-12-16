package org.firstinspires.ftc.teamcode._Test._Drive;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.teamcode._Libs.AutoLib;
import org.firstinspires.ftc.teamcode._Libs.BNO055IMUHeadingSensor;
import org.firstinspires.ftc.teamcode._Libs.HeadingSensor;
import org.firstinspires.ftc.teamcode._Libs.SensorLib;


/**
 * simple example of using a Step that uses encoder and gyro input to drive to given field positions.
 * Created by phanau on 12/15/18
 */

// use a single motor encoder and gyro to track absolute field position
class EncoderGyroPosInt extends SensorLib.PositionIntegrator {
    OpMode mOpMode;
    HeadingSensor mGyro;
    DcMotor mEncoderMotor;

    int mEncoderPrev;		// previous reading of motor encoder
    boolean mFirstLoop;

    public EncoderGyroPosInt(OpMode opmode, HeadingSensor gyro, DcMotor encoderMotor)
    {
        mOpMode = opmode;
        mGyro = gyro;
        mEncoderMotor = encoderMotor;
        mFirstLoop = true;
    }

    public boolean loop() {
        // get initial encoder value
        if (mFirstLoop) {
            mEncoderPrev = mEncoderMotor.getCurrentPosition();
            mFirstLoop = false;
        }

        // get current encoder value and compute delta since last read
        int encoder = mEncoderMotor.getCurrentPosition();
        int encoderDist = encoder - mEncoderPrev;
        mEncoderPrev = encoder;

        // get bearing from IMU gyro
        double imuBearingDeg = mGyro.getHeading();

        // update accumulated field position
        final int countsPerRev = 28*20;		// for 20:1 gearbox motor @ 28 counts/motorRev
        final double wheelDiam = 4.0;		// wheel diameter (in)
        double dist = (encoderDist * wheelDiam * Math.PI)/countsPerRev;
        this.move(dist, imuBearingDeg);

        mOpMode.telemetry.addData("EncoderGyroPosInt position", String.format("%.2f", this.getX())+", " + String.format("%.2f", this.getY()));

        return true;
    }

    public HeadingSensor getGyro() {
        return mGyro;
    }
}

// return done when we're within tolerance distance of target position
class PositionTerminatorStep extends AutoLib.MotorGuideStep {

    OpMode mOpMode;
    SensorLib.PositionIntegrator mPosInt;
    Position mTarget;
    double mTol;

    public PositionTerminatorStep(OpMode opmode, SensorLib.PositionIntegrator posInt, Position target, double tol) {
        mOpMode = opmode;
        mPosInt = posInt;
        mTarget = target;
        mTol = tol;
    }

    @Override
    public boolean loop() {
        super.loop();
        Position current = mPosInt.getPosition();
        double dist = Math.sqrt((mTarget.x-current.x)*(mTarget.x-current.x) + (mTarget.y-current.y)*(mTarget.y-current.y));
        mOpMode.telemetry.addData("PositionTerminatorStep target", String.format("%.2f", mTarget.x)+", " + String.format("%.2f", mTarget.y));
        mOpMode.telemetry.addData("PositionTerminatorStep current", String.format("%.2f", current.x)+", " + String.format("%.2f", current.y));
        mOpMode.telemetry.addData("PositionTerminatorStep dist", String.format("%.2f", dist));
        boolean bDone = (dist < mTol);
        return bDone;
    }
}


// Step: drive to given absolute field position using given EncoderGyroPosInt
class PosIntDriveToStep extends AutoLib.Step {

    OpMode mOpMode;
    EncoderGyroPosInt mPosInt;
    Position mTarget;
    AutoLib.GuidedTerminatedDriveStep mSubStep;
    AutoLib.GyroGuideStep mGuideStep;
    PositionTerminatorStep mTerminatorStep;

    public PosIntDriveToStep(OpMode opmode, EncoderGyroPosInt posInt, DcMotor[] motors,
                             float power, Position target, double tolerance, boolean stop)
    {
        mOpMode = opmode;
        mPosInt = posInt;
        mTarget = target;

        // create the sub-steps that will actually do the move
        mGuideStep = new AutoLib.GyroGuideStep(mOpMode, 0, mPosInt.getGyro(), null, null, power);
        mTerminatorStep = new PositionTerminatorStep(mOpMode, mPosInt, mTarget, tolerance);
        mSubStep = new AutoLib.GuidedTerminatedDriveStep(mOpMode, mGuideStep, mTerminatorStep, motors);
    }

    public boolean loop() {
        super.loop();

        // run the EncoderGyroPosInt to update its position based on encoders and gyro
        mPosInt.loop();

        // update the GyroGuideStep heading to continue heading for the target
        mGuideStep.setHeading((float)HeadingToTarget(mTarget, mPosInt.getPosition()));

        // run the GuidedTerminatedDriveStep and return its result
        return mSubStep.loop();
    }

    private double HeadingToTarget(Position target, Position current) {
        double headingXrad = Math.atan2((target.y-current.y), (target.x-current.x));  // pos CCW from X-axis
        double headingYdeg = SensorLib.Utils.wrapAngle(Math.toDegrees(headingXrad) - 90.0);
        mOpMode.telemetry.addData("PosIntDriveToStep.HeadingToTarget target", String.format("%.2f", target.x)+", " + String.format("%.2f", target.y));
        mOpMode.telemetry.addData("PosIntDriveToStep.HeadingToTarget current", String.format("%.2f", current.x)+", " + String.format("%.2f", current.y));
        mOpMode.telemetry.addData("PosIntDriveToStep.HeadingToTarget heading", String.format("%.2f", headingYdeg));
        return headingYdeg;
    }
}


// simple example sequence that tests encoder/gyro-based position integration to drive along a given path
@Autonomous(name="Test: Pos Int Drive Test", group ="Test")
//@Disabled
public class PosIntDriveTestOp extends OpMode {

    AutoLib.Sequence mSequence;             // the root of the sequence tree
    boolean bDone;                          // true when the programmed sequence is done
    DcMotor mMotors[];                      // motors, some of which can be null: assumed order is fr, br, fl, bl
    BNO055IMUHeadingSensor mGyro;           // gyro to use for heading information
    boolean bSetup;                         // true when we're in "setup mode" where joysticks tweak parameters
    SensorLib.PID mPid;                     // PID controller for the sequence
    EncoderGyroPosInt mPosInt;              // Encoder/gyro-based position integrator to keep track of where we are

    // parameters of the PID controller for this sequence - assumes 20-gear motors (fast)
    float Kp = 0.02f;        // motor power proportional term correction per degree of deviation
    float Ki = 0.025f;         // ... integrator term
    float Kd = 0;             // ... derivative term
    float KiCutoff = 10.0f;    // maximum angle error for which we update integrator

    @Override
    public void init() {
        bSetup = false;      // start out in Kp/Ki setup mode
        AutoLib.HardwareFactory mf = null;
        final boolean debug = false;
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

        // get hardware IMU and wrap gyro in HeadingSensor object usable below
        mGyro = new BNO055IMUHeadingSensor(hardwareMap.get(BNO055IMU.class, "imu"));
        mGyro.init(7);  // orientation of REV hub in my ratbot

        // create a PID controller for the sequence
        mPid = new SensorLib.PID(Kp, Ki, Kd, KiCutoff);    // make the object that implements PID control algorithm

        // create Encoder/gyro-based PositionIntegrator to keep track of where we are on the field
        mPosInt = new EncoderGyroPosInt(this, mGyro, mMotors[1]);


        // create an autonomous sequence with the steps to drive
        // several legs of a polygonal course ---
        float movePower = 0.20f;
        float turnPower = 0.25f;

        // create the root Sequence for this autonomous OpMode
        mSequence = new AutoLib.LinearSequence();

        // add a bunch of timed "legs" to the sequence - use Gyro heading convention of positive degrees CW from initial heading
        float tol = 3.0f;   // tolerance in inches
        float timeout = 2.0f;   // seconds

        // add a bunch of position integrator "legs" to the sequence -- uses absolute field coordinate system in inches
        for (int i=0; i<3; i++) {
            mSequence.add(new PosIntDriveToStep(this, mPosInt, mMotors, movePower,
                    new Position(DistanceUnit.INCH, 0, 24, 0., 0), tol, false));
            mSequence.add(new PosIntDriveToStep(this, mPosInt, mMotors, movePower,
                    new Position(DistanceUnit.INCH, 36, 36, 0, 0), tol, false));
            mSequence.add(new PosIntDriveToStep(this, mPosInt, mMotors, movePower,
                    new Position(DistanceUnit.INCH, 48, 0, 0, 0), tol, false));
            mSequence.add(new PosIntDriveToStep(this, mPosInt, mMotors, movePower,
                    new Position(DistanceUnit.INCH, 0, 0, 0, 0), tol, false));
        }

        // turn to heading zero to finish up
        mSequence.add(new AutoLib.AzimuthTolerancedTurnStep(this, 0, mGyro, mPid, mMotors, turnPower, tol, timeout));
        mSequence.add(new AutoLib.MoveByTimeStep(mMotors, 0, 0, true));     // stop all motors

        // start out not-done
        bDone = false;
    }

    @Override
    public void loop() {

        if (gamepad1.y)
            bSetup = true;      // Y button: enter "setup mode" using controller inputs to set Kp and Ki
        if (gamepad1.x)
            bSetup = false;     // X button: exit "setup mode"
        if (bSetup) {           // "setup mode"
            // adjust PID parameters by joystick inputs
            Kp -= (gamepad1.left_stick_y * 0.0001f);
            Ki -= (gamepad1.right_stick_y * 0.0001f);
            // update the parameters of the PID used by all Steps in this test
            mPid.setK(Kp, Ki, Kd, KiCutoff);
            // log updated values to the operator's console
            telemetry.addData("Kp = ", Kp);
            telemetry.addData("Ki = ", Ki);
            return;
        }

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


package org.firstinspires.ftc.teamcode._TeleOp;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name="Xdrive", group="TeleOp")
public class Xdrive extends OpMode {

    DcMotor NorthEast;
    DcMotor SouthEast;
    DcMotor SouthWest;
    DcMotor NorthWest;

    boolean bDebug = false;


    public Xdrive() {

    }

    @Override
    public void init() {
        try {
            NorthEast = hardwareMap.dcMotor.get("ne");
            SouthEast = hardwareMap.dcMotor.get("se");
            SouthWest = hardwareMap.dcMotor.get("sw");
            NorthWest = hardwareMap.dcMotor.get("nw");
            NorthEast.setDirection(DcMotor.Direction.REVERSE);
            SouthEast.setDirection(DcMotor.Direction.REVERSE);
        }
        catch (IllegalArgumentException iax) {
            bDebug = true;
        }
    }


    @Override
    public void loop() {
        double pi=3.14159;
        double y=-gamepad1.left_stick_y;
        double x = gamepad1.left_stick_x;
        double theta = Math.atan2(y,x)-pi/4;

        if(Math.abs(y)>=Math.abs(x)){
            x = y*Math.sin(theta);
            y = 1;
        }
        else if(Math.abs(x)>Math.abs(y)){
            y = x*Math.cos(theta);
            x = 1;
        }

        if (!bDebug) {
            NorthEast.setPower(y);
            SouthEast.setPower(x);
            SouthWest.setPower(y);
            NorthWest.setPower(x);
        }

        telemetry.addData("x", gamepad1.left_stick_x);
        telemetry.addData("y", gamepad1.left_stick_y);
    }

    @Override
    public void stop() {

    }
    double scaleInput(double dVal) {
        return dVal * dVal * dVal;
    }
}

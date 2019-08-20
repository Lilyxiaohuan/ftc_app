package org.firstinspires.ftc.teamcode._TeleOp;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name="XdriveSquirrely", group="TeleOp")
public class XdriveSquirrely extends OpMode {

    DcMotor NorthEast;
    DcMotor SouthEast;
    DcMotor SouthWest;
    DcMotor NorthWest;

    boolean bDebug = false;

    @Override
    public void init() {
        try {
            NorthEast = hardwareMap.dcMotor.get("ne");
            SouthEast = hardwareMap.dcMotor.get("se");
            SouthWest = hardwareMap.dcMotor.get("sw");
            NorthWest = hardwareMap.dcMotor.get("nw");
        }
        catch (IllegalArgumentException iax) {
            bDebug = true;
        }
    }


    @Override
    public void loop() {
        double pi=3.14159;
        double y=-gamepad1.left_stick_y;
        double x=gamepad1.left_stick_x;
        double spin=(-gamepad1.right_stick_y-y);
        double stretchx=0;
        double stretchy=0;

        //convert circle jystick input into a square
        if(Math.abs(x)>.01 || Math.abs(y)>.01){//if non zero
            double theta = Math.atan2(y,x)-pi/4;
            double power=Math.sqrt(x*x+y*y);
            x=Math.cos(theta)*power;
            y=Math.sin(theta)*power;
            if(Math.abs(y)>Math.abs(x)){
                stretchx=x/Math.abs(y);
                stretchy=y/Math.abs(y);
            }
            else if(Math.abs(x)>Math.abs(y)){
                stretchy=y/Math.abs(x);
                stretchx=x/Math.abs(x);
            }
            double stretch=Math.sqrt(stretchx*stretchx+stretchy*stretchy);
            x=x*stretch;
            y=y*stretch;
        }


        x=scaleInput(x);
        y=scaleInput(y);
        spin=scaleInput(spin);
        
        if(!bDebug){
            NorthEast.setPower(-y+spin);
            SouthEast.setPower(x+spin);
            SouthWest.setPower(y+spin);
            NorthWest.setPower(-x+spin);
        }
        telemetry.addData("x", x);
        telemetry.addData("y", y);
    }

    double scaleInput(double dVal) {
        return dVal * dVal * dVal * Math.abs(dVal);
    }
}

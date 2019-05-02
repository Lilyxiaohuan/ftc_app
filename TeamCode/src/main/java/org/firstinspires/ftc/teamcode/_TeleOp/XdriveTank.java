package org.firstinspires.ftc.teamcode._TeleOp;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name="XdriveTank", group="TeleOp")
public class XdriveTank extends OpMode{
    DcMotor northEast;
    DcMotor southEast;
    DcMotor southWest;
    DcMotor northWest;

    boolean bDebug = false;

    @Override
    public void init() {
        try {
            northEast = hardwareMap.dcMotor.get("ne");
            southEast = hardwareMap.dcMotor.get("se");
            southWest = hardwareMap.dcMotor.get("sw");
            northWest = hardwareMap.dcMotor.get("nw");
        }
        catch (IllegalArgumentException iax) {
            bDebug = true;
        }
    }

    @Override
    public void loop() {
        double pi=3.14159;
        double y1=-gamepad1.left_stick_y;
        double x1=-gamepad1.left_stick_x;
        double y2=-gamepad1.right_stick_y;
        double x2=-gamepad1.right_stick_x;

        double yaverage=(y1+y2)/2;
        if(Math.abs(y1-yaverage)<.1){
            y1=yaverage;
            y2=yaverage;
        }

        x1=scaleInput(x1);
        y1=scaleInput(y1);
        x2=scaleInput(x2);
        y2=scaleInput(y2);

        double scale=.25;
        y1=yaverage+scale*(y1-yaverage);
        y2=yaverage+scale*(y2-yaverage);

        //rotate the joysticks 45 degrees counterclockwise and get the new (x,y)
        double theta1=Math.atan2(y1,x1)-pi/4;
        double theta2=Math.atan2(y2,x2)-pi/4;
        double power1=Math.sqrt(x1*x1+y1*y1);
        double power2=Math.sqrt(x2*x2+y2*y2);
        x1=Math.cos(theta1)*power1;
        y1=Math.sin(theta1)*power1;
        x2=Math.cos(theta2)*power2;
        y2=Math.sin(theta2)*power2;


        //convert the new (x,y) from a unit circle to a sort of unit square
        double stretch1=toSquare(x1,y1);
        double stretch2=toSquare(x2,y2);
        x1=x1*stretch1;
        y1=y1*stretch1;
        x2=x2*stretch2;
        y2=y2*stretch2;

        if(!bDebug){
            northEast.setPower(x2);
            southEast.setPower(-y1);
            southWest.setPower(-x1);
            northWest.setPower(y2);
        }
    }

    double scaleInput(double dVal) {
        return dVal * dVal * dVal;
    }

    double toSquare(double x,double y){
        double stretchx=0;
        double stretchy=0;
        if(Math.abs(y)>Math.abs(x)){
            stretchx=x/Math.abs(y);
            stretchy=y/Math.abs(y);
        }
        else if(Math.abs(x)>Math.abs(y)){
            stretchy=y/Math.abs(x);
            stretchx=x/Math.abs(x);
        }
        return(Math.sqrt(stretchx*stretchx+stretchy*stretchy));
    }
}

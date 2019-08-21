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
    public void loop() {//left joystick controls left wheels, right controls right
        double pi=3.14159;
        double y1=-gamepad1.left_stick_y;
        double x1=-gamepad1.left_stick_x;
        double y2=-gamepad1.right_stick_y;
        double x2=-gamepad1.right_stick_x;//dont remember why i had all of these negative

        double yaverage=(y1+y2)/2;//gets average of both joysticks so if you push one forward and dont touch the other, it will go forward and turn instead of spinning in place
        if(Math.abs(y1-yaverage)<.1){//basically if the joysticks are close enough to the same value, set the power values to the same value (makes it easier to go straight forward
            y1=yaverage;
            y2=yaverage;
        }

        x1=scaleInput(x1);//scale it by cubing, easier low speed control
        y1=scaleInput(y1);
        x2=scaleInput(x2);
        y2=scaleInput(y2);

        double scale=.25;
        y1=yaverage+scale*(y1-yaverage);
        y2=yaverage+scale*(y2-yaverage);


        //code past this point just converts a normal drive into an xdrive
        //rotate the joysticks 45 degrees counterclockwise and get the new (x,y)
        double theta1=Math.atan2(y1,x1)-pi/4;
        double theta2=Math.atan2(y2,x2)-pi/4;//get each joystick angle, and rotate 45 degrees
        double power1=Math.sqrt(x1*x1+y1*y1);//set a power to the length of the hypotenuse
        double power2=Math.sqrt(x2*x2+y2*y2);
        x1=Math.cos(theta1)*power1;//new values that are rotated 45 degrees
        y1=Math.sin(theta1)*power1;
        x2=Math.cos(theta2)*power2;
        y2=Math.sin(theta2)*power2;


        //convert the new (x,y) from a unit circle to a sort of unit square
        double stretch1=toSquare(x1,y1);//get how much we need to increase our input values by
        double stretch2=toSquare(x2,y2);
        x1=x1*stretch1;//update the values
        y1=y1*stretch1;
        x2=x2*stretch2;
        y2=y2*stretch2;

        if(!bDebug){
            northEast.setPower(x2);//set each power, negative because they are backwards
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
        if(Math.abs(y)>Math.abs(x)){//if |y| > |x| means for theta values (45,135)U(225,315)
            stretchx=x/Math.abs(y);//stretch x is how much we need to scale the x input value by to get power for an xdrive
            stretchy=y/Math.abs(y);
        }
        else if(Math.abs(x)>Math.abs(y)){//same but for the rest of the values
            stretchy=y/Math.abs(x);
            stretchx=x/Math.abs(x);
        }
        return(Math.sqrt(stretchx*stretchx+stretchy*stretchy));
    }
}

package org.firstinspires.ftc.teamcode.RoboticArm;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

@Config
@TeleOp(name = "ArmManipulation JOYSTICK")
public class ArmManipulationJOYSTICK extends LinearOpMode {

    // SERVOS
    private Servo servo0;
    private Servo servo1;
    // MOTORS
    private DcMotorEx motor0;
    private DcMotorEx motor1;

    public static double SPIN_VEL = 250;
    public static double LIFT_VEL = 250;

    @Override
    public void runOpMode() {
        ElapsedTime runtime = new ElapsedTime();
        FtcDashboard dashboard = FtcDashboard.getInstance();

        // SERVOS
        servo0 = hardwareMap.get(Servo.class, "hoodR");
        servo0.setDirection(Servo.Direction.FORWARD);
        servo1 = hardwareMap.get(Servo.class, "hoodL");
        servo1.setDirection(Servo.Direction.REVERSE);
        servo0.setPosition(0.5);
        servo1.setPosition(0.5);

        // MOTORS
        motor0 = hardwareMap.get(DcMotorEx.class, "motor0");
        motor0.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor0.setDirection(DcMotorSimple.Direction.FORWARD);

        motor0.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor0.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        motor1 = hardwareMap.get(DcMotorEx.class, "motor1");
        motor1.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor1.setDirection(DcMotorSimple.Direction.REVERSE);
        
        motor1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        telemetry.addLine("ArmManipulation JOYSTICK Ready");
        telemetry.update();
        waitForStart();
        runtime.reset();

        while (opModeIsActive() && !isStopRequested()) {

            // Servo Module Control
            double tilt = 0.5 + (-gamepad1.left_stick_y * 0.25);
            double maxSpin = Math.min(1.0 - tilt, tilt);
            double spin = gamepad1.left_stick_x * maxSpin;
            double s0 = tilt + spin;
            double s1 = tilt - spin;
            servo0.setPosition(s0);
            servo1.setPosition(s1);

            // Motor Base Control
            double liftInput = -gamepad1.right_stick_y;
            double spinInput =  gamepad1.right_stick_x;
            motor0.setVelocity((liftInput * LIFT_VEL) + (spinInput * SPIN_VEL));
            motor1.setVelocity((liftInput * LIFT_VEL) - (spinInput * SPIN_VEL));

            // TELEMETRY
            telemetry.addData("Tilt",        "%.3f", tilt);
            telemetry.addData("Spin",        "%.3f", spin);
            telemetry.addData("Max Spin",    "%.3f", maxSpin);
            telemetry.addData("Servo 0",     "%.3f", s0);
            telemetry.addData("Servo 1",     "%.3f", s1);
            telemetry.addData("Lift Input",  "%.3f", liftInput);
            telemetry.addData("Spin Input",  "%.3f", spinInput);
            telemetry.addData("Motor 0 Vel", motor0.getVelocity());
            telemetry.addData("Motor 1 Vel", motor1.getVelocity());
            telemetry.addData("Motor 0 Pos", motor0.getCurrentPosition());
            telemetry.addData("Motor 1 Pos", motor1.getCurrentPosition());
            telemetry.update();
        }
    }
}
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
@TeleOp(name = "ArmManipulation STEP")
public class ArmManipulationSTEP extends LinearOpMode {

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
        motor0.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        motor1 = hardwareMap.get(DcMotorEx.class, "motor1");
        motor1.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor1.setDirection(DcMotorSimple.Direction.REVERSE);
        motor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        telemetry.addLine("ArmManipulation STEP Ready");
        telemetry.update();
        waitForStart();
        runtime.reset();

        double s0 = 0.5;
        double s1 = 0.5;

        while (opModeIsActive() && !isStopRequested()) {

            final double SERVO_MIN = 0.0;
            final double SERVO_MAX = 1.0;
            final double TOTAL_MIN = 0.5;
            final double TOTAL_MAX = 1.5;
            final double STEP      = 0.015;

            // Compute candidate positions
            double newS0 = s0 + (-gamepad1.left_stick_y + gamepad1.left_stick_x) * STEP;
            double newS1 = s1 + (-gamepad1.left_stick_y - gamepad1.left_stick_x) * STEP;
            // Clamp each servo to [0.0, 1.0]
            newS0 = Math.min(Math.max(newS0, SERVO_MIN), SERVO_MAX);
            newS1 = Math.min(Math.max(newS1, SERVO_MIN), SERVO_MAX);
            // Check totalPos — if valid, accept; otherwise reject entire update
            double newTotal = newS0 + newS1;
            if (newTotal >= TOTAL_MIN && newTotal <= TOTAL_MAX) {
                s0 = newS0;
                s1 = newS1;
            }
            // Apply
            servo0.setPosition(s0);
            servo1.setPosition(s1);

            // Motor Base Control
            double liftInput = -gamepad1.right_stick_y;
            double spinInput =  gamepad1.right_stick_x;
            motor0.setVelocity((liftInput * LIFT_VEL) + (spinInput * SPIN_VEL));
            motor1.setVelocity((liftInput * LIFT_VEL) - (spinInput * SPIN_VEL));

            // TELEMETRY
            telemetry.addData("Servo 0",     "%.3f", s0);
            telemetry.addData("Servo 1",     "%.3f", s1);
            telemetry.addData("Total Pos",   "%.3f", s0 + s1);
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
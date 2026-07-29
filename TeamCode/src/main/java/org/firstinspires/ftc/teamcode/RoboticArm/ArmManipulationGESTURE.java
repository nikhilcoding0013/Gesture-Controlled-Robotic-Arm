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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Config
@TeleOp(name = "ArmManipulation GESTURE")
public class ArmManipulationGESTURE extends LinearOpMode {

    // SERVOS
    private Servo servo0;
    private Servo servo1;
    // MOTORS
    private DcMotorEx motor0;
    private DcMotorEx motor1;

    public static double SPIN_VEL = 250;
    public static double LIFT_VEL = 250;

    // Gesture input values — written by UDP thread, read by OpMode loop
    private volatile float gestureLeftX  = 0;
    private volatile float gestureLeftY  = 0;
    private volatile float gestureRightX = 0;
    private volatile float gestureRightY = 0;

    private volatile boolean udpRunning = false;
    private DatagramSocket udpSocket;

    private void startUdpReceiver() {
        udpRunning = true;
        Thread udpThread = new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(9000);
                byte[] buf = new byte[16];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                while (udpRunning) {
                    udpSocket.receive(packet);
                    ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
                    gestureLeftX  = bb.getFloat();
                    gestureLeftY  = bb.getFloat();
                    gestureRightX = bb.getFloat();
                    gestureRightY = bb.getFloat();
                }
            } catch (Exception e) {
                // socket closed on stop — expected
            }
        });
        udpThread.setDaemon(true);
        udpThread.start();
    }

    private void stopUdpReceiver() {
        udpRunning = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }

    @Override
    public void runOpMode() {
        ElapsedTime runtime = new ElapsedTime();
        FtcDashboard dashboard = FtcDashboard.getInstance();

        // SERVOS
        servo0 = hardwareMap.get(Servo.class, "servo0");
        servo0.setDirection(Servo.Direction.FORWARD);
        servo1 = hardwareMap.get(Servo.class, "servo1");
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

        // Start UDP receiver before waitForStart
        startUdpReceiver();

        // Last known servo positions — start at midpoint
        double lastS0 = 0.5;
        double lastS1 = 0.5;

        telemetry.addLine("ArmManipulation GESTURE Ready");
        telemetry.addLine("Waiting for gesture input on port 9000...");
        telemetry.update();
        waitForStart();
        runtime.reset();

        while (opModeIsActive() && !isStopRequested()) {

            // --- SERVO MODULE — left hand ---
            // Only update servo position when left hand is actively sending input
            if (Math.abs(gestureLeftX) > 0.01 || Math.abs(gestureLeftY) > 0.01) {
                double tilt    = 0.5 + (-gestureLeftY * 0.25);
                double maxSpin = Math.min(1.0 - tilt, tilt);
                double spin    = gestureLeftX * maxSpin;
                lastS0 = tilt + spin;
                lastS1 = tilt - spin;
            }
            // Always write last known — holds position on fist
            servo0.setPosition(lastS0);
            servo1.setPosition(lastS1);

            // --- MOTOR MODULE — right hand ---
            // Only move when right hand is actively sending input
            if (Math.abs(gestureRightX) > 0.01 || Math.abs(gestureRightY) > 0.01) {
                double liftInput = -gestureRightY;
                double spinInput =  gestureRightX;
                motor0.setVelocity((liftInput * LIFT_VEL) + (spinInput * SPIN_VEL));
                motor1.setVelocity((liftInput * LIFT_VEL) - (spinInput * SPIN_VEL));
            } else {
                // Fist or no input — stop and brake
                motor0.setVelocity(0);
                motor1.setVelocity(0);
            }

            // TELEMETRY
            telemetry.addData("Gesture LX",  "%.3f", gestureLeftX);
            telemetry.addData("Gesture LY",  "%.3f", gestureLeftY);
            telemetry.addData("Gesture RX",  "%.3f", gestureRightX);
            telemetry.addData("Gesture RY",  "%.3f", gestureRightY);
            telemetry.addData("Servo 0",     "%.3f", lastS0);
            telemetry.addData("Servo 1",     "%.3f", lastS1);
            telemetry.addData("Motor 0 Pos", motor0.getCurrentPosition());
            telemetry.addData("Motor 1 Pos", motor1.getCurrentPosition());
            telemetry.update();
        }

        stopUdpReceiver();
    }
}
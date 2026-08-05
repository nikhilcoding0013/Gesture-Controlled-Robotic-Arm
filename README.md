# Gesture-Controlled Differential 4-DOF Robotic Arm

A robotic arm built from FTC (FIRST Tech Challenge) hardware, controllable via 
joystick teleop or real-time hand gesture recognition. Built independently to 
explore differential joint mechanisms, control systems, and human-robot interaction.

https://github.com/user-attachments/assets/0a7cc894-7853-4540-9973-29fe1fd12886

## Overview

This project uses differential joints to achieve smooth, stable motion across 4 degrees of freedom. Each joint distributes motion across two motors/servos via bevel gearing, similar to a differential drivetrain. The arm supports two control modes:
- **Joystick teleop** — direct gamepad control
- **Gesture control** — real-time hand tracking via webcam (OpenCV + MediaPipe)

[**CAD model (Onshape)**](https://cad.onshape.com/documents/c2f5a4e819de2befe6db6986/w/63769ecc12f9438944d01059/e/e78894e2433cfe468ae38a7b?renderMode=0&uiState=6a6b8af9cacf84581c5f01bb)  
[**Project Summary**](https://github.com/nikhilcoding0013/Robotic-Arm/blob/master/doc/ProjectSummary.pdf)

## Hardware
- REV Smart Servos (upper differential joint)
- REV HD Hex 40:1 motors (base differential joint)
- REV Control Hub
- Custom laser-cut and 3D-printed pieces, herringbone + bevel gearing, 
  ball/sliding bearings

<img width="513" height="461" alt="armImage" src="https://github.com/user-attachments/assets/c9e8d3f4-98a9-4b66-9252-381acc1c8137" />

## Software

The control stack is split into two independent input modes that feed the 
same underlying joint-control logic (teleoperation and gesture-control).

**Joint Control**
- Differential joints require coordinated dual-motor output; a single target angle is resolved into two motor commands (`tilt ± spin`) so each joint's two actuators drive together correctly
- Joystick input is mapped continuously to servo/motor position, clamped within physical joint limits, with auto-return to neutral on release

```
servo0 = hardwareMap.get(Servo.class, "hoodR");
servo0.setDirection(Servo.Direction.FORWARD);
servo1 = hardwareMap.get(Servo.class, "hoodL");
servo1.setDirection(Servo.Direction.REVERSE);
servo0.setPosition(0.5);
servo1.setPosition(0.5);
```

**Gesture Control Logic**
- Each hand is classified as **open** or **fist**
- A fist → open transition sets the origin for that hand
- While open, palm displacement from its origin (outside a dead zone, clamped to a max range) becomes the joystick `(x, y)` output
- Left hand drives the upper (servo) joint, right hand drives the base (motor) joint; a closed fist zeroes that hand's output, holding the joint in place

**Gesture Control Pipeline**
- OpenCV captures webcam frames on a laptop; MediaPipe's HandLandmarker tracks up to two hands, 21 landmarks each, per frame
- Each hand's `(x, y)` joystick value is packed into a 16-byte UDP payload and streamed every frame to the REV Control Hub
- A background thread on the Java side receives these packets and stores the latest values; the main OpMode loop reads them as `gamepad.left_stick_x/y`, for example
- Follows same joint-control logic as joystick teleop

## TeamCode
`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/RoboticArm`

**Java (robot-side, runs on the REV Control Hub):**
- `ArmManipulationGESTURE.java` — receives gesture input over UDP, drives the arm
- `ArmManipulationJOYSTICK.java` — analog joystick teleop
- `ArmManipulationSTEP.java` — D-pad discrete-step teleop (initial validation build)

**Python (laptop-side, gesture control only):**
- `gestureCtrl.py` — main gesture control script; runs hand tracking and 
  streams joystick values to the robot over UDP
- `hand_test.py` — standalone hand-tracking test script (no UDP/robot 
  connection), useful for verifying MediaPipe detection on its own

## Dependencies

**Robot-side (Java / Android)**
- FTC SDK 11.0.0
- Android Studio

**Laptop-side (Python 3.11)**
```bash
pip install opencv-python mediapipe numpy
```

## Run Process
1. Deploy `ArmManipulationGESTURE.java` to the Control Hub via Android Studio 
   and run it as the active OpMode
2. On a laptop connected to the Control Hub's network run:
```bash
   python gestureCtrl.py
```

## Results
- Stable multi-joint differential motion under load
- Reliable joystick and gesture-based control
- Real-time computer vision integration for contactless operation

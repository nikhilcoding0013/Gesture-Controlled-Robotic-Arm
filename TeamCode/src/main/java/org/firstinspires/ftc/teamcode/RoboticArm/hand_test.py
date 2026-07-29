import cv2
import mediapipe as mp
from mediapipe.tasks import python
from mediapipe.tasks.python import vision
import numpy as np
import urllib.request
import os

# Download model if not present
model_path = "hand_landmarker.task"
if not os.path.exists(model_path):
    print("Downloading hand landmark model...")
    urllib.request.urlretrieve(
        "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task",
        model_path
    )
    print("Download complete.")

last_left_state  = None
last_right_state = None
latest_result    = None

left_origin  = None
right_origin = None
left_prev    = "FIST"
right_prev   = "FIST"

DEAD_ZONE  = 0.03
MAX_RANGE  = 0.18
LIGHT_BLUE = (230, 216, 173)

def is_fist(landmarks):
    tips  = [8, 12, 16, 20]
    bases = [6, 10, 14, 18]
    folded = sum(1 for t, b in zip(tips, bases)
                 if landmarks[t].y > landmarks[b].y)
    return folded >= 3

def palm_center(lm):
    pts = [0, 5, 9, 13, 17]
    x = sum(lm[p].x for p in pts) / len(pts)
    y = sum(lm[p].y for p in pts) / len(pts)
    return round(x, 3), round(y, 3)

def compute_joystick(current_x, current_y, origin):
    if origin is None:
        return 0.0, 0.0
    dx = current_x - origin[0]
    dy = current_y - origin[1]
    dist = (dx**2 + dy**2) ** 0.5
    if dist < DEAD_ZONE:
        return 0.0, 0.0
    scale = min((dist - DEAD_ZONE) / (MAX_RANGE - DEAD_ZONE), 1.0)
    ratio = scale / dist
    return round(dx * ratio, 3), round(dy * ratio, 3)

def draw_arrow(frame, cx, cy, direction, r, size=10):
    offset = r - 6
    base   = size
    if direction == "up":
        tip = (cx,          cy - offset)
        bl  = (cx - base,   cy - offset + size)
        br  = (cx + base,   cy - offset + size)
    elif direction == "down":
        tip = (cx,          cy + offset)
        bl  = (cx - base,   cy + offset - size)
        br  = (cx + base,   cy + offset - size)
    elif direction == "left":
        tip = (cx - offset, cy)
        bl  = (cx - offset + size, cy - base)
        br  = (cx - offset + size, cy + base)
    elif direction == "right":
        tip = (cx + offset, cy)
        bl  = (cx + offset - size, cy - base)
        br  = (cx + offset - size, cy + base)
    else:
        return
    pts = np.array([tip, bl, br], np.int32)
    cv2.fillPoly(frame, [pts], LIGHT_BLUE)
    cv2.polylines(frame, [pts], True, (255, 255, 255), 1)

def bar_color(val):
    if val < 0.5:
        r = int(255 * (val / 0.5))
        g = 255
    else:
        r = 255
        g = int(255 * (1.0 - (val - 0.5) / 0.5))
    return (0, g, r)

def draw_bar_graphs(frame, label, jx, jy, status, panel_x, panel_y):
    pw, ph = 125, 88
    pad    = 7

    overlay = frame.copy()
    cv2.rectangle(overlay, (panel_x, panel_y),
                  (panel_x + pw, panel_y + ph), (0, 0, 0), -1)
    cv2.addWeighted(overlay, 0.6, frame, 0.4, 0, frame)
    cv2.rectangle(frame, (panel_x, panel_y),
                  (panel_x + pw, panel_y + ph), LIGHT_BLUE, 1)

    cv2.putText(frame, f"{label} ({status})",
                (panel_x + pad, panel_y + 12),
                cv2.FONT_HERSHEY_SIMPLEX, 0.38, LIGHT_BLUE, 1)

    bar_w  = pw - pad * 2
    bar_h  = 10
    center = panel_x + pad + bar_w // 2

    # jx bar
    jx_y   = panel_y + 28
    fill_w = int(abs(jx) * (bar_w // 2))
    color  = bar_color(abs(jx))
    cv2.rectangle(frame, (panel_x + pad, jx_y),
                  (panel_x + pad + bar_w, jx_y + bar_h), (40, 40, 40), -1)
    cv2.line(frame, (center, jx_y), (center, jx_y + bar_h), (100, 100, 100), 1)
    if jx >= 0:
        cv2.rectangle(frame, (center, jx_y),
                      (center + fill_w, jx_y + bar_h), color, -1)
    else:
        cv2.rectangle(frame, (center - fill_w, jx_y),
                      (center, jx_y + bar_h), color, -1)
    cv2.rectangle(frame, (panel_x + pad, jx_y),
                  (panel_x + pad + bar_w, jx_y + bar_h), LIGHT_BLUE, 1)
    cv2.putText(frame, f"jx {jx:+.2f}",
                (panel_x + pad, jx_y - 2),
                cv2.FONT_HERSHEY_SIMPLEX, 0.32, (200, 200, 200), 1)

    # jy bar
    jy_y   = panel_y + 62
    fill_w = int(abs(jy) * (bar_w // 2))
    color  = bar_color(abs(jy))
    cv2.rectangle(frame, (panel_x + pad, jy_y),
                  (panel_x + pad + bar_w, jy_y + bar_h), (40, 40, 40), -1)
    cv2.line(frame, (center, jy_y), (center, jy_y + bar_h), (100, 100, 100), 1)
    if jy >= 0:
        cv2.rectangle(frame, (center, jy_y),
                      (center + fill_w, jy_y + bar_h), color, -1)
    else:
        cv2.rectangle(frame, (center - fill_w, jy_y),
                      (center, jy_y + bar_h), color, -1)
    cv2.rectangle(frame, (panel_x + pad, jy_y),
                  (panel_x + pad + bar_w, jy_y + bar_h), LIGHT_BLUE, 1)
    cv2.putText(frame, f"jy {jy:+.2f}",
                (panel_x + pad, jy_y - 2),
                cv2.FONT_HERSHEY_SIMPLEX, 0.32, (200, 200, 200), 1)

def draw_joystick_circles(frame, origin, current_x, current_y):
    h, w   = frame.shape[:2]
    ox     = int(origin[0] * w)
    oy     = int(origin[1] * h)
    dead_r = int(DEAD_ZONE * w)
    max_r  = int(MAX_RANGE * w)

    cv2.line(frame, (ox - max_r, oy), (ox + max_r, oy), LIGHT_BLUE, 1)
    cv2.line(frame, (ox, oy - max_r), (ox, oy + max_r), LIGHT_BLUE, 1)
    cv2.circle(frame, (ox, oy), max_r,  LIGHT_BLUE, 3)
    cv2.circle(frame, (ox, oy), dead_r, LIGHT_BLUE, 3)
    cv2.circle(frame, (ox, oy), 5, (255, 255, 255), -1)

    for direction in ["up", "down", "left", "right"]:
        draw_arrow(frame, ox, oy, direction, max_r)

    cx   = int(current_x * w)
    cy   = int(current_y * h)
    dist = ((current_x - origin[0])**2 + (current_y - origin[1])**2) ** 0.5
    if dist < DEAD_ZONE:
        color = (0, 0, 255)
    elif dist < MAX_RANGE:
        color = (0, 255, 0)
    else:
        color = (255, 0, 0)
    cv2.circle(frame, (cx, cy), 8, color, -1)

def result_callback(result, output_image, timestamp_ms):
    global latest_result
    latest_result = result

base_options = python.BaseOptions(model_asset_path=model_path)
options = vision.HandLandmarkerOptions(
    base_options=base_options,
    running_mode=vision.RunningMode.LIVE_STREAM,
    num_hands=2,
    min_hand_detection_confidence=0.7,
    min_hand_presence_confidence=0.7,
    min_tracking_confidence=0.5,
    result_callback=result_callback
)
detector = vision.HandLandmarker.create_from_options(options)

cap = cv2.VideoCapture(0)
timestamp = 0

PW = 125  # panel width — must match draw_bar_graphs

while cap.isOpened():
    ret, frame = cap.read()
    if not ret:
        break

    frame = cv2.flip(frame, 1)
    timestamp += 1

    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
    detector.detect_async(mp_image, timestamp)

    left_info  = None
    right_info = None

    if latest_result and latest_result.hand_landmarks:
        for i, hand_landmarks in enumerate(latest_result.hand_landmarks):
            label = "Right" if latest_result.handedness[i][0].display_name == "Left" else "Left"
            lm    = hand_landmarks
            x, y  = palm_center(lm)
            state = "FIST" if is_fist(lm) else "OPEN"

            if label == "Left":
                left_info  = (x, y, state)
            else:
                right_info = (x, y, state)

            h, w = frame.shape[:2]
            for connection in [(0,1),(1,2),(2,3),(3,4),
                               (0,5),(5,6),(6,7),(7,8),
                               (0,9),(9,10),(10,11),(11,12),
                               (0,13),(13,14),(14,15),(15,16),
                               (0,17),(17,18),(18,19),(19,20),
                               (5,9),(9,13),(13,17)]:
                x1 = int(lm[connection[0]].x * w)
                y1 = int(lm[connection[0]].y * h)
                x2 = int(lm[connection[1]].x * w)
                y2 = int(lm[connection[1]].y * h)
                cv2.line(frame, (x1, y1), (x2, y2), (255, 255, 255), 2)
            for lmk in lm:
                cx2 = int(lmk.x * w)
                cy2 = int(lmk.y * h)
                cv2.circle(frame, (cx2, cy2), 4, (255, 255, 255), -1)

    # LEFT HAND
    left_jx, left_jy = 0.0, 0.0
    left_status       = "ND"
    if left_info:
        x, y, state = left_info
        if left_prev == "FIST" and state == "OPEN":
            left_origin = (x, y)
            print("Left origin set")
        if state == "OPEN" and left_origin:
            left_jx, left_jy = compute_joystick(x, y, left_origin)
            draw_joystick_circles(frame, left_origin, x, y)
            left_status = "A"
        else:
            left_status = "F"
        if state != last_left_state:
            print(f"Left hand: {state}")
        left_prev       = state
        last_left_state = state
    else:
        left_origin     = None
        left_prev       = "FIST"
        if last_left_state is not None:
            print("Left hand: not detected")
        last_left_state = None

    # RIGHT HAND
    right_jx, right_jy = 0.0, 0.0
    right_status        = "ND"
    if right_info:
        x, y, state = right_info
        if right_prev == "FIST" and state == "OPEN":
            right_origin = (x, y)
            print("Right origin set")
        if state == "OPEN" and right_origin:
            right_jx, right_jy = compute_joystick(x, y, right_origin)
            draw_joystick_circles(frame, right_origin, x, y)
            right_status = "A"
        else:
            right_status = "F"
        if state != last_right_state:
            print(f"Right hand: {state}")
        right_prev       = state
        last_right_state = state
    else:
        right_origin     = None
        right_prev       = "FIST"
        if last_right_state is not None:
            print("Right hand: not detected")
        last_right_state = None

    # Bar graphs — left top left, right top right
    frame_w = frame.shape[1]
    draw_bar_graphs(frame, "LEFT",  left_jx,  left_jy,  left_status,  10,            10)
    draw_bar_graphs(frame, "RIGHT", right_jx, right_jy, right_status, frame_w - PW - 10, 10)

    frame = cv2.resize(frame, None, fx=1.7, fy=1.7, interpolation=cv2.INTER_LINEAR)
    cv2.imshow("Hand Test", frame)
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
detector.close()
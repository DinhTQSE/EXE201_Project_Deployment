"""
V-Sign Feature Engineering Module
Chuẩn hóa tọa độ landmarks bằng Nose làm điểm gốc.
"""
import numpy as np


def normalize_landmarks(raw_landmarks):
    landmarks = np.array(raw_landmarks, dtype=float)
    
    nose_x = landmarks[0]
    nose_y = landmarks[1]
    nose_z = landmarks[2]
    
    for i in range(33):
        base_idx = i * 4
        landmarks[base_idx] -= nose_x
        landmarks[base_idx + 1] -= nose_y
        landmarks[base_idx + 2] -= nose_z
    
    for i in range(21):
        base_idx = 132 + i * 3
        landmarks[base_idx] -= nose_x
        landmarks[base_idx + 1] -= nose_y
        landmarks[base_idx + 2] -= nose_z
    
    for i in range(21):
        base_idx = 195 + i * 3
        landmarks[base_idx] -= nose_x
        landmarks[base_idx + 1] -= nose_y
        landmarks[base_idx + 2] -= nose_z
    
    return landmarks

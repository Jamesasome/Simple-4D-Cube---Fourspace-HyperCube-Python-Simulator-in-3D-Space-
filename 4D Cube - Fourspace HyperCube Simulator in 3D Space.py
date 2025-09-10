import numpy as np
import pyqtgraph.opengl as gl
from PyQt5 import QtWidgets, QtCore

# Vertices of 4D cube
data = np.array([
    [0,0,0,0], [1,0,0,0], [0,1,0,0], [1,1,0,0],
    [0,0,1,0], [1,0,1,0], [0,1,1,0], [1,1,1,0],
    [0,0,0,1], [1,0,0,1], [0,1,0,1], [1,1,0,1],
    [0,0,1,1], [1,0,1,1], [0,1,1,1], [1,1,1,1]
])

edges = [
    (0,1), (0,2), (0,4), (0,8),
    (1,3), (1,5), (1,9),
    (2,3), (2,6), (2,10),
    (3,7), (3,11),
    (4,5), (4,6), (4,12),
    (5,7), (5,13),
    (6,7), (6,14),
    (7,15),
    (8,9), (8,10), (8,12),
    (9,11), (9,13),
    (10,11), (10,14),
    (11,15),
    (12,13), (12,14),
    (13,15),
    (14,15)
]

# 4D → 3D perspective projection
def project_4d_to_3d(points4d, w_distance=2.5):
    w = points4d[:,3]
    factor = w_distance / (w_distance - w)
    return points4d[:,:3] * factor[:, np.newaxis]

# 4D rotation
def rotation_4d(theta, plane):
    c, s = np.cos(theta), np.sin(theta)
    R = np.eye(4)
    if plane == 'XY':
        R[0,0] = R[1,1] = c; R[0,1] = -s; R[1,0] = s
    elif plane == 'XZ':
        R[0,0] = R[2,2] = c; R[0,2] = -s; R[2,0] = s
    elif plane == 'XW':
        R[0,0] = R[3,3] = c; R[0,3] = -s; R[3,0] = s
    elif plane == 'YZ':
        R[1,1] = R[2,2] = c; R[1,2] = -s; R[2,1] = s
    elif plane == 'YW':
        R[1,1] = R[3,3] = c; R[1,3] = -s; R[3,1] = s
    elif plane == 'ZW':
        R[2,2] = R[3,3] = c; R[2,3] = -s; R[3,2] = s
    return R

# Qt Application
app = QtWidgets.QApplication([])
window = QtWidgets.QWidget()
layout = QtWidgets.QVBoxLayout()
window.setLayout(layout)
window.setWindowTitle("4D Cube Simulate in 3D Space")

# 3D view
view = gl.GLViewWidget()
view.setMinimumSize(800, 600)
layout.addWidget(view)
view.setCameraPosition(distance=6)

# Slider and checkbox panel
control_layout = QtWidgets.QGridLayout()
layout.addLayout(control_layout)

planes = ['XY','XZ','XW','YZ','YW','ZW']
sliders = {}
auto_checkboxes = {}

for i, plane in enumerate(planes):
    label = QtWidgets.QLabel(plane)
    control_layout.addWidget(label, i, 0)
    
    slider = QtWidgets.QSlider(QtCore.Qt.Horizontal)
    slider.setMinimum(0)
    slider.setMaximum(628)  # 0 -> 2pi
    slider.setValue(0)
    control_layout.addWidget(slider, i, 1)
    sliders[plane] = slider
    
    checkbox = QtWidgets.QCheckBox("Auto")
    checkbox.setChecked(True)  # automatic rotation on by default
    control_layout.addWidget(checkbox, i, 2)
    auto_checkboxes[plane] = checkbox

# Create line objects
lines = []
for edge in edges:
    pts = np.array([data[edge[0], :3], data[edge[1], :3]])  # initial placeholder
    line = gl.GLLinePlotItem(pos=pts, color=(1,1,0,1), width=2, antialias=True)
    view.addItem(line)
    lines.append(line)

# Automatic rotation angles
auto_angles = {plane: 0.0 for plane in planes}
auto_speeds = {'XY':0.01,'XZ':0.008,'XW':0.012,'YZ':0.007,'YW':0.009,'ZW':0.011}

# Update function
def update():
    R = np.eye(4)
    for plane in planes:
        # Update automatic angles if auto checkbox is checked
        if auto_checkboxes[plane].isChecked():
            auto_angles[plane] += auto_speeds[plane]
            auto_angles[plane] %= 2*np.pi
        # Use slider value if non-zero, otherwise use auto angle
        angle = sliders[plane].value() / 100.0
        if sliders[plane].value() == 0:
            angle = auto_angles[plane]
        R = R @ rotation_4d(angle, plane)
    rotated = (data - 0.5) @ R.T + 0.5
    projected = project_4d_to_3d(rotated)
    for i, edge in enumerate(edges):
        pts = np.array([projected[edge[0]], projected[edge[1]]])
        lines[i].setData(pos=pts)

timer = QtCore.QTimer()
timer.timeout.connect(update)
timer.start(30)

window.show()
QtWidgets.QApplication.instance().exec_()

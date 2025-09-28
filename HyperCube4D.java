import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.stage.Stage;

public class HyperCube4D extends Application {

    private static final double[][] DATA = {
            {0,0,0,0}, {1,0,0,0}, {0,1,0,0}, {1,1,0,0},
            {0,0,1,0}, {1,0,1,0}, {0,1,1,0}, {1,1,1,0},
            {0,0,0,1}, {1,0,0,1}, {0,1,0,1}, {1,1,0,1},
            {0,0,1,1}, {1,0,1,1}, {0,1,1,1}, {1,1,1,1}
    };

    private static final int[][] EDGES = {
            {0,1},{0,2},{0,4},{0,8},
            {1,3},{1,5},{1,9},
            {2,3},{2,6},{2,10},
            {3,7},{3,11},
            {4,5},{4,6},{4,12},
            {5,7},{5,13},
            {6,7},{6,14},
            {7,15},
            {8,9},{8,10},{8,12},
            {9,11},{9,13},
            {10,11},{10,14},
            {11,15},
            {12,13},{12,14},
            {13,15},
            {14,15}
    };

    private final String[] PLANES = {"XY","XZ","XW","YZ","YW","ZW"};
    private final DoubleProperty[] sliderAngles = new DoubleProperty[PLANES.length];
    private final boolean[] autoRotate = new boolean[PLANES.length];
    private final double[] autoAngles = new double[PLANES.length];
    private final double[] autoSpeeds = {0.01,0.008,0.012,0.007,0.009,0.011};

    private final Group edgeGroup = new Group();

    @Override
    public void start(Stage stage) {
        // Camera & Scene
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-8);

        SubScene subScene = new SubScene(edgeGroup, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.BLACK);
        subScene.setCamera(camera);

        // Controls
        GridPane controlPane = new GridPane();
        controlPane.setPadding(new Insets(10));
        controlPane.setHgap(10);
        controlPane.setVgap(5);

        for (int i = 0; i < PLANES.length; i++) {
            sliderAngles[i] = new SimpleDoubleProperty(0);
            autoRotate[i] = true;

            Label label = new Label(PLANES[i]);
            Slider slider = new Slider(0, 2*Math.PI, 0);
            slider.setShowTickLabels(false);
            slider.setShowTickMarks(false);

            sliderAngles[i].bind(slider.valueProperty());

            CheckBox autoBox = new CheckBox("Auto");
            autoBox.setSelected(true);
            final int idx = i;
            autoBox.selectedProperty().addListener((_, __, newVal) -> autoRotate[idx] = newVal);

            controlPane.addRow(i, label, slider, autoBox);
        }

        VBox root = new VBox(subScene, controlPane);
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("4D Hypercube Projection (JavaFX)");
        stage.show();

        // Build edges
        for (int[] edge : EDGES) {
            Cylinder line = makeEdge();
            edgeGroup.getChildren().add(line);
        }

        // Animation loop
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateEdges();
            }
        }.start();
    }

    private void updateEdges() {
        double[][] rotated = rotateAll(DATA);
        double[][] projected = project(rotated);

        int idx = 0;
        for (int[] edge : EDGES) {
            double[] p1 = projected[edge[0]];
            double[] p2 = projected[edge[1]];
            Cylinder line = (Cylinder) edgeGroup.getChildren().get(idx++);
            updateLine(line, p1, p2);
        }
    }

    // 4D rotation application
    private double[][] rotateAll(double[][] points) {
        double[][] R = identity(4);
        for (int i = 0; i < PLANES.length; i++) {
            if (autoRotate[i]) {
                autoAngles[i] += autoSpeeds[i];
                if (autoAngles[i] > 2*Math.PI) autoAngles[i] -= 2*Math.PI;
            }
            double angle = sliderAngles[i].get();
            if (angle == 0) angle = autoAngles[i];
            R = multiply(R, rotation4d(angle, PLANES[i]));
        }

        double[][] out = new double[points.length][4];
        for (int i = 0; i < points.length; i++) {
            double[] v = new double[4];
            for (int j = 0; j < 4; j++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) sum += (points[i][k]-0.5) * R[j][k];
                v[j] = sum + 0.5;
            }
            out[i] = v;
        }
        return out;
    }

    // Projection 4D → 3D
    private double[][] project(double[][] pts4d) {
        double wDist = 2.5;
        double[][] out = new double[pts4d.length][3];
        for (int i = 0; i < pts4d.length; i++) {
            double w = pts4d[i][3];
            double factor = wDist / (wDist - w);

            // Center around origin (-0.5 .. +0.5) before projecting
            out[i][0] = (pts4d[i][0] - 0.5) * factor;
            out[i][1] = (pts4d[i][1] - 0.5) * factor;
            out[i][2] = (pts4d[i][2] - 0.5) * factor;
        }
        return out;
    }


    // Edge as thin yellow cylinder
    private Cylinder makeEdge() {
        Cylinder cyl = new Cylinder(0.02, 1);
        cyl.setMaterial(new PhongMaterial(Color.YELLOW));
        return cyl;
    }

    // Place cylinder between p1 and p2
    private void updateLine(Cylinder line, double[] p1, double[] p2) {
        Point3D start = new Point3D(p1[0], p1[1], p1[2]);
        Point3D end = new Point3D(p2[0], p2[1], p2[2]);
        Point3D diff = end.subtract(start);
        double len = diff.magnitude();

        line.setHeight(len);
        line.setTranslateX((start.getX() + end.getX()) / 2);
        line.setTranslateY((start.getY() + end.getY()) / 2);
        line.setTranslateZ((start.getZ() + end.getZ()) / 2);

        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D axis = yAxis.crossProduct(diff);
        double angle = Math.acos(yAxis.dotProduct(diff) / len);
        line.setRotationAxis(axis.normalize());
        line.setRotate(Math.toDegrees(angle));
    }

    // Rotation matrix
    private double[][] rotation4d(double theta, String plane) {
        double c = Math.cos(theta), s = Math.sin(theta);
        double[][] R = identity(4);
        switch (plane) {
            case "XY": R[0][0]=c;R[0][1]=-s;R[1][0]=s;R[1][1]=c; break;
            case "XZ": R[0][0]=c;R[0][2]=-s;R[2][0]=s;R[2][2]=c; break;
            case "XW": R[0][0]=c;R[0][3]=-s;R[3][0]=s;R[3][3]=c; break;
            case "YZ": R[1][1]=c;R[1][2]=-s;R[2][1]=s;R[2][2]=c; break;
            case "YW": R[1][1]=c;R[1][3]=-s;R[3][1]=s;R[3][3]=c; break;
            case "ZW": R[2][2]=c;R[2][3]=-s;R[3][2]=s;R[3][3]=c; break;
        }
        return R;
    }

    private double[][] identity(int n) {
        double[][] I = new double[n][n];
        for (int i=0;i<n;i++) I[i][i]=1;
        return I;
    }

    private double[][] multiply(double[][] A, double[][] B) {
        double[][] C = new double[A.length][B[0].length];
        for (int i=0;i<A.length;i++)
            for (int j=0;j<B[0].length;j++)
                for (int k=0;k<B.length;k++)
                    C[i][j]+=A[i][k]*B[k][j];
        return C;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.fhooe.mc.mtproject.helpers.pose;

import android.util.Log;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;

import at.fhooe.mc.mtproject.PoseClassification;

/**
 * Counts reps for the give class.
 */
public class RepetitionCounter {
    // These thresholds can be tuned in conjunction with the Top K values in {@link PoseClassifier}.
    // The default Top K value is 10 so the range here is [0-10].
    private static final float DEFAULT_ENTER_THRESHOLD = 6f;
    private static final float DEFAULT_EXIT_THRESHOLD = 4f;

    private final String className;
    private final float enterThreshold;
    private final float exitThreshold;

    private int numRepeats;
    private boolean poseEntered;

    private ArrayList<Double> score;
    private ArrayList<Double> angleValues = new ArrayList<>();
    private ArrayList<Double> angleValuesElbow = new ArrayList<>();

    public RepetitionCounter(String className) {
        this(className, DEFAULT_ENTER_THRESHOLD, DEFAULT_EXIT_THRESHOLD);
    }

    public RepetitionCounter(String className, float enterThreshold, float exitThreshold) {
        this.className = className;
        this.enterThreshold = enterThreshold;
        this.exitThreshold = exitThreshold;
        numRepeats = 0;
        poseEntered = false;
        score = new ArrayList<Double>();
    }

    /**
     * Adds a new Pose classification result and updates reps for given class.
     *
     * @param classificationResult {link ClassificationResult} of class to confidence values.
     * @return number of reps.
     */
    public int addClassificationResult(ClassificationResult classificationResult, Pose pose) {
        float poseConfidence = classificationResult.getClassConfidence(className);

        if (!poseEntered) {
            poseEntered = poseConfidence > enterThreshold;
            return numRepeats;
        }

        calculateAngle(pose);

        if (poseConfidence < exitThreshold) {
            checkAngleValues();
            numRepeats++;
            poseEntered = false;
        }

        return numRepeats;
    }

    private void calculateAngle(Pose pose) {
        if (pose.getAllPoseLandmarks().isEmpty()) {
            return;
        }

        switch (className) {
            case PoseClassification.PUSHUPS_CLASS: {
                PoseLandmark lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
                PoseLandmark lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
                PoseLandmark lAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);

                PoseLandmark rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
                PoseLandmark rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
                PoseLandmark rAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);

                PoseLandmark lWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
                PoseLandmark rWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
                PoseLandmark lElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
                PoseLandmark rElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);

                double lHipAngle = PoseClassification.Companion.getAngle(lShoulder, lHip, lAnkle);
                double rHipAngle = PoseClassification.Companion.getAngle(rShoulder, rHip, rAnkle);

                double lElbowAngle = PoseClassification.Companion.getAngle(lShoulder, lElbow, lWrist);
                double rElbowAngle = PoseClassification.Companion.getAngle(rShoulder, rElbow, rWrist);

                angleValues.add(lHipAngle);
                angleValues.add(rHipAngle);

                angleValuesElbow.add(rElbowAngle);
                angleValuesElbow.add(lElbowAngle);
            }
            break;
            case PoseClassification.SQUATS_CLASS: {
                PoseLandmark lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
                PoseLandmark lKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
                PoseLandmark lAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);

                PoseLandmark rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
                PoseLandmark rKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);
                PoseLandmark rAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);

                double lKneeAngle = PoseClassification.Companion.getAngle(lHip, lKnee, lAnkle);
                double rKneeAngle = PoseClassification.Companion.getAngle(rHip, rKnee, rAnkle);

                angleValues.add(lKneeAngle);
                angleValues.add(rKneeAngle);

//                Log.e("LEFT KNEE: ", Double.toString(lKneeAngle));
//                Log.e("RIGHT KNEE: ", Double.toString(rKneeAngle));
            }
            break;
            case PoseClassification.SITUPS_CLASS: {

            }
            break;
        }
    }

    private void checkAngleValues() {
        switch (className) {
            case PoseClassification.SQUATS_CLASS: {
                score.add(calculateScore(100, angleValues));
                angleValues.clear();
            }
            break;
            case PoseClassification.PUSHUPS_CLASS: {
                score.add((calculateScore(175, angleValues) + calculateScore(90, angleValuesElbow)) / 2);
                angleValues.clear();
                angleValuesElbow.clear();
            }
            break;
            case PoseClassification.SITUPS_CLASS: {
                score.add(-1.0);
            }
            break;
        }
    }

    public String getClassName() {
        return className;
    }

    public int getNumRepeats() {
        return numRepeats;
    }

    public ArrayList<Double> getScore() {
        return score;
    }

    public double getAverageScore() {
        if (score.isEmpty()) {
            return -1;
        }
        double avg = 0;
        int positionsSkipped = 0;
        for (double i : score) {
            if (i < 0) {
                positionsSkipped++;
            }
            avg += i;
        }

        double result = avg / (score.size() - positionsSkipped);
        return Double.isNaN(result) ? -1 : result;
    }

    private double calculateScore(Integer thresholdAngle, ArrayList<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double avg = values.stream().mapToDouble(a -> a).average().getAsDouble();

        return Math.min(1.0, (thresholdAngle / avg)) * 10;
    }
}

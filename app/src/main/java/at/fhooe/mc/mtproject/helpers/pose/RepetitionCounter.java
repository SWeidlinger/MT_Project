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


import com.google.mlkit.vision.pose.Pose;

import java.util.ArrayList;

import at.fhooe.mc.mtproject.PoseClassification;
import at.fhooe.mc.mtproject.PushUpRatingAlgorithms;
import at.fhooe.mc.mtproject.SquatRatingAlgorithms;
import at.fhooe.mc.mtproject.bottomSheet.recyclerView.CategoryConstants;
import at.fhooe.mc.mtproject.bottomSheet.recyclerView.DetailedRepCategoryData;

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

    private ArrayList<Double> angleValuesHip = new ArrayList<>();
    private ArrayList<Double> angleValuesElbow = new ArrayList<>();
    private Double maxSquatDepth = 0.0;
    private ArrayList<Double> maxSquatDepthList = new ArrayList<>();
    private ArrayList<Double> stanceWidthList = new ArrayList<>();
    private ArrayList<Double> torsoLengthList = new ArrayList<>();

    private ArrayList<ArrayList<DetailedRepCategoryData>> categoryDataList;

    public RepetitionCounter(String className) {
        this(className, DEFAULT_ENTER_THRESHOLD, DEFAULT_EXIT_THRESHOLD);
        numRepeats = 0;
        poseEntered = false;
        categoryDataList = new ArrayList<>();
        maxSquatDepth = 0.0;
        stanceWidthList = new ArrayList<>();
        maxSquatDepthList = new ArrayList<>();
        torsoLengthList = new ArrayList<>();
    }

    public RepetitionCounter(String className, float enterThreshold, float exitThreshold) {
        this.className = className;
        this.enterThreshold = enterThreshold;
        this.exitThreshold = exitThreshold;
        numRepeats = 0;
        poseEntered = false;
        categoryDataList = new ArrayList<>();
        maxSquatDepth = 0.0;
        stanceWidthList = new ArrayList<>();
        maxSquatDepthList = new ArrayList<>();
        torsoLengthList = new ArrayList<>();
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

        getRating(pose);

        if (poseConfidence < exitThreshold) {
            calculateCategoryData();
            numRepeats++;
            poseEntered = false;
        }

        return numRepeats;
    }

    private void getRating(Pose pose) {
        if (pose.getAllPoseLandmarks().isEmpty()) {
            return;
        }

        switch (className) {
            case PoseClassification.SQUATS_CLASS: {
                maxSquatDepth = SquatRatingAlgorithms.Companion.getSquatDepthAngle(pose, maxSquatDepth);
                stanceWidthList.add(SquatRatingAlgorithms.Companion.getStanceWidth(pose));
                torsoLengthList.add(SquatRatingAlgorithms.Companion.getTorsoAlignment(pose));
            }
            break;
            case PoseClassification.PUSHUPS_CLASS: {
                angleValuesElbow.add(PushUpRatingAlgorithms.Companion.getElbowAngle(pose));
                angleValuesHip.add(PushUpRatingAlgorithms.Companion.getHipAngle(pose));
            }
            break;
            case PoseClassification.SITUPS_CLASS: {

            }
            break;
        }
    }

    private void calculateCategoryData() {
        switch (className) {
            case PoseClassification.SQUATS_CLASS: {
                String squatDepthScore = SquatRatingAlgorithms.Companion.calculateSquatDepthScore(maxSquatDepth);
                String stanceWidthScore = SquatRatingAlgorithms.Companion.getStanceWidthScore(stanceWidthList);
                String torsoAlignmentScore = SquatRatingAlgorithms.Companion.getTorsoAlignmentScore(torsoLengthList);

                ArrayList<DetailedRepCategoryData> categoryDataExercise = new ArrayList<>();
                categoryDataExercise.add(new DetailedRepCategoryData(CategoryConstants.STANCE_WIDTH, stanceWidthScore, className));
                categoryDataExercise.add(new DetailedRepCategoryData(CategoryConstants.TORSO_ALIGNMENT, torsoAlignmentScore, className));
                categoryDataExercise.add(new DetailedRepCategoryData(CategoryConstants.SQUAT_DEPTH, squatDepthScore, className));

                categoryDataList.add(categoryDataExercise);
                maxSquatDepthList.add(maxSquatDepth);
                resetCategoryData();
            }
            break;
            case PoseClassification.PUSHUPS_CLASS: {
                String bodyAlignmentScore = PushUpRatingAlgorithms.Companion.calculateBodyAlignmentScore(angleValuesElbow,angleValuesHip);
                ArrayList<DetailedRepCategoryData> categoryDataExercise = new ArrayList<>();
                categoryDataExercise.add(new DetailedRepCategoryData(CategoryConstants.BODY_ALIGNMENT, bodyAlignmentScore, className));

                categoryDataList.add(categoryDataExercise);
                angleValuesHip.clear();
                angleValuesElbow.clear();
            }
            break;
            case PoseClassification.SITUPS_CLASS: {
                categoryDataList.add(new ArrayList<>());
            }
            break;
        }
    }

    //reset the data related for the rating
    private void resetCategoryData() {
        maxSquatDepth = 0.0;
        stanceWidthList.clear();
        torsoLengthList.clear();
    }

    public String getClassName() {
        return className;
    }

    public int getNumRepeats() {
        return numRepeats;
    }

    private ArrayList<Double> getScore() {
        ArrayList<Double> scoreList = new ArrayList<>();
        for (ArrayList<DetailedRepCategoryData> exercise : categoryDataList) {
            for (DetailedRepCategoryData category : exercise) {
                scoreList.add(Double.parseDouble(category.getScore()));
            }
        }

        return scoreList;
    }

    public double getScore(int index) {
        double avg = 0;
        ArrayList<DetailedRepCategoryData> categoryList = categoryDataList.get(index);
        for (DetailedRepCategoryData category : categoryList) {
            avg += Double.parseDouble(category.getScore());
        }

        if (categoryList.size() <= 0) {
            return -1.0;
        } else {
            return avg / categoryList.size();
        }
    }

    public double getAverageScore() {
        ArrayList<Double> scoreList = getScore();

        if (scoreList.isEmpty()) {
            return -1;
        }

        double avg = 0;
        for (double score : scoreList) {
            avg += score;
        }

        return avg / scoreList.size();
    }

    public ArrayList<Double> getMaxSquatDepthList() {
        return maxSquatDepthList;
    }

    public ArrayList<ArrayList<DetailedRepCategoryData>> getCategoryDataList() {
        return categoryDataList;
    }
}

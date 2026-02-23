package com.loadly.mvp.ai;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIWeeklyPlan {
    private List<String> warnings = new ArrayList<>();
    private List<StudyBlock> studyBlocks = new ArrayList<>();
    private List<String> tips = new ArrayList<>();
}

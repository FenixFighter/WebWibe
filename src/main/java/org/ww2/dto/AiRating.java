package org.ww2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRating {
    private int score; // 0-100
    private String answer; // Explanation of the rating
}

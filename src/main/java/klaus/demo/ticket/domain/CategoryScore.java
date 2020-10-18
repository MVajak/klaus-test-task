package klaus.demo.ticket.domain;

import java.time.LocalDateTime;

public class CategoryScore {
    private String categoryName;
    private int score;
    private String createdAt;

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getParsedDate() {
        return LocalDateTime.parse(createdAt);
    }
}

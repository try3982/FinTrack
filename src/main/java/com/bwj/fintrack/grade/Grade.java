package com.bwj.fintrack.grade;


import com.bwj.fintrack.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "f_grades")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "grade", fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private GradeType gradeType;

    public void promoteTo(GradeType newType) {
        // 방어: null은 그냥 무시. 도메인 입장에선 승급 불가능으로 해석.
        if (newType == null) {
            return;
        }

        // 만약 현재 등급이 이미 더 높거나 같으면(=downgrade or same) 아무 것도 안 함
        if (isHigherOrEqual(this.gradeType, newType)) {
            return;
        }

        // 여기까지 왔으면 실제로 승급
        this.gradeType = newType;
    }

    private boolean isHigherOrEqual(GradeType current, GradeType candidate) {
        return rank(current) >= rank(candidate);
    }

    private int rank(GradeType gradeType) {
        return switch (gradeType) {
            case BRONZE -> 1;
            case SILVER -> 2;
            case GOLD -> 3;
            case PREMIUM -> 4;
            case DIAMOND -> 5;
        };
    }

}

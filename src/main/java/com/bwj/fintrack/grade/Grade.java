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
}

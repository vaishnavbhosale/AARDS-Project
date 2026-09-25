package com.aards.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String seatNo;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 50)
    private String department;

    @Column(length = 20)
    private String academicYear;

    @Column(length = 20)
    private String semester;

    private Double sgpa;

    @Column(length = 20)
    private String result;
}

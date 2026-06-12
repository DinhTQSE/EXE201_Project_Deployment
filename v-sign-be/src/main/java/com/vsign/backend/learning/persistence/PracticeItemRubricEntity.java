package com.vsign.backend.learning.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "practice_item_rubrics")
public class PracticeItemRubricEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "practice_item_id", nullable = false, length = 100)
    private String practiceItemId;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected PracticeItemRubricEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getPracticeItemId() {
        return practiceItemId;
    }

    public String getCode() {
        return code;
    }

    public int getOrderIndex() {
        return orderIndex;
    }
}

package com.example.space.repository;

import com.example.space.entity.SpaceCategory;

public interface SpaceCategoryCountProjection {

    SpaceCategory getCategory();

    Long getCount();
}

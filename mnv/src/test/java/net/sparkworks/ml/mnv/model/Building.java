package net.sparkworks.ml.mnv.model;

import lombok.Builder;
import lombok.Data;
import net.sparkworks.cargo.common.dto.ResourceDTO;

import java.util.Objects;

@Data
@Builder
public class Building {
    private String name;
    private ResourceDTO small;
    private ResourceDTO large;
}

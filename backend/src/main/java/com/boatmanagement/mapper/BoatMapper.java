package com.boatmanagement.mapper;

import com.boatmanagement.dto.BoatDto;
import com.boatmanagement.entity.Boat;
import org.mapstruct.*;
import org.springframework.lang.NonNull;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BoatMapper {

    @NonNull BoatDto.Response toResponse(@NonNull Boat boat);

    @NonNull
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Boat toEntity(@NonNull BoatDto.Request request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget @NonNull Boat boat, @NonNull BoatDto.Request request);
}

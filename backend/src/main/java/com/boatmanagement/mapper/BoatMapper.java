package com.boatmanagement.mapper;

import com.boatmanagement.dto.BoatDto;
import com.boatmanagement.entity.Boat;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BoatMapper {

    BoatDto.Response toResponse(Boat boat);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Boat toEntity(BoatDto.Request request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget Boat boat, BoatDto.Request request);
}

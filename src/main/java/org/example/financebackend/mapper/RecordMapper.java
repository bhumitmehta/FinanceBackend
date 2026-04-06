package org.example.financebackend.mapper;

import org.example.financebackend.dto.request.CreateRecordRequest;
import org.example.financebackend.dto.request.UpdateRecordRequest;
import org.example.financebackend.dto.response.RecordResponse;
import org.example.financebackend.model.FinancialRecord;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface RecordMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "user",      ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    FinancialRecord toEntity(CreateRecordRequest request);

    RecordResponse toResponse(FinancialRecord record);

    /** Partial update — null fields in the request are skipped. */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "user",      ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateRecordRequest request, @MappingTarget FinancialRecord record);
}

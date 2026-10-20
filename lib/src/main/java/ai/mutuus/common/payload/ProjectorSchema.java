package ai.mutuus.common.payload;
import java.util.*;
public record ProjectorSchema(String projectorId,List<AllowedScalarField> fields){
 public ProjectorSchema{
  if(projectorId==null || !projectorId.matches("[A-Za-z][A-Za-z0-9_.-]{0,63}"))throw new IllegalArgumentException("Invalid projector ID");
  fields=List.copyOf(fields);
  if(fields.isEmpty() || fields.size()>32 || fields.stream().map(AllowedScalarField::name).distinct().count()!=fields.size())throw new IllegalArgumentException("Invalid schema fields");
 }
}

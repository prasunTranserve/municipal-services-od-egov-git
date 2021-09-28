package org.egov.mr.service;


import org.egov.mr.web.models.Difference;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.tracer.model.CustomException;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ValueChange;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static org.egov.mr.util.MRConstants.*;

@Service
public class DiffService {


   
    public Map<String, Difference> getDifference(MarriageRegistrationRequest request, List<MarriageRegistration> searchResult) {

        List<MarriageRegistration> registrations = request.getMarriageRegistrations();
        Map<String, Difference> diffMap = new LinkedHashMap<>();
        Map<String, MarriageRegistration> idToMarriageRegistrationMap = new HashMap<>();

        searchResult.forEach(marriageRegistration -> {
            idToMarriageRegistrationMap.put(marriageRegistration.getId(), marriageRegistration);
        });

        MarriageRegistration marriageRegistrationFromSearch;

        for (MarriageRegistration marriageRegistration : registrations) {
            marriageRegistrationFromSearch = idToMarriageRegistrationMap.get(marriageRegistration.getId());
            Difference diff = new Difference();
            diff.setId(marriageRegistration.getId());
            diff.setFieldsChanged(getUpdatedFields(marriageRegistration, marriageRegistrationFromSearch));
            diff.setClassesAdded(getObjectsAdded(marriageRegistration, marriageRegistrationFromSearch));
            diff.setClassesRemoved(getObjectsRemoved(marriageRegistration, marriageRegistrationFromSearch));
            diffMap.put(marriageRegistration.getId(), diff);
        }

        return diffMap;
    }


    
    private List<String> getUpdatedFields(MarriageRegistration marriageRegistrationFromUpdate, MarriageRegistration marriageRegistrationFromSearch) {

        Javers javers = JaversBuilder.javers().build();

        Diff diff = javers.compare(marriageRegistrationFromUpdate, marriageRegistrationFromSearch);
        List<ValueChange> changes = diff.getChangesByType(ValueChange.class);

        List<String> updatedFields = new LinkedList<>();

        if (CollectionUtils.isEmpty(changes))
            return updatedFields;

        changes.forEach(change -> {
            if (!FIELDS_TO_IGNORE.contains(change.getPropertyName())) {
                updatedFields.add(change.getPropertyName());
            }
        });
        return updatedFields;
    }


   
    private List<String> getObjectsAdded(MarriageRegistration marriageRegistrationFromUpdate, MarriageRegistration marriageRegistrationFromSearch) {

        Javers javers = JaversBuilder.javers().build();
        Diff diff = javers.compare(marriageRegistrationFromSearch, marriageRegistrationFromUpdate);
        List objectsAdded = diff.getObjectsByChangeType(NewObject.class);
        ;

        List<String> classModified = new LinkedList<>();

        if (CollectionUtils.isEmpty(objectsAdded))
            return classModified;

        objectsAdded.forEach(object -> {
            String className = object.getClass().toString().substring(object.getClass().toString().lastIndexOf('.') + 1);
            if (!classModified.contains(className))
                classModified.add(className);
        });
        return classModified;
    }


    
    private List<String> getObjectsRemoved(MarriageRegistration marriageRegistrationFromUpdate, MarriageRegistration marriageRegistrationFromSearch) {

        Javers javers = JaversBuilder.javers().build();
        Diff diff = javers.compare(marriageRegistrationFromUpdate, marriageRegistrationFromSearch);
        List<ValueChange> changes = diff.getChangesByType(ValueChange.class);

        List<String> classRemoved = new LinkedList<>();

        if (CollectionUtils.isEmpty(changes))
            return classRemoved;

        changes.forEach(change -> {
            if (change.getPropertyName().equalsIgnoreCase(VARIABLE_ACTIVE)
                    || change.getPropertyName().equalsIgnoreCase(VARIABLE_USERACTIVE)) {
                classRemoved.add(getObjectClassName(change.getAffectedObject().toString()));
            }
        });
        return classRemoved;
    }

    
    private String getObjectClassName(String affectedObject) {
        String className = null;
        try {
            String firstSplit = affectedObject.substring(affectedObject.lastIndexOf('.') + 1);
            className = firstSplit.split("@")[0];
        } catch (Exception e) {
            throw new CustomException("NOTIFICATION ERROR", "Failed to fetch notification");
        }
        return className;
    }


}

package org.egov.bpa.calculator.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.bpa.calculator.edcr.model.Occupancy;
import org.egov.bpa.calculator.edcr.model.OccupancytypeHelper;
import org.egov.bpa.calculator.services.CalculationService;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EdcrHelperUtils {
	
	public static List<Occupancy> getOccupancieswiseDetails(LinkedHashMap edcrResponse){
		
		List<Occupancy> finalList = new ArrayList<>();
	
		try {
	
		List edcrdetails = (List) edcrResponse.get("edcrDetail");
		
		Object planDetails  = ((Map)(edcrdetails.get(0))).get("planDetail");
		
		List blocks  = (List) (((Map) planDetails).get("blocks"));
		
		List<Occupancy> occupancyHelper = new ArrayList<>();
		
		Map<String,Map<String,Occupancy>> occupancyMap = new HashMap<>();
 		
		
for(int i=0; i<blocks.size();i++) {
		log.info("inside block info");	
			Object block =  blocks.get(i);
			
			Object building = ((Map)block).get("building");
			
			List floors = (List) ((Map)building).get("floors");
			
			for(int j=0;j<floors.size();j++) {
			
				Object fllor = floors.get(j);
				
				List  Occupanc = (List) ((Map)fllor).get("occupancies");
				
			for(int k=0;k<Occupanc.size();k++) {
				
				log.info("inside occupancy info");
				Object occ = Occupanc.get(k);
				
				Occupancy occupancy = new  Occupancy() ; 
				
				Object typehelper = ((Map)occ).get("typeHelper");
		    
				occupancy.setType((String)((Map)occ).get("type"));
				ObjectMapper mapper = new ObjectMapper();
				OccupancytypeHelper occupancytypeHelper  = mapper.convertValue(
						typehelper, new TypeReference<OccupancytypeHelper>() {});
				
				occupancy.setTypeHelper(occupancytypeHelper);
				
				//occupancy.setDeduction((double)(Integer)(((Map)occ).get("deduction")));
				Object builtArea=((Map)occ).get("builtUpArea");
				
				BigDecimal builtUpArea = new BigDecimal(builtArea.toString());
				
				occupancy.setBuiltUpArea(builtUpArea.doubleValue());
				
				Object floorArea=((Map)occ).get("floorArea");
				
				BigDecimal fllrarea=  new BigDecimal(floorArea.toString());
				
				occupancy.setFloorArea(fllrarea.doubleValue());
			
                //occupancy.setCarpetArea((double)(Integer)(((Map)occ).get("carpetArea")));
				
				//occupancy.setCarpetAreaDeduction((double)(Integer)(((Map)occ).get("carpetAreaDeduction")));
				
				Object existbuiltup=((Map)occ).get("existingBuiltUpArea");
				
				BigDecimal existBuiltUpArea=  new BigDecimal(existbuiltup.toString());
				occupancy.setExistingBuiltUpArea(existBuiltUpArea.doubleValue());
				
                Object existFllorup=((Map)occ).get("existingFloorArea");
				
				BigDecimal existfloorArea=  new BigDecimal(existFllorup.toString());
				
				occupancy.setExistingFloorArea(existfloorArea.doubleValue());
			
               // occupancy.setExistingBuiltUpArea((double)(Integer)(((Map)occ).get("existingCarpetArea")));
				
				//occupancy.setExistingCarpetAreaDeduction((double)(Integer)(((Map)occ).get("existingCarpetAreaDeduction")));
				

               // occupancy.setExistingDeduction((double)(Integer)(((Map)occ).get("existingDeduction")));
                
                occupancy.setSubOccupancyCode(occupancy.getTypeHelper().getOccupancySubType().getCode());
                
                
                occupancy.setOccupancyCode(occupancy.getTypeHelper().getOccupancytype().getCode());
				
                occupancyHelper.add(occupancy);
				
				
		        
			}
			}}


Set<String> typehelper = occupancyHelper.stream().filter(o->o.getSubOccupancyCode()!=null).map(Occupancy::getSubOccupancyCode).collect(Collectors.toSet());	
//Set<OccupancytypeHelper> subType = occupancyHelper.stream().filter(o->o.getTypeHelper()!=null).map(Occupancy::getTypeHelper).collect(Collectors.toSet());	

for(String code:typehelper) {
	
	log.info("inside final cal info");
	
	Occupancy occ = new Occupancy();
	List<Double> builtupArea = occupancyHelper.stream().filter(o->o.getSubOccupancyCode().equals(code)).map(Occupancy::getBuiltUpArea).collect(Collectors.toList());
	List<Double> floorArea   = occupancyHelper.stream().filter(o->o.getSubOccupancyCode().equals(code)).map(Occupancy::getFloorArea).collect(Collectors.toList());
	List<Double> existbuiltupArea = occupancyHelper.stream().filter(o->o.getSubOccupancyCode().equals(code)).map(Occupancy::getExistingBuiltUpArea).collect(Collectors.toList());
	List<Double> existfloorArea   = occupancyHelper.stream().filter(o->o.getSubOccupancyCode().equals(code)).map(Occupancy::getExistingFloorArea).collect(Collectors.toList());

	Set<OccupancytypeHelper> subType =occupancyHelper.stream().filter(o->o.getSubOccupancyCode().equals(code)).map(Occupancy::getTypeHelper).collect(Collectors.toSet());

Double finalBuiltupArea = builtupArea.stream().mapToDouble(a -> a)
		    .sum();
	Double finalfloorArea = floorArea.stream().mapToDouble(a -> a)
		    .sum();
	Double existbuiltUpArea =existbuiltupArea.stream().mapToDouble(a -> a)
		    .sum();
	Double existFloorArea = existfloorArea.stream().mapToDouble(a -> a)
		    .sum();
   occ.setBuiltUpArea(finalBuiltupArea);
   System.out.println("final"+finalfloorArea);
   occ.setFloorArea(finalfloorArea);
   occ.setExistingBuiltUpArea(existbuiltUpArea);
   occ.setExistingFloorArea(existFloorArea);
 // occ.setType(code);
  // occ.setTypeHelper(null);
  occ.setSubOccupancyCode(code);
  occ.setTypeHelper(subType.stream().findFirst().get());
  occ.setOccupancyCode(occ.getTypeHelper().getOccupancytype().getCode());
  
   finalList.add(occ);
  
}	
return finalList;
		}catch(Exception ex) {
			throw new CustomException("Drwaing Detail Error","Error while fetching drawing Detail :"+ex);
		}
		//return finalList;
		
	}

	public List<Occupancy> getOccupancieswiseDetailsforpreApproved(Object drawingDetail) {
		
		try {
			
			log.info("inside preApproved info");
		List<Occupancy> finalList = new ArrayList<>();
		
		Occupancy occ = new Occupancy();
		
		Object subocc = ((Map) drawingDetail).get("subOccupancy");
		
		occ.setSubOccupancyCode((String)  ((Map) subocc).get("value"));
		
		log.info("inside preAprroved occupancy info"+occ.getSubOccupancyCode());
		
		Object builtArea = ((Map)drawingDetail).get("totalBuitUpArea");
		
		BigDecimal builtUpArea = new BigDecimal(builtArea.toString());
		
		occ.setBuiltUpArea(builtUpArea.doubleValue());
		occ.setOccupancyCode("A");
         
		
		finalList.add(occ);
		
		return finalList;
		
		}catch(Exception ex) {
			throw new CustomException("Drwaing Detail Error","Error while fetching drawing Detail :"+ex);
		}
		
		
	}
	
	
}

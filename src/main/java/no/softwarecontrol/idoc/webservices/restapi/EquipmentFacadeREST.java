/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.data.requestparams.EquipmentRequestParameters;
import no.softwarecontrol.idoc.data.requestparams.PopupSortMode;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.equipment")
@RolesAllowed({"ApplicationRole"})
public class EquipmentFacadeREST extends AbstractFacade<Equipment> {

    public EquipmentFacadeREST() {
        super(Equipment.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return "Equipment.findAll";
    }


    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Equipment entity) {
        setEquipmentType(entity);
        super.create(entity);
    }

    @POST
    @Path("createWithAsset/{assetId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithAsset(@PathParam("assetId") String assetId, Equipment entity) {
        Equipment existing = findNative(entity.getEquipmentId());
        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        MeasurementFacadeREST measurementFacadeREST = new MeasurementFacadeREST();
        List<Measurement> measurements = new ArrayList<>(entity.getMeasurementList());
        if (existing == null) {
            entity.setDeleted(false);
            setEquipmentType(entity);
            for (Equipment child : entity.getEquipmentList()) {
                child.setParent(entity);
            }
            Asset asset = assetFacadeREST.findNative(assetId);
            entity.setAsset(asset);
            entity.getMeasurementList().clear();
            super.create(entity);

            for (Measurement measurement : measurements) {
                // Check if measurement already exists
                Measurement existingMeasurement = measurementFacadeREST.find(measurement.getMeasurementId());
                if (existingMeasurement == null) {
                    entity.getMeasurementList().add(measurement);
                    //edit(entity);
                } else {
                    linkMeasurement(measurement.getMeasurementId(), entity.getEquipmentId());
                }
            }

            setLocation(entity);

            edit(entity.getEquipmentId(), entity);
        } else {
            setAsset(entity, assetId);
            edit(entity.getEquipmentId(), entity);
        }
    }

    private void setLocation(Equipment entity) {
        LocationFacadeREST locationFacadeREST = new LocationFacadeREST();
        if (entity.getLocation() != null) {
            Location location = locationFacadeREST.find(entity.getLocation().getLocationId());
            if (location != null) {
                entity.setLocation(location);
//                if (!location.getEquipmentList().contains(entity)) {
//                    location.getEquipmentList().add(entity);
//                }
            }
        }
    }

    private void setEquipmentType(Equipment entity) {
        EquipmentTypeFacadeREST equipmentTypeFacadeREST = new EquipmentTypeFacadeREST();
        if (entity.getEquipmentType() != null) {
            EquipmentType equipmentType = equipmentTypeFacadeREST.find(entity.getEquipmentType().getEquipmentTypeId());
            if (equipmentType == null) {
                equipmentType = equipmentTypeFacadeREST.find("d0dd7761-0c29-4e3f-93a5-564d666c1510");
            }
            entity.setEquipmentType(equipmentType);
        } else {

            EquipmentType equipmentType = equipmentTypeFacadeREST.find("d0dd7761-0c29-4e3f-93a5-564d666c1510");
            entity.setEquipmentType(equipmentType);
        }
    }

    private void setAsset(Equipment entity, String assetId) {
        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        Asset asset = assetFacadeREST.find(assetId);
        if (asset != null) {
            entity.setAsset(asset);
            if (!asset.getEquipmentList().contains(entity)) {
                asset.getEquipmentList().add(entity);
            }
            assetFacadeREST.edit(asset.getAssetId(), asset);
        }
    }

    @POST
    @Path("createWithParent/{parentId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithParent(@PathParam("parentId") String parentId, Equipment entity) {
        Equipment existing = findNative(entity.getEquipmentId());
        if (existing == null) {
            EquipmentTypeFacadeREST equipmentTypeFacadeREST = new EquipmentTypeFacadeREST();
            EquipmentType equipmentType = equipmentTypeFacadeREST.find(entity.getEquipmentType().getEquipmentTypeId());
            entity.setEquipmentType(equipmentType);
            entity.setDeleted(false);

            Equipment parent = this.find(parentId);
            entity.setParent(parent);
            if (!parent.getEquipmentList().contains(entity)) {
                parent.getEquipmentList().add(entity);
            }
            super.create(entity);
        }
    }

    public void linkMeasurement(String measurementId, String equipmentId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM equipment_has_measurement \n " +
                            " WHERE equipment_equipment_id = ?1 AND measurement_measurement_id = ?2")
                    .setParameter(1, equipmentId)
                    .setParameter(2, measurementId);

            Number counter = (Number) query.getSingleResult();
            if (counter.intValue() == 0) {
                tx.begin();
                final int i = em.createNativeQuery(
                                "INSERT INTO equipment_has_measurement (equipment_equipment_id, measurement_measurement_id)\n" +
                                        "VALUES (?, ?);"
                        ).setParameter(1, equipmentId)
                        .setParameter(2, measurementId)
                        .executeUpdate();
                tx.commit();
            } else {
                //System.out.println("No problem: equipment_has_measurement already exists");
            }
        } catch (Exception exp) {
            tx.rollback();
            //System.out.println("Exception while inserting into equipment_has_measurement: " + exp.getMessage());
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Equipment entity) {
        Equipment equipment = findNative(entity.getEquipmentId());
        if (equipment != null) {
            for (Equipment child : entity.getEquipmentList()) {
                if (!equipment.getEquipmentList().contains(child)) {
                    child.setParent(equipment);
                    equipment.getEquipmentList().add(child);
                }
            }
            if (entity.getLocation() != null) {
                LocationFacadeREST locationFacadeREST = new LocationFacadeREST();
                Location location = locationFacadeREST.find(entity.getLocation().getLocationId());
                if (location != null) {
                    equipment.setLocation(location);
                }
            } else {
                if(entity.getLocationId() == null) {
                    equipment.setLocation(null);
                }
            }
            setEquipmentType(entity);
            equipment.setEquipmentType(entity.getEquipmentType());
            equipment.setName(entity.getName());
            equipment.setTagId(entity.getTagId());
            equipment.setDeleted(entity.isDeleted());
            if(!entity.getMeasurementList().isEmpty()) {

            }
            super.edit(equipment);

            if (equipment.getLocation() != null) {
                ObservationFacadeREST observationFacadeREST = new ObservationFacadeREST();
                List<Observation> equipmentObservations = observationFacadeREST.findEquipmentObservationsNative(equipment.getEquipmentId());
                for (Observation observation : equipmentObservations) {
                    if (observation.getEquipment() != null && observation.getLocation() != null && equipment.getLocation() != null) {
                        if (!observation.getLocation().getLocationId().equalsIgnoreCase(equipment.getLocation().getLocationId())) {
                            observation.setLocation(equipment.getLocation());
                            observationFacadeREST.edit(observation);
                        }
                    }
                }
            }
        } else {
            System.out.println("Oppdatering av equipment feilet: Equipment mangler: equipmentId = " + entity.getEquipmentId());
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("findByAsset/{assetId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Equipment> findByAsset(@PathParam("assetId") String assetId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Equipment> equipments = (List<Equipment>) em.createNativeQuery("SELECT "
                                + "e.* FROM equipment e\n"
                                + "WHERE e.asset = ?1",
                        Equipment.class)
                .setParameter(1, assetId)
                .getResultList();
        em.close();
        return equipments;
    }

    private String createSqlString(EquipmentRequestParameters parameters, Boolean isCounting) {
        String sqlString = "";
        String selectString = "";
        String whereString = "";
        String deleteParameter = "0";
        String statusString = "Status";
        if (parameters.showDeleted) {
            deleteParameter = "1";
        }
        String projectId = parameters.projectId;
        String assetId = "'" + parameters.entityIds.get(0) + "'";
        String disiplineId = null;
        if(projectId != null) {
            ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
            Project project = projectFacadeREST.findNative(projectId);
            if(project != null) {
                if (project.getDisipline() != null) {
                    disiplineId = project.getDisipline().getDisiplineId();
                }
            }
        }
        if (isCounting) {
            selectString = "SELECT count(distinct e.equipment_id), \n";
        } else {
            selectString = "SELECT e.*, \n";
        }
        if (projectId != null) {

            selectString += "\t(SELECT o.observation_id from observation o where o.equipment = e.equipment_id and o.project = '" + projectId + "' and o.deleted = 0 ORDER BY o.created_date DESC LIMIT 1) as observationId, \n";
            selectString += "\t(SELECT o.project from observation o where o.equipment = e.equipment_id and o.project = '" + projectId + "' and o.deleted = 0 ORDER BY o.created_date DESC LIMIT 1) as projectId, \n";
            selectString += "\t(SELECT ohm.measurement_measurement_id from observation o \n";
            selectString += "\t\tLEFT JOIN observation_has_measurement ohm on ohm.observation_observation_id = o.observation_id \n";
            selectString += "\t\tLEFT JOIN measurement m on m.measurement_id = ohm.measurement_measurement_id \n";
            selectString += "\twhere \n";
            selectString += "\t\tm.name like 'Status' and \n";
            selectString += "\t\to.equipment = e.equipment_id and \n";
            selectString += "\t\to.project = '" + projectId + "' and \n";
            selectString += "\t\to.deleted = 0 \n";
            selectString += "\tORDER BY o.created_date DESC LIMIT 1) as measurementId, \n";

            selectString += "\t(SELECT m.name from observation o \n";
            selectString += "\t\tLEFT JOIN observation_has_measurement ohm on ohm.observation_observation_id = o.observation_id \n";
            selectString += "\t\tLEFT JOIN measurement m on m.measurement_id = ohm.measurement_measurement_id \n";
            selectString += "\twhere \n";
            selectString += "\t\tm.name like 'Status' and \n";
            selectString += "\t\to.equipment = e.equipment_id and \n";
            selectString += "\t\to.project = '" + projectId + "' and \n";
            selectString += "\t\to.deleted = 0 \n";
            selectString += "\tORDER BY o.created_date DESC LIMIT 1) as measurementName, \n";

            selectString += "\t(SELECT m.value_object from observation o \n";
            selectString += "\t\tLEFT JOIN observation_has_measurement ohm on ohm.observation_observation_id = o.observation_id \n";
            selectString += "\t\tLEFT JOIN measurement m on m.measurement_id = ohm.measurement_measurement_id \n";
            selectString += "\twhere \n";
            selectString += "\t\tm.name like 'Status' and \n";
            selectString += "\t\to.equipment = e.equipment_id and \n";
            selectString += "\t\to.project = '" + projectId + "' and \n";
            selectString += "\t\to.deleted = 0 \n";
            selectString += "\tORDER BY o.created_date DESC LIMIT 1) as measurementValue, \n";

            selectString += "\t(SELECT m.value_default from observation o \n";
            selectString += "\t\tLEFT JOIN observation_has_measurement ohm on ohm.observation_observation_id = o.observation_id \n";
            selectString += "\t\tLEFT JOIN measurement m on m.measurement_id = ohm.measurement_measurement_id \n";
            selectString += "\twhere \n";
            selectString += "\t\tm.name like 'Status' and \n";
            selectString += "\t\to.equipment = e.equipment_id and \n";
            selectString += "\t\to.project = '" + projectId + "' and \n";
            selectString += "\t\to.deleted = 0 \n";
            selectString += "\tORDER BY o.created_date DESC LIMIT 1) as measurementChoices, \n";
            selectString += "\t(SELECT max(created_date) from observation o where o.equipment = e.equipment_id and o.project != '" + projectId + "' and o.deleted = 0 ORDER BY o.created_date DESC) as previousObservationDate,\n" +
                            "\t(SELECT max(created_date) from observation o where o.equipment = e.equipment_id and o.project = '" + projectId + "' and o.deleted = 0) as currentObservationDate,\n" +
                            "\t(SELECT count(*) from observation o where o.equipment = e.equipment_id and o.project = '" + projectId + "' and o.deleted = " + deleteParameter + ") as observationCount,\n" +
                            "\t(SELECT count(*) from observation o where o.equipment = e.equipment_id and o.project = '" + projectId + "' and o.deleted = " + deleteParameter + " and o.deviation > 0) as deviationCount,\n" + "\te.equipment_id as equipmentResultId,\n";
            selectString += "\t(SELECT p.disipline from project p \n" +
                    "\twhere \n" +
                    "\t\tp.project_id = '" + projectId + "' and \n" +
                    "\t\tp.deleted = 0 \n" +
                    "    LIMIT 1) as disiplineId, \n";
          }
        selectString += "\te.equipment_type as equipmentTypeId,\n" +
               "\tconcat(COALESCE(concat(l5.name,': '),''), COALESCE(concat(l4.name,': '),''), COALESCE(concat(l3.name,': '),''), COALESCE(concat(l2.name,': '),''),COALESCE(l1.name,'')) as 'locationString',\n" +
                "\te.location as locationId,\n" +
                "\tconcat(CASE WHEN e.name != '' THEN e.name ELSE et.name END, concat(' ',e.tag_id))as nameString, \n" +
                "\t(SELECT count(*) from equipment_type_has_check_list ethcl where e.equipment_type = ethcl.equipment_type_equipment_type_id) as checkListCount\n" +
                "FROM equipment e \n";
        selectString += "\tLEFT JOIN equipment_type et on et.equipment_type_id = e.equipment_type \n";
        if(parameters.showRelevant) {
            selectString += "LEFT JOIN disipline_has_equipment_type dhet on dhet.equipment_type_equipment_type_id = et.equipment_type_id \n";
        }
        selectString += "\tLEFT JOIN location l1 on l1.location_id = e.location \n";
        selectString += "\tLEFT JOIN location l2 on l1.parent = l2.location_id \n";
        selectString += "\tLEFT JOIN location l3 on l2.parent = l3.location_id \n";
        selectString += "\tLEFT JOIN location l4 on l3.parent = l4.location_id \n";
        selectString += "\tLEFT JOIN location l5 on l4.parent = l5.location_id \n";
        whereString += "WHERE e.deleted = " + deleteParameter + " AND e.asset = " + assetId + "\n";
        if (parameters.projectId != null) {
            if (parameters.showOnlyControlled) {
                whereString += "\tAND (SELECT count(*) from observation o where o.equipment = e.equipment_id and o.project = '" + parameters.projectId + "' and o.deleted = " + deleteParameter + ") > 0 \n";
            } else if (parameters.hideControlled) {
                whereString += "\tAND (SELECT count(*) from observation o where o.equipment = e.equipment_id and o.project = '" + parameters.projectId + "' and o.deleted = " + deleteParameter + ") = 0 \n";
            }
            /*if (!isCounting) {
                //whereString += "\tAND (o.project = '" + projectId + "' or o.project is null) ";
                whereString += "\tAND (m.name like '" + statusString + "' or m.name is null) ";
            }*/
            if(parameters.showRelevant) {
                whereString += "\tAND dhet.disipline_disipline_id = '" +  disiplineId + "' ";
            }
            if (parameters.showNeverControlled) {
                whereString += "\tAND(SELECT count(o.created_date)from observation o where o.equipment = e.equipment_id and o.deleted = 0 ORDER BY o.created_date DESC) = 0\n";
            } else if (parameters.fromDate != null && parameters.toDate != null) {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                String strFromDate = dateFormatter.format(parameters.fromDate);
                String strToDate = dateFormatter.format(parameters.toDate);

                whereString += "\tAND(SELECT max(created_date)from observation o where o.equipment = e.equipment_id and o.project != '" + projectId + "' and o.deleted = 0 ORDER BY o.created_date DESC) > '" + strFromDate + "' \n";
                whereString += "\tAND(SELECT max(created_date)from observation o where o.equipment = e.equipment_id and o.project != '" + projectId + "' and o.deleted = 0 ORDER BY o.created_date DESC) < '" + strToDate + "' \n";
            }
        }

        String orderByString = "";
        if(parameters.sortMode.sortMode == PopupSortMode.SortMode.EQUIPMENT_TYPE) {
            orderByString = "\nORDER by nameString";
        } else if(parameters.sortMode.sortMode == PopupSortMode.SortMode.EQUIPMENT_LOCATION) {
            orderByString = "\nORDER by locationString";
        } else if(parameters.sortMode.sortMode == PopupSortMode.SortMode.EQUIPMENT_TAG) {
            orderByString = "\nORDER by e.tag_id";
        }

        //String limitString = "";
        int batchSize = parameters.batchSize;
        if (batchSize == 0) {
            batchSize = 2000;
        }
        String limitString = "";
        if (!isCounting) {
            limitString = " LIMIT " + parameters.batchOffset + "," + batchSize + " ";
        }

        if (parameters.searchString != null) {
            whereString += """
                          AND CONCAT(
                            COALESCE(l4.name, ''),
                            COALESCE(l3.name, ''),
                            COALESCE(l2.name, ''),
                            COALESCE(l1.name, ''),
                            COALESCE(et.name, ''),
                            COALESCE(e.tag_id, ''),
                            COALESCE(e.name, '')
                            )\s
                    """;
            whereString += "like '%" + parameters.searchString + "%' ";
            limitString = " LIMIT 0,20";
        }
        sqlString = selectString + whereString + orderByString + limitString;
        return sqlString;
    }

    @PUT
    @Path("countEquipments/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public Integer countEquipments(EquipmentRequestParameters parameters) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        String sqlQuery = createSqlString(parameters, true);

        Long equipmentCounter = 0L;
        List<Object[]> results = em.createNativeQuery(sqlQuery)
                .getResultList();
        for (int i = 0; i < results.size(); i++) {
            equipmentCounter = (Long) results.get(i)[0];
        }
//        Query queryCounter = em.createNativeQuery(sqlQuery);
//        Number counterUnassigned = (Number) queryCounter.getSingleResult();
//        Integer integerCounter = Integer.parseInt(counterUnassigned.toString());
//
//        em.close();
//        return integerCounter;

        return equipmentCounter.intValue();
    }


    @PUT
    @Path("loadEquipments/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Equipment> loadEquipments(EquipmentRequestParameters parameters) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        String queryString = createSqlString(parameters, false);

        List<Equipment> resultList = new ArrayList<>();
        List<Object[]> results = em.createNativeQuery(queryString, "EquipmentResultMapping")
                .getResultList();
        String previousEquipment = "-XXX-";
        Equipment equipment = null;
        for (int i = 0; i < results.size(); i++) {
            if (equipment == null) {
                equipment = ((Equipment) results.get(i)[0]);
                resultList.add(equipment);
                previousEquipment = equipment.getEquipmentId();
            }
            if (!previousEquipment.equalsIgnoreCase(((Equipment) results.get(i)[0]).getEquipmentId())) {
                equipment = ((Equipment) results.get(i)[0]);
                resultList.add(equipment);
                previousEquipment = equipment.getEquipmentId();
            }
            if (results.get(i)[1] != null && results.get(i)[3] != null) { // measurementObservation
                Observation observation = new Observation();
                observation.setObservationId((String) results.get(i)[1]);
                Measurement measurement = new Measurement();
                measurement.setValueType("CHOICE");
                measurement.setMeasurementId((String) results.get(i)[3]);
                measurement.setName((String) results.get(i)[4]);
                measurement.setStringValue((String) results.get(i)[5]);
                measurement.setValueDefault((String) results.get(i)[6]);
                if(!observation.getMeasurementList().contains(measurement)) {
                    observation.getMeasurementList().add(measurement);
                }
                if(!equipment.getMeasurementObservations().contains(observation)) {
                    equipment.getMeasurementObservations().add(observation);
                }
            }
            if (results.get(i)[7] != null) {
                equipment.setPreviousObservationDate((Date) results.get(i)[7]);
            }
            if (results.get(i)[8] != null) {
                equipment.setCurrentObservationDate((Date) results.get(i)[8]);
            }
            Long deviationCounter = (Long) results.get(i)[9];
            Long observationCounter = (Long) results.get(i)[10];
            if (deviationCounter != null) {
                equipment.setDeviationCount(deviationCounter.intValue());
            }
            if (observationCounter != null) {
                equipment.setObservationCount(observationCounter.intValue());
            }
            equipment.setLocationString((String) results.get(i)[12]);
            equipment.setLocationId((String) results.get(i)[13]);
            equipment.setEquipmentTypeId((String) results.get(i)[14]);
            equipment.setNameString((String) results.get(i)[15]);
            Long checkListCount = (Long) results.get(i)[16];
            equipment.setCheckListCount(checkListCount.intValue());

        }
        // Optimalisering av 850 kontrollpunkter tar nesten 2 minutter.
        // Finn en bedre løsning
//            for (Equipment equipment : resultList) {
//                optimizeEquipment(equipment, parameters);
//            }
        if (parameters.searchString == null) {
            if (parameters.showNeverControlled) {
                List<Equipment> neverControlled = resultList.stream().filter(r ->
                                r.getCurrentObservationDate() == null && r.getObservationCount() == 0)
                        .collect(Collectors.toList());
                resultList = neverControlled;
            }
//            else if (parameters.fromDate != null && parameters.toDate != null) {
//                List<Equipment> intervalControlled = resultList.stream().filter(r -> {
//                    if (!r.getOlderObservations().isEmpty()) {
//                        return r.getOlderObservations().get(0).getCreatedDate().compareTo(parameters.fromDate) > 0
//                                && r.getOlderObservations().get(0).getCreatedDate().compareTo(parameters.toDate) <= 0;
//                    } else {
//                        return false;
//                    }
//                }).collect(Collectors.toList());
//                resultList = intervalControlled;
//            }
//            if (parameters.showOnlyControlled) {
//                List<Equipment> filtered = resultList.stream().filter(r -> !r.getObservationList().isEmpty()).collect(Collectors.toList());
//                resultList = filtered;
//            } else if (parameters.hideControlled) {
//                List<Equipment> filtered = resultList.stream().filter(r -> r.getObservationList().isEmpty()).collect(Collectors.toList());
//                resultList = filtered;
//            }
//            if (parameters.showRelevant && parameters.projectId != null) {
//                ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
//                Project project = projectFacadeREST.findOptimized(parameters.projectId);
//                List<EquipmentType> relevantEquipmentTypes = project.getDisipline().getEquipmentTypeList();
//                List<Equipment> relevantEquipments = resultList.stream().filter(r -> {
//                    if (r.getEquipmentType() != null) {
//                        return relevantEquipmentTypes.contains(r.getEquipmentType());
//                    } else {
//                        return false;
//                    }
//                }).collect(Collectors.toList());
//                resultList = relevantEquipments;
//            }
        }

        for (Equipment resultEquipment : resultList) {
            resultEquipment.setObservationList(new ArrayList<>());
            resultEquipment.setMeasurementList(new ArrayList<>());
            resultEquipment.setEquipmentType(null);
            resultEquipment.setLocation(null);
            resultEquipment.setAsset(null);
        }
        return resultList;
    }

    private void optimizeEquipment(Equipment equipment, EquipmentRequestParameters parameters) {
        if (equipment.getEquipmentType() != null) {
            equipment.setEquipmentTypeId(equipment.getEquipmentType().getEquipmentTypeId());
            equipment.setCheckListCount(equipment.getEquipmentType().getCheckListList().size());
        }
        if (equipment.getLocation() != null) {
            equipment.setLocationString(equipment.getLocation().getFullName());
            equipment.setLocationId(equipment.getLocation().getLocationId());
        }
        List<Observation> observations = equipment.getObservationList().stream().filter(r ->
                        r.isDeleted() == false && r.getProjectId().equalsIgnoreCase(parameters.projectId))
                .collect(Collectors.toList());
        List<Observation> deviations = observations.stream().filter(r ->
                        r.getDeviation() > 0)
                .collect(Collectors.toList());

        List<Observation> olderObservations = equipment.getObservationList().stream().filter(r ->
                        r.isDeleted() == false && !r.getProjectId().equalsIgnoreCase(parameters.projectId))
                .collect(Collectors.toList());
        if (!olderObservations.isEmpty()) {
            Collections.sort(olderObservations, (Observation o1, Observation o2) -> o2.getCreatedDate().compareTo(o1.getCreatedDate()));
            for (Observation olderObservation : olderObservations) {
                optimizeObservation(olderObservation);
                olderObservation.setEquipment(null);
            }
            equipment.setOlderObservations(olderObservations);
            equipment.setPreviousObservationDate(olderObservations.get(0).getCreatedDate());
        }
        equipment.setObservationCount(observations.size());
        equipment.setDeviationCount(deviations.size());
        equipment.setNameString(equipment.getFullName());
        equipment.setMeasurementList(new ArrayList<>());
        for (Observation obs : observations) {
            optimizeObservation(obs);
            obs.setEquipment(null);
        }
        equipment.setMeasurementObservations(observations);
    }

    private void optimizeObservation(Observation observation) {
        if (observation.getEquipment() != null) {
            observation.setEquipmentId(observation.getEquipment().getEquipmentId());
            observation.setEquipmentString(observation.getEquipment().getFullName());
            if (observation.getEquipment().getTagId() != null) {
                observation.setEquipmentTagId(observation.getEquipment().getTagId());
            }
            observation.setEquipment(null);
        }
        if (observation.getLocation() != null) {
            observation.setLocationId(observation.getLocation().getLocationId());
            observation.setLocationString(observation.getLocation().getFullName());
            observation.setLocation(null);
        }
        if (observation.getProject() != null) {
            observation.setProjectNumber(observation.getProject().getProjectNumber());
        }
        if (observation.getQuickChoiceItem() != null) {
            observation.setQuickChoiceItem(null);
        }
        if (!observation.getMeasurementList().isEmpty()) {
            for (Measurement measurement : observation.getMeasurementList()) {
                measurement.getObservationList().clear();
            }
            List<Measurement> statusMeasurements = observation.getMeasurementList().stream().filter(r -> r.getName().equalsIgnoreCase("Status")).collect(Collectors.toList());
            observation.getMeasurementList().clear();
            if (!statusMeasurements.isEmpty()) {
                observation.getMeasurementList().addAll(statusMeasurements);
                observation.setMeasurementStatusString(statusMeasurements.get(0).getStringValue());
            }
        }
        observation.setProject(null);
        observation.setOldProject(null);
        observation.setQuickChoiceItem(null);
        observation.setEquipment(null);
        observation.setLocation(null);
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Equipment find(@PathParam("id") String id) {
        return findNative(id);
    }

    @GET
    @Path("loadWithProject/{id}/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Equipment loadWithProject(@PathParam("id") String id, @PathParam("projectId") String projectId) {
        Equipment equipment = findNative(id);
        if (equipment != null) {
            List<Observation> projectObservations = equipment.getObservationList().stream().filter(r ->
                    !r.isDeleted() && r.getProject().getProjectId().equalsIgnoreCase(projectId)).toList();
            List<Observation> deviations = projectObservations.stream().filter(r ->
                    r.getDeviation() > 0).toList();
            equipment.setObservationCount(projectObservations.size());
            equipment.setDeviationCount(deviations.size());
            for (Observation observation : projectObservations) {
                observation.setProject(null);
                observation.setEquipment(null);
                observation.setOldProject(null);
                observation.setLocation(null);
                observation.setQuickChoiceItem(null);
                observation.getImageList().clear();
                observation.getAnswerValueList().clear();
            }
            equipment.setMeasurementObservations(projectObservations);
            equipment.setNameString(equipment.getFullName());
            if (equipment.getLocation() != null) {
                equipment.setLocationString(equipment.getLocation().getFullName());
                equipment.setLocationId(equipment.getLocation().getLocationId());
            }
            return equipment;
        } else {
            return null;
        }
    }

    public Equipment findNative(String id) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Equipment> resultList = (List<Equipment>) em.createNativeQuery("SELECT DISTINCT "
                                + "* FROM equipment e\n"
                                + "    left join equipment_has_measurement ehm on e.equipment_id = ehm.equipment_equipment_id\n"
                                + "    left join measurement m on ehm.measurement_measurement_id = m.measurement_id\n"
                                + "WHERE e.equipment_id = ?1",
                        Equipment.class)
                .setParameter(1, id)
                .getResultList();
        em.close();
        if (!resultList.isEmpty()) {
            Equipment equipment = resultList.get(0);
            if(equipment.getLocation() != null) {
                equipment.setLocationString(equipment.getLocation().getFullName());
                equipment.setLocationId(equipment.getLocation().getLocationId());
            }
            return equipment;
        }
        return null;
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<Equipment> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Equipment> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("countByEquipmentTypeAndAsset/{equipmentTypeId}/{assetId}")
    @Produces({MediaType.APPLICATION_JSON})
    //@Produces(MediaType.TEXT_PLAIN)
    public String countByEquipmentTypeAndAsset(@PathParam("equipmentTypeId") String equipmentTypeId, @PathParam("assetId") String assetId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        Query query = em.createNativeQuery(
                        "SELECT count(e.equipment_id) " +
                                "FROM " +
                                "    equipment e " +
                                "JOIN asset a ON e.asset = a.asset_id " +
                                "WHERE\n" +
                                "e.equipment_type = ?1 " +
                                "AND a.asset_id = ?2 " +
                                "AND a.deleted = 0")
                .setParameter(1, equipmentTypeId)
                .setParameter(2, assetId);

        Number counter = (Number) query.getSingleResult();
        return counter.toString();
    }

    @GET
    @Path("loadByEquipmentType/{equipmentTypeId}/{assetId}")
    @Produces({MediaType.APPLICATION_JSON})
    //@Produces(MediaType.TEXT_PLAIN)
    public List<Equipment> loadByEquipmentType(@PathParam("equipmentTypeId") String equipmentTypeId, @PathParam("assetId") String assetId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Equipment> resultList = (List<Equipment>) em.createNativeQuery(
                        "SELECT * " +
                                "FROM " +
                                "    equipment e " +
                                "JOIN asset a ON e.asset = a.asset_id " +
                                "WHERE\n" +
                                "e.equipment_type = ?1 " +
                                "AND a.asset_id = ?2 " +
                                "AND a.deleted = 0", Equipment.class)
                .setParameter(1, equipmentTypeId)
                .setParameter(2, assetId)
                .getResultList();

        return resultList;
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

    @PUT
    @Path("findMissing/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> findMissing(List<String> equipmentIds) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<String> missingIds = new ArrayList<>();
        for (String equipmentId : equipmentIds) {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM equipment e " +
                            " WHERE e.equipment_id = ?1")
                    .setParameter(1, equipmentId);

            Number counter = (Number) query.getSingleResult();
            int intCounter = Integer.parseInt(counter.toString());

            if (intCounter == 0) {
                missingIds.add(equipmentId);
            }
        }
        em.close();

        return missingIds;
    }

}

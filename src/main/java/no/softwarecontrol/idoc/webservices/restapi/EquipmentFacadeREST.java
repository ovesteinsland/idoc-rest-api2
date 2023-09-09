/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityhelper.WalletProject;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.data.requestparams.EquipmentRequestParameters;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

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

            //entity.getMeasurementList().clear();
            for (Equipment child : entity.getEquipmentList()) {
                child.setParent(entity);
            }
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

            setAsset(entity, assetId);
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
                if (!location.getEquipmentList().contains(entity)) {
                    location.getEquipmentList().add(entity);
                }
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
            System.out.println("Exception while inserting into equipment_has_measurement: " + exp.getMessage());
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Equipment entity) {
        Equipment equipment = find(entity.getEquipmentId());
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
            }
            setEquipmentType(entity);
            equipment.setEquipmentType(entity.getEquipmentType());
            equipment.setName(entity.getName());
            equipment.setTagId(entity.getTagId());
            equipment.setDeleted(entity.isDeleted());
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
        String whereString = "WHERE ";
        String deleteParameter = "0";
        if (parameters.showDeleted) {
            deleteParameter = "1";
        }
        String assetId = "'" + parameters.entityIds.get(0) + "'";
        if (isCounting) {
            selectString = "SELECT count(distinct e.equipment_id) FROM equipment e\n";
        } else {
            selectString = "SELECT e.* \n" +
                    "FROM equipment e\n";
        }
        whereString += "e.deleted = " + deleteParameter + " AND e.asset = " + assetId;

        String limitString = "";
        //String limitString = " LIMIT " + parameters.batchOffset + "," + parameters.batchSize + " ";
        sqlString = selectString + whereString + limitString;
        return sqlString;
    }

    private String createSqlSearchString(EquipmentRequestParameters parameters) {

        String query = parameters.searchString;
        String projectId = parameters.projectId;
        String assetId = parameters.entityIds.get(0);
        String deleted = "0";
        if(parameters.showDeleted) {
            deleted = "1";
        }
        if(false) {
            String observationJoin = """
                    LEFT JOIN observation o on o.equipment = e.equipment_id
                    LEFT JOIN observation_has_measurement ohm on ohm.observation_observation_id = o.observation_id
                    LEFT JOIN measurement m on m.measurement_id = ohm.measurement_measurement_id
                    LEFT JOIN project p on p.project_id = o.project
                    
                    COALESCE(m.name, ''),
                    COALESCE(m.value_object, '')
                    """;
        }
        String searchSql = """
                SELECT\s
                    *\s
                FROM equipment e
                	LEFT JOIN equipment_type et on e.equipment_type = et.equipment_type_id
                    LEFT JOIN equipment_type p1 on p1.equipment_type_id = et.parent
                	LEFT JOIN location l1 on l1.location_id = e.location
                	LEFT JOIN location l2 on l1.parent = l2.location_id
                	LEFT JOIN location l3 on l2.parent = l3.location_id
                	LEFT JOIN location l4 on l3.parent = l4.location_id
                WHERE CONCAT(
                    COALESCE(l4.name, ''),
                    COALESCE(l3.name, ''),
                    COALESCE(l2.name, ''),
                    COALESCE(l1.name, ''),
                    COALESCE(et.name, ''),
                    COALESCE(e.tag_id, ''),
                    COALESCE(e.name, '')
                    )\s
                    """;
        searchSql += "like '%" + query + "%' ";
        searchSql += "and e.asset = '" + assetId + "' ";
        //searchSql += "and (o.project = '" + projectId + "' OR p.project_id is NULL) ";
        searchSql += "and e.deleted = " + deleted + " ";
        //searchSql += "group by o.observation_id";
        return searchSql;
    }

    @PUT
    @Path("countEquipments/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public Integer countEquipments(EquipmentRequestParameters parameters) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        String sqlQuery = createSqlString(parameters, true);
        Query queryCounter = em.createNativeQuery(sqlQuery);
        Number counterUnassigned = (Number) queryCounter.getSingleResult();
        Integer integerCounter = Integer.parseInt(counterUnassigned.toString());

        em.close();
        return integerCounter;
    }

    @PUT
    @Path("loadEquipments/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Equipment> loadEquipments(EquipmentRequestParameters parameters) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        if (parameters.searchString != null) {
            String queryString = createSqlSearchString(parameters);
            List<Equipment> resultList = em.createNativeQuery(queryString, Equipment.class)
                    .getResultList();
            List<Equipment> listWithoutDuplicates = new ArrayList<>(
                    new HashSet<>(resultList));
            for (Equipment equipment : listWithoutDuplicates) {
                optimizeEquipment(equipment, parameters);
                equipment.setNameString(equipment.getFullName());
            }
            for (Equipment equipment : listWithoutDuplicates) {
                equipment.setEquipmentType(null);
                equipment.setLocation(null);
                equipment.setAsset(null);
            }
            return listWithoutDuplicates;
        } else {
            String queryString = createSqlString(parameters, false);
            List<Equipment> resultList = em.createNativeQuery(queryString, Equipment.class)
                    .getResultList();

            for (Equipment equipment : resultList) {
                optimizeEquipment(equipment, parameters);
            }
            if (parameters.showNeverControlled) {
                List<Equipment> neverControlled = resultList.stream().filter(r ->
                                r.getLastObservationDate() == null && r.getObservationCount() == 0)
                        .collect(Collectors.toList());
                resultList = neverControlled;
            } else if (parameters.fromDate != null && parameters.toDate != null) {
                List<Equipment> intervalControlled = resultList.stream().filter(r -> {
//                    if (r.getLastObservationDate() != null) {
//                        return r.getLastObservationDate().compareTo(parameters.fromDate) > 0
//                                && r.getLastObservationDate().compareTo(parameters.toDate) <= 0;
//                    } else {
//                        return false;
//                    }
                    if(r.getEquipmentId().equalsIgnoreCase("DBC8EECD-8212-4E5A-9694-CE04C44CB9A0")) {
                        System.out.println("Skal ha en observasjon fra 2021");
                    }
                    if (!r.getOlderObservations().isEmpty()) {
                        return r.getOlderObservations().get(0).getCreatedDate().compareTo(parameters.fromDate) > 0
                                && r.getOlderObservations().get(0).getCreatedDate().compareTo(parameters.toDate) <= 0;
                    } else {
                        return false;
                    }
                }).collect(Collectors.toList());
                resultList = intervalControlled;
            }
            if (parameters.showOnlyControlled) {
                List<Equipment> filtered = resultList.stream().filter(r -> !r.getObservationList().isEmpty()).collect(Collectors.toList());
                resultList = filtered;
            } else if (parameters.hideControlled) {
                List<Equipment> filtered = resultList.stream().filter(r -> r.getObservationList().isEmpty()).collect(Collectors.toList());
                resultList = filtered;
            }
            if (parameters.showRelevant && parameters.projectId != null) {
                ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
                Project project = projectFacadeREST.findOptimized(parameters.projectId);
                List<EquipmentType> relevantEquipmentTypes = project.getDisipline().getEquipmentTypeList();
                List<Equipment> relevantEquipments = resultList.stream().filter(r -> {
                    if (r.getEquipmentType() != null) {
                        return relevantEquipmentTypes.contains(r.getEquipmentType());
                    } else {
                        return false;
                    }
                }).collect(Collectors.toList());
                resultList = relevantEquipments;
            }

            for (Equipment equipment : resultList) {
                equipment.setEquipmentType(null);
                equipment.setLocation(null);
                equipment.setAsset(null);
            }
            return resultList;
        }
    }

    private void optimizeEquipment(Equipment equipment, EquipmentRequestParameters parameters) {
        if (equipment.getEquipmentType() != null) {
            equipment.setEquipmentTypeId(equipment.getEquipmentType().getEquipmentTypeId());
            equipment.setHasCheckLists(!equipment.getEquipmentType().getCheckListList().isEmpty());
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

            for(Observation olderObservation: olderObservations) {
                olderObservation.setEquipment(null);
            }
            //equipment.setOlderObservations(olderObservations);
            equipment.setLastObservationDate(olderObservations.get(0).getCreatedDate());

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
            List<Measurement> statusMeasurements = observation.getMeasurementList().stream().filter(r -> r.getName().equalsIgnoreCase("Status")).collect(Collectors.toList());
            observation.getMeasurementList().clear();
            if (!statusMeasurements.isEmpty()) {
                observation.getMeasurementList().addAll(statusMeasurements);
                observation.setMeasurementStatusString(statusMeasurements.get(0).getStringValue());
            }
        }
        observation.setProject(null);
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
        if(equipment != null) {
            List<Observation> projectObservations = equipment.getObservationList().stream().filter(r ->
                    !r.isDeleted() && r.getProject().getProjectId().equalsIgnoreCase(projectId)).toList();
            List<Observation> deviations = projectObservations.stream().filter(r ->
                    r.getDeviation()>0).toList();
            equipment.setObservationCount(projectObservations.size());
            equipment.setDeviationCount(deviations.size());
            //equipment.setMeasurementObservations(projectObservations);
            equipment.setNameString(equipment.getFullName());
            if(equipment.getLocation() != null) {
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
        List<Equipment> resultList = (List<Equipment>) em.createNativeQuery("SELECT "
                                + "* FROM equipment o\n"
                                + "WHERE o.equipment_id = ?1",
                        Equipment.class)
                .setParameter(1, id)
                .getResultList();
        em.close();
        if (!resultList.isEmpty()) {
            Equipment equipment = resultList.get(0);
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

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
import no.softwarecontrol.idoc.data.entityhelper.DeviationCounter;
import no.softwarecontrol.idoc.data.entityhelper.ObservationCounter;
import no.softwarecontrol.idoc.data.entityhelper.TgCounter;
import no.softwarecontrol.idoc.data.entityhelper.UserRoleParameter;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.data.requestparams.ObservationRequestParameters;
import no.softwarecontrol.idoc.restclient.ObservationClient;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.observation")
@RolesAllowed({"ApplicationRole"})
public class ObservationFacadeREST extends AbstractFacade<Observation> {

    public ObservationFacadeREST() {
        super(Observation.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return "Observation.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Observation entity) {
        entity.setCreatedDate(new Date());
        super.create(entity);
    }

    @Deprecated
    @POST
    @Path("createWithProject/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithProject(@PathParam("projectId") String projectId, Observation entity) {
        System.out.println("ObservationFacadeRest.createWithProject: Oppretter observasjon: " + entity.getObservationId());
        ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
        Observation existingObservation = this.find(entity.getObservationId());

        if (existingObservation == null) {
            entity.setCreatedDate(new Date());

            Project project = projectFacadeREST.find(projectId);
            if (project != null) {
                if (!project.getObservationList().contains(entity)) {
                    project.getObservationList().add(entity);
                }
                entity.setProject(project);

                String equipmentId = entity.getEquipmentId();
                EquipmentFacadeREST equipmentFacadeREST = new EquipmentFacadeREST();
                if (equipmentId != null) {
                    Equipment equipment = equipmentFacadeREST.find(equipmentId);
                    if (equipment != null) {
                        entity.setEquipment(equipment);
                    } else {
                        System.out.println("ObservationFacadeRest.createWithProject: Her skjer det ein feil med equipment");
                    }
                }
                projectFacadeREST.edit(project);
            }
        }
    }

    @POST
    @Path("createSynchronizedList/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createSynchronizedList(@PathParam("projectId") String projectId, List<Observation> entityList) {
        for (Observation observation : entityList) {

        }
    }

    @POST
    @Path("createWithProject2/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithProject2(@PathParam("projectId") String projectId, Observation entity) throws Exception {
        Observation existingObservation = this.find(entity.getObservationId());
        MeasurementFacadeREST measurementFacadeREST = new MeasurementFacadeREST();
        List<Measurement> measurements = new ArrayList<>(entity.getMeasurementList());
        if (existingObservation == null) {
            entity.setCreatedDate(new Date());

            String equipmentId = entity.getEquipmentId();
            if (entity.getEquipment() != null) {
                equipmentId = entity.getEquipment().getEquipmentId();
            }
            EquipmentFacadeREST equipmentFacadeREST = new EquipmentFacadeREST();
            if (equipmentId != null) {
                Equipment equipment = equipmentFacadeREST.findNative(equipmentId);
                if (equipment != null) {
                    entity.setEquipment(equipment);
                } else {
                    throw new Exception("Equipment not yet synchronized - Throw ERROR");
                }
            }

            // Check if location is created
            if (entity.getLocation() != null) {
                LocationFacadeREST locationFacadeREST = new LocationFacadeREST();
                Location existingLocation = locationFacadeREST.find(entity.getLocation().getLocationId());
                if (existingLocation != null) {
                    entity.setLocation(existingLocation);
                } else {
                    throw new Exception("Location not yet synchronized - Throw ERROR");
                }
            }

            if (entity.getQuickChoiceItem() != null) {
                QuickChoiceItemFacadeREST quickChoiceItemFacadeREST = new QuickChoiceItemFacadeREST();
                QuickChoiceItem quickChoiceItem = quickChoiceItemFacadeREST.find(entity.getQuickChoiceItem().getQuickChoiceItemId());
                if (quickChoiceItem != null) {
                    entity.setQuickChoiceItem(quickChoiceItem);
                    entity.setDeviation(quickChoiceItem.getDeviationGrade());
                }
            }

            ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
            Project project = projectFacadeREST.find(projectId);
            entity.setProject(project);
            //int observationCount = findProjectObservationsNative(project.getProjectId()).size();
            //entity.setObservationNo(observationCount + 1);
//            if (!project.getObservationList().contains(entity)) {
//                project.getObservationList().add(entity);
//            }

            entity.getMeasurementList().clear();
            if (entity.getProject() != null) {
                super.create(entity);

                for (Measurement measurement : measurements) {
                    // Check if measurement already exists
                    Measurement existing = measurementFacadeREST.find(measurement.getMeasurementId());
                    if (existing == null) {
                        entity.getMeasurementList().add(measurement);
                        edit(entity);
                    } else {
                        linkMeasurement(measurement.getMeasurementId(), entity);
                    }
                }

            }
        } else {
            System.out.println("Observasjonen er opprettet fra før...?");
            String locationId = "-1";
            if (entity.getLocation() != null) {
                locationId = entity.getLocation().getLocationId();
            }
            String quickChoiceItemId = "-1";
            if (entity.getQuickChoiceItem() != null) {
                quickChoiceItemId = entity.getQuickChoiceItem().getQuickChoiceItemId();
            }
            String equipmentId = "-1";
            if (entity.getEquipment() != null) {
                equipmentId = entity.getEquipment().getEquipmentId();
            }
            edit(entity.getObservationId(), locationId, quickChoiceItemId, equipmentId, entity);
        }
    }

//    private void createNative(Observation observation) {
//        EntityManager em = LocalEntityManagerFactory.createEntityManager();
//        EntityTransaction tx = em.getTransaction();
//        try {
//            Query query = em.createNativeQuery("SELECT COUNT(*) FROM company_has_project \n " +
//                    " WHERE company_company_id = ?1 AND project_project_id = ?2")
//                    .setParameter(1, companyId)
//                    .setParameter(2, entity.getProjectId());
//
//            Number counter = (Number) query.getSingleResult();
//            if(counter.intValue() == 0) {
//                tx.begin();
//                final int i = em.createNativeQuery(
//                        "INSERT INTO observation (" +
//                                "observation_id, " +    // 1
//                                "project," +            // 2
//                                "description," +        // 3
//                                "equipment," +          // 4
//                                "created_date," +       // 5
//                                "modified_date," +      // 6
//                                "created_user," +       // 7
//                                "modified_user," +
//                                "deviation," +
//                                "quick_choice_item," +
//                                "action," +
//                                "location)\n" +
//                                "VALUES (?1, ?2);"
//                ).setParameter(1, companyId)
//                        .setParameter(2, entity.getProjectId())
//                        .executeUpdate();
//                tx.commit();
//            } else {
//                //System.out.println("No problem: company_has_project already exists");
//            }
//        } catch (Exception exp) {
//            tx.rollback();
//            System.out.println("Exception while inserting into company_has_project: " + exp.getMessage());
//        } finally {
//            em.close();
//        }
//    }

    @PUT
    @Deprecated
    @Path("editLocation/{locationId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editLocation(@PathParam("locationId") String locationId,
                             Observation entity) {

        LocationFacadeREST locationFacadeREST = new LocationFacadeREST();
        Observation observation = this.findNative(entity.getObservationId());
        if (observation == null) {
            //System.out.println("observation id missing = " + entity.getObservationId());
        } else {
            Location location = locationFacadeREST.find(locationId);
            if (location != null) {
                observation.setLocation(location);
                if (entity.getModifiedDate() != null) {
                    observation.setModifiedDate(entity.getModifiedDate());
                } else {
                    observation.setModifiedDate(new Date());
                }
                if (!location.getObservationList().contains(observation)) {
                    location.getObservationList().add(observation);
                }
                super.edit(observation);
                System.out.println("Observation.editLocation = SAVED");
            }
        }
    }

    @PUT
    @Path("editAndroid/{id}/{locationId}/{quickChoiceItemId}/{equipmentId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editAndroid(@PathParam("id") String id,
                            @PathParam("locationId") String locationId,
                            @PathParam("quickChoiceItemId") String quickChoiceItemId,
                            @PathParam("equipmentId") String equipmentId,
                            Observation entity) {
        LocationFacadeREST locationFacadeREST = new LocationFacadeREST();
        System.out.println("Lagresr observasjon ID: " + entity.getObservationId());
        QuickChoiceItemFacadeREST quickChoiceItemFacadeREST = new QuickChoiceItemFacadeREST();
        EquipmentFacadeREST equipmentFacadeREST = new EquipmentFacadeREST();

        Observation observation = this.find(id);
        if (observation != null) {
            observation.setDescription(entity.getDescription());
            observation.setDeviation(entity.getDeviation());
            observation.setTitle(entity.getTitle());
            observation.setAction(entity.getAction());
            observation.setImprovement(entity.getImprovement());
            observation.setImprovementDeadline(entity.getImprovementDeadline());
            observation.setDeleted(entity.isDeleted());
            observation.setModifiedUser(entity.getModifiedUser());
            observation.setModifiedDate(new Date());
            observation.setInfrared(entity.isInfrared());
            observation.setDeviationReason(entity.getDeviationReason());
            observation.setObservationNo(entity.getObservationNo());
            observation.setObservationType(entity.getObservationType());
            observation.setObservationState(entity.getObservationState());
            observation.setQuickChoiceItem(entity.getQuickChoiceItem());

            observation.setPerformingUserId(entity.getPerformingUserId());
            observation.setPerformingDate(entity.getPerformingDate());
            observation.setMeasurementList(entity.getMeasurementList());

            if (entity.getModifiedDate() != null) {
                observation.setModifiedDate(entity.getModifiedDate());
            } else {
                observation.setModifiedDate(new Date());
            }
            if (!quickChoiceItemId.isEmpty()) {
                QuickChoiceItem quickChoiceItem = quickChoiceItemFacadeREST.find(quickChoiceItemId);
                if (quickChoiceItem != null) {
                    observation.setQuickChoiceItem(quickChoiceItem);
                }
            }
            if (!equipmentId.isEmpty()) {
                Equipment equipment = equipmentFacadeREST.find(equipmentId);
                if (equipment != null) {
                    observation.setEquipment(equipment);
                } else {
                    if (entity.getEquipment() == null) {
                        //observation.setEquipment(null);
                    }
                }
            }
            if (!locationId.isEmpty()) {
                Location location = locationFacadeREST.find(locationId);
                if (location != null) {
                    observation.setLocation(location);
                    if (!location.getObservationList().contains(observation)) {
                        location.getObservationList().add(observation);
                    }
                } else {

                }
            }
            super.edit(observation);

        } else {
            try {
                throw new Exception("Merkelig fenomen... Denne observasjonen eksisterer ikke");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //    @PUT
//    @Path("linkMeasurement/{companyId}")
//    @Consumes({MediaType.APPLICATION_JSON})
    public void linkMeasurement(String measurementId, Observation entity) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM observation_has_measurement \n " +
                            " WHERE observation_observation_id = ?1 AND measurement_measurement_id = ?2")
                    .setParameter(1, entity.getObservationId())
                    .setParameter(2, measurementId);

            Number counter = (Number) query.getSingleResult();
            if (counter.intValue() == 0) {
                tx.begin();
                final int i = em.createNativeQuery(
                                "INSERT INTO observation_has_measurement (observation_observation_id, measurement_measurement_id)\n" +
                                        "VALUES (?, ?);"
                        ).setParameter(1, entity.getObservationId())
                        .setParameter(2, measurementId)
                        .executeUpdate();
                tx.commit();
            } else {
                //System.out.println("No problem: observation_has_measurement already exists");
            }
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into observation_has_measurement: " + exp.getMessage());
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("edit/{id}/{locationId}/{quickChoiceItemId}/{equipmentId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id,
                     @PathParam("locationId") String locationId,
                     @PathParam("quickChoiceItemId") String quickChoiceItemId,
                     @PathParam("equipmentId") String equipmentId,
                     Observation entity) {
        DateTime start = new DateTime(new Date());
//        System.out.println("Start saving observation no " + entity.getObservationNo() + ".... ");
//        System.out.println("============================ ");

        LocationFacadeREST locationFacadeREST = new LocationFacadeREST();
        QuickChoiceItemFacadeREST quickChoiceItemFacadeREST = new QuickChoiceItemFacadeREST();
        EquipmentFacadeREST equipmentFacadeREST = new EquipmentFacadeREST();

        Observation observation = this.find(id);
        if (observation != null) {
            for (Integration integration : entity.getIntegrationList()) {
                if (!observation.getIntegrationList().contains(integration)) {
                    observation.getIntegrationList().add(integration);
                }
            }


            observation.setDescription(entity.getDescription());
            observation.setDeviation(entity.getDeviation());
            observation.setTitle(entity.getTitle());
            observation.setAction(entity.getAction());
            observation.setImprovement(entity.getImprovement());
            observation.setImprovementDeadline(entity.getImprovementDeadline());
            observation.setDeleted(entity.isDeleted());
            observation.setModifiedUser(entity.getModifiedUser());
            observation.setModifiedDate(new Date());
            observation.setInfrared(entity.isInfrared());
            observation.setDeviationReason(entity.getDeviationReason());
            observation.setObservationNo(entity.getObservationNo());
            observation.setObservationType(entity.getObservationType());
            observation.setObservationState(entity.getObservationState());
            observation.setQuickChoiceItem(entity.getQuickChoiceItem());

            observation.setAlternativeLocation(entity.getAlternativeLocation());
            observation.setPerformingUserId(entity.getPerformingUserId());
            observation.setPerformingDate(entity.getPerformingDate());
            observation.setAlternativeLocation(entity.getAlternativeLocation());

            if (entity.getModifiedDate() != null) {
                observation.setModifiedDate(entity.getModifiedDate());
            } else {
                observation.setModifiedDate(new Date());
            }
            if (!quickChoiceItemId.isEmpty()) {
                QuickChoiceItem quickChoiceItem = quickChoiceItemFacadeREST.find(quickChoiceItemId);
                if (quickChoiceItem != null) {
                    observation.setQuickChoiceItem(quickChoiceItem);
                }
            }
            if (!equipmentId.isEmpty()) {
                Equipment equipment = equipmentFacadeREST.find(equipmentId);
                if (equipment != null) {
                    observation.setEquipment(equipment);
                    /*if (!equipment.getObservationList().contains(equipment)) {
                        equipment.getObservationList().add(observation);
                    }*/
                } else {
                    if (entity.getEquipment() == null) {
                        //observation.setEquipment(null);
                    }
                }
            }
            if (!locationId.isEmpty()) {
                Location location = locationFacadeREST.find(locationId);
                if (location != null) {
                    observation.setLocation(location);
                    if (!location.getObservationList().contains(observation)) {
                        location.getObservationList().add(observation);
                    }
                } else {
                    //System.out.println("Location is missing!!!" + locationId);
                }
            }
            //observation.setMeasurementList(measurements);
            try {
                super.edit(observation);

                // Add/update the measurements
                List<Measurement> measurements = new ArrayList<>(entity.getMeasurementList());
                for (Measurement measurement : measurements) {
                    MeasurementFacadeREST measurementFacadeREST = new MeasurementFacadeREST();
                    final Measurement existingMeasurement = measurementFacadeREST.find(measurement.getMeasurementId());
                    if (existingMeasurement != null) {
                        measurementFacadeREST.editMeasurementOnly(measurement.getMeasurementId(), measurement);
                        linkMeasurement(measurement.getMeasurementId(), observation);
                    } else {
                        measurementFacadeREST.createWithObservation(observation.getObservationId(), measurement);
                        //linkMeasurement(measurement.getMeasurementId(),observation);
//                    if (!observation.getMeasurementList().contains(measurement)) {
//                        observation.getMeasurementList().add(measurement);
//                    }
                    }
                }
                DateTime end = new DateTime(new Date());

                Interval interval = new Interval(start, end);
                //System.out.println(String.format("Finish saving observation in %.2f secs" ,interval.toDurationMillis()/1000.0));

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            try {
                throw new Exception("Merkelig fenomen... Denne observasjonen eksisterer ikke. Mulig at kontrollen ikke er ferdig opprettet");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This save function is used by web app. Used to avoid messing up the measurement-values entered on ios-version
     *
     * @param id
     * @param locationId
     * @param quickChoiceItemId
     * @param equipmentId
     * @param entity
     */
    @PUT
    @Path("editWeb/{id}/{locationId}/{quickChoiceItemId}/{equipmentId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editWeb(@PathParam("id") String id,
                        @PathParam("locationId") String locationId,
                        @PathParam("quickChoiceItemId") String quickChoiceItemId,
                        @PathParam("equipmentId") String equipmentId,
                        Observation entity) {
        LocationFacadeREST locationFacadeREST = new LocationFacadeREST();
        QuickChoiceItemFacadeREST quickChoiceItemFacadeREST = new QuickChoiceItemFacadeREST();
        EquipmentFacadeREST equipmentFacadeREST = new EquipmentFacadeREST();

        Observation observation = this.find(id);
        if (observation != null) {
            for (Integration integration : entity.getIntegrationList()) {
                if (!observation.getIntegrationList().contains(integration)) {
                    observation.getIntegrationList().add(integration);
                }
            }

            List<Measurement> measurements = new ArrayList<>(entity.getMeasurementList());
            for (Measurement measurement : measurements) {
                MeasurementFacadeREST measurementFacadeREST = new MeasurementFacadeREST();
                final Measurement existingMeasurement = measurementFacadeREST.find(measurement.getMeasurementId());
                if (existingMeasurement != null) {
                    measurementFacadeREST.editMeasurementOnly(measurement.getMeasurementId(), measurement);
                    if (!existingMeasurement.getObservationList().contains(observation)) {
                        existingMeasurement.getObservationList().add(observation);
                    }
                } else {
                    if (!observation.getMeasurementList().contains(measurement)) {
                        observation.getMeasurementList().add(measurement);
                    }
//                    if (!measurement.getObservationList().contains(observation)) {
//                        measurement.getObservationList().add(observation);
//                    }
                }
            }

            observation.setDescription(entity.getDescription());
            observation.setDeviation(entity.getDeviation());
            observation.setTitle(entity.getTitle());
            observation.setAction(entity.getAction());
            observation.setImprovement(entity.getImprovement());
            observation.setImprovementDeadline(entity.getImprovementDeadline());
            observation.setDeleted(entity.isDeleted());
            observation.setModifiedUser(entity.getModifiedUser());
            observation.setModifiedDate(new Date());
            observation.setInfrared(entity.isInfrared());
            observation.setDeviationReason(entity.getDeviationReason());
            observation.setObservationNo(entity.getObservationNo());
            observation.setObservationType(entity.getObservationType());
            observation.setObservationState(entity.getObservationState());
            observation.setQuickChoiceItem(entity.getQuickChoiceItem());
            observation.setPerformingUserId(entity.getPerformingUserId());
            observation.setPerformingDate(entity.getPerformingDate());
            observation.setAlternativeLocation(entity.getAlternativeLocation());

            if (!quickChoiceItemId.isEmpty()) {
                QuickChoiceItem quickChoiceItem = quickChoiceItemFacadeREST.find(quickChoiceItemId);
                if (quickChoiceItem != null) {
                    observation.setQuickChoiceItem(quickChoiceItem);
                }
            }
            if (!equipmentId.isEmpty()) {
                Equipment equipment = equipmentFacadeREST.find(equipmentId);
                if (equipment != null) {
                    observation.setEquipment(equipment);
                    /*if (!equipment.getObservationList().contains(equipment)) {
                        equipment.getObservationList().add(observation);
                    }*/
                } else {
                    if (entity.getEquipment() == null) {
                        observation.setEquipment(null);
                    }
                }
            }
            if (!locationId.isEmpty()) {
                Location location = locationFacadeREST.find(locationId);
                if (location != null) {
                    observation.setLocation(location);
                    if (!location.getObservationList().contains(observation)) {
                        location.getObservationList().add(observation);
                    }
                } else {
                    //System.out.println("Location is missing!!!" + locationId);
                }
            }
            //observation.setMeasurementList(measurements);
            try {
                super.edit(observation);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            Logger.getLogger(ObservationClient.class.getName()).log(Level.INFO, "Merkelig fenomen... Denne observasjonen eksisterer ikke: observationId = " + id);
            try {
                throw new Exception("Merkelig fenomen... Denne observasjonen eksisterer ikke");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @PUT
    @Path("linkUser/{userId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToUser(@PathParam("userId") String userId, Observation entity) {
        UserFacadeREST userFacadeREST = new UserFacadeREST();
        Observation observation = this.find(entity.getObservationId());
        User user = userFacadeREST.find(userId);
        if (user != null && observation != null) {
            if (!user.getObservationList().contains(observation)) {
                user.getObservationList().add(observation);
                userFacadeREST.edit(user);
            }
            if (!observation.getUserList().contains(user)) {
                observation.getUserList().add(user);
                this.edit(observation);
            }
        }
    }

    @PUT
    @Path("linkUser2/{userId}/{observationId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToUser2(@PathParam("userId") String userId, @PathParam("observationId") String observationId) {
        UserFacadeREST userFacadeREST = new UserFacadeREST();
        Observation observation = this.find(observationId);
        User user = userFacadeREST.find(userId);
        if (user != null && observation != null) {
            if (!user.getObservationList().contains(observation)) {
                user.getObservationList().add(observation);
                userFacadeREST.edit(user);
            }
            if (!observation.getUserList().contains(user)) {
                observation.getUserList().add(user);
                this.edit(observation);
            }
        }
    }

    @PUT
    @Path("unlinkUser/{userId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkToUser(@PathParam("userId") String userId, Observation entity) {
        UserFacadeREST userFacadeREST = new UserFacadeREST();
        Observation observation = this.find(entity.getObservationId());
        User user = userFacadeREST.find(userId);
        if (user != null && observation != null) {
            if (user.getObservationList().contains(observation)) {
                user.getObservationList().remove(observation);
                userFacadeREST.edit(user);
            }
            if (observation.getUserList().contains(user)) {
                observation.getUserList().remove(user);
                this.edit(observation);
            }
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        Observation observation = super.find(id);
        if (observation != null) {
            Project project = observation.getProject();
            if (project.getObservationList().contains(observation)) {
                project.getObservationList().remove(observation);
            }
            super.remove(super.find(id));
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Observation find(@PathParam("id") String id) {
        //return super.find(id);
        return findNative(id);
    }

    public Observation findNative(String id) {
        Observation observation = null;
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Observation> resultList = (List<Observation>) em.createNativeQuery("SELECT "
                                + "* FROM observation o\n"
                                + "WHERE o.observation_id = ?1",
                        Observation.class)
                .setParameter(1, id)
                .getResultList();

        if (!resultList.isEmpty()) {
            observation = resultList.get(0);
            List<Media> mediaList = (List<Media>) em.createNativeQuery("SELECT "
                                    + "* FROM image img\n"
                                    + "JOIN observation_has_image ohi on ohi.image = img.image_id\n"
                                    + "WHERE ohi.observation = ?1",
                            Media.class)
                    .setParameter(1, observation.getObservationId())
                    .getResultList();

            List<Measurement> measurementList = (List<Measurement>) em.createNativeQuery("SELECT "
                                    + "* FROM measurement meas\n"
                                    + "JOIN observation_has_measurement ohm on ohm.measurement_measurement_id = meas.measurement_id\n"
                                    + "WHERE ohm.observation_observation_id = ?1",
                            Measurement.class)
                    .setParameter(1, observation.getObservationId())
                    .getResultList();
            observation.setImageList(mediaList);
            observation.setMeasurementList(measurementList);
            em.close();

            observation.setProjectNumber(observation.getProject().getProjectNumber());
            if(observation.getEquipment() != null) {
                observation.setEquipmentString(observation.getEquipment().getFullName());
                observation.setEquipmentTagId(observation.getEquipment().getTagId());
                observation.setEquipmentId(observation.getEquipment().getEquipmentId());
            }
            if(observation.getLocation() != null) {
                observation.setLocationString(observation.getLocation().getFullName());
                observation.setLocationId(observation.getLocation().getLocationId());
            }
            return observation;
        }
        em.close();
        return observation;
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

    @Deprecated
    @GET
    @Path("projectobservations/{projectid}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> findProjectObservations(@PathParam("projectid") String projectId) {
        List<Observation> observations = findProjectObservationsNative(projectId);

        for (Observation observation : observations) {
            if (observation.getEquipment() != null) {
                observation.setEquipmentId(observation.getEquipment().getEquipmentId());
            }
            observation.setEquipmentId(null);
            if (observation.getOldProject() != null) {
                observation.setOldProjectId(observation.getOldProject().getProjectId());
                observation.setOldProjectNumber(observation.getOldProject().getProjectNumber());
            }
        }

        return observations;
    }

    @GET
    @Path("loadByProject/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadByProject(@PathParam("projectId") String projectId) {
        List<Observation> observations = findProjectObservationsNative(projectId);
        List<Observation> optimizedObservations = new ArrayList<>();
        for (Observation observation : observations) {
            optimizedObservations.add(optimizeObservation(observation));
        }

        Collections.sort(optimizedObservations, (Observation o1, Observation o2) -> o1.getObservationNo().compareTo(o2.getObservationNo()));
        List<Observation> filteredObservations = optimizedObservations.stream().filter(r -> !r.isDeleted()).collect(Collectors.toList());

        return filteredObservations;
    }

    @GET
    @Path("loadByProjectOptimized/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadByProjectOptimized(@PathParam("projectId") String projectId) {
        List<Observation> observations = findProjectObservationsNative(projectId);
        List<Observation> optimizedObservations = new ArrayList<>();
        for (Observation observation : observations) {
            List<Measurement> statusMeasurements = observation.getMeasurementList().stream().filter(r -> r.getName().equalsIgnoreCase("Status")).collect(Collectors.toList());
            if(!statusMeasurements.isEmpty()) {
                observation.setMeasurementStatusId(statusMeasurements.get(0).getMeasurementId());
                observation.setMeasurementStatusString(statusMeasurements.get(0).getStringValue());
            }
            if(observation.getEquipment() != null) {
                observation.setEquipmentString(observation.getEquipment().getFullName());
                observation.setEquipmentId(observation.getEquipment().getEquipmentId());
                //observation.setEquipment(null);
            }
            observation.setImageList(new ArrayList<>());
            observation.setMeasurementList(new ArrayList<>());
            if(observation.getQuickChoiceItem() != null) {
                observation.getQuickChoiceItem().setMeasurementList(new ArrayList<>());
            }
            if(observation.getOldProject() != null) {
                observation.setOldProjectId(observation.getOldProject().getProjectId());
                observation.setOldProjectNumber(observation.getOldProject().getProjectNumber());
                //observation.setOldProject(null);
            }
            optimizedObservations.add(observation);
        }

        Collections.sort(optimizedObservations, Comparator.comparing(Observation::getObservationNo));
        List<Observation> filteredObservations = optimizedObservations.stream().filter(r -> !r.isDeleted()).collect(Collectors.toList());

        return filteredObservations;
    }

    @GET
    @Path("loadByProjectOptimizedTemp/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadByProjectOptimizedTemp(@PathParam("projectId") String projectId) {
        List<Observation> observations = findProjectObservationsNative(projectId);
        List<Observation> optimizedObservations = new ArrayList<>();
        for (Observation observation : observations) {
            List<Measurement> statusMeasurements = observation.getMeasurementList().stream().filter(r -> r.getName().equalsIgnoreCase("Status")).collect(Collectors.toList());
            if(!statusMeasurements.isEmpty()) {
                observation.setMeasurementStatusId(statusMeasurements.get(0).getMeasurementId());
                observation.setMeasurementStatusString(statusMeasurements.get(0).getStringValue());
            }
            if(observation.getEquipment() != null) {
                observation.setEquipmentString(observation.getEquipment().getFullName());
                observation.setEquipmentId(observation.getEquipment().getEquipmentId());
                observation.setEquipment(null);
            }
            if(observation.getOldProject() != null) {
                observation.setOldProjectId(observation.getOldProject().getProjectId());
                observation.setOldProjectNumber(observation.getOldProject().getProjectNumber());
                observation.setOldProject(null);
            }
            //observation.setImageList(new ArrayList<>());
            //observation.setMeasurementList(new ArrayList<>());
            if(observation.getQuickChoiceItem() != null) {
                observation.getQuickChoiceItem().setMeasurementList(new ArrayList<>());
            }
            optimizedObservations.add(observation);
        }

        Collections.sort(optimizedObservations, Comparator.comparing(Observation::getObservationNo));
        List<Observation> filteredObservations = optimizedObservations.stream().filter(r -> !r.isDeleted()).collect(Collectors.toList());

        return filteredObservations;
    }


    private Observation optimizeObservation(Observation observation) {
        observation.getMeasurementList().clear();
        observation.getImageList().clear();
        if (observation.getLocation() != null) {
            observation.setLocationId(observation.getLocation().getLocationId());
            observation.setLocationString(observation.getLocation().getFullName());
        }
        observation.setLocation(null);
        if (observation.getEquipment() != null) {
            observation.setEquipmentId(observation.getEquipment().getEquipmentId());
            observation.setEquipmentString(observation.getEquipmentName());
        }
        observation.setEquipment(null);

        if (observation.getProject() != null) {
            String observationNo = String.format("%d.%03d", observation.getProject().getProjectNumber(), observation.getObservationNo());
            observation.setNumberString(observationNo);
            observation.setProjectNumber(observation.getProject().getProjectNumber());
        }
        return observation;
    }

    @GET
    //@Deprecated
    @Path("projectObservationsReduced/{projectid}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> findProjectObservationsReduced(@PathParam("projectid") String projectId) {
        List<Observation> observations = findProjectObservationsNative(projectId);
        List<Observation> reducedObservations = new ArrayList<>();
        for (Observation observation : observations) {
            Observation reduced = observation.duplicate(true);
            reduced.setMeasurementList(new ArrayList<>());
            if (reduced.getEquipment() != null) {
                reduced.setEquipmentId(reduced.getEquipment().getEquipmentId());
                reduced.setEquipment(null);
            }
            reducedObservations.add(reduced);
        }
//        for (Observation observation : observations) {
//            if (observation.getEquipment() != null) {
//                observation.setEquipmentId(observation.getEquipment().getEquipmentId());
//            }
//            observation.setEquipmentId(null);
//        }
        return reducedObservations;
    }

    @GET
    @Path("findProjectObservationsForCompany/{projectId}/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> findProjectObservationsForCompany(@PathParam("projectId") String projectId, @PathParam("companyId") String companyId) {
        List<Observation> observations = findProjectObservationsForCompanyNative(projectId, companyId);
        return observations;
    }

    @PUT
    @Path("loadObservationsForCompanyOLD/{companyId}/{stateString}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadObservationsForCompany(@PathParam("companyId") String companyId,
                                                        @PathParam("stateString") String stateString, List<Integer> tgs) {
        Integer state = Integer.parseInt(stateString);
        String tgsString = "(";
        for (Integer tg : tgs) {
            tgsString += Integer.toString(tg) + ",";
        }
        tgsString = tgsString.substring(0, tgsString.length() - 1);
        tgsString += ")";

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Observation> resultList = (List<Observation>) em.createNativeQuery("SELECT "
                                + "* FROM observation obs\n"
                                + "join project p on p.project_id = obs.project\n"
                                + "join company_has_project chp on chp.project_project_id = p.project_id\n"
                                + "where obs.deleted = 0 and chp.company_company_id = ?1 and observation_state <= ?2 and deviation in " + tgsString + "\n"
                                + "LIMIT 0,50",
                        Observation.class)
                .setParameter(1, companyId)
                .setParameter(2, state)
                .getResultList();
        em.close();
        for (Observation observation : resultList) {
            observation.setProjectId(observation.getProject().getProjectId());
        }
        return resultList;
    }

    @GET
    @Path("loadByEquipmentWithProject/{equipmentId}/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadByEquipmentWithProject(@PathParam("equipmentId") String equipmentId,
                                                        @PathParam("projectId") String projectId) {


        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Observation> resultList = (List<Observation>) em.createNativeQuery("SELECT "
                                + "* FROM observation obs\n"
                                + "where obs.deleted = 0 and obs.equipment = ?1 and obs.project = ?2 " + "\n"
                                + "order by obs.created_date DESC\n",
                        Observation.class)
                .setParameter(1, equipmentId)
                .setParameter(2, projectId)
                .getResultList();
        em.close();
        for (Observation observation : resultList) {
            optimizeObservation(observation);
            //observation.setProjectId(observation.getProject().getProjectId());
        }
        return resultList;
    }
    @PUT
    @Path("loadObservationsForCompany/{companyId}/{stateString}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadObservationsForCompany(@PathParam("companyId") String companyId,
                                                        @PathParam("stateString") String stateString,
                                                        @PathParam("batchOffset") String batchOffsetString,
                                                        @PathParam("batchSize") String batchSizeString,
                                                        List<Integer> tgs) {

        Integer batchOffset = Integer.parseInt(batchOffsetString);
        Integer batchSize = Integer.parseInt(batchSizeString);
        Integer state = Integer.parseInt(stateString);
        String tgsString = "(";
        for (Integer tg : tgs) {
            tgsString += Integer.toString(tg) + ",";
        }
        tgsString = tgsString.substring(0, tgsString.length() - 1);
        tgsString += ")";

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Observation> resultList = (List<Observation>) em.createNativeQuery("SELECT "
                                + "* FROM observation obs\n"
                                + "join project p on p.project_id = obs.project\n"
                                + "join company_has_project chp on chp.project_project_id = p.project_id\n"
                                + "where obs.deleted = 0 and chp.company_company_id = ?1 and obs.observation_state <= ?2 and obs.deviation in " + tgsString + "\n"
                                + "order by obs.created_date DESC\n LIMIT ?3,?4",
                        Observation.class)
                .setParameter(1, companyId)
                .setParameter(2, state)
                .setParameter(3, batchOffset)
                .setParameter(4, batchSize)
                .getResultList();
        em.close();
        for (Observation observation : resultList) {
            observation.setProjectId(observation.getProject().getProjectId());
        }
        return resultList;
    }

    @PUT
    @Path("loadObservationsForCompanyInPeriod/{companyId}/{stateString}/{batchOffset}/{batchSize}/{fromDate}/{toDate}/{showOpenOnly}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadObservationsForCompanyInPeriod(@PathParam("companyId") String companyId,
                                                                @PathParam("stateString") String stateString,
                                                                @PathParam("batchOffset") String batchOffsetString,
                                                                @PathParam("batchSize") String batchSizeString,
                                                                @PathParam("fromDate") String fromDate,
                                                                @PathParam("toDate") String toDate,
                                                                @PathParam("showOpenOnly") Boolean showOpenOnly,
                                                                List<Integer> tgs) {

        Integer batchOffset = Integer.parseInt(batchOffsetString);
        Integer batchSize = Integer.parseInt(batchSizeString);
        Integer state = Integer.parseInt(stateString);
        String tgsString = "(";
        for (Integer tg : tgs) {
            tgsString += Integer.toString(tg) + ",";
        }
        tgsString = tgsString.substring(0, tgsString.length() - 1);
        tgsString += ")";
        String showOpenOnlySql = "";
        if (showOpenOnly) {
            showOpenOnlySql = " AND p.project_state < 7 and obs.observation_state <= " + stateString + " ";
        }
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Observation> resultList = (List<Observation>) em.createNativeQuery("SELECT "
                                + "* FROM observation obs\n"
                                + "join project p on p.project_id = obs.project\n"
                                + "join company_has_project chp on chp.project_project_id = p.project_id\n"
                                + "where obs.deleted = 0 AND p.deleted = 0 and chp.company_company_id = ?1 and obs.deviation in " + tgsString + " AND\n"
                                + "   obs.created_date > ?5 AND "
                                + "   obs.created_date < ?6 " + showOpenOnlySql + " "
                                + "order by obs.created_date DESC\n LIMIT ?3,?4",
                        Observation.class)
                .setParameter(1, companyId)
                .setParameter(3, batchOffset)
                .setParameter(4, batchSize)
                .setParameter(5, fromDate)
                .setParameter(6, toDate)
                .getResultList();
        em.close();
        for (Observation observation : resultList) {
            observation.setProjectId(observation.getProject().getProjectId());
        }
        return resultList;

    }

    @PUT
    @Path("loadObservationsForAsset/{assetId}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadObservationsForAsset(@PathParam("assetId") String assetId,
                                                      @PathParam("batchOffset") String strBatchOffset,
                                                      @PathParam("batchSize") String strBatchSize,
                                                      List<Integer> states) {
        int batchOffset = Integer.parseInt(strBatchOffset);
        int batchSize = Integer.parseInt(strBatchSize);
        String stateString = "(";
        for (Integer state : states) {
            stateString += Integer.toString(state) + ",";
        }
        stateString = stateString.substring(0, stateString.length() - 1);
        stateString += ")";

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Observation> resultList = (List<Observation>) em.createNativeQuery("SELECT "
                                + "* FROM observation obs\n"
                                + "join project p on p.project_id = obs.project\n"
                                + "join asset a on a.asset_id = p.asset\n"
                                + "where obs.deleted = 0 and a.asset_id = ?1 and p.project_state in " + stateString + "\n"
                                + "order by obs.created_date DESC\n LIMIT ?2,?3",
                        Observation.class)
                .setParameter(1, assetId)
                .setParameter(2, batchOffset)
                .setParameter(3, batchSize)
                .getResultList();
        em.close();
        for(Observation observation: resultList) {
            if(observation.getEquipment() != null) {
                observation.setEquipmentId(observation.getEquipment().getEquipmentId());
                observation.setEquipmentString(observation.getEquipment().getFullName());
                if(observation.getEquipment().getTagId() != null) {
                    observation.setEquipmentTagId(observation.getEquipment().getTagId());
                }
                observation.setEquipment(null);
            }
            if(observation.getLocation() != null) {
                observation.setLocationId(observation.getLocation().getLocationId());
                observation.setLocationString(observation.getLocation().getFullName());
                observation.setLocation(null);
            }
            if(observation.getProject() != null) {
                observation.setProjectNumber(observation.getProject().getProjectNumber());
            }
            if(observation.getQuickChoiceItem() != null) {
                observation.setQuickChoiceItem(null);
            }
            if(!observation.getMeasurementList().isEmpty()) {
                List<Measurement> statusMeasurements = observation.getMeasurementList().stream().filter(r -> r.getName().equalsIgnoreCase("Status")).collect(Collectors.toList());
                if(!statusMeasurements.isEmpty()) {
                    observation.setMeasurementStatusId(statusMeasurements.get(0).getMeasurementId());
                    observation.setMeasurementStatusString(statusMeasurements.get(0).getStringValue());
                }
            }
            observation.setProject(null);
            observation.setQuickChoiceItem(null);
            observation.setEquipment(null);
            observation.setLocation(null);
            observation.getMeasurementList().clear();
        }
        return resultList;
    }

    public List<Observation> findProjectObservationsForCompanyNative(String projectId, String companyId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Observation> resultList = (List<Observation>) em.createNativeQuery("SELECT "
                                + "* FROM observation o\n"
                                + "JOIN observation_has_user ohu on observation_id = ohu.observation_observation_id\n"
                                + "JOIN user u on ohu.user_user_id = u.user_id\n"
                                + "JOIN company_has_user chu on u.user_id = chu.user\n"
                                + "JOIN company c on chu.company = c.company_id\n"
                                + "WHERE o.project = ?1 and c.company_id = ?2",
                        Observation.class)
                .setParameter(1, projectId)
                .setParameter(2, companyId)
                .getResultList();
        em.close();
        return resultList;
    }

    @GET
    @Path("findProjectObservationsForUser/{projectId}/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> findProjectObservationsForUser(@PathParam("projectId") String projectId, @PathParam("userId") String userId) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Observation> resultList = (List<Observation>) em.createNativeQuery("SELECT "
                                + "* FROM observation o\n"
                                + "JOIN observation_has_user ohu on observation_id = ohu.observation_observation_id\n"
                                + "JOIN user u on ohu.user_user_id = u.user_id\n"
                                + "WHERE o.project = ?1 and u.user_id = ?2",
                        Observation.class)
                .setParameter(1, projectId)
                .setParameter(2, userId)
                .getResultList();
        em.close();

        UserRoleFacadeREST userRoleFacadeREST = new UserRoleFacadeREST();
        List<UserRole> userRoles = userRoleFacadeREST.loadByProject(projectId, userId);
        if (!userRoles.isEmpty()) {
            UserRole userRole = userRoles.get(0);
            if (userRole.getRole().getRoleType().contains("_RESTRICTED")) {
                if (userRole.getParameter() != null) {
                    if (!userRole.getParameter().isEmpty()) {
                        List<Observation> observations = findProjectObservations(projectId);
                        UserRoleParameter userRoleParameter = UserRoleParameter.fromJsonString(userRole.getParameter());
                        List<Observation> filtered = observations
                                .stream().filter(r -> userRoleParameter.tgList.contains(r.getDeviation())).collect(Collectors.toList());
                        for (Observation observation : filtered) {
                            if (!resultList.contains(observation)) {
                                resultList.add(observation);
                            }
                        }
                    }
                }
            }
        }
        return resultList;
    }

    public List<Observation> findProjectObservationsNative(String projectId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Observation> resultList = (List<Observation>) em.createNativeQuery("SELECT "
                                + "* FROM observation o\n"
                                + "WHERE o.project = ?1 OR o.old_project = ?1",
                        Observation.class)
                .setParameter(1, projectId)
                .getResultList();
        em.close();
        return resultList;
    }

    public Integer countProjectObservationsNative(String projectId, List<Integer> tgs) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        // find projectUserRole
        String tgsString = "(";
        for (Integer tg : tgs) {
            tgsString += Integer.toString(tg) + ",";
        }
        tgsString = tgsString.substring(0, tgsString.length() - 1);
        tgsString += ")";
        Long counter = (Long) em.createNativeQuery("SELECT "
                        + "count(o.observation_id) FROM observation o\n"
                        + "WHERE (o.project = ?1 or o.old_project = ?1) AND o.deviation IN " + tgsString + " AND o.deleted = 0")
                .setParameter(1, projectId)
                .getSingleResult();

        em.close();
        return counter.intValue();
    }

    public List<Observation> findEquipmentObservationsNative(String equipmentId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Observation> resultList = (List<Observation>) em.createNativeQuery("SELECT "
                                + "* FROM observation o\n"
                                + "WHERE o.equipment = ?1",
                        Observation.class)
                .setParameter(1, equipmentId)
                .getResultList();
        em.close();
        return resultList;
    }


    @GET
    @Path("findProjectObservations/{projectid}/{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> findProjectObservations(@PathParam("projectid") String projectId, @PathParam("from") Integer from, @PathParam("to") Integer to) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Observation> resultList = (List<Observation>) em.createNativeQuery("SELECT "
                                + "* FROM observation o\n"
                                + "WHERE o.project = ?1",
                        Observation.class)
                .setParameter(1, projectId)
                .getResultList();
        em.close();
        return resultList;
    }

    @Deprecated
    @GET
    @Path("countprojectobservation/{projectid}")
    //@Produces({MediaType.APPLICATION_JSON})
    @Produces(MediaType.TEXT_PLAIN)
    public String countProjectObservations(@PathParam("projectid") String projectId) {
        ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
        Project project = projectFacadeREST.find(projectId);
        if (project != null) {
            List<Observation> observations = new ArrayList<>(project.getObservationList());
            List<Observation> filteredObservations = observations.stream().filter(r -> r.isDeleted() == false && r.getObservationType() >= 0).collect(Collectors.toList());
            return String.valueOf(filteredObservations.size());
        }
        return "0";
    }

    @GET
    @Path("countSingleProjectObservations/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ObservationCounter countSingleProjectObservations(@PathParam("projectId") String projectId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        Query query = em.createNativeQuery("SELECT COUNT(*) FROM observation o\n " +
                        " WHERE o.project = ?1 AND o.deleted = ?2")
                .setParameter(1, projectId)
                .setParameter(2, false);

        Number counter = (Number) query.getSingleResult();
        int intCounter = Integer.parseInt(counter.toString());

        ObservationCounter observationCounter = new ObservationCounter();
        observationCounter.setCounter(intCounter);
        observationCounter.setProjectId(projectId);
        em.close();
        return observationCounter;
    }

    private String createSqlString(ObservationRequestParameters parameters, Boolean isCounting) {
        String sqlString = "";
        String selectString = "";
        String inTgString = "";
        String whereString = "WHERE ";
        String projectId = "'" + parameters.entityIds.get(0) + "'";
        String deletedFlag = "0";
        if (isCounting) {
            selectString = "SELECT count(distinct o.observation_id) FROM observation o\n";
        } else {
            selectString = "SELECT * FROM observation o\n";
            selectString += "LEFT JOIN location l1 on l1.location_id = o.location\n";
            selectString += "LEFT JOIN location l2 on l1.parent = l2.location_id\n";
            selectString += "LEFT JOIN location l3 on l2.parent = l3.location_id\n";
            selectString += "LEFT JOIN location l4 on l3.parent = l4.location_id\n";
            selectString += "LEFT JOIN equipment e on e.equipment_id = o.equipment\n";
            selectString += "LEFT JOIN equipment_type et on et.equipment_type_id = e.equipment_type\n";
        }
        if(parameters.showDeletedOnly) {
            deletedFlag = "1";
        }
        if (!parameters.tgs.isEmpty()) {
            inTgString = "(";
            for (Integer tg : parameters.tgs) {
                inTgString += tg.toString() + ",";
            }
            inTgString = inTgString.substring(0, inTgString.length() - 1);
            inTgString += ")";
            inTgString = " o.deviation in " + inTgString + " AND \n";
        }
        if(!inTgString.isEmpty()) {
            whereString += inTgString;
        }
        whereString += "o.project = " + projectId + " AND o.deleted = " + deletedFlag;
        if(parameters.showOpenOnly) {
            whereString += " AND o.observation_state = 0";
        }
        if(parameters.showImprovingOnly) {
            whereString += " AND o.observation_state = 1";
        }
        if(parameters.showOverdue) {
            Date today = new Date();
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            String todayString = dateFormatter.format(today);
            whereString += " AND o.observation_state = 0 AND o.improvement_deadline < '" + todayString + "'";
        }
        if(isCounting) {
            whereString += " AND o.deleted = 0";
        }
        if(!parameters.queryString.isEmpty()) {
            String query = parameters.queryString;
            whereString += " AND \n";
            whereString += " (concat(o.description, o.action, o.improvement) like '%" + query +"%'\n";
            whereString += " OR concat(COALESCE(l4.name,''), COALESCE(l4.name,''), COALESCE(l2.name,''),COALESCE(l1.name,'')) like '%" + query +"%'\n";
            whereString += " OR concat(COALESCE(e.name,''), COALESCE(e.tag_id,'')) like '%" + query +"%'\n";
            whereString += " OR concat(COALESCE(et.name,'')) like '%" + query +"%'\n)";
        }

        sqlString = selectString + whereString;
        return sqlString;
    }

    @PUT
    @Path("countObservations")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Integer countObservations(ObservationRequestParameters parameters) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        Query queryCounter = em.createNativeQuery(createSqlString(parameters, true));

        Number counterUnassigned = (Number) queryCounter.getSingleResult();
        Integer integerCounter = Integer.parseInt(counterUnassigned.toString());

        em.close();
        return integerCounter;
    }

    @PUT
    @Path("loadProjectObservations/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Observation> loadProjectObservations(ObservationRequestParameters parameters) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        String queryString = createSqlString(parameters, false);
        List<Observation> resultList = em.createNativeQuery(queryString, Observation.class)
                .getResultList();
        for(Observation observation: resultList) {
            if(observation.getEquipment() != null) {
                observation.setEquipmentId(observation.getEquipment().getEquipmentId());
                observation.setEquipmentString(observation.getEquipment().getFullName());
                if(observation.getEquipment().getTagId() != null) {
                    observation.setEquipmentTagId(observation.getEquipment().getTagId());
                }
                observation.setEquipment(null);
            }
            if(observation.getLocation() != null) {
                observation.setLocationId(observation.getLocation().getLocationId());
                observation.setLocationString(observation.getLocation().getFullName());
                observation.setLocation(null);
            }
            if(observation.getProject() != null) {
                observation.setProjectNumber(observation.getProject().getProjectNumber());
            }
            if(observation.getQuickChoiceItem() != null) {
                observation.setQuickChoiceItem(null);
            }
            if(!observation.getMeasurementList().isEmpty()) {
                List<Measurement> statusMeasurements = observation.getMeasurementList().stream().filter(r -> r.getName().equalsIgnoreCase("Status")).collect(Collectors.toList());
                if(!statusMeasurements.isEmpty()) {
                    observation.getMeasurementList().clear();
                    observation.getMeasurementList().addAll(statusMeasurements);
                    observation.setMeasurementStatusId(statusMeasurements.get(0).getMeasurementId());
                    observation.setMeasurementStatusString(statusMeasurements.get(0).getStringValue());
                } else {
                    observation.getMeasurementList().clear();
                }
            }
        }
        return resultList;
    }

    @PUT
    @Path("countProjectObservations/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<ObservationCounter> countProjectObservations(List<String> projectIds) {
        List<ObservationCounter> observationCounters = new ArrayList<>();
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        for (String projectId : projectIds) {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM observation o\n " +
                            " WHERE (o.project = ?1 or o.old_project = ?1) AND o.deleted = ?2")
                    .setParameter(1, projectId)
                    .setParameter(2, false);

            Number counter = (Number) query.getSingleResult();
            int intCounter = Integer.parseInt(counter.toString());

            ObservationCounter observationCounter = new ObservationCounter();
            observationCounter.setCounter(intCounter);
            observationCounter.setProjectId(projectId);
            observationCounters.add(observationCounter);
        }
        em.close();
        return observationCounters;
    }

    @PUT
    @Path("countCompanyDeviations")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviationCounter> countCompanyDeviations(ObservationRequestParameters parameters) {

        List<DeviationCounter> observationCounters = new ArrayList<>();
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        DeviationCounter deviationCounter = new DeviationCounter();
        for (String companyId : parameters.entityIds) {
            for (int i = 0; i < 4; i++) {
                String showOpenOnlySql = "";
                if (parameters.showOpenOnly) {
                    showOpenOnlySql = "p.project_state < 7 and o.observation_state < 1 AND ";
                }

                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                String strFromDate = dateFormatter.format(parameters.fromDate);
                String strToDate = dateFormatter.format(parameters.toDate);
                // Count deviation for grade with state
                Query queryTG = em.createNativeQuery("SELECT COUNT(*) FROM observation o\n " +
                                "JOIN project p ON p.project_id = o.project\n" +
                                "JOIN company_has_project chp ON chp.project_project_id = p.project_id \n " +
                                " WHERE " +
                                "   chp.company_company_id = ?1 AND " +
                                "   o.deleted = ?2 AND p.deleted = 0 AND o.deviation = ?4 AND " + showOpenOnlySql +
                                "   o.created_date > ?5 AND " +
                                "   o.created_date < ?6")

                        .setParameter(1, companyId)
                        .setParameter(2, false)
                        .setParameter(4, i)
                        .setParameter(5, strFromDate)
                        .setParameter(6, strToDate);


                Number counterTG = (Number) queryTG.getSingleResult();
                int intCounterTG = Integer.parseInt(counterTG.toString());
                TgCounter tgCounter = new TgCounter(i, intCounterTG);
                deviationCounter.getTgCounters().add(tgCounter);

                // Count deviation overdue improvement
                //improvement_deadline
                // '2021-11-01 00:00:00'
                Date today = new Date();

                String strToday = dateFormatter.format(today);
                Query queryOverdue = em.createNativeQuery("SELECT COUNT(*) FROM observation o\n " +
                                "JOIN project p ON p.project_id = o.project\n" +
                                "JOIN company_has_project chp ON chp.project_project_id = p.project_id \n " +
                                "WHERE chp.company_company_id = ?1 " +
                                "AND o.deleted = ?2 AND p.deleted = 0 AND o.observation_state < 1 AND o.deviation = ?4 " +
                                "AND o.improvement_deadline < ?5 AND" +
                                "   o.created_date > ?6 AND o.created_date < ?7")
                        .setParameter(1, companyId)
                        .setParameter(2, false)
                        .setParameter(4, i)
                        .setParameter(5, strToday)
                        .setParameter(6, strFromDate)
                        .setParameter(7, strToDate);

                Number counterOverdue = (Number) queryOverdue.getSingleResult();
                int intCounterOverdue = Integer.parseInt(counterOverdue.toString());
                tgCounter.setOverdueCounter(intCounterOverdue);

                Query queryUnassigned = em.createNativeQuery(
                                "SELECT count(*) FROM (SELECT p.project_number," +
                                        "(SELECT count(*) FROM observation_has_user ohu\n " +
                                        "  where ohu.observation_observation_id = o.observation_id) as counter" +
                                        "  FROM observation o\n " +
                                        "JOIN project p ON p.project_id = o.project\n" +
                                        "JOIN company_has_project chp ON chp.project_project_id = p.project_id \n " +
                                        "WHERE chp.company_company_id = ?1 " +
                                        "AND o.deleted = 0 AND p.deleted = 0 AND o.observation_state < 1 AND o.deviation = ?3 AND " +
                                        "o.created_date > ?4 AND o.created_date < ?5) " +
                                        "as assigned_observations WHERE assigned_observations.counter = 0")
                        .setParameter(1, companyId)
                        .setParameter(3, i)
                        .setParameter(4, strFromDate)
                        .setParameter(5, strToDate);

                Number counterUnassigned = (Number) queryUnassigned.getSingleResult();
                int intCounterUnassigned = Integer.parseInt(counterUnassigned.toString());
                tgCounter.setUnassignedCounter(intCounterUnassigned);
            }
            deviationCounter.setEntityId(companyId);
            deviationCounter.setEntityName("company");
            observationCounters.add(deviationCounter);
        }
        em.close();
        return observationCounters;
    }

    @PUT
    @Path("countAssetDeviations")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviationCounter> countAssetDeviations(ObservationRequestParameters parameters) {
        List<DeviationCounter> observationCounters = new ArrayList<>();
        for (String assetId : parameters.entityIds) {
            DeviationCounter deviationCounter = new DeviationCounter();
            for (int i = 0; i < 4; i++) {
                String showOpenOnlySql = "";
                if (parameters.showOpenOnly) {
                    showOpenOnlySql = "(p.project_state < 7 AND o.observation_state < 1) AND ";
                }
                // Count deviation for grade with state
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                String strFromDate = dateFormatter.format(parameters.fromDate);
                String strToDate = dateFormatter.format(parameters.toDate);

                EntityManager em = LocalEntityManagerFactory.createEntityManager();
                Query queryTG = em.createNativeQuery("SELECT COUNT(*) FROM observation o\n " +
                                "JOIN project p ON p.project_id = o.project\n" +
                                "JOIN company_has_project chp ON chp.project_project_id = p.project_id \n " +
                                "JOIN asset a ON a.asset_id = p.asset" +
                                " WHERE " +
                                "   chp.company_company_id = ?1 AND a.asset_id = ?2 AND " +
                                "   o.deleted = ?3 AND p.deleted = 0 AND o.deviation = ?4 AND " + showOpenOnlySql +
                                "   o.created_date > ?5 AND " +
                                "   o.created_date < ?6")

                        .setParameter(1, parameters.authorityId)
                        .setParameter(2, assetId)
                        .setParameter(3, false)
                        .setParameter(4, i)
                        .setParameter(5, strFromDate)
                        .setParameter(6, strToDate);


                Number counterTG = (Number) queryTG.getSingleResult();
                int intCounterTG = Integer.parseInt(counterTG.toString());
                TgCounter tgCounter = new TgCounter(i, intCounterTG);
                // Count deviation overdue improvement
                //improvement_deadline
                // '2021-11-01 00:00:00'
                Date today = new Date();
                String strToday = dateFormatter.format(today);
                Query queryOverdue = em.createNativeQuery("SELECT COUNT(*) FROM observation o\n " +
                                "JOIN project p ON p.project_id = o.project\n" +
                                "JOIN company_has_project chp ON chp.project_project_id = p.project_id \n " +
                                "WHERE chp.company_company_id = ?1 " +
                                "AND o.deleted = ?2 AND p.deleted = 0 AND o.observation_state < 1 AND o.deviation = ?4 " +
                                "AND o.improvement_deadline < ?5 AND" +
                                "   o.created_date > ?6 AND o.created_date < ?7")
                        .setParameter(1, parameters.parentEntity)
                        .setParameter(2, false)
                        .setParameter(4, i)
                        .setParameter(5, strToday)
                        .setParameter(6, strFromDate)
                        .setParameter(7, strToDate);

                Number counterOverdue = (Number) queryOverdue.getSingleResult();
                int intCounterOverdue = Integer.parseInt(counterOverdue.toString());
                tgCounter.setOverdueCounter(intCounterOverdue);

                Query queryUnassigned = em.createNativeQuery(
                                "SELECT count(*) FROM (SELECT p.project_number," +
                                        "(SELECT count(*) FROM observation_has_user ohu\n " +
                                        "  where ohu.observation_observation_id = o.observation_id) as counter" +
                                        "  FROM observation o\n " +
                                        "JOIN project p ON p.project_id = o.project\n" +
                                        "JOIN company_has_project chp ON chp.project_project_id = p.project_id \n " +
                                        "WHERE chp.company_company_id = ?1 " +
                                        "AND o.deleted = 0 AND p.deleted = 0 AND o.observation_state < 1 AND o.deviation = ?3 AND " +
                                        "o.created_date > ?4 AND o.created_date < ?5) " +
                                        "as assigned_observations WHERE assigned_observations.counter = 0")
                        .setParameter(1, parameters.parentEntity)
                        .setParameter(3, i)
                        .setParameter(4, strFromDate)
                        .setParameter(5, strToDate);

                Number counterUnassigned = (Number) queryUnassigned.getSingleResult();
                int intCounterUnassigned = Integer.parseInt(counterUnassigned.toString());
                tgCounter.setUnassignedCounter(intCounterUnassigned);
                deviationCounter.getTgCounters().add(tgCounter);

                em.close();
            }
            deviationCounter.setEntityId(assetId);
            deviationCounter.setEntityName("asset");
            observationCounters.add(deviationCounter);
        }
        return observationCounters;
    }

    @PUT
    @Path("countProjectDeviations")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviationCounter> countProjectDeviations(ObservationRequestParameters parameters) {

        if (parameters.fromDate == null) {
            parameters.fromDate = new Date(0);
        }
        if (parameters.toDate == null) {
            parameters.toDate = new Date();
        }
        List<DeviationCounter> observationCounters = new ArrayList<>();
        for (String projectId : parameters.entityIds) {
            DeviationCounter deviationCounter = new DeviationCounter();
            for (int i = 0; i < 4; i++) {
                EntityManager em = LocalEntityManagerFactory.createEntityManager();
                String showOpenOnlySql = "";
                if (parameters.showOpenOnly) {
                    showOpenOnlySql = "p.project_state < 7 AND o.observation_state < 1 AND ";
                }

                String filterProjectSql = "";
                if (parameters.parentEntity.equalsIgnoreCase("project")) {
                    filterProjectSql = "p.project_id = '" + projectId + "' AND ";
                }

                // Count deviation for grade with state i
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                String strFromDate = dateFormatter.format(parameters.fromDate);
                String strToDate = dateFormatter.format(parameters.toDate);

                Query queryTG = em.createNativeQuery("" +
                                "SELECT COUNT(*) FROM observation o\n " +
                                "   JOIN project p ON p.project_id = o.project\n" +
                                "   JOIN company_has_project chp ON chp.project_project_id = p.project_id \n " +
                                " WHERE " +
                                "   chp.company_company_id = ?1 AND " +
                                "   o.project = ?2 AND " +
                                "   o.deleted = 0 AND " +
                                "   p.deleted = 0 AND " +
                                "   o.deviation = ?3 AND " +
                                showOpenOnlySql +
                                "   o.created_date > ?4 AND " +
                                "   o.created_date < ?5")

                        .setParameter(1, parameters.authorityId)
                        .setParameter(2, projectId)
                        .setParameter(3, i)
                        .setParameter(4, strFromDate)
                        .setParameter(5, strToDate);


                Number counterTG = (Number) queryTG.getSingleResult();
                int intCounterTG = Integer.parseInt(counterTG.toString());
                TgCounter tgCounter = new TgCounter(i, intCounterTG);

                // Count deviation overdue improvement
                //improvement_deadline
                Date today = new Date();
                String strToday = dateFormatter.format(today);
                Query queryOverdue = em.createNativeQuery("SELECT COUNT(*) FROM observation o\n " +
                                "JOIN project p ON p.project_id = o.project\n" +
                                "JOIN company_has_project chp ON chp.project_project_id = p.project_id \n " +
                                "WHERE " +
                                "chp.company_company_id = ?1 AND " +
                                " o.deleted = ?2 AND " +
                                " p.deleted = 0 AND " +
                                " o.observation_state < 1 AND " +
                                " o.deviation = ?4 AND " +
                                " o.improvement_deadline < ?5 AND " +
                                filterProjectSql +
                                " o.created_date > ?6 AND " +
                                "o.created_date < ?7")
                        .setParameter(1, parameters.authorityId)
                        .setParameter(2, false)
                        .setParameter(4, i)
                        .setParameter(5, strToday)
                        .setParameter(6, strFromDate)
                        .setParameter(7, strToDate);

                Number counterOverdue = (Number) queryOverdue.getSingleResult();
                int intCounterOverdue = Integer.parseInt(counterOverdue.toString());
                tgCounter.setOverdueCounter(intCounterOverdue);

                Query queryUnassigned = em.createNativeQuery(
                                "SELECT count(*) FROM (SELECT p.project_number," +
                                        "(SELECT count(*) FROM observation_has_user ohu\n " +
                                        "  where ohu.observation_observation_id = o.observation_id) as counter" +
                                        "  FROM observation o\n " +
                                        "JOIN project p ON p.project_id = o.project\n" +
                                        "JOIN company_has_project chp ON chp.project_project_id = p.project_id \n " +
                                        "WHERE " +
                                        "chp.company_company_id = ?1 AND " +
                                        " o.deleted = 0 AND " +
                                        " p.deleted = 0 AND " +
                                        " o.observation_state < 1 AND " +
                                        " o.deviation = ?3 AND " +
                                        filterProjectSql +
                                        " o.created_date > ?4 AND o.created_date < ?5) " +
                                        "as assigned_observations WHERE assigned_observations.counter = 0")
                        .setParameter(1, parameters.authorityId)
                        .setParameter(3, i)
                        .setParameter(4, strFromDate)
                        .setParameter(5, strToDate);

                Number counterUnassigned = (Number) queryUnassigned.getSingleResult();
                int intCounterUnassigned = Integer.parseInt(counterUnassigned.toString());
                tgCounter.setUnassignedCounter(intCounterUnassigned);
                deviationCounter.getTgCounters().add(tgCounter);

                em.close();
            }
            deviationCounter.setEntityId(projectId);
            deviationCounter.setEntityName("project");
            observationCounters.add(deviationCounter);
        }

        return observationCounters;
    }

    @PUT
    @Path("countProjectObservationsForCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<ObservationCounter> countProjectObservationsForCompany(@PathParam("companyId") String companyId, List<String> projectIds) {
        ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
        List<ObservationCounter> observationCounters = new ArrayList<>();
        for (String projectId : projectIds) {
            Project project = projectFacadeREST.find(projectId);
            List<Observation> observations;
            String createdCompanyId = "";
            if (project.getCreatedCompany() == null) {
                Company authority = project.getAuthority();
                if (authority != null) {
                    createdCompanyId = authority.getCompanyId();
                }
            } else {
                createdCompanyId = project.getCreatedCompany();
            }
            if (createdCompanyId.equalsIgnoreCase(companyId)) {
                observations = findProjectObservations(projectId);
            } else {
                observations = findProjectObservationsForCompanyNative(projectId, companyId);
            }
            List<Observation> filteredObservations = observations.stream().filter(r -> r.isDeleted() == false).collect(Collectors.toList());
            ObservationCounter observationCounter = new ObservationCounter();
            observationCounter.setCounter(filteredObservations.size());
            observationCounter.setProjectId(projectId);
            observationCounters.add(observationCounter);
        }
        return observationCounters;
    }

    public List<Observation> loadObservationsInPeriod(String fromDate,
                                                      String toDate) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        DateTime jodaFromTime = new DateTime(fromDate);
        DateTime jodaToTime = new DateTime(toDate);

        List<Observation> resultList = (List<Observation>) em.createNativeQuery(
                        "SELECT DISTINCT " +
                                "    obs.*" +
                                "FROM " +
                                "    observation obs " +
                                "        JOIN " +
                                "    project p ON obs.project = p.project_id " +
                                "        JOIN " +
                                "    company_has_project chp ON chp.project_project_id = p.project_id " +
                                "WHERE\n" +
                                "    chp.company_company_id != 'E07121A7-024A-4D0E-8B58-A064F0BC4A22' " +
                                "AND obs.created_date > ?1 " +
                                "AND obs.created_date < ?2 " +
                                "AND obs.deleted = '0' " +
                                "AND p.deleted = '0' " +
                                "AND obs.modified_user != 'F7B6EFB5-467F-46BD-A9FA-CF4CAB9D9AD9' " +
                                "AND obs.modified_user != 'klaus' ", Observation.class)
                .setParameter(1, jodaFromTime.toString())
                .setParameter(2, jodaToTime.toString())
                .getResultList();
        em.close();
        return resultList;
    }

    public int countObservationsInPeriod(String fromDate, String toDate, int deviation) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        DateTime jodaFromTime = new DateTime(fromDate);
        DateTime jodaToTime = new DateTime(toDate);

        Query query = em.createNativeQuery(
                        "SELECT count(obs.observation_id) " +
                                "FROM " +
                                "    observation obs " +
                                "        JOIN " +
                                "    project p ON obs.project = p.project_id " +
                                "WHERE\n" +
                                "    p.created_company != 'E07121A7-024A-4D0E-8B58-A064F0BC4A22' " +
                                "AND obs.created_date > ?1 " +
                                "AND obs.created_date < ?2 " +
                                "AND obs.deviation = ?3 " +
                                "AND obs.deleted = '0' " +
                                "AND p.deleted = '0' ")
                .setParameter(1, jodaFromTime.toString())
                .setParameter(2, jodaToTime.toString())
                .setParameter(3, deviation);

        Number counter = (Number) query.getSingleResult();
        int intCounter = Integer.parseInt(counter.toString());
        em.close();
        return intCounter;
    }

    @PUT
    @Path("findMissing/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> findMissing(@PathParam("projectId") String projectId, List<String> observationIds) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<String> missingIds = new ArrayList<>();
        for (String observationId : observationIds) {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM observation o " +
                            " WHERE o.observation_id = ?1 AND o.project = ?2")
                    .setParameter(1, observationId)
                    .setParameter(2, projectId);

            Number counter = (Number) query.getSingleResult();
            int intCounter = Integer.parseInt(counter.toString());

            if (intCounter == 0) {
                missingIds.add(observationId);
            }
        }
        em.close();
        return missingIds;
    }

    @GET
    @Path("resetRenew/{observationId}")
    //@Produces({MediaType.APPLICATION_JSON})
    public void resetRenew(@PathParam("observationId") String observationId) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Observation observation = findNative(observationId);
            if(observation != null) {
                if(observation.getOldProject() != null) {
                    final int i = em.createNativeQuery(
                                    "UPDATE observation SET project = ?, old_project = NULL \n" +
                                            "WHERE (observation_id = ?);"
                            ).setParameter(1, observation.getOldProject().getProjectId())
                            .setParameter(2, observationId)
                            .executeUpdate();
                }
            }
            tx.commit();
        } catch (Exception exp) {
            tx.rollback();
        } finally {
            em.close();
        }

    }
}

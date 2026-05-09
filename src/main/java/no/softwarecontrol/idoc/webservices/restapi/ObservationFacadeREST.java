/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
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
import no.softwarecontrol.idoc.data.requestparams.PopupSortMode;
import no.softwarecontrol.idoc.restclient.ObservationClient;
import no.softwarecontrol.idoc.webservices.data.requestdata.ObservationImage;
import no.softwarecontrol.idoc.webservices.exception.UnsupportedMediaException;
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

    public static ObservationFacadeREST instance;

    public ObservationFacadeREST() {
        super(Observation.class);
        instance = this;
    }

    public static ObservationFacadeREST getInstance() {
        if (instance == null) {
            instance = new ObservationFacadeREST();
        }
        return instance;
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

        Observation existingObservation = this.find(entity.getObservationId());

        if (existingObservation == null) {
            entity.setCreatedDate(new Date());

            Project project = ProjectFacadeREST.getInstance().find(projectId);
            if (project != null) {
                if (!project.getObservationList().contains(entity)) {
                    project.getObservationList().add(entity);
                }
                entity.setProject(project);

                String equipmentId = entity.getEquipmentId();
                if (equipmentId != null) {
                    Equipment equipment = EquipmentFacadeREST.getInstance().find(equipmentId);
                    if (equipment != null) {
                        entity.setEquipment(equipment);
                    } else {
                        System.out.println("ObservationFacadeRest.createWithProject: Her skjer det ein feil med equipment");
                    }
                }
                ProjectFacadeREST.getInstance().editProjectOnly(project.getProjectId(),project);
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

        List<Measurement> measurements = new ArrayList<>(entity.getMeasurementList());
        if (existingObservation == null) {
            entity.setCreatedDate(new Date());

            String equipmentId = entity.getEquipmentId();
            if (entity.getEquipment() != null) {
                equipmentId = entity.getEquipment().getEquipmentId();
            }

            // Check if location is created
            if (entity.getLocation() != null) {

                Location existingLocation = LocationFacadeREST.getInstance().find(entity.getLocation().getLocationId());
                if (existingLocation != null) {
                    entity.setLocation(existingLocation);
                } else {
                    throw new UnsupportedMediaException("Location not yet synchronized");
                }
            }

            if (equipmentId != null) {
                Equipment equipment = EquipmentFacadeREST.getInstance().findNative(equipmentId);
                if (equipment != null) {
                    entity.setEquipment(equipment);
                    if(entity.getLocation() == null && equipment.getLocation() != null) {
                        System.out.println("CREATE: Observation has lost location, but equipment has location");
                        System.out.println("---------------------------------------------------------");
                        entity.setLocation(equipment.getLocation());
                    }
                } else {
                    throw new UnsupportedMediaException("Equipment not yet synchronized");
                }
            }

            if (entity.getQuickChoiceItem() != null) {
                QuickChoiceItem quickChoiceItem = QuickChoiceItemFacadeREST.getInstance().find(entity.getQuickChoiceItem().getQuickChoiceItemId());
                if (quickChoiceItem != null) {
                    entity.setQuickChoiceItem(quickChoiceItem);
                    entity.setDeviation(quickChoiceItem.getDeviationGrade());
                }
            }


            Project project = ProjectFacadeREST.getInstance().find(projectId);
            if(project != null) {
                entity.setProject(project);
            } else {
                throw new UnsupportedMediaException("Project not yet synchronized");
            }

            //int observationCount = findProjectObservationsNative(project.getProjectId()).size();
            //entity.setObservationNo(observationCount + 1);
//            if (!project.getObservationList().contains(entity)) {
//                project.getObservationList().add(entity);
//            }
            for(ObservationLanguage observationLanguage : entity.getObservationLanguageList()) {
                observationLanguage.setObservation(entity);
            }

            entity.getMeasurementList().clear();
            entity.getImageList().clear();
            if (entity.getProject() != null) {
                super.create(entity);

                for (Measurement measurement : measurements) {
                    for(MeasurementLanguage measurementLanguage: measurement.getMeasurementLanguageList()) {
                        measurementLanguage.setMeasurement(measurement);
                    }
                    // Check if measurement already exists
                    Measurement existing = MeasurementFacadeREST.getInstance().find(measurement.getMeasurementId());
                    if (existing == null) {
                        entity.getMeasurementList().add(measurement);
                        edit(entity);
                    } else {
                        linkMeasurement(measurement.getMeasurementId(), entity);
                    }
                }

            }
        } else {
            System.out.println("Observasjonen er opprettet fra før: ID = " + entity.getObservationId());
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

    @POST
    @Path("createWithImage/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithImage(@PathParam("projectId") String projectId, ObservationImage entity) throws Exception {
        Observation observation = entity.getObservation();
        observation.getImageList().clear();
        Media media = entity.getMedia();

        createWithProject2(projectId, observation);


        ImageFacadeREST.getInstance().createWithObservation(observation.getObservationId(), media);
    }

    @PUT
    @Deprecated
    @Path("editLocation/{locationId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editLocation(@PathParam("locationId") String locationId,
                             Observation entity) {

        Observation observation = this.findNative(entity.getObservationId());
        if (observation == null) {
            //System.out.println("observation id missing = " + entity.getObservationId());
        } else {
            Location location = LocationFacadeREST.getInstance().find(locationId);
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
                QuickChoiceItem quickChoiceItem = QuickChoiceItemFacadeREST.getInstance().find(quickChoiceItemId);
                if (quickChoiceItem != null) {
                    observation.setQuickChoiceItem(quickChoiceItem);
                }
            }
            if (!equipmentId.isEmpty()) {
                Equipment equipment = EquipmentFacadeREST.getInstance().find(equipmentId);
                if (equipment != null) {
                    observation.setEquipment(equipment);
                } else {
                    if (entity.getEquipment() == null) {
                        //observation.setEquipment(null);
                    }
                }
            }
            if (!locationId.isEmpty()) {
                Location location = LocationFacadeREST.getInstance().find(locationId);
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
                throw new UnsupportedMediaException("Merkelig fenomen... Denne observasjonen eksisterer ikke");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //    @PUT
//    @Path("linkMeasurement/{companyId}")
//    @Consumes({MediaType.APPLICATION_JSON})
    public void linkMeasurement(String measurementId, Observation entity) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                Query query = em.createNativeQuery("""
                                SELECT COUNT(*) FROM observation_has_measurement 
                                WHERE observation_observation_id = ?1 AND measurement_measurement_id = ?2
                                """)
                        .setParameter(1, entity.getObservationId())
                        .setParameter(2, measurementId);

                Number counter = (Number) query.getSingleResult();
                if (counter.intValue() == 0) {
                    tx.begin();
                    final int i = em.createNativeQuery("""
                                    INSERT INTO observation_has_measurement (observation_observation_id, measurement_measurement_id)
                                    VALUES (?, ?);
                                    """)
                            .setParameter(1, entity.getObservationId())
                            .setParameter(2, measurementId)
                            .executeUpdate();
                    tx.commit();
                }
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while inserting into observation_has_measurement for observationId: " +
                        entity.getObservationId() + ", measurementId: " + measurementId + " - " + exp.getMessage());
                exp.printStackTrace(System.err);
                throw new RuntimeException("Failed to link measurement to observation", exp);
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for linkMeasurement: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
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


        Observation observation = this.find(id);
        if (observation != null) {
            for (Integration integration : entity.getIntegrationList()) {
                if (!observation.getIntegrationList().contains(integration)) {
                    observation.getIntegrationList().add(integration);
                }
            }
            for(ObservationLanguage observationLanguage : entity.getObservationLanguageList()) {
                observationLanguage.setObservation(observation);
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
            observation.setObservationLanguageList(entity.getObservationLanguageList());

            if (entity.getModifiedDate() != null) {
                observation.setModifiedDate(entity.getModifiedDate());
            } else {
                observation.setModifiedDate(new Date());
            }
            if(entity.getOldProjectId() != null) {
                Project oldProject = ProjectFacadeREST.getInstance().findNative(entity.getOldProjectId());
                observation.setOldProject(oldProject);
            }
            if (!quickChoiceItemId.isEmpty()) {
                QuickChoiceItem quickChoiceItem = QuickChoiceItemFacadeREST.getInstance().find(quickChoiceItemId);
                if (quickChoiceItem != null) {
                    observation.setQuickChoiceItem(quickChoiceItem);
                }
            }

            if (!locationId.isEmpty()) {
                Location location = LocationFacadeREST.getInstance().find(locationId);
                if (location != null) {
                    observation.setLocation(location);
                }
            }
            if (!equipmentId.isEmpty()) {
                Equipment equipment = EquipmentFacadeREST.getInstance().find(equipmentId);
                if (equipment != null) {
                    observation.setEquipment(equipment);
                    if(observation.getLocation() == null && equipment.getLocation() != null) {
                        System.out.println("EDIT: Observation has lost location, but equipment has location");
                        System.out.println("---------------------------------------------------------");
                        observation.setLocation(equipment.getLocation());
                    }
                }
            }
            try {
                super.edit(observation);

                // Add/update the measurements
                List<Measurement> measurements = new ArrayList<>(entity.getMeasurementList());
                for (Measurement measurement : measurements) {
                    final Measurement existingMeasurement = MeasurementFacadeREST.getInstance().find(measurement.getMeasurementId());
                    if (existingMeasurement != null) {
                        MeasurementFacadeREST.getInstance().editMeasurementOnly(measurement.getMeasurementId(), measurement);
                        linkMeasurement(measurement.getMeasurementId(), observation);
                    } else {
                        MeasurementFacadeREST.getInstance().createWithObservation(observation.getObservationId(), measurement);
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
                throw new UnsupportedMediaException("Merkelig fenomen... Denne observasjonen eksisterer ikke. Mulig at kontrollen ikke er ferdig opprettet");
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

        Observation observation = this.find(id);
        if (observation != null) {
            for (Integration integration : entity.getIntegrationList()) {
                if (!observation.getIntegrationList().contains(integration)) {
                    observation.getIntegrationList().add(integration);
                }
            }

            List<Measurement> measurements = new ArrayList<>(entity.getMeasurementList());
            for (Measurement measurement : measurements) {
                final Measurement existingMeasurement = MeasurementFacadeREST.getInstance().find(measurement.getMeasurementId());
                if (existingMeasurement != null) {
                    MeasurementFacadeREST.getInstance().editMeasurementOnly(measurement.getMeasurementId(), measurement);
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
            if(entity.getOldProjectId() != null) {
                Project oldProject = ProjectFacadeREST.getInstance().findNative(entity.getOldProjectId());
                observation.setOldProject(oldProject);
            }

            if (!quickChoiceItemId.isEmpty()) {
                QuickChoiceItem quickChoiceItem = QuickChoiceItemFacadeREST.getInstance().find(quickChoiceItemId);
                if (quickChoiceItem != null) {
                    observation.setQuickChoiceItem(quickChoiceItem);
                }
            }
            if (!equipmentId.isEmpty()) {
                Equipment equipment = EquipmentFacadeREST.getInstance().find(equipmentId);
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
                Location location = LocationFacadeREST.getInstance().find(locationId);
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
                throw new UnsupportedMediaException("Merkelig fenomen... Denne observasjonen eksisterer ikke");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @PUT
    @Path("linkUser/{userId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToUser(@PathParam("userId") String userId, Observation entity) {

        Observation observation = this.find(entity.getObservationId());
        User user = UserFacadeREST.getInstance().find(userId);
        if (user != null && observation != null) {
            if (!user.getObservationList().contains(observation)) {
                user.getObservationList().add(observation);
                UserFacadeREST.getInstance().edit(user);
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
        Observation observation = this.find(observationId);
        User user = UserFacadeREST.getInstance().find(userId);
        if (user != null && observation != null) {
            if (!user.getObservationList().contains(observation)) {
                user.getObservationList().add(observation);
                UserFacadeREST.getInstance().edit(user);
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
        Observation observation = this.find(entity.getObservationId());
        User user = UserFacadeREST.getInstance().find(userId);
        if (user != null && observation != null) {
            if (user.getObservationList().contains(observation)) {
                user.getObservationList().remove(observation);
                UserFacadeREST.getInstance().edit(user);
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Observation> resultList = (List<Observation>) em.createNativeQuery("""
                            SELECT * FROM observation o
                            WHERE o.observation_id = ?1
                            """,
                            Observation.class)
                    .setParameter(1, id)
                    .getResultList();

            if (!resultList.isEmpty()) {
                Observation observation = resultList.get(0);

                List<Media> mediaList = (List<Media>) em.createNativeQuery("""
                                SELECT * FROM image img
                                JOIN observation_has_image ohi ON ohi.image = img.image_id
                                WHERE ohi.observation = ?1
                                """,
                                Media.class)
                        .setParameter(1, observation.getObservationId())
                        .getResultList();

                List<Measurement> measurementList = (List<Measurement>) em.createNativeQuery("""
                                SELECT * FROM measurement meas
                                JOIN observation_has_measurement ohm ON ohm.measurement_measurement_id = meas.measurement_id
                                WHERE ohm.observation_observation_id = ?1
                                """,
                                Measurement.class)
                        .setParameter(1, observation.getObservationId())
                        .getResultList();

                observation.setImageList(mediaList);
                observation.setMeasurementList(measurementList);

                observation.setProjectNumber(observation.getProject().getProjectNumber());
                if (observation.getEquipment() != null) {
                    observation.setEquipmentString(observation.getEquipment().getFullName());
                    observation.setEquipmentTagId(observation.getEquipment().getTagId());
                    observation.setEquipmentId(observation.getEquipment().getEquipmentId());
                }
                if (observation.getLocation() != null) {
                    observation.setLocationString(observation.getLocation().getFullName());
                    observation.setLocationId(observation.getLocation().getLocationId());
                }
                if (observation.getOldProject() != null) {
                    observation.setOldProjectId(observation.getOldProject().getProjectId());
                    observation.setOldProjectNumber(observation.getOldProject().getProjectNumber());
                    observation.setOldProject(null);
                }
                return observation;
            }
            return null;
        } catch (Exception e) {
            System.out.println("Exception while finding observation by id: " + id + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to find observation", e);
        }
    }

//    @GET
//    @Override
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<Observation> findAll() {
//        return super.findAll();
//    }
//
//    @GET
//    @Path("{from}/{to}")
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<Observation> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
//        return super.findRange(new int[]{from, to});
//    }

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

        return loadByProjectOptimized(projectId);
//        List<Observation> observations = findProjectObservationsNative(projectId);
//
//        for (Observation observation : observations) {
//            if (observation.getEquipment() != null) {
//                observation.setEquipmentId(observation.getEquipment().getEquipmentId());
//            }
//            if(observation.getOldProject() != null) {
//                observation.setOldProjectId(observation.getOldProject().getProjectId());
//                observation.setOldProjectNumber(observation.getOldProject().getProjectNumber());
//                observation.setOldProject(null);
//            }
//            observation.setEquipmentId(null);
//            if (observation.getOldProject() != null) {
//                observation.setOldProjectId(observation.getOldProject().getProjectId());
//                observation.setOldProjectNumber(observation.getOldProject().getProjectNumber());
//            }
//        }
//
//        return observations;
    }

    @Deprecated
    @GET
    @Path("projectobservations2/{projectid}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> findProjectObservations2(@PathParam("projectid") String projectId) {

        List<Observation> observations = findProjectObservationsNative(projectId);

        for (Observation observation : observations) {
            if (observation.getEquipment() != null) {
                observation.setEquipmentId(observation.getEquipment().getEquipmentId());
            }
            if(observation.getOldProject() != null) {
                observation.setOldProjectId(observation.getOldProject().getProjectId());
                observation.setOldProjectNumber(observation.getOldProject().getProjectNumber());
                observation.setOldProject(null);
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
    @Path("loadTitleGroups/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<String> loadTitleGroups(@PathParam("projectId") String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Observation> resultList = (List<Observation>) em.createNativeQuery("""
                            SELECT * FROM observation o
                            WHERE o.project = ?1
                            GROUP BY o.title
                            """,
                            Observation.class)
                    .setParameter(1, projectId)
                    .getResultList();

            List<String> groups = new ArrayList<>();
            for (Observation observation : resultList) {
                if (!observation.getTitle().isEmpty()) {
                    groups.add(observation.getTitle());
                }
            }
            return groups;
        } catch (Exception e) {
            System.out.println("Exception while loading title groups for projectId: " + projectId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load title groups", e);
        }
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

    @Deprecated
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
                observation.setOldProject(null);
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
        if(observation.getOldProject() != null) {
            observation.setOldProjectId(observation.getOldProject().getProjectId());
            observation.setOldProjectNumber(observation.getOldProject().getProjectNumber());
            observation.setOldProject(null);
//            if(observation.getOldProject().getAsset() != null) {
//                observation.getOldProject().assetId = observation.getOldProject().getAsset().getAssetId();
//                observation.getOldProject().setAsset(null);
//            }
//            if(observation.getOldProject().getDisipline() != null) {
//                observation.getOldProject().disiplineId = observation.getOldProject().getDisipline().getDisiplineId();
//                observation.getOldProject().setDisipline(null);
//            }

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
        try {
            Integer state = Integer.parseInt(stateString);
            String tgsString = "(";
            for (Integer tg : tgs) {
                tgsString += Integer.toString(tg) + ",";
            }
            tgsString = tgsString.substring(0, tgsString.length() - 1);
            tgsString += ")";

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                List<Observation> resultList = (List<Observation>) em.createNativeQuery("""
                                SELECT * FROM observation obs
                                JOIN project p ON p.project_id = obs.project
                                JOIN company_has_project chp ON chp.project_project_id = p.project_id
                                WHERE obs.deleted = 0 AND chp.company_company_id = ?1 AND observation_state <= ?2 AND deviation IN
                                """ + tgsString + """ 
                                LIMIT 0,50
                                """,
                                Observation.class)
                        .setParameter(1, companyId)
                        .setParameter(2, state)
                        .getResultList();

                for (Observation observation : resultList) {
                    observation.setProjectId(observation.getProject().getProjectId());
                }
                return resultList;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid state string for companyId: " + companyId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Invalid state parameter", e);
        } catch (Exception e) {
            System.out.println("Exception while loading observations for companyId: " + companyId + ", state: " + stateString + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load observations for company", e);
        }
    }

    @GET
    @Path("loadByEquipmentWithProject/{equipmentId}/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadByEquipmentWithProject(@PathParam("equipmentId") String equipmentId,
                                                        @PathParam("projectId") String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Observation> resultList = (List<Observation>) em.createNativeQuery("""
                            SELECT * FROM observation obs
                            WHERE obs.deleted = 0 AND obs.equipment = ?1 AND obs.project = ?2
                            ORDER BY obs.created_date DESC
                            """,
                            Observation.class)
                    .setParameter(1, equipmentId)
                    .setParameter(2, projectId)
                    .getResultList();

            for (Observation observation : resultList) {
                optimizeObservation(observation);
            }
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while loading observations for equipmentId: " + equipmentId +
                    ", projectId: " + projectId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load observations by equipment and project", e);
        }
    }

    @GET
    @Path("loadByCheckListAnswerWithProject/{checkListAnswerId}/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadByCheckListAnswerWithProject(@PathParam("checkListAnswerId") String checkListAnswerId,
                                                              @PathParam("projectId") String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Observation> resultList = (List<Observation>) em.createNativeQuery("""
                            SELECT o.* FROM observation o
                            JOIN iDocDatabase.answer_value av ON o.observation_id = av.observation
                            JOIN check_list_answer cla ON av.check_list_answer = cla.check_list_answer_id
                            WHERE o.project = ?1 AND cla.check_list_answer_id = ?2 AND o.deleted = 0
                            """,
                            Observation.class)
                    .setParameter(1, projectId)
                    .setParameter(2, checkListAnswerId)
                    .getResultList();

            for (Observation observation : resultList) {
                optimizeObservation(observation);
            }
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while loading observations for checkListAnswerId: " + checkListAnswerId +
                    ", projectId: " + projectId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load observations by checklist answer and project", e);
        }
    }

    @PUT
    @Path("loadObservationsForCompany/{companyId}/{stateString}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadObservationsForCompany(@PathParam("companyId") String companyId,
                                                        @PathParam("stateString") String stateString,
                                                        @PathParam("batchOffset") String batchOffsetString,
                                                        @PathParam("batchSize") String batchSizeString,
                                                        List<Integer> tgs) {
        try {
            Integer batchOffset = Integer.parseInt(batchOffsetString);
            Integer batchSize = Integer.parseInt(batchSizeString);
            Integer state = Integer.parseInt(stateString);
            String tgsString = "(";
            for (Integer tg : tgs) {
                tgsString += Integer.toString(tg) + ",";
            }
            tgsString = tgsString.substring(0, tgsString.length() - 1);
            tgsString += ")";

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                List<Observation> resultList = (List<Observation>) em.createNativeQuery("""
                                SELECT * FROM observation obs
                                JOIN project p ON p.project_id = obs.project
                                JOIN company_has_project chp ON chp.project_project_id = p.project_id
                                WHERE obs.deleted = 0 AND chp.company_company_id = ?1 AND obs.observation_state <= ?2 AND obs.deviation IN 
                                """ + tgsString + """
                                
                                ORDER BY obs.created_date DESC
                                LIMIT ?3, ?4
                                """,
                                Observation.class)
                        .setParameter(1, companyId)
                        .setParameter(2, state)
                        .setParameter(3, batchOffset)
                        .setParameter(4, batchSize)
                        .getResultList();

                for (Observation observation : resultList) {
                    observation.setProjectId(observation.getProject().getProjectId());
                }
                return resultList;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid numeric parameter for companyId: " + companyId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Invalid numeric parameter", e);
        } catch (Exception e) {
            System.out.println("Exception while loading observations for companyId: " + companyId +
                    ", state: " + stateString + ", batchOffset: " + batchOffsetString +
                    ", batchSize: " + batchSizeString + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load observations for company", e);
        }
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
        try {
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
                showOpenOnlySql = " AND p.project_state < 7 AND obs.observation_state <= " + state + " ";
            }

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                List<Observation> resultList = (List<Observation>) em.createNativeQuery("""
                                SELECT * FROM observation obs
                                JOIN project p ON p.project_id = obs.project
                                JOIN company_has_project chp ON chp.project_project_id = p.project_id
                                WHERE obs.deleted = 0 AND p.deleted = 0 AND chp.company_company_id = ?1 AND obs.deviation IN 
                                """ + tgsString + """
                                 AND obs.created_date > ?5 AND obs.created_date < ?6
                                """ + showOpenOnlySql + """
                                
                                ORDER BY obs.created_date DESC
                                LIMIT ?3, ?4
                                """,
                                Observation.class)
                        .setParameter(1, companyId)
                        .setParameter(3, batchOffset)
                        .setParameter(4, batchSize)
                        .setParameter(5, fromDate)
                        .setParameter(6, toDate)
                        .getResultList();

                for (Observation observation : resultList) {
                    observation.setProjectId(observation.getProject().getProjectId());
                }
                return resultList;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid numeric parameter for companyId: " + companyId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Invalid numeric parameter", e);
        } catch (Exception e) {
            System.out.println("Exception while loading observations in period for companyId: " + companyId +
                    ", state: " + stateString + ", fromDate: " + fromDate + ", toDate: " + toDate + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load observations for company in period", e);
        }
    }

    @PUT
    @Path("loadObservationsForAsset/{assetId}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> loadObservationsForAsset(@PathParam("assetId") String assetId,
                                                      @PathParam("batchOffset") String strBatchOffset,
                                                      @PathParam("batchSize") String strBatchSize,
                                                      List<Integer> states) {
        try {
            int batchOffset = Integer.parseInt(strBatchOffset);
            int batchSize = Integer.parseInt(strBatchSize);
            String stateString = "(";
            for (Integer state : states) {
                stateString += Integer.toString(state) + ",";
            }
            stateString = stateString.substring(0, stateString.length() - 1);
            stateString += ")";

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                List<Observation> resultList = (List<Observation>) em.createNativeQuery("""
                                SELECT * FROM observation obs
                                JOIN project p ON p.project_id = obs.project
                                JOIN asset a ON a.asset_id = p.asset
                                WHERE obs.deleted = 0 AND a.asset_id = ?1 AND p.project_state IN 
                                """ + stateString + """
                                
                                ORDER BY obs.created_date DESC
                                LIMIT ?2, ?3
                                """,
                                Observation.class)
                        .setParameter(1, assetId)
                        .setParameter(2, batchOffset)
                        .setParameter(3, batchSize)
                        .getResultList();

                for (Observation observation : resultList) {
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
                        List<Measurement> statusMeasurements = observation.getMeasurementList().stream()
                                .filter(r -> r.getName().equalsIgnoreCase("Status"))
                                .collect(Collectors.toList());
                        if (!statusMeasurements.isEmpty()) {
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
        } catch (NumberFormatException e) {
            System.out.println("Invalid numeric parameter for assetId: " + assetId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Invalid numeric parameter", e);
        } catch (Exception e) {
            System.out.println("Exception while loading observations for assetId: " + assetId +
                    ", batchOffset: " + strBatchOffset + ", batchSize: " + strBatchSize + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load observations for asset", e);
        }
    }

    public List<Observation> findProjectObservationsForCompanyNative(String projectId, String companyId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Observation> resultList = (List<Observation>) em.createNativeQuery("""
                            SELECT * FROM observation o
                            JOIN observation_has_user ohu ON observation_id = ohu.observation_observation_id
                            JOIN user u ON ohu.user_user_id = u.user_id
                            JOIN company_has_user chu ON u.user_id = chu.user
                            JOIN company c ON chu.company = c.company_id
                            WHERE o.project = ?1 AND c.company_id = ?2
                            """,
                            Observation.class)
                    .setParameter(1, projectId)
                    .setParameter(2, companyId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while finding project observations for projectId: " + projectId +
                    ", companyId: " + companyId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to find project observations for company", e);
        }
    }

    @GET
    @Path("findProjectObservationsForUser/{projectId}/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> findProjectObservationsForUser(@PathParam("projectId") String projectId, @PathParam("userId") String userId) {
        try {
            List<Observation> resultList;

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                resultList = (List<Observation>) em.createNativeQuery("""
                                SELECT * FROM observation o
                                JOIN observation_has_user ohu ON observation_id = ohu.observation_observation_id
                                JOIN user u ON ohu.user_user_id = u.user_id
                                WHERE o.project = ?1 AND u.user_id = ?2
                                """,
                                Observation.class)
                        .setParameter(1, projectId)
                        .setParameter(2, userId)
                        .getResultList();
            }


            List<UserRole> userRoles = UserRoleFacadeREST.getInstance().loadByProject(projectId, userId);
            if (!userRoles.isEmpty()) {
                UserRole userRole = userRoles.get(0);
                if (userRole.getRole().getRoleType().contains("_RESTRICTED")) {
                    if (userRole.getParameter() != null) {
                        if (!userRole.getParameter().isEmpty()) {
                            List<Observation> observations = findProjectObservations(projectId);
                            UserRoleParameter userRoleParameter = UserRoleParameter.fromJsonString(userRole.getParameter());
                            List<Observation> filtered = observations
                                    .stream()
                                    .filter(r -> userRoleParameter.tgList.contains(r.getDeviation()))
                                    .collect(Collectors.toList());
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
        } catch (Exception e) {
            System.out.println("Exception while finding project observations for user, projectId: " + projectId +
                    ", userId: " + userId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to find project observations for user", e);
        }
    }

    public List<Observation> findProjectObservationsNative(String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Observation> resultList = em.createQuery("""
                            SELECT o FROM Observation o
                            LEFT JOIN FETCH o.location
                            LEFT JOIN FETCH o.equipment
                            LEFT JOIN FETCH o.project
                            WHERE o.project.projectId = :projectId OR o.oldProject.projectId = :projectId
                            """,
                            Observation.class)
                    .setParameter("projectId", projectId)
                    .getResultList();

            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while finding project observations for projectId: " + projectId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to find project observations", e);
        }
    }

    public Integer countProjectObservationsNative(String projectId, List<Integer> tgs) {
        try {
            String tgsString = "(";
            for (Integer tg : tgs) {
                tgsString += Integer.toString(tg) + ",";
            }
            tgsString = tgsString.substring(0, tgsString.length() - 1);
            tgsString += ")";

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                Long counter = (Long) em.createNativeQuery("""
                                SELECT COUNT(o.observation_id) FROM observation o
                                WHERE (o.project = ?1 OR o.old_project = ?1) AND o.deviation IN 
                                """ + tgsString + """
                                 AND o.deleted = 0
                                """)
                        .setParameter(1, projectId)
                        .getSingleResult();

                return counter.intValue();
            }
        } catch (Exception e) {
            System.out.println("Exception while counting project observations for projectId: " + projectId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to count project observations", e);
        }
    }

    public List<Observation> findEquipmentObservationsNative(String equipmentId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Observation> resultList = (List<Observation>) em.createNativeQuery("""
                            SELECT * FROM observation o
                            WHERE o.equipment = ?1
                            """,
                            Observation.class)
                    .setParameter(1, equipmentId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while finding equipment observations for equipmentId: " + equipmentId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to find equipment observations", e);
        }
    }


    @GET
    @Path("findProjectObservations/{projectid}/{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Observation> findProjectObservations(@PathParam("projectid") String projectId, @PathParam("from") Integer from, @PathParam("to") Integer to) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Observation> resultList = (List<Observation>) em.createNativeQuery("""
                            SELECT * FROM observation o
                            WHERE o.project = ?1
                            """,
                            Observation.class)
                    .setParameter(1, projectId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while finding project observations for projectId: " + projectId +
                    ", from: " + from + ", to: " + to + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to find project observations", e);
        }
    }

    @Deprecated
    @GET
    @Path("countprojectobservation/{projectid}")
    //@Produces({MediaType.APPLICATION_JSON})
    @Produces(MediaType.TEXT_PLAIN)
    public String countProjectObservations(@PathParam("projectid") String projectId) {

        Project project = ProjectFacadeREST.getInstance().find(projectId);
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            Query query = em.createNativeQuery("""
                            SELECT COUNT(*) FROM observation o
                            WHERE o.project = ?1 AND o.deleted = ?2
                            """)
                    .setParameter(1, projectId)
                    .setParameter(2, false);

            Number counter = (Number) query.getSingleResult();
            int intCounter = Integer.parseInt(counter.toString());

            ObservationCounter observationCounter = new ObservationCounter();
            observationCounter.setCounter(intCounter);
            observationCounter.setProjectId(projectId);

            return observationCounter;
        } catch (Exception e) {
            System.out.println("Exception while counting single project observations for projectId: " + projectId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to count single project observations", e);
        }
    }

    private String createSqlString(ObservationRequestParameters parameters, Boolean isCounting) {
        if(parameters.entityIds.isEmpty()) {
            parameters.entityIds.add("1");
        }
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
            selectString += "LEFT JOIN location l5 on l4.parent = l5.location_id\n";
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
        whereString += "(o.project = " + projectId + " or o.old_project = " + projectId + ") AND o.deleted = " + deletedFlag + " \n";
        if(parameters.showOpenOnly && !isCounting) {
            whereString += " AND o.observation_state = 0 \n";
        }
        if(parameters.showImprovingOnly && !isCounting) {
            whereString += " AND o.observation_state = 1 \n";
        }
        if(parameters.showOverdue && !isCounting) {
            Date today = new Date();
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            String todayString = dateFormatter.format(today);
            whereString += " AND o.observation_state = 0 AND o.improvement_deadline < '" + todayString + "' \n";
        }
//        if(isCounting) {
//            whereString += " AND o.deleted = 0 \n";
//        }
        if(!parameters.queryTitle.isEmpty() && !isCounting) {
            whereString += "AND o.title like '" + parameters.queryTitle+ "' AND o.deviation > 0\n";
        }
        if(!parameters.queryString.isEmpty() && !isCounting) {
            String query = parameters.queryString;
            whereString += " AND \n";
            whereString += " (concat(o.description, o.action, o.improvement) like '%" + query +"%'\n";
            whereString += " OR concat(COALESCE(l4.name,''), COALESCE(l3.name,''), COALESCE(l2.name,''),COALESCE(l1.name,'')) like '%" + query +"%'\n";
            whereString += " OR concat(COALESCE(e.name,''), COALESCE(e.tag_id,'')) like '%" + query +"%'\n";
            whereString += " OR concat(COALESCE(et.name,'')) like '%" + query +"%'\n)";
        }
        var limitString = "";
        if(!parameters.queryString.isEmpty() && !isCounting) {
            limitString = " LIMIT 0,10 \n";
        }
        var orderByString = " ORDER BY observation_no \n";
        if(parameters.sortMode.sortMode == PopupSortMode.SortMode.OBSERVATION_LOCATION && !isCounting) {
            orderByString = " ORDER BY l5.name, l4.name, l3.name, l2.name, l1.name \n";
        } else if (parameters.sortMode.sortMode == PopupSortMode.SortMode.OBSERVATION_EQUIPMENT && !isCounting) {
            orderByString = " ORDER BY et.name, e.name, e.tag_id \n";
        }
        if(isCounting) {
            limitString = " LIMIT 0,10 \n";
        } else {
            if (parameters.batchSize > 0) {
                limitString = " LIMIT " + parameters.batchOffset + "," + parameters.batchSize + " ";
            }
        }
        sqlString = selectString + whereString + orderByString + limitString;
        return sqlString;
    }

    @PUT
    @Path("countObservations")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Integer countObservations(ObservationRequestParameters parameters) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            String sql = createSqlString(parameters, true);
            Query queryCounter = em.createNativeQuery(sql);

            Number counterUnassigned = (Number) queryCounter.getSingleResult();
            Integer integerCounter = Integer.parseInt(counterUnassigned.toString());

            return integerCounter;
        } catch (Exception e) {
            System.out.println("Exception while counting observations with parameters: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to count observations", e);
        }
    }

    @PUT
    @Path("loadProjectObservations")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)

    public List<Observation> loadProjectObservations(ObservationRequestParameters parameters) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            String queryString = createSqlString(parameters, false);
            List<Observation> resultList = em.createNativeQuery(queryString, Observation.class)
                    .getResultList();

            for (Observation observation : resultList) {
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
                if (observation.getOldProject() != null) {
                    observation.setOldProjectId(observation.getOldProject().getProjectId());
                    observation.setOldProjectNumber(observation.getOldProject().getProjectNumber());
                }
                if (observation.getQuickChoiceItem() != null) {
                    observation.setQuickChoiceItem(null);
                }
                if (!observation.getMeasurementList().isEmpty()) {
                    List<Measurement> activeMeasurements = observation.getMeasurementList().stream()
                            .filter(r -> !r.getDeleted())
                            .collect(Collectors.toList());
                    observation.setMeasurementCount(activeMeasurements.size());

                    List<Measurement> statusMeasurements = observation.getMeasurementList().stream()
                            .filter(r -> r.getName().equalsIgnoreCase("Status"))
                            .collect(Collectors.toList());
                    if (!statusMeasurements.isEmpty()) {
                        observation.getMeasurementList().clear();
                        observation.getMeasurementList().addAll(statusMeasurements);
                        observation.setMeasurementStatusId(statusMeasurements.get(0).getMeasurementId());
                        observation.setMeasurementStatusString(statusMeasurements.get(0).getStringValue());
                    } else {
                        observation.getMeasurementList().clear();
                    }
                }
                if (!observation.getImageList().isEmpty()) {
                    List<Media> activeImages = observation.getImageList().stream()
                            .filter(r -> !r.isDeleted())
                            .collect(Collectors.toList());
                    observation.setImageCount(activeImages.size());
                }
            }
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while loading project observations: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load project observations", e);
        }
    }

    @PUT
    @Path("loadProjectObservationsOptimized")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)

    public List<Observation> loadProjectObservationsOptimized(ObservationRequestParameters parameters) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
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
                if(observation.getOldProject() != null) {
                    observation.setOldProjectId(observation.getOldProject().getProjectId());
                    observation.setOldProjectNumber(observation.getOldProject().getProjectNumber());
                }
                if(observation.getQuickChoiceItem() != null) {
                    observation.setQuickChoiceItem(null);
                }
                if(!observation.getMeasurementList().isEmpty()) {
                    List<Measurement> activeMeasurements = observation.getMeasurementList().stream().filter(r -> !r.getDeleted()).collect(Collectors.toList());
                    observation.setMeasurementCount(activeMeasurements.size());

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
                if(!observation.getImageList().isEmpty()) {
                    List<Media> activeImages = observation.getImageList().stream().filter(r -> !r.isDeleted()).collect(Collectors.toList());
                    observation.setImageCount(activeImages.size());
                }
                if(observation.getOldProject() != null) {
                    observation.setOldProjectId(observation.getOldProject().getProjectId());
                    observation.setOldProjectNumber(observation.getOldProject().getProjectNumber());
                    observation.setOldProject(null);
                }
            }
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while loading optimized project observations: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load project observations", e);
        }
    }



    @PUT
    @Path("countProjectObservations/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<ObservationCounter> countProjectObservations(List<String> projectIds) {
        List<ObservationCounter> observationCounters = new ArrayList<>();
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
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
            return observationCounters;
        } catch (Exception e) {
            System.out.println("Exception while counting project observations: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to count project observations", e);
        }
    }

/*    private Query createCounterSqlQuery(EntityManager em) {
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
    }*/

    @PUT
    @Path("countCompanyDeviations")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)

    public List<DeviationCounter> countCompanyDeviations(ObservationRequestParameters parameters) {
        List<DeviationCounter> observationCounters = new ArrayList<>();
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
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
            return observationCounters;
        } catch (Exception e) {
            System.out.println("Exception while counting company deviations: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to count company deviations", e);
        }
    }
    @PUT
    @Path("countAssetDeviations")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)

    public List<DeviationCounter> countAssetDeviations(ObservationRequestParameters parameters) {
        List<DeviationCounter> observationCounters = new ArrayList<>();
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
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
                }
                deviationCounter.setEntityId(assetId);
                deviationCounter.setEntityName("asset");
                observationCounters.add(deviationCounter);
            }
            return observationCounters;
        } catch (Exception e) {
            System.out.println("Exception while counting asset deviations: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to count asset deviations", e);
        }
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            for (String projectId : parameters.entityIds) {
                DeviationCounter deviationCounter = new DeviationCounter();
                for (int i = 0; i < 4; i++) {
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
                }
                deviationCounter.setEntityId(projectId);
                deviationCounter.setEntityName("project");
                observationCounters.add(deviationCounter);
            }
            return observationCounters;
        } catch (Exception e) {
            System.out.println("Exception while counting project deviations: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to count project deviations", e);
        }
    }

    @PUT
    @Path("countProjectObservationsForCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<ObservationCounter> countProjectObservationsForCompany(@PathParam("companyId") String companyId, List<String> projectIds) {
        List<ObservationCounter> observationCounters = new ArrayList<>();
        for (String projectId : projectIds) {
            Project project = ProjectFacadeREST.getInstance().find(projectId);
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


    public int countObservationsInPeriod(String fromDate, String toDate, int deviation) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
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
            return intCounter;
        } catch (Exception e) {
            System.out.println("Exception while counting observations in period: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to count observations in period", e);
        }
    }

    @PUT
    @Path("findMissing/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)

    public List<String> findMissing(@PathParam("projectId") String projectId, List<String> observationIds) {
        List<String> missingIds = new ArrayList<>();
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
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
            return missingIds;
        } catch (Exception e) {
            System.out.println("Exception while finding missing observations for project " + projectId + ": " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to find missing observations", e);
        }
    }

    @GET
    @Path("resetRenew/{observationId}/{oldProjectId}")
    //@Produces({MediaType.APPLICATION_JSON})

    public void resetRenew(@PathParam("observationId") String observationId, @PathParam("oldProjectId") String oldProjectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Observation observation = findNative(observationId);
                if(observation != null) {
                    if(observation.getOldProjectId() != null) {
                        Project oldProject = ProjectFacadeREST.getInstance().findNative(oldProjectId);
                        if (oldProject != null) {
                            final int i = em.createNativeQuery(
                                            "UPDATE observation SET project = ?, old_project = NULL \n" +
                                                    "WHERE (observation_id = ?);"
                                    ).setParameter(1, oldProject.getProjectId())
                                    .setParameter(2, observationId)
                                    .executeUpdate();
                        }
                    }
                }
                tx.commit();
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while resetting renew for observation " + observationId + ": " + exp.getMessage());
                exp.printStackTrace(System.err);
                throw new RuntimeException("Failed to reset renew for observation", exp);
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for resetRenew: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
        }
    }
}

package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Measurement;
import no.softwarecontrol.idoc.data.entityobject.MeasurementLanguage;
import no.softwarecontrol.idoc.data.entityobject.Observation;
import no.softwarecontrol.idoc.data.entityobject.QuickChoiceItem;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.List;

/**
 * Created by ovesteinsland on 28/05/2017.
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.measurement")
@RolesAllowed({"ApplicationRole"})
public class MeasurementFacadeREST extends AbstractFacade<Measurement> {

    public MeasurementFacadeREST() {
        super(Measurement.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return "Measurement.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Measurement entity) {
        // Check if the measurement exists
        Measurement existing = find(entity.getMeasurementId());
        if (existing == null) {
            super.create(entity);

        }
    }

    @POST
    @Path("createListWithObservationId/{observationId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createListWithObservationId(@PathParam("observationId") String observationId, List<Measurement> entities) {
        // Kjør hele batchen i én transaksjon for bedre determinisme/feilsøking
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            for (Measurement measurement : entities) {
                // 1) Sørg for at measurement finnes (samme logikk som før)
                Number mCount = (Number) em.createNativeQuery(
                                "SELECT COUNT(*) FROM measurement WHERE measurement_id = ?1")
                        .setParameter(1, measurement.getMeasurementId())
                        .getSingleResult();
                if (mCount.intValue() == 0) {
                    Query q = em.createNativeQuery(
                            "INSERT INTO measurement (" +
                                    "measurement_id," +
                                    "name, " +
                                    "unit, " +
                                    "value, " +
                                    "deviation_grade, " +
                                    "sort_index ," +
                                    "deleted, " +
                                    "variable_name, " +
                                    "java_script, " +
                                    "value_type, " +
                                    "value_object, " +
                                    "value_decimal_count, " +
                                    "value_format, " +
                                    "deviation_grade_observation, " +
                                    "trigger_script, " +
                                    "layout_type, " +
                                    "value_default, " +
                                    "display_type, " +
                                    "text_align, " +
                                    "description, " +
                                    "title_group, " +
                                    "measurement_group, " +
                                    "thermal_identity, " +
                                    "hidden, " +
                                    "fixed) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                    q.setParameter(1, measurement.getMeasurementId());
                    q.setParameter(2, measurement.getName());
                    q.setParameter(3, measurement.getUnit());
                    q.setParameter(4, measurement.getNumberValue() != null ? measurement.getNumberValue() : null);
                    q.setParameter(5, measurement.getDeviationGrade());
                    q.setParameter(6, measurement.getSortIndex());
                    q.setParameter(7, measurement.getDeleted());
                    q.setParameter(8, measurement.getVariableName());
                    q.setParameter(9, measurement.getJavaScript());
                    q.setParameter(10, measurement.getValueType());
                    q.setParameter(11, measurement.getStringValue());
                    q.setParameter(12, measurement.getValueDecimalCount());
                    q.setParameter(13, measurement.getValueFormat());
                    q.setParameter(14, measurement.getDeviationGradeObservation());
                    q.setParameter(15, measurement.getTriggerScript());
                    q.setParameter(16, measurement.getLayoutType());
                    q.setParameter(17, measurement.getValueDefault());
                    q.setParameter(18, measurement.getDisplayType());
                    q.setParameter(19, measurement.getTextAlign() != null ? measurement.getTextAlign().getValue() : null);
                    q.setParameter(20, measurement.getDescription());
                    q.setParameter(21, measurement.getTitleGroup());
                    q.setParameter(22, measurement.getMeasurementGroup());
                    q.setParameter(23, measurement.getThermalIdentity());
                    q.setParameter(24, measurement.getHidden());
                    q.setParameter(25, measurement.getFixed());
                    q.executeUpdate();
                }

                // 2) Link idempotent: sjekk om finnes først, med en liten delay
                try {
                    Thread.sleep(50); // vent 50 ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // gjenopprett interrupt-flagget
                }
                Number linkCount = (Number) em.createNativeQuery(
                                "SELECT COUNT(*) FROM observation_has_measurement WHERE observation_observation_id = ?1 AND measurement_measurement_id = ?2")
                        .setParameter(1, observationId)
                        .setParameter(2, measurement.getMeasurementId())
                        .getSingleResult();
                if (linkCount.intValue() == 0) {
                    em.createNativeQuery(
                            "INSERT INTO observation_has_measurement (observation_observation_id, measurement_measurement_id) " +
                                    "VALUES (?1, ?2) " +
                                    "ON DUPLICATE KEY UPDATE measurement_measurement_id = measurement_measurement_id")
                            .setParameter(1, observationId)
                            .setParameter(2, measurement.getMeasurementId())
                            .executeUpdate();
                } else {
                    System.out.println("Link finnes allerede for observation=" + observationId + " og measurement=" + measurement.getMeasurementId());
                }

                // 3) Verifiser umiddelbart (diagnostikk)
                Number verify = (Number) em.createNativeQuery(
                                "SELECT COUNT(*) FROM observation_has_measurement WHERE observation_observation_id = ?1 AND measurement_measurement_id = ?2")
                        .setParameter(1, observationId)
                        .setParameter(2, measurement.getMeasurementId())
                        .getSingleResult();
                if (verify.intValue() == 0) {
                    System.out.println("ADVARSEL: Link mangler ETTER insert for observation=" + observationId + ", measurement=" + measurement.getMeasurementId());
                } else {
                    System.out.println("Link opprettet for observation=" + observationId + ", measurement=" + measurement.getMeasurementId());
                }

                // 4) Insert all measurement_language
                insertMeasurementLanguages(em,measurement);
            }

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            System.out.println("Feil i batch createListWithObservationId: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    private void insertMeasurementLanguages(EntityManager em, Measurement measurement) {
        if (measurement == null || measurement.getMeasurementId() == null) {
            return;
        }
        if (measurement.getMeasurementLanguageList() == null || measurement.getMeasurementLanguageList().isEmpty()) {
            return;
        }

        for (MeasurementLanguage ml : measurement.getMeasurementLanguageList()) {
            if (ml == null || ml.getMeasurementLanguageId() == null) {
                continue;
            }

            // Sjekk idempotent: finnes allerede?
            Number exists = (Number) em.createNativeQuery(
                            "SELECT COUNT(*) FROM measurement_language WHERE measurement_language_id = ?1")
                    .setParameter(1, ml.getMeasurementLanguageId())
                    .getSingleResult();

            if (exists.intValue() == 0) {
                em.createNativeQuery(
                                "INSERT INTO measurement_language (" +
                                        "measurement_language_id, " +   // 1
                                        "measurement, " +               // 2 (FK til measurement.measurement_id)
                                        "unit, " +                      // 3
                                        "name, " +                      // 4
                                        "description, " +               // 5
                                        "language_code, " +             // 6
                                        "title_group, " +               // 7
                                        "measurement_group," +           // 8
                                        "value_default" +
                                        ") VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)")
                        .setParameter(1, ml.getMeasurementLanguageId())
                        .setParameter(2, measurement.getMeasurementId())
                        .setParameter(3, ml.getUnit())
                        .setParameter(4, ml.getName())
                        .setParameter(5, ml.getDescription())
                        .setParameter(6, ml.getLanguageCode())
                        .setParameter(7, ml.getTitleGroup())
                        .setParameter(8, ml.getMeasurementGroup())
                        .setParameter(9, ml.getValueDefault())
                        .executeUpdate();
            }
        }
    }



//    @POST
//    @Path("createListWithObservationId/{observationId}")
//    @Consumes({MediaType.APPLICATION_JSON})
//    public void createListWithObservationId(@PathParam("observationId") String observationId, List<Measurement> entities) {
//        // Check if the measurement exists
//        //ObservationFacadeREST observationFacadeREST = new ObservationFacadeREST();
//        //Observation observation = observationFacadeREST.findNative(observationId);
//
//        StringBuilder measurementIds = new StringBuilder("(");
//        for (Measurement measurement : entities) {
//            createWithObservation(observationId, measurement);
//            measurementIds.append("'");
//            measurementIds.append(measurement.getMeasurementId()).append("',");
//            try {
//                Thread.sleep(200); // vent 50 ms
//            } catch (InterruptedException ie) {
//                Thread.currentThread().interrupt(); // gjenopprett interrupt-flagget
//            }
//
//        }
//        measurementIds.append(")");
//        System.out.println("measurementIds = " + measurementIds);
//
//        System.out.println("createListWithObservationId for observationId = " + observationId);
//
//    }

    @POST
    @Path("createWithObservation/{observationId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithObservation(@PathParam("observationId") String observationId, Measurement entity) {
        //super.create(entity);
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Query query = em.createNativeQuery(
                    "INSERT INTO measurement (" +
                            "measurement_id," +
                            "name, " +
                            "unit, " +
                            "value, " +
                            "deviation_grade, " +
                            "sort_index ," +
                            "deleted, " +
                            "variable_name, " +
                            "java_script, " +
                            "value_type, " +
                            "value_object, " +
                            "value_decimal_count, " +
                            "value_format, " +
                            "deviation_grade_observation, " +
                            "trigger_script, " +
                            "layout_type, " +
                            "value_default, " +
                            "display_type, " +
                            "text_align, " +
                            "description, " +
                            "title_group, " +
                            "measurement_group, " +
                            "thermal_identity, " +
                            "hidden, " +
                            "fixed) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
            );

            query.setParameter(1, entity.getMeasurementId());
            query.setParameter(2, entity.getName());
            query.setParameter(3, entity.getUnit());
            if (entity.getNumberValue() != null) {
                query.setParameter(4, entity.getNumberValue());
            } else {
                query.setParameter(4, null);
            }
            query.setParameter(5, entity.getDeviationGrade());
            query.setParameter(6, entity.getSortIndex());
            query.setParameter(7, entity.getDeleted());
            query.setParameter(8, entity.getVariableName());
            query.setParameter(9, entity.getJavaScript());
            query.setParameter(10, entity.getValueType());
            query.setParameter(11, entity.getStringValue());
            query.setParameter(12, entity.getValueDecimalCount());
            query.setParameter(13, entity.getValueFormat());
            query.setParameter(14, entity.getDeviationGradeObservation());
            query.setParameter(15, entity.getTriggerScript());
            query.setParameter(16, entity.getLayoutType());
            query.setParameter(17, entity.getValueDefault());
            query.setParameter(18, entity.getDisplayType());
            if (entity.getTextAlign() != null) {
                query.setParameter(19, entity.getTextAlign().getValue());
            } else {
                query.setParameter(19, null);
            }
            query.setParameter(20, entity.getDescription());
            query.setParameter(21, entity.getTitleGroup());
            query.setParameter(22, entity.getMeasurementGroup());
            query.setParameter(23, entity.getThermalIdentity());
            if (entity.getHidden() != null) {
                query.setParameter(24, entity.getHidden());
            } else {
                query.setParameter(24, null);
            }
            if (entity.getFixed() != null) {
                query.setParameter(25, entity.getFixed());
            } else {
                query.setParameter(25, null);
            }
            final int i = query.executeUpdate();

            //tx.begin();
            final int linkInsertCount = em.createNativeQuery(
                            "INSERT INTO observation_has_measurement (observation_observation_id, measurement_measurement_id)\n" +
                                    "VALUES (?, ?);"
                    ).setParameter(1, observationId)
                    .setParameter(2, entity.getMeasurementId())
                    .executeUpdate();
            if (linkInsertCount > 0) {
                System.out.println("Insert LINK with observation SUCCEEDED:" + linkInsertCount + " rows inserted");
            } else {
                System.out.println("Insert measurement: " + entity.getMeasurementId() + "LINK with observation FAILED:" + linkInsertCount + " rows inserted");
            }

            // 3) Verifiser umiddelbart (diagnostikk)
            Number verify = (Number) em.createNativeQuery(
                            "SELECT COUNT(*) FROM observation_has_measurement WHERE observation_observation_id = ?1 AND measurement_measurement_id = ?2")
                    .setParameter(1, observationId)
                    .setParameter(2, entity.getMeasurementId())
                    .getSingleResult();
            if (verify.intValue() == 0) {
                System.out.println("ADVARSEL: Link mangler ETTER insert for observation=" + observationId + ", measurement=" + entity.getMeasurementId());
            }
            tx.commit();

            // Now we need to create the measurement_languages
            for (MeasurementLanguage measurementLanguage : entity.getMeasurementLanguageList()) {
                measurementLanguage.setMeasurement(entity);
                MeasurementLanguageFacadeREST measurementLanguageFacadeREST = new MeasurementLanguageFacadeREST();
                measurementLanguageFacadeREST.create(measurementLanguage);
            }
            //System.out.println(entity.getSortIndex() + ": Successfully created measurement: " + entity.getName() + " (" + entity.getUnit() + ")");
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into measurement: " + exp.getMessage());
        } finally {

            em.close();
        }
    }

//    @POST
//    @Path("createWithObservation/{observationId}")
//    @Consumes({MediaType.APPLICATION_JSON})
//    public void createWithObservation(@PathParam("observationId") String observationId, Measurement entity) {
//        EntityManager em = LocalEntityManagerFactory.createEntityManager();
//        EntityTransaction tx = em.getTransaction();
//        try {
//            tx.begin();
//
//            // 1) Valider/trim verdier som kan være for lange (unngå constraint-feil som ruller alt tilbake)
//            if (entity.getStringValue() != null && entity.getStringValue().length() > 250) {
//                entity.setStringValue(entity.getStringValue().substring(0, 250));
//            }
//
//            // 2) Sjekk om measurement allerede finnes (idempotent)
//            Number mCount = (Number) em.createNativeQuery(
//                            "SELECT COUNT(*) FROM measurement WHERE measurement_id = ?1")
//                    .setParameter(1, entity.getMeasurementId())
//                    .getSingleResult();
//
//            if (mCount.intValue() == 0) {
//                Query insertMeasurement = em.createNativeQuery(
//                        "INSERT INTO measurement (" +
//                                "measurement_id," +
//                                "name, " +
//                                "unit, " +
//                                "value, " +
//                                "deviation_grade, " +
//                                "sort_index ," +
//                                "deleted, " +
//                                "variable_name, " +
//                                "java_script, " +
//                                "value_type, " +
//                                "value_object, " +
//                                "value_decimal_count, " +
//                                "value_format, " +
//                                "deviation_grade_observation, " +
//                                "trigger_script, " +
//                                "layout_type, " +
//                                "value_default, " +
//                                "display_type, " +
//                                "text_align, " +
//                                "description, " +
//                                "title_group, " +
//                                "measurement_group, " +
//                                "thermal_identity, " +
//                                "hidden, " +
//                                "fixed) " +
//                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
//                );
//
//                insertMeasurement.setParameter(1, entity.getMeasurementId());
//                insertMeasurement.setParameter(2, entity.getName());
//                insertMeasurement.setParameter(3, entity.getUnit());
//                insertMeasurement.setParameter(4, entity.getNumberValue() != null ? entity.getNumberValue() : null);
//                insertMeasurement.setParameter(5, entity.getDeviationGrade());
//                insertMeasurement.setParameter(6, entity.getSortIndex());
//                insertMeasurement.setParameter(7, entity.getDeleted());
//                insertMeasurement.setParameter(8, entity.getVariableName());
//                insertMeasurement.setParameter(9, entity.getJavaScript());
//                insertMeasurement.setParameter(10, entity.getValueType());
//                insertMeasurement.setParameter(11, entity.getStringValue());
//                insertMeasurement.setParameter(12, entity.getValueDecimalCount());
//                insertMeasurement.setParameter(13, entity.getValueFormat());
//                insertMeasurement.setParameter(14, entity.getDeviationGradeObservation());
//                insertMeasurement.setParameter(15, entity.getTriggerScript());
//                insertMeasurement.setParameter(16, entity.getLayoutType());
//                insertMeasurement.setParameter(17, entity.getValueDefault());
//                insertMeasurement.setParameter(18, entity.getDisplayType());
//                insertMeasurement.setParameter(19, entity.getTextAlign() != null ? entity.getTextAlign().getValue() : null);
//                insertMeasurement.setParameter(20, entity.getDescription());
//                insertMeasurement.setParameter(21, entity.getTitleGroup());
//                insertMeasurement.setParameter(22, entity.getMeasurementGroup());
//                insertMeasurement.setParameter(23, entity.getThermalIdentity());
//                insertMeasurement.setParameter(24, entity.getHidden() != null ? entity.getHidden() : null);
//                insertMeasurement.setParameter(25, entity.getFixed() != null ? entity.getFixed() : null);
//
//                insertMeasurement.executeUpdate();
//            } // else: finnes allerede, gå videre til linking
//
//            // 3) Link – idempotent: sjekk først om link finnes
//            Number linkCount = (Number) em.createNativeQuery(
//                            "SELECT COUNT(*) FROM observation_has_measurement " +
//                                    "WHERE observation_observation_id = ?1 AND measurement_measurement_id = ?2")
//                    .setParameter(1, observationId)
//                    .setParameter(2, entity.getMeasurementId())
//                    .getSingleResult();
//
//            if (linkCount.intValue() == 0) {
//                em.createNativeQuery(
//                                "INSERT INTO observation_has_measurement (observation_observation_id, measurement_measurement_id) " +
//                                        "VALUES (?1, ?2)")
//                        .setParameter(1, observationId)
//                        .setParameter(2, entity.getMeasurementId())
//                        .executeUpdate();
//            } // else: link finnes allerede – det er OK/idempotent
//
//            tx.commit();
//
//            // 4) Lagre språk etterpå (egen transaksjon i facade-kallene)
//            for (MeasurementLanguage measurementLanguage : entity.getMeasurementLanguageList()) {
//                measurementLanguage.setMeasurement(entity);
//                MeasurementLanguageFacadeREST measurementLanguageFacadeREST = new MeasurementLanguageFacadeREST();
//                measurementLanguageFacadeREST.create(measurementLanguage);
//            }
//        } catch (Exception exp) {
//            try {
//                if (tx.isActive()) {
//                    tx.rollback();
//                }
//            } catch (Exception ignore) {}
//            // For bedre feilsøk: logg mer informasjon (SQLState hvis tilgjengelig)
//            System.out.println("Exception while inserting measurement/link: " + exp.getMessage());
//        } finally {
//            em.close();
//        }
//    }


    @POST
    @Path("createWithEquipment/{equipmentId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithEquipment(@PathParam("equipmentId") String equipmentId, Measurement entity) {
        super.create(entity);
        EquipmentFacadeREST equipmentFacadeREST = new EquipmentFacadeREST();
        //String measurementId, String equipmentId
        equipmentFacadeREST.linkMeasurement(entity.getMeasurementId(), equipmentId);
    }

    @POST
    @Path("createWithQuickChoiceItem/{quickChoiceItemId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithQuickChoiceItem(@PathParam("quickChoiceItemId") String quickChoiceItemId, Measurement entity) {
        super.create(entity);

        QuickChoiceItemFacadeREST quickChoiceItemFacadeREST = new QuickChoiceItemFacadeREST();
        QuickChoiceItem quickChoiceItem = quickChoiceItemFacadeREST.find(quickChoiceItemId);
        if (!quickChoiceItem.getMeasurementList().contains(entity)) {
            quickChoiceItem.getMeasurementList().add(entity);
        }
        if (!entity.getQuickChoiceItemList().contains(quickChoiceItem)) {
            entity.getQuickChoiceItemList().add(quickChoiceItem);
        }
        quickChoiceItemFacadeREST.edit(quickChoiceItem);
        edit(entity);
    }


    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(Measurement entity) {
        super.edit(entity);

    }

    @PUT
    @Path("editMeasurementOnly/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editMeasurementOnly(@PathParam("id") String id, Measurement entity) {
        //String DATE_FORMAT_STRING = "yyyy-MM-dd'T'hh:mm:ss'Z'";
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING);
        Measurement measurement = this.find(id);
        if (measurement != null) {
            measurement.setName(entity.getName());
            measurement.setDescription(entity.getDescription());
            measurement.setSortIndex(entity.getSortIndex());
            measurement.setDeleted(entity.getDeleted());
            measurement.setDeviationGrade(entity.getDeviationGrade());
            measurement.setJavaScript(entity.getJavaScript());
            measurement.setUnit(entity.getUnit());
            if (entity.getNumberValue() != null) {
                measurement.setNumberValue(entity.getNumberValue());
            } else {
                measurement.setNumberValue(null);
            }
            measurement.setVariableName(entity.getVariableName());
            measurement.setValueDecimalCount(entity.getValueDecimalCount());
            measurement.setValueFormat(entity.getValueFormat());
            if (entity.getStringValue().length() > 250) {
                entity.setStringValue(entity.getStringValue().substring(0, 250));
            }
            measurement.setStringValue(entity.getStringValue());
            measurement.setValueType(entity.getValueType());
            measurement.setValueDefault(entity.getValueDefault());
            measurement.setTriggerScript(entity.getTriggerScript());
            measurement.setDeviationGradeObservation(entity.getDeviationGradeObservation());
            measurement.setLayoutType(entity.getLayoutType());
            measurement.setDisplayType(entity.getDisplayType());
            measurement.setTitleGroup(entity.getTitleGroup());
            measurement.setTextAlign(entity.getTextAlign());
            measurement.setMeasurementGroup(entity.getMeasurementGroup());
            measurement.setThermalIdentity(entity.getThermalIdentity());
            super.edit(measurement);
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        Measurement measurement = super.find(id);
        if (measurement != null) {
            super.remove(measurement);
        }
    }


    @GET
    @Path("loadByEquipment/{equipmentId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Measurement> loadMeasurements(@PathParam("equipmentId") String equipmentId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Measurement> resultList = (List<Measurement>) em.createNativeQuery("SELECT " +
                                "m.* FROM measurement m " +
                                "JOIN equipment_has_measurement ehm ON ehm.measurement_measurement_id = m.measurement_id " +
                                "WHERE ehm.equipment_equipment_id = ?1",
                        Measurement.class)
                .setParameter(1, equipmentId)
                .getResultList();
        em.close();
        return resultList;
    }

    @GET
    @Path("loadByQuickChoiceItem/{quickChoiceItemId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Measurement> loadByQuickChoiceItem(@PathParam("quickChoiceItemId") String quickChoiceItemId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Measurement> measurements = em.createNativeQuery("SELECT * FROM measurement m\n" +
                                "JOIN quick_choice_item_has_measurement qcihm\n" +
                                "   ON qcihm.measurement_measurement_id = m.measurement_id\n" +
                                "WHERE qcihm.quick_choice_item_quick_choice_item_id = ?1",
                        Measurement.class)
                .setParameter(1, quickChoiceItemId)
                .getResultList();
        em.close();
        return measurements;
    }

    @GET
    @Path("loadMaster")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Measurement> loadMaster() {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        String sqlScript = """
                select distinct m.* from measurement m
                join disipline_has_measurement dhm on m.measurement_id = dhm.measurement_measurement_id
                where m.deleted = 0
                
                union
                select distinct m.* from measurement m
                join equipment_type_has_measurement ethm on m.measurement_id = ethm.measurement_measurement_id
                where m.deleted = 0
                
                union
                select distinct m.* from measurement m
                join quick_choice_item_has_measurement qcihm on m.measurement_id = qcihm.measurement_measurement_id
                where m.deleted = 0
                """;
        List<Measurement> measurements = em.createNativeQuery(sqlScript,
                        Measurement.class)
                .getResultList();
        em.close();
        return measurements;
    }

//    @GET
//    @Path("loadByEquipment/{equipmentId}")
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<Measurement> loadByEquipment(@PathParam("equipmentId") String equipmentId) {
//        EntityManager em = LocalEntityManagerFactory.createEntityManager();
//        List<Measurement> measurements = em.createNativeQuery("SELECT * FROM measurement m\n" +
//                                "JOIN equipment_has_measurement ehm\n" +
//                                "   ON ehm.measurement_measurement_id = m.measurement_id\n" +
//                                "WHERE ehm.equipment_equipment_id = ?1",
//                        Measurement.class)
//                .setParameter(1, equipmentId)
//                .getResultList();
//        em.close();
//        return measurements;
//    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Measurement find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public List<Measurement> findAll() {
        return super.findAll();
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public int count() {
        return super.count();
    }
}

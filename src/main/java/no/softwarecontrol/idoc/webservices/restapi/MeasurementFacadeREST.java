package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.List;

/**
 * Created by ovesteinsland on 28/05/2017.
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.measurement")
@RolesAllowed({"ApplicationRole"})
public class MeasurementFacadeREST extends AbstractFacade<Measurement> {
    public static MeasurementFacadeREST instance;

    public MeasurementFacadeREST() {
        super(Measurement.class);
        instance = this;
    }

    public static MeasurementFacadeREST getInstance() {
        if (instance == null) {
            instance = new MeasurementFacadeREST();
        }
        return instance;
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
        if (entities == null || entities.isEmpty()) {
            return;
        }

        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();

            // Definer SQL-spørringer én gang
            String sqlMeasurement = """
                INSERT IGNORE INTO measurement (
                    measurement_id, name, unit, value, deviation_grade, sort_index, deleted, 
                    variable_name, java_script, value_type, value_object, value_decimal_count, 
                    value_format, deviation_grade_observation, trigger_script, layout_type, 
                    value_default, display_type, text_align, description, title_group, 
                    measurement_group, thermal_identity, hidden, fixed
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            String sqlLink = """
                INSERT IGNORE INTO observation_has_measurement (observation_observation_id, measurement_measurement_id) 
                VALUES (?, ?)
                """;

            String sqlLanguage = """
                INSERT IGNORE INTO measurement_language (
                    measurement_language_id, measurement, unit, name, description, 
                    language_code, title_group, measurement_group, value_default
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try {
                tx.begin();
                // Pakk ut JDBC Connection for batch-ytelse
                java.sql.Connection connection = em.unwrap(java.sql.Connection.class);

                try (java.sql.PreparedStatement psMeasurement = connection.prepareStatement(sqlMeasurement);
                     java.sql.PreparedStatement psLink = connection.prepareStatement(sqlLink);
                     java.sql.PreparedStatement psLanguage = connection.prepareStatement(sqlLanguage)) {

                    int count = 0;
                    int batchSize = 1000;

                    for (Measurement measurement : entities) {
                        // 1. Prepare Measurement
                        psMeasurement.setString(1, measurement.getMeasurementId());
                        psMeasurement.setString(2, measurement.getName());
                        psMeasurement.setString(3, measurement.getUnit());
                        setNullableDouble(psMeasurement, 4, measurement.getNumberValue());
                        setNullableInt(psMeasurement, 5, measurement.getDeviationGrade());
                        setNullableInt(psMeasurement, 6, measurement.getSortIndex());
                        psMeasurement.setBoolean(7, Boolean.TRUE.equals(measurement.getDeleted()));
                        psMeasurement.setString(8, measurement.getVariableName());
                        psMeasurement.setString(9, measurement.getJavaScript());
                        psMeasurement.setString(10, measurement.getValueType());
                        psMeasurement.setString(11, measurement.getStringValue());
                        setNullableInt(psMeasurement, 12, measurement.getValueDecimalCount());
                        psMeasurement.setString(13, measurement.getValueFormat());
                        setNullableInt(psMeasurement, 14, measurement.getDeviationGradeObservation());
                        psMeasurement.setString(15, measurement.getTriggerScript());
                        psMeasurement.setInt(16, measurement.getLayoutType());
                        psMeasurement.setString(17, measurement.getValueDefault());
                        psMeasurement.setInt(18, measurement.getDisplayType());
                        psMeasurement.setString(19, measurement.getTextAlign() != null ? measurement.getTextAlign().getValue() : null);
                        psMeasurement.setString(20, measurement.getDescription());
                        psMeasurement.setString(21, measurement.getTitleGroup());
                        psMeasurement.setString(22, measurement.getMeasurementGroup());
                        psMeasurement.setString(23, measurement.getThermalIdentity());
                        setNullableBoolean(psMeasurement, 24, measurement.getHidden());
                        setNullableBoolean(psMeasurement, 25, measurement.getFixed());
                        psMeasurement.addBatch();

                        // 2. Prepare Link
                        psLink.setString(1, observationId);
                        psLink.setString(2, measurement.getMeasurementId());
                        psLink.addBatch();

                        // 3. Prepare Languages
                        if (measurement.getMeasurementLanguageList() != null) {
                            for (MeasurementLanguage ml : measurement.getMeasurementLanguageList()) {
                                if (ml.getMeasurementLanguageId() != null) {
                                    psLanguage.setString(1, ml.getMeasurementLanguageId());
                                    psLanguage.setString(2, measurement.getMeasurementId());
                                    psLanguage.setString(3, ml.getUnit());
                                    psLanguage.setString(4, ml.getName());
                                    psLanguage.setString(5, ml.getDescription());
                                    psLanguage.setString(6, ml.getLanguageCode());
                                    psLanguage.setString(7, ml.getTitleGroup());
                                    psLanguage.setString(8, ml.getMeasurementGroup());
                                    psLanguage.setString(9, ml.getValueDefault());
                                    psLanguage.addBatch();
                                }
                            }
                        }

                        // Execute batch periodically
                        if (++count % batchSize == 0) {
                            psMeasurement.executeBatch();
                            psLink.executeBatch();
                            psLanguage.executeBatch();
                        }
                    }

                    // Execute remaining items
                    psMeasurement.executeBatch();
                    psLink.executeBatch();
                    psLanguage.executeBatch();
                }

                tx.commit();
            } catch (Exception e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Feil i batch createListWithObservationId for observationId: " + observationId + " - " + e.getMessage());
                e.printStackTrace(System.err);
                throw new RuntimeException("Failed to create measurement list with observation", e);
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for createListWithObservationId: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
        }
    }

    // Helper methods for null-safe JDBC parameters
    private void setNullableInt(java.sql.PreparedStatement ps, int index, Integer value) throws java.sql.SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, java.sql.Types.INTEGER);
        }
    }

    private void setNullableDouble(java.sql.PreparedStatement ps, int index, Double value) throws java.sql.SQLException {
        if (value != null) {
            ps.setDouble(index, value);
        } else {
            ps.setNull(index, java.sql.Types.DOUBLE);
        }
    }

    private void setNullableBoolean(java.sql.PreparedStatement ps, int index, Boolean value) throws java.sql.SQLException {
        if (value != null) {
            ps.setBoolean(index, value);
        } else {
            ps.setNull(index, java.sql.Types.BOOLEAN);
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


    @POST
    @Path("createWithObservation/{observationId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithObservation(@PathParam("observationId") String observationId, Measurement entity) {

        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            Measurement existingMeasurement = find(entity.getMeasurementId());
            if (existingMeasurement == null) {
                EntityTransaction tx = em.getTransaction();
                try {
                    tx.begin();
                    Query query = em.createNativeQuery("""
                            INSERT INTO measurement (
                                measurement_id,
                                name, 
                                unit, 
                                value, 
                                deviation_grade, 
                                sort_index,
                                deleted, 
                                variable_name, 
                                java_script, 
                                value_type, 
                                value_object, 
                                value_decimal_count, 
                                value_format, 
                                deviation_grade_observation, 
                                trigger_script, 
                                layout_type, 
                                value_default, 
                                display_type, 
                                text_align, 
                                description, 
                                title_group, 
                                measurement_group, 
                                thermal_identity, 
                                hidden, 
                                fixed
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                            """
                    );

                    query.setParameter(1, entity.getMeasurementId());
                    query.setParameter(2, entity.getName());
                    query.setParameter(3, entity.getUnit());
                    query.setParameter(4, entity.getNumberValue() != null ? entity.getNumberValue() : null);
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
                    query.setParameter(19, entity.getTextAlign() != null ? entity.getTextAlign().getValue() : null);
                    query.setParameter(20, entity.getDescription());
                    query.setParameter(21, entity.getTitleGroup());
                    query.setParameter(22, entity.getMeasurementGroup());
                    query.setParameter(23, entity.getThermalIdentity());
                    query.setParameter(24, entity.getHidden() != null ? entity.getHidden() : null);
                    query.setParameter(25, entity.getFixed() != null ? entity.getFixed() : null);
                    final int i = query.executeUpdate();

                    final int linkInsertCount = em.createNativeQuery("""
                                    INSERT INTO observation_has_measurement (observation_observation_id, measurement_measurement_id)
                                    VALUES (?, ?);
                                    """)
                            .setParameter(1, observationId)
                            .setParameter(2, entity.getMeasurementId())
                            .executeUpdate();
                    if (linkInsertCount > 0) {
                        System.out.println("Insert LINK with observation SUCCEEDED:" + linkInsertCount + " rows inserted");
                    } else {
                        System.out.println("Insert measurement: " + entity.getMeasurementId() + " LINK with observation FAILED:" + linkInsertCount + " rows inserted");
                    }

                    // 3) Verifiser umiddelbart (diagnostikk)
                    Number verify = (Number) em.createNativeQuery("""
                                    SELECT COUNT(*) FROM observation_has_measurement 
                                    WHERE observation_observation_id = ?1 AND measurement_measurement_id = ?2
                                    """)
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
                        MeasurementLanguageFacadeREST.getInstance().create(measurementLanguage);
                    }
                } catch (Exception exp) {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                    System.out.println("Exception while inserting measurement with observationId: " + observationId + " - " + exp.getMessage());
                    exp.printStackTrace(System.err);
                    throw new RuntimeException("Failed to create measurement with observation", exp);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for createWithObservation: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
        }
    }


    @POST
    @Path("createWithEquipment/{equipmentId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithEquipment(@PathParam("equipmentId") String equipmentId, Measurement entity) {
        // Check if measurement exits
        Measurement measurement = MeasurementFacadeREST.getInstance().find(entity.getMeasurementId());
        if (measurement == null) {
            super.create(entity);
        }
        //String measurementId, String equipmentId
        EquipmentFacadeREST.getInstance().linkMeasurement(entity.getMeasurementId(), equipmentId);
    }

    @POST
    @Path("createWithQuickChoiceItem/{quickChoiceItemId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithQuickChoiceItem(@PathParam("quickChoiceItemId") String quickChoiceItemId, Measurement entity) {
        super.create(entity);

        QuickChoiceItem quickChoiceItem = QuickChoiceItemFacadeREST.getInstance().find(quickChoiceItemId);
        if (!quickChoiceItem.getMeasurementList().contains(entity)) {
            quickChoiceItem.getMeasurementList().add(entity);
        }
        if (!entity.getQuickChoiceItemList().contains(quickChoiceItem)) {
            entity.getQuickChoiceItemList().add(quickChoiceItem);
        }
        QuickChoiceItemFacadeREST.getInstance().edit(quickChoiceItem);
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Measurement> resultList = (List<Measurement>) em.createNativeQuery("""
                                    SELECT m.* FROM measurement m 
                                    JOIN equipment_has_measurement ehm ON ehm.measurement_measurement_id = m.measurement_id 
                                    WHERE ehm.equipment_equipment_id = ?1
                                    """,
                            Measurement.class)
                    .setParameter(1, equipmentId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while loading measurements for equipmentId: " + equipmentId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load measurements for equipment", e);
        }
    }

    @GET
    @Path("loadByQuickChoiceItem/{quickChoiceItemId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Measurement> loadByQuickChoiceItem(@PathParam("quickChoiceItemId") String quickChoiceItemId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Measurement> measurements = em.createNativeQuery("""
                                    SELECT * FROM measurement m
                                    JOIN quick_choice_item_has_measurement qcihm
                                       ON qcihm.measurement_measurement_id = m.measurement_id
                                    WHERE qcihm.quick_choice_item_quick_choice_item_id = ?1
                                    """,
                            Measurement.class)
                    .setParameter(1, quickChoiceItemId)
                    .getResultList();
            return measurements;
        } catch (Exception e) {
            System.out.println("Exception while loading measurements for quickChoiceItemId: " + quickChoiceItemId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load measurements by quick choice item", e);
        }
    }

    @GET
    @Path("loadMaster")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Measurement> loadMaster() {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            String sqlScript = """
                    SELECT DISTINCT m.* FROM measurement m
                    JOIN disipline_has_measurement dhm ON m.measurement_id = dhm.measurement_measurement_id
                    WHERE m.deleted = 0
                    
                    UNION
                    SELECT DISTINCT m.* FROM measurement m
                    JOIN equipment_type_has_measurement ethm ON m.measurement_id = ethm.measurement_measurement_id
                    WHERE m.deleted = 0
                    
                    UNION
                    SELECT DISTINCT m.* FROM measurement m
                    JOIN quick_choice_item_has_measurement qcihm ON m.measurement_id = qcihm.measurement_measurement_id
                    WHERE m.deleted = 0
                    """;
            List<Measurement> measurements = em.createNativeQuery(sqlScript, Measurement.class)
                    .getResultList();
            return measurements;
        } catch (Exception e) {
            System.out.println("Exception while loading master measurements: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load master measurements", e);
        }
    }


    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Measurement find(@PathParam("id") String id) {
        return super.find(id);
    }

//    @GET
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<Measurement> findAll() {
//        return super.findAll();
//    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public int count() {
        return super.count();
    }
}

package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Measurement;
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
        // Check if measurement exists
        Measurement existing = find(entity.getMeasurementId());
        if (existing == null) {
            super.create(entity);
        }
    }

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
            if(entity.getNumberValue() != null) {
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
            if(entity.getTextAlign() != null) {
                query.setParameter(19, entity.getTextAlign().getValue());
            } else {
                query.setParameter(19, null);
            }
            query.setParameter(20, entity.getDescription());
            query.setParameter(21, entity.getTitleGroup());
            query.setParameter(22, entity.getMeasurementGroup());
            query.setParameter(23, entity.getThermalIdentity());
            if(entity.getHidden() != null) {
                query.setParameter(24, entity.getHidden());
            } else {
                query.setParameter(24, null);
            }
            if(entity.getFixed() != null) {
                query.setParameter(25, entity.getFixed());
            } else {
                query.setParameter(25, null);
            }

            final int i = query.executeUpdate();
            tx.commit();
            if (i > 0) {
                Measurement measurement = find(entity.getMeasurementId());
                ObservationFacadeREST observationFacadeREST = new ObservationFacadeREST();
                if (measurement != null) {
                    Observation observation = observationFacadeREST.findNative(observationId);
                    observationFacadeREST.linkMeasurement(entity.getMeasurementId(), observation);
                } else {
                    System.out.println("Exception while linking observation with measurement: NO measurement found!!!");
                    Thread.sleep(1000);
                    Observation observation = observationFacadeREST.findNative(observationId);
                    observationFacadeREST.linkMeasurement(entity.getMeasurementId(), observation);
                }
            } else {
                System.out.println("Exception while inserting into measurement: ZERO rows inserted");
            }
            //System.out.println(entity.getSortIndex() + ": Successfully created measurement: " + entity.getName() + " (" + entity.getUnit() + ")");
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into measurement: " + exp.getMessage());
        } finally {

            em.close();
        }
    }


    @POST
    @Path("createWithEquipment/{equipmentId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithEquipment(@PathParam("equipmentId") String equipmentId, Measurement entity) {
        super.create(entity);
        EquipmentFacadeREST equipmentFacadeREST = new EquipmentFacadeREST();
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

package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.webservices.exception.UnsupportedMediaException;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.answervalue")
@RolesAllowed({"ApplicationRole"})
public class AnswerValueFacadeREST extends AbstractFacade<AnswerValue> {

    private static AnswerValueFacadeREST instance;

    public AnswerValueFacadeREST() {
        super(AnswerValue.class);
        instance = this;
    }

    public static AnswerValueFacadeREST getInstance() {
        if (instance == null) {
            instance = new AnswerValueFacadeREST();
        }
        return instance;
    }

    @Override
    protected String getSelectAllQuery() {
        return null;
    }


    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Override
    public void create(AnswerValue entity) {
        super.create(entity);
    }

    @POST
    @Path("createWithEquipment/{checkListAnswerId}/{equipmentId}/{projectId}/{observationId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithEquipment(@PathParam("checkListAnswerId") String checkListAnswerId,
                                    @PathParam("equipmentId") String equipmentId,
                                    @PathParam("projectId") String projectId,
                                    @PathParam("observationId") String observationId,
                                    AnswerValue entity) throws Exception {

        try {
            create(checkListAnswerId, projectId, observationId, entity);
            Equipment equipment = EquipmentFacadeREST.getInstance().find(equipmentId);
            if (equipment != null) {
                entity.setEquipment(equipment);
                edit(entity);
            }
        } catch (Exception e) {
            throw new UnsupportedMediaException("Create answerValue: Observation not yet created - Throw ERROR");
        }
    }

    @POST
    @Path("{checkListAnswerId}/{projectId}/{observationId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("checkListAnswerId") String checkListAnswerId,
                       @PathParam("projectId") String projectId,
                       @PathParam("observationId") String observationId,
                       AnswerValue entity) throws Exception {

        if (observationId != null) {
            Observation observation = ObservationFacadeREST.getInstance().findNative(observationId);
            if (observation != null) {
                entity.setObservation(observation);
            } else {
                //System.out.println("Create answerValue: Observation not yet created...");
                throw new UnsupportedMediaException("Create answerValue: Observation not yet created - Throw ERROR");
            }
        }
        Project project = ProjectFacadeREST.getInstance().findNative(projectId);

        CheckListAnswer checkListAnswer = CheckListAnswerFacadeREST.getInstance().find(checkListAnswerId);
        if (project != null && checkListAnswer != null) {
            AnswerValue old = find(entity.getAnswerValueId());
            if (old == null) {
                entity.setProject(project);
                entity.setCheckListAnswer(checkListAnswer);
                super.create(entity);
//                checkListAnswer.getAnswerValueList().add(entity);
//                CheckListAnswerFacadeREST.getInstance().edit(checkListAnswer);
            }
        }
    }

    @POST
    @Path("{checkListAnswerId}/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("checkListAnswerId") String checkListAnswerId, @PathParam("projectId") String projectId, AnswerValue entity) throws Exception {
        // Check if answer exists
        try {
            create(checkListAnswerId, projectId, null, entity);
        } catch (Exception e) {
            throw new UnsupportedMediaException("Error creating answerValue: UNKNOWN REASON");
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        AnswerValue answerValue = find(id);
        if (answerValue != null) {
            super.remove(answerValue);
        }
    }

    @PUT
    @Path("{projectId}/{observationId}/{equipmentId}")
    public void edit(
            @PathParam("projectId") String projectId,
            @PathParam("observationId") String observationId,
            @PathParam("equipmentId") String equipmentId,
            AnswerValue entity) {

        AnswerValue existing = find(entity.getAnswerValueId());
        if(existing != null) {
            entity.setCheckListAnswer(existing.getCheckListAnswer());
        }

        Project project = ProjectFacadeREST.getInstance().findNative(projectId);
        if(project != null) {
            entity.setProject(project);
        }

        Equipment equipment = EquipmentFacadeREST.getInstance().findNative(equipmentId);
        if(equipment != null) {
            entity.setEquipment(equipment);
        }

        Observation observation = ObservationFacadeREST.getInstance().findNative(observationId);
        if(observation != null) {
            entity.setObservation(observation);
        }
        super.edit(entity);
    }

    @GET
    @Path("loadByCheckList/{checkList}/{project}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<AnswerValue> loadByCheckList(@PathParam("checkList") String checkListId, @PathParam("project") String project) {

        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<AnswerValue> resultList = (List<AnswerValue>) em.createNativeQuery("SELECT * \n"
                                    + "FROM answer_value av \n"
                                    //+ "JOIN observation obs\n"
                                    //+ "	ON obs.observation_id = av.observation\n"
                                    + "WHERE av.project = ?1",
                            AnswerValue.class)
                    .setParameter(1, project)
                    .getResultList();

            List<AnswerValue> answerValues = new ArrayList<>();
            for (AnswerValue answerValue : resultList) {
                if (answerValue.getObservation() != null) {
                    answerValue.setObservation(optimizeObservation(answerValue.getObservation()));
                }
                if (answerValue.getCheckListAnswer() != null) {
                    CheckListAnswer checkListAnswer = optimizeCheckListAnswer(answerValue.getCheckListAnswer());
                    if (checkListAnswer.getCheckListQuestion() != null) {
                        CheckListQuestion checkListQuestion = checkListAnswer.getCheckListQuestion();
                        if (checkListQuestion.getCheckListSection() != null) {
                            CheckListSection checkListSection = checkListQuestion.getCheckListSection();
                            if (checkListSection.getCheckList() != null) {
                                CheckList checkListo = checkListSection.getCheckList();
                                if (checkListo.getCheckListId().equalsIgnoreCase(checkListId)) {
                                    answerValues.add(answerValue);
                                }
                            }
                        } else {
                            System.out.println("checkListQuestion.getCheckListSection() == null: AnswerValue id = " + answerValue.getAnswerValueId());
                        }
                    } else {
                        System.out.println("checkListAnswer.getCheckListQuestion() == null: AnswerValue id = " + answerValue.getAnswerValueId());
                    }
                } else {
                    System.out.println("answerValue.getCheckListAnswer() == null: AnswerValue id = " + answerValue.getAnswerValueId());
                }
            }
            return answerValues;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private CheckListAnswer optimizeCheckListAnswer(CheckListAnswer checkListAnswer) {
        checkListAnswer.getQuickChoiceItemList().clear();
        return checkListAnswer;
    }

    private Observation optimizeObservation(Observation observation) {
        observation.getImageList().clear();
        observation.getMeasurementList().clear();
        observation.setQuickChoiceItem(null);
        observation.setEquipment(null);
        observation.setLocation(null);
        return observation;
    }
    @GET
    @Path("loadByEquipmentCheckList/{checkListId}/{projectId}/{equipmentId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<AnswerValue> loadByEquipmentCheckList(
            @PathParam("checkListId") String checkListId,
            @PathParam("projectId") String projectId,
            @PathParam("equipmentId") String equipmentId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<AnswerValue> resultList = (List<AnswerValue>) em.createNativeQuery("SELECT * \n"
                                    + "FROM answer_value av \n"
                                    //+ "JOIN observation obs\n"
                                    //+ "	ON obs.observation_id = av.observation\n"
                                    + "WHERE av.project = ?1 AND av.equipment = ?2",
                            AnswerValue.class)
                    .setParameter(1, projectId)
                    .setParameter(2, equipmentId)
                    .getResultList();
            List<AnswerValue> filtered = resultList.stream()
                    .filter(r -> r
                            .getCheckListAnswer()
                            .getCheckListQuestion()
                            .getCheckListSection()
                            .getCheckList()
                            .getCheckListId().equals(checkListId))
                    .collect(Collectors.toList());
            return filtered;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @GET
    @Path("loadByCheckListAnswer/{checkListAnswer}/{project}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<AnswerValue> loadByCheckListAnswer(@PathParam("checkListAnswer") String checklistanswer, @PathParam("project") String project) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<AnswerValue> resultList = (List<AnswerValue>) em.createNativeQuery("SELECT * \n"
                                    + "FROM answer_value av \n"
                                    + "WHERE av.check_list_answer = ?1 AND av.project = ?2",
                            AnswerValue.class)
                    .setParameter(1, checklistanswer)
                    .setParameter(2, project)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @GET
    @Path("loadByObservation/{observationId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<AnswerValue> loadByObservation(@PathParam("observationId") String observationId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<AnswerValue> resultList = (List<AnswerValue>) em.createNativeQuery("SELECT * \n"
                                    + "FROM answer_value av \n"
                                    + "WHERE av.observation = ?1",
                            AnswerValue.class)
                    .setParameter(1, observationId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
